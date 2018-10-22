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
package com.espertech.esper.common.internal.view.intersect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopServices;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Iterator;
import java.util.List;

/**
 * A view that represents an intersection of multiple data windows.
 * <p>
 * The view is parameterized by two or more data windows. From an external viewpoint, the
 * view retains all events that is in all of the data windows at the same time (an intersection)
 * and removes all events that leave any of the data windows.
 * <p>
 * This special batch-version has the following logic:
 * - only one batching view allowed as sub-view
 * - all externally-received newData events are inserted into each view
 * - all externally-received oldData events are removed from each view
 * - any non-batch view has its newData output ignored
 * - the single batch-view has its newData posted to child views, and removed from all non-batch views
 * - all oldData events received from all non-batch views are removed from each view
 */
public class IntersectBatchView extends ViewSupport implements LastPostObserver, AgentInstanceStopCallback, DataWindowView, IntersectViewMarker, ViewDataVisitableContainer {
    protected final AgentInstanceContext agentInstanceContext;
    protected final IntersectViewFactory factory;
    protected final View[] views;

    public IntersectBatchView(AgentInstanceViewFactoryChainContext agentInstanceContext, IntersectViewFactory factory, List<View> viewList) {
        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
        this.factory = factory;
        this.views = viewList.toArray(new View[viewList.size()]);

        for (int i = 0; i < viewList.size(); i++) {
            LastPostObserverView view = new LastPostObserverView(i);
            views[i].setChild(view);
            view.setObserver(this);
        }
    }

    public View[] getViewContained() {
        return views;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, factory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(factory, newData, oldData);

        IntersectBatchViewLocalState localState = factory.getBatchViewLocalStatePerThread();

        // handle remove stream: post oldData to all views
        if (oldData != null && oldData.length != 0) {
            try {
                localState.setIgnoreViewIRStream(true);
                for (int i = 0; i < views.length; i++) {
                    View view = views[i];
                    agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newData, oldData);
                    view.update(newData, oldData);
                    agentInstanceContext.getInstrumentationProvider().aViewIndicate();
                }
            } finally {
                localState.setIgnoreViewIRStream(false);
            }
        }

        if (newData != null) {
            // post to all non-batch views first to let them decide the remove stream, if any
            try {
                localState.setCaptureIRNonBatch(true);
                for (int i = 0; i < views.length; i++) {
                    if (i != factory.getBatchViewIndex()) {
                        View view = views[i];
                        agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newData, oldData);
                        view.update(newData, oldData);
                        agentInstanceContext.getInstrumentationProvider().aViewIndicate();
                    }
                }
            } finally {
                localState.setCaptureIRNonBatch(false);
            }

            // if there is any data removed from non-batch views, remove from all views
            // collect removed events
            localState.getRemovedEvents().clear();
            for (int i = 0; i < views.length; i++) {
                if (localState.getOldEventsPerView()[i] != null) {
                    for (int j = 0; j < views.length; j++) {
                        if (i == j) {
                            continue;
                        }
                        View view = views[j];
                        EventBean[] oldEvents = localState.getOldEventsPerView()[i];

                        agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, null, oldEvents);
                        view.update(null, oldEvents);
                        agentInstanceContext.getInstrumentationProvider().aViewIndicate();

                        for (int k = 0; k < localState.getOldEventsPerView()[i].length; k++) {
                            localState.getRemovedEvents().add(localState.getOldEventsPerView()[i][k]);
                        }
                    }
                    localState.getOldEventsPerView()[i] = null;
                }
            }

            // post only new events to the batch view that have not been removed
            EventBean[] newDataNonRemoved;
            if (factory.isHasAsymetric()) {
                newDataNonRemoved = EventBeanUtility.getNewDataNonRemoved(newData, localState.getRemovedEvents(), localState.getNewEventsPerView());
            } else {
                newDataNonRemoved = EventBeanUtility.getNewDataNonRemoved(newData, localState.getRemovedEvents());
            }
            if (newDataNonRemoved != null) {
                View view = views[factory.getBatchViewIndex()];
                agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newDataNonRemoved, null);
                view.update(newDataNonRemoved, null);
                agentInstanceContext.getInstrumentationProvider().aViewIndicate();
            }
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public EventType getEventType() {
        return factory.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return views[factory.getBatchViewIndex()].iterator();
    }

    public void newData(int streamId, EventBean[] newEvents, EventBean[] oldEvents) {
        IntersectBatchViewLocalState localState = factory.getBatchViewLocalStatePerThread();

        if (localState.isIgnoreViewIRStream()) {
            return;
        }

        if (localState.isCaptureIRNonBatch()) {
            localState.getOldEventsPerView()[streamId] = oldEvents;
            if (factory.isHasAsymetric()) {
                localState.getNewEventsPerView()[streamId] = newEvents;
            }
            return;
        }

        // handle case where irstream originates from view, i.e. timer-based
        if (streamId == factory.getBatchViewIndex()) {
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, newEvents, oldEvents);
            child.update(newEvents, oldEvents);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();

            if (newEvents != null) {
                try {
                    localState.setIgnoreViewIRStream(true);
                    for (int i = 0; i < views.length; i++) {
                        if (i != streamId) {
                            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, null, newEvents);
                            views[i].update(null, newEvents);
                            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
                        }
                    }
                } finally {
                    localState.setIgnoreViewIRStream(false);
                }
            }
        } else {
            // post remove stream to all other views
            if (oldEvents != null) {
                try {
                    localState.setIgnoreViewIRStream(true);
                    for (int i = 0; i < views.length; i++) {
                        if (i != streamId) {
                            agentInstanceContext.getInstrumentationProvider().qViewIndicate(factory, null, oldEvents);
                            views[i].update(null, oldEvents);
                            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
                        }
                    }
                } finally {
                    localState.setIgnoreViewIRStream(false);
                }
            }
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
        IntersectDefaultView.visitViewContained(viewDataVisitor, factory, views);
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        throw new UnsupportedOperationException("Must visit container");
    }

    public ViewFactory getViewFactory() {
        return factory;
    }
}