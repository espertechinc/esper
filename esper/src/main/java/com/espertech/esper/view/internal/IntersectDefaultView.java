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
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.Iterator;
import java.util.List;

/**
 * A view that represents an intersection of multiple data windows.
 * <p>
 * The view is parameterized by two or more data windows. From an external viewpoint, the
 * view retains all events that is in all of the data windows at the same time (an intersection)
 * and removes all events that leave any of the data windows.
 */
public class IntersectDefaultView extends ViewSupport implements LastPostObserver, StoppableView, DataWindowView, IntersectViewMarker, ViewDataVisitableContainer, ViewContainer {
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final IntersectViewFactory factory;
    protected final View[] views;

    public IntersectDefaultView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, IntersectViewFactory factory, List<View> viewList) {
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.factory = factory;
        this.views = viewList.toArray(new View[viewList.size()]);

        for (int i = 0; i < viewList.size(); i++) {
            LastPostObserverView view = new LastPostObserverView(i);
            views[i].removeAllViews();
            views[i].addView(view);
            view.setObserver(this);
        }
    }

    public View[] getViewContained() {
        return views;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, factory.getViewName(), newData, oldData);
        }

        IntersectDefaultViewLocalState localState = factory.getDefaultViewLocalStatePerThread();

        if (newData != null) {
            // new events must go to all views
            // old events, such as when removing from a named window, get removed from all views
            localState.setHasRemovestreamData(false);  // changed by observer logic to indicate new data
            localState.setIsRetainObserverEvents(true);  // enable retain logic in observer
            try {
                for (View view : views) {
                    view.update(newData, oldData);
                }
            } finally {
                localState.setIsRetainObserverEvents(false);
            }

            // see if any child view has removed any events.
            // if there was an insert stream, handle pushed-out events
            if (localState.hasRemovestreamData()) {
                localState.getRemovalEvents().clear();

                // process each buffer
                for (int i = 0; i < localState.getOldEventsPerView().length; i++) {
                    if (localState.getOldEventsPerView()[i] == null) {
                        continue;
                    }

                    EventBean[] viewOldData = localState.getOldEventsPerView()[i];
                    localState.getOldEventsPerView()[i] = null;  // clear entry

                    // add each event to the set of events removed
                    for (EventBean oldEvent : viewOldData) {
                        localState.getRemovalEvents().add(oldEvent);
                    }

                    localState.setIsDiscardObserverEvents(true);
                    try {
                        for (int j = 0; j < views.length; j++) {
                            if (i != j) {
                                views[j].update(null, viewOldData);
                            }
                        }
                    } finally {
                        localState.setIsDiscardObserverEvents(false);
                    }
                }

                oldData = localState.getRemovalEvents().toArray(new EventBean[localState.getRemovalEvents().size()]);
            }

            // indicate new and, possibly, old data
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, factory.getViewName(), newData, oldData);
            }
            updateChildren(newData, oldData);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        } else if (oldData != null) {
            // handle remove stream
            localState.setIsDiscardObserverEvents(true);    // disable reaction logic in observer
            try {
                for (View view : views) {
                    view.update(null, oldData);
                }
            } finally {
                localState.setIsDiscardObserverEvents(false);
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, factory.getViewName(), null, oldData);
            }
            updateChildren(null, oldData);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public EventType getEventType() {
        return factory.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return views[0].iterator();
    }

    public void newData(int streamId, EventBean[] newEvents, EventBean[] oldEvents) {
        IntersectDefaultViewLocalState localState = factory.getDefaultViewLocalStatePerThread();

        if ((oldEvents == null) || (localState.isDiscardObserverEvents())) {
            return;
        }

        if (localState.isRetainObserverEvents()) {
            localState.getOldEventsPerView()[streamId] = oldEvents;
            localState.setHasRemovestreamData(true);
            return;
        }

        // remove old data from all other views
        localState.setIsDiscardObserverEvents(true);
        try {
            for (int i = 0; i < views.length; i++) {
                if (i != streamId) {
                    views[i].update(null, oldEvents);
                }
            }
        } finally {
            localState.setIsDiscardObserverEvents(false);
        }

        updateChildren(null, oldEvents);
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
        visitViewContained(viewDataVisitor, factory, views);
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        throw new UnsupportedOperationException();
    }

    public static void visitViewContained(ViewDataVisitorContained viewDataVisitor, ViewFactory viewFactory, View[] views) {
        viewDataVisitor.visitPrimary(viewFactory.getViewName(), views.length);
        for (int i = 0; i < views.length; i++) {
            viewDataVisitor.visitContained(i, views[i]);
        }
    }

    public ViewFactory getViewFactory() {
        return factory;
    }
}
