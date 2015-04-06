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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A view that represents an intersection of multiple data windows.
 * <p>
 * The view is parameterized by two or more data windows. From an external viewpoint, the
 * view retains all events that is in all of the data windows at the same time (an intersection)
 * and removes all events that leave any of the data windows.
 */
public class IntersectView extends ViewSupport implements LastPostObserver, CloneableView, StoppableView, DataWindowView, IntersectViewMarker, ViewDataVisitableContainer
{
    private static final Log log = LogFactory.getLog(IntersectView.class);

    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final IntersectViewFactory intersectViewFactory;
    private final EventType eventType;
    protected final View[] views;
    private final EventBean[][] oldEventsPerView;
    private final Set<EventBean> removalEvents = new HashSet<EventBean>();

    private boolean isHasRemovestreamData;
    private boolean isRetainObserverEvents;
    private boolean isDiscardObserverEvents;

    /**
     * Ctor.
     * @param factory the view factory
     * @param eventType the parent event type
     * @param viewList the list of data window views
     */
    public IntersectView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, IntersectViewFactory factory, EventType eventType, List<View> viewList)
    {
        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.intersectViewFactory = factory;
        this.eventType = eventType;
        this.views = viewList.toArray(new View[viewList.size()]);
        oldEventsPerView = new EventBean[viewList.size()][];

        for (int i = 0; i < viewList.size(); i++)
        {
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
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qViewProcessIRStream(this, intersectViewFactory.getViewName(), newData, oldData);}

        if (newData != null)
        {
            // new events must go to all views
            // old events, such as when removing from a named window, get removed from all views
            isHasRemovestreamData = false;  // changed by observer logic to indicate new data
            isRetainObserverEvents = true;  // enable retain logic in observer
            try
            {
                for (View view : views)
                {
                    view.update(newData, oldData);
                }
            }
            finally
            {
                isRetainObserverEvents = false;
            }

            // see if any child view has removed any events.
            // if there was an insert stream, handle pushed-out events
            if (isHasRemovestreamData)
            {
                removalEvents.clear();

                // process each buffer
                for (int i = 0; i < oldEventsPerView.length; i++)
                {
                    if (oldEventsPerView[i] == null)
                    {
                        continue;
                    }

                    EventBean[] viewOldData = oldEventsPerView[i];
                    oldEventsPerView[i] = null;  // clear entry

                    // add each event to the set of events removed
                    for (EventBean oldEvent : viewOldData) {
                        removalEvents.add(oldEvent);
                    }

                    isDiscardObserverEvents = true;
                    try
                    {
                        for (int j = 0; j < views.length; j++)
                        {
                            if (i != j)
                            {
                                views[j].update(null, viewOldData);
                            }
                        }
                    }
                    finally
                    {
                        isDiscardObserverEvents = false;
                    }
                }

                oldData = removalEvents.toArray(new EventBean[removalEvents.size()]);
            }

            // indicate new and, possibly, old data
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qViewIndicate(this, intersectViewFactory.getViewName(), newData, oldData);}
            updateChildren(newData, oldData);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aViewIndicate();}
        }

        // handle remove stream
        else if (oldData != null)
        {
            isDiscardObserverEvents = true;    // disable reaction logic in observer
            try
            {
                for (View view : views)
                {
                    view.update(null, oldData);
                }
            }
            finally
            {
                isDiscardObserverEvents = false;
            }

            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().qViewIndicate(this, intersectViewFactory.getViewName(), null, oldData);}
            updateChildren(null, oldData);
            if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aViewIndicate();}
        }

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.get().aViewProcessIRStream();}
    }

    public EventType getEventType()
    {
        return eventType;
    }

    public Iterator<EventBean> iterator()
    {
        return views[0].iterator();
    }

    public void newData(int streamId, EventBean[] newEvents, EventBean[] oldEvents)
    {
        if ((oldEvents == null) || (isDiscardObserverEvents))
        {
            return;
        }

        if (isRetainObserverEvents)
        {
            oldEventsPerView[streamId] = oldEvents;
            isHasRemovestreamData = true;
            return;
        }

        // remove old data from all other views
        isDiscardObserverEvents = true;
        try
        {
            for (int i = 0; i < views.length; i++)
            {
                if (i != streamId)
                {
                    views[i].update(null, oldEvents);
                }
            }
        }
        finally
        {
            isDiscardObserverEvents = false;
        }

        updateChildren(null, oldEvents);
    }

    @Override
    public void stopView() {
        for (View view : views) {
            if (view instanceof StoppableView) {
                ((StoppableView) view).stopView();
            }
        }
    }

    public void visitViewContainer(ViewDataVisitorContained viewDataVisitor) {
        visitViewContained(viewDataVisitor, intersectViewFactory, views);
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
        return intersectViewFactory;
    }
}
