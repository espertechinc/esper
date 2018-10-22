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
package com.espertech.esper.common.internal.view.union;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.collection.RefCountedSet;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.view.core.*;
import com.espertech.esper.common.internal.view.intersect.IntersectDefaultView;
import com.espertech.esper.common.internal.view.intersect.LastPostObserver;
import com.espertech.esper.common.internal.view.intersect.LastPostObserverView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A view that represents a union of multiple data windows wherein at least one is asymetric:
 * it does not present a insert stream for each insert stream event received.
 * <p>
 * The view is parameterized by two or more data windows. From an external viewpoint, the
 * view retains all events that is in any of the data windows (a union).
 */
public class UnionAsymetricView extends ViewSupport implements LastPostObserver, AgentInstanceStopCallback, DataWindowView, ViewDataVisitableContainer {
    private static final Logger log = LoggerFactory.getLogger(UnionAsymetricView.class);

    protected final AgentInstanceContext agentInstanceContext;
    private final UnionViewFactory unionViewFactory;
    protected final View[] views;
    private final EventBean[][] oldEventsPerView;
    protected final RefCountedSet<EventBean> unionWindow;
    private final List<EventBean> removalEvents = new ArrayList<EventBean>();
    private final ArrayDeque<EventBean> newEvents = new ArrayDeque<EventBean>();

    private EventBean[] newDataChildView;
    private boolean isHasRemovestreamData;
    private boolean isRetainObserverEvents;
    private boolean isDiscardObserverEvents;

    public UnionAsymetricView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, UnionViewFactory factory, List<View> viewList) {
        this.agentInstanceContext = agentInstanceViewFactoryContext.getAgentInstanceContext();
        this.unionViewFactory = factory;
        this.views = viewList.toArray(new View[viewList.size()]);
        this.unionWindow = new RefCountedSet<EventBean>();
        oldEventsPerView = new EventBean[viewList.size()][];

        for (int i = 0; i < viewList.size(); i++) {
            LastPostObserverView view = new LastPostObserverView(i);
            views[i].setChild(view);
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

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, unionViewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(unionViewFactory, newData, oldData);

        // handle remove stream
        OneEventCollection oldDataColl = null;
        EventBean[] newDataPosted = null;
        if (oldData != null) {
            isDiscardObserverEvents = true;    // disable reaction logic in observer

            try {
                for (View view : views) {
                    agentInstanceContext.getInstrumentationProvider().qViewIndicate(unionViewFactory, null, oldData);
                    view.update(null, oldData);
                    agentInstanceContext.getInstrumentationProvider().aViewIndicate();
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
            boolean[][] removedByView = new boolean[newData.length][views.length];

            for (EventBean newEvent : newData) {
                unionWindow.add(newEvent, views.length);
            }

            // new events must go to all views
            // old events, such as when removing from a named window, get removed from all views
            isHasRemovestreamData = false;  // changed by observer logic to indicate new data
            isRetainObserverEvents = true;  // enable retain logic in observer
            try {
                for (int viewIndex = 0; viewIndex < views.length; viewIndex++) {
                    View view = views[viewIndex];
                    agentInstanceContext.getInstrumentationProvider().qViewIndicate(unionViewFactory, newData, null);
                    view.update(newData, null);
                    agentInstanceContext.getInstrumentationProvider().aViewIndicate();

                    // first-X asymetric view post no insert stream for events that get dropped, remove these
                    if (newDataChildView != null) {
                        for (int i = 0; i < newData.length; i++) {
                            boolean found = false;
                            for (int j = 0; j < newDataChildView.length; j++) {
                                if (newDataChildView[i] == newData[i]) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                removedByView[i][viewIndex] = true;
                            }
                        }
                    } else {
                        for (int i = 0; i < newData.length; i++) {
                            removedByView[i][viewIndex] = true;
                        }
                    }
                }
            } finally {
                isRetainObserverEvents = false;
            }

            // determine removed events, those that have a "true" in the remove by view index for all views
            removalEvents.clear();
            for (int i = 0; i < newData.length; i++) {
                boolean allTrue = true;
                for (int j = 0; j < views.length; j++) {
                    if (!removedByView[i][j]) {
                        allTrue = false;
                        break;
                    }
                }
                if (allTrue) {
                    removalEvents.add(newData[i]);
                    unionWindow.removeAll(newData[i]);
                }
            }

            // remove if any
            if (!removalEvents.isEmpty()) {
                isDiscardObserverEvents = true;
                EventBean[] viewOldData = removalEvents.toArray(new EventBean[removalEvents.size()]);
                try {
                    for (int j = 0; j < views.length; j++) {
                        agentInstanceContext.getInstrumentationProvider().qViewIndicate(unionViewFactory, null, viewOldData);
                        views[j].update(null, viewOldData);
                        agentInstanceContext.getInstrumentationProvider().aViewIndicate();
                    }
                } finally {
                    isDiscardObserverEvents = false;
                }
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

            newEvents.clear();
            for (int i = 0; i < newData.length; i++) {
                if (!removalEvents.contains(newData[i])) {
                    newEvents.add(newData[i]);
                }
            }

            if (!newEvents.isEmpty()) {
                newDataPosted = newEvents.toArray(new EventBean[newEvents.size()]);
            }
        }

        // indicate new and, possibly, old data
        if (child != null && ((newDataPosted != null) || (oldDataColl != null))) {
            EventBean[] oldDataToPost = oldDataColl != null ? oldDataColl.toArray() : null;
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(unionViewFactory, newDataPosted, oldDataToPost);
            child.update(newDataPosted, oldDataToPost);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public EventType getEventType() {
        return unionViewFactory.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return unionWindow.keyIterator();
    }

    public void newData(int streamId, EventBean[] newEvents, EventBean[] oldEvents) {
        newDataChildView = newEvents;

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
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(unionViewFactory, null, removed);
            child.update(null, removed);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }
    }


    public void stop(AgentInstanceStopServices services) {
        for (View view : views) {
            if (view instanceof AgentInstanceStopCallback) {
                ((AgentInstanceStopCallback) view).stop(services);
            }
        }
    }

    public void visitViewContainer(ViewDataVisitorContained viewDataVisitor) {
        IntersectDefaultView.visitViewContained(viewDataVisitor, unionViewFactory, views);
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        throw new UnsupportedOperationException("Must visit container");
    }

    public UnionViewFactory getViewFactory() {
        return unionViewFactory;
    }
}
