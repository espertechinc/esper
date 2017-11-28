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
public class IntersectAsymetricView extends ViewSupport implements LastPostObserver, StoppableView, DataWindowView, IntersectViewMarker, ViewDataVisitableContainer, ViewContainer {
    private final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final IntersectViewFactory factory;
    protected final View[] views;

    public IntersectAsymetricView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, IntersectViewFactory factory, List<View> viewList) {
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
        IntersectAsymetricViewLocalState localState = factory.getAsymetricViewLocalStatePerThread();

        localState.getOldEvents().clear();
        EventBean[] newDataPosted = null;

        // handle remove stream
        if (oldData != null) {
            localState.setIsDiscardObserverEvents(true);    // disable reaction logic in observer
            try {
                for (View view : views) {
                    view.update(null, oldData);
                }
            } finally {
                localState.setIsDiscardObserverEvents(false);
            }

            for (int i = 0; i < oldData.length; i++) {
                localState.getOldEvents().add(oldData[i]);
            }
        }

        if (newData != null) {
            localState.getRemovalEvents().clear();

            // new events must go to all views
            // old events, such as when removing from a named window, get removed from all views
            localState.setHasRemovestreamData(false);  // changed by observer logic to indicate new data
            localState.setIsRetainObserverEvents(true);  // enable retain logic in observer
            try {
                for (View view : views) {
                    localState.setNewDataChildView(null);
                    view.update(newData, oldData);

                    // first-X asymetric view post no insert stream for events that get dropped, remove these
                    if (localState.getNewDataChildView() != null) {
                        for (int i = 0; i < newData.length; i++) {
                            boolean found = false;
                            for (int j = 0; j < localState.getNewDataChildView().length; j++) {
                                if (localState.getNewDataChildView()[i] == newData[i]) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                localState.getRemovalEvents().add(newData[i]);
                            }
                        }
                    } else {
                        for (int i = 0; i < newData.length; i++) {
                            localState.getRemovalEvents().add(newData[i]);
                        }
                    }
                }
            } finally {
                localState.setIsRetainObserverEvents(false);
            }

            if (!localState.getRemovalEvents().isEmpty()) {
                localState.setIsDiscardObserverEvents(true);
                EventBean[] viewOldData = localState.getRemovalEvents().toArray(new EventBean[localState.getRemovalEvents().size()]);
                try {
                    for (int j = 0; j < views.length; j++) {
                        views[j].update(null, viewOldData);
                    }
                } finally {
                    localState.setIsDiscardObserverEvents(false);
                }
            }

            // see if any child view has removed any events.
            // if there was an insert stream, handle pushed-out events
            if (localState.hasRemovestreamData()) {
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

                localState.getOldEvents().addAll(localState.getRemovalEvents());
            }

            localState.getNewEvents().clear();
            for (int i = 0; i < newData.length; i++) {
                if (!localState.getRemovalEvents().contains(newData[i])) {
                    localState.getNewEvents().add(newData[i]);
                }
            }

            if (!localState.getNewEvents().isEmpty()) {
                newDataPosted = localState.getNewEvents().toArray(new EventBean[localState.getNewEvents().size()]);
            }

        }

        // indicate new and, possibly, old data
        EventBean[] oldDataPosted = null;
        if (!localState.getOldEvents().isEmpty()) {
            oldDataPosted = localState.getOldEvents().toArray(new EventBean[localState.getOldEvents().size()]);
        }
        if ((newDataPosted != null) || (oldDataPosted != null)) {
            updateChildren(newDataPosted, oldDataPosted);
        }
        localState.getOldEvents().clear();
    }

    public EventType getEventType() {
        return factory.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return views[0].iterator();
    }

    public void newData(int streamId, EventBean[] newEvents, EventBean[] oldEvents) {
        IntersectAsymetricViewLocalState localState = factory.getAsymetricViewLocalStatePerThread();
        localState.setNewDataChildView(newEvents);

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
        IntersectDefaultView.visitViewContained(viewDataVisitor, factory, views);
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        throw new UnsupportedOperationException();
    }

    public ViewFactory getViewFactory() {
        return factory;
    }
}