/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.view.internal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.collection.RefCountedSet;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A view that represents a union of multiple data windows.
 * <p>
 * The view is parameterized by two or more data windows. From an external viewpoint, the
 * view retains all events that is in any of the data windows (a union).
 */
public class UnionView extends ViewSupport implements LastPostObserver, StoppableView, DataWindowView, ViewDataVisitableContainer, ViewContainer {
    private static final Logger log = LoggerFactory.getLogger(UnionView.class);

    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final UnionViewFactory unionViewFactory;
    private final EventType eventType;
    protected final View[] views;
    private final EventBean[][] oldEventsPerView;
    protected final RefCountedSet<EventBean> unionWindow;
    private final List<EventBean> removalEvents = new ArrayList<EventBean>();

    private boolean isHasRemovestreamData;
    private boolean isRetainObserverEvents;
    private boolean isDiscardObserverEvents;

    public UnionView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, UnionViewFactory factory, EventType eventType, List<View> viewList) {
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.unionViewFactory = factory;
        this.eventType = eventType;
        this.views = viewList.toArray(new View[viewList.size()]);
        this.unionWindow = new RefCountedSet<EventBean>();
        oldEventsPerView = new EventBean[viewList.size()][];

        for (int i = 0; i < viewList.size(); i++) {
            LastPostObserverView view = new LastPostObserverView(i);
            views[i].removeAllViews();
            views[i].addView(view);
            view.setObserver(this);
        }

        // recover
        for (int i = 0; i < views.length; i++) {
            Iterator<EventBean> viewSnapshot = views[i].iterator();
            for (; viewSnapshot.hasNext(); ) {
                EventBean theEvent = viewSnapshot.next();
                unionWindow.add(theEvent);
            }
        }
    }

    public View[] getViewContained() {
        return this.views;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, unionViewFactory.getViewName(), newData, oldData);
        }

        OneEventCollection oldDataColl = null;
        if (oldData != null) {
            isDiscardObserverEvents = true;    // disable reaction logic in observer

            try {
                for (View view : views) {
                    view.update(null, oldData);
                }
            } finally {
                isDiscardObserverEvents = false;
            }

            // remove from union
            for (EventBean oldEvent : oldData) {
                unionWindow.removeAll(oldEvent);
            }

            oldDataColl = new OneEventCollection();
            oldDataColl.add(oldData);
        }

        // add new event to union
        if (newData != null) {
            for (EventBean newEvent : newData) {
                unionWindow.add(newEvent, views.length);
            }

            // new events must go to all views
            // old events, such as when removing from a named window, get removed from all views
            isHasRemovestreamData = false;  // changed by observer logic to indicate new data
            isRetainObserverEvents = true;  // enable retain logic in observer
            try {
                for (View view : views) {
                    view.update(newData, null);
                }
            } finally {
                isRetainObserverEvents = false;
            }

            // see if any child view has removed any events.
            // if there was an insert stream, handle pushed-out events
            if (isHasRemovestreamData) {
                List<EventBean> removedEvents = null;

                // process each buffer
                for (int i = 0; i < oldEventsPerView.length; i++) {
                    if (oldEventsPerView[i] == null) {
                        continue;
                    }

                    EventBean[] viewOldData = oldEventsPerView[i];
                    oldEventsPerView[i] = null;  // clear entry

                    // remove events for union, if the last event was removed then add it
                    for (EventBean old : viewOldData) {
                        boolean isNoMoreRef = unionWindow.remove(old);
                        if (isNoMoreRef) {
                            if (removedEvents == null) {
                                removalEvents.clear();
                                removedEvents = removalEvents;
                            }
                            removedEvents.add(old);
                        }
                    }
                }

                if (removedEvents != null) {
                    if (oldDataColl == null) {
                        oldDataColl = new OneEventCollection();
                    }
                    for (EventBean oldItem : removedEvents) {
                        oldDataColl.add(oldItem);
                    }
                }
            }

        }

        if (this.hasViews()) {
            // indicate new and, possibly, old data
            EventBean[] oldEvents = oldDataColl != null ? oldDataColl.toArray() : null;
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, unionViewFactory.getViewName(), newData, oldEvents);
            }
            updateChildren(newData, oldEvents);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public Iterator<EventBean> iterator() {
        return unionWindow.keyIterator();
    }

    public void newData(int streamId, EventBean[] newEvents, EventBean[] oldEvents) {
        if ((oldEvents == null) || isDiscardObserverEvents) {
            return;
        }

        if (isRetainObserverEvents) {
            oldEventsPerView[streamId] = oldEvents;
            isHasRemovestreamData = true;
            return;
        }

        // handle time-based removal
        List<EventBean> removedEvents = null;

        // remove events for union, if the last event was removed then add it
        for (EventBean old : oldEvents) {
            boolean isNoMoreRef = unionWindow.remove(old);
            if (isNoMoreRef) {
                if (removedEvents == null) {
                    removalEvents.clear();
                    removedEvents = removalEvents;
                }
                removedEvents.add(old);
            }
        }

        if (removedEvents != null) {
            EventBean[] removed = removedEvents.toArray(new EventBean[removedEvents.size()]);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, unionViewFactory.getViewName(), null, removed);
            }
            updateChildren(null, removed);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }
    }

    @Override
    public void stop() {
        for (View view : views) {
            if (view instanceof StoppableView) {
                ((StoppableView) view).stop();
            }
        }
    }

    public void visitViewContainer(ViewDataVisitorContained viewDataVisitor) {
        IntersectDefaultView.visitViewContained(viewDataVisitor, unionViewFactory, views);
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        throw new UnsupportedOperationException();
    }

    public ViewFactory getViewFactory() {
        return unionViewFactory;
    }
}
