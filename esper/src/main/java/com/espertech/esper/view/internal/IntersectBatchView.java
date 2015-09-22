/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.view.internal;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.view.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
public class IntersectBatchView extends ViewSupport implements LastPostObserver, CloneableView, StoppableView, DataWindowView, IntersectViewMarker, ViewDataVisitableContainer
{
    private static final Log log = LogFactory.getLog(IntersectBatchView.class);

    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    protected final IntersectViewFactory intersectViewFactory;
    protected final EventType eventType;
    protected final View[] views;
    private int batchViewIndex;
    private final EventBean[][] oldEventsPerView;
    private final EventBean[][] newEventsPerView;
    private final HashSet<EventBean> removedEvents = new LinkedHashSet<EventBean>();
    protected final boolean hasAsymetric;

    private boolean captureIRNonBatch;
    private boolean ignoreViewIRStream;

    /**
     * Ctor.
     * @param factory the view factory
     * @param eventType the parent event type
     * @param viewList the list of data window views
     * @param viewFactories view factories
     */
    public IntersectBatchView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, IntersectViewFactory factory, EventType eventType, List<View> viewList, List<ViewFactory> viewFactories, boolean hasAsymetric)
    {
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.intersectViewFactory = factory;
        this.eventType = eventType;
        this.views = viewList.toArray(new View[viewList.size()]);
        this.oldEventsPerView = new EventBean[viewList.size()][];
        this.newEventsPerView = new EventBean[viewList.size()][];
        this.hasAsymetric = hasAsymetric;

        // determine index of batch view
        batchViewIndex = -1;
        for (int i = 0; i < viewFactories.size(); i++) {
            if (viewFactories.get(i) instanceof DataWindowBatchingViewFactory) {
                batchViewIndex = i;
            }
        }
        if (batchViewIndex == -1) {
            throw new IllegalStateException("Failed to find batch data window view");
        }

        for (int i = 0; i < viewList.size(); i++) {
            LastPostObserverView view = new LastPostObserverView(i);
            views[i].removeAllViews();
            views[i].addView(view);
            view.setObserver(this);
        }
    }

    public View cloneView()
    {
        return intersectViewFactory.makeView(agentInstanceViewFactoryContext);
    }

    public void update(EventBean[] newData, EventBean[] oldData)
    {
        // handle remove stream: post oldData to all views
        if (oldData != null && oldData.length != 0) {
            try {
                ignoreViewIRStream = true;
                for (int i = 0; i < views.length; i++) {
                    views[i].update(newData, oldData);
                }
            }
            finally {
                ignoreViewIRStream = false;
            }
        }

        if (newData != null) {
            // post to all non-batch views first to let them decide the remove stream, if any
            try {
                captureIRNonBatch = true;
                for (int i = 0; i < views.length; i++) {
                    if (i != batchViewIndex) {
                        views[i].update(newData, oldData);
                    }
                }
            }
            finally {
                captureIRNonBatch = false;
            }

            // if there is any data removed from non-batch views, remove from all views
            // collect removed events
            removedEvents.clear();
            for (int i = 0; i < views.length; i++) {
                if (oldEventsPerView[i] != null) {
                    for (int j = 0; j < views.length; j++) {
                        if (i == j) {
                            continue;
                        }
                        views[j].update(null, oldEventsPerView[i]);

                        for (int k = 0; k < oldEventsPerView[i].length; k++) {
                            removedEvents.add(oldEventsPerView[i][k]);
                        }
                    }
                    oldEventsPerView[i] = null;
                }
            }

            // post only new events to the batch view that have not been removed
            EventBean[] newDataNonRemoved;
            if (hasAsymetric) {
                newDataNonRemoved = EventBeanUtility.getNewDataNonRemoved(newData, removedEvents, newEventsPerView);
            }
            else {
                newDataNonRemoved = EventBeanUtility.getNewDataNonRemoved(newData, removedEvents);
            }
            if (newDataNonRemoved != null) {
                views[batchViewIndex].update(newDataNonRemoved, null);
            }
        }
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public Iterator<EventBean> iterator()
    {
        return views[batchViewIndex].iterator();
    }

    public void newData(int streamId, EventBean[] newEvents, EventBean[] oldEvents)
    {
        if (ignoreViewIRStream) {
            return;
        }

        if (captureIRNonBatch) {
            oldEventsPerView[streamId] = oldEvents;
            if (hasAsymetric) {
                newEventsPerView[streamId] = newEvents;
            }
            return;
        }

        // handle case where irstream originates from view, i.e. timer-based
        if (streamId == batchViewIndex) {
            updateChildren(newEvents, oldEvents);
            if (newEvents != null) {
                try {
                    ignoreViewIRStream = true;
                    for (int i = 0; i < views.length; i++) {
                        if (i != streamId) {
                            views[i].update(null, newEvents);
                        }
                    }
                }
                finally {
                    ignoreViewIRStream = false;
                }
            }
        }
        // post remove stream to all other views
        else {
            if (oldEvents != null) {
                try {
                    ignoreViewIRStream = true;
                    for (int i = 0; i < views.length; i++) {
                        if (i != streamId) {
                            views[i].update(null, oldEvents);
                        }
                    }
                }
                finally {
                    ignoreViewIRStream = false;
                }
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
        IntersectView.visitViewContained(viewDataVisitor, intersectViewFactory, views);
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        throw new UnsupportedOperationException();
    }

    public ViewFactory getViewFactory() {
        return intersectViewFactory;
    }
}