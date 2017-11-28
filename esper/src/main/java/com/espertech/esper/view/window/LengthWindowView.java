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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * This view is a moving window extending the specified number of elements into the past.
 */
public class LengthWindowView extends ViewSupport implements DataWindowView {
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final LengthWindowViewFactory lengthWindowViewFactory;
    private final int size;
    private final ViewUpdatedCollection viewUpdatedCollection;
    protected final ArrayDeque<EventBean> events = new ArrayDeque<EventBean>();

    /**
     * Constructor creates a moving window extending the specified number of elements into the past.
     *
     * @param size                            is the specified number of elements into the past
     * @param viewUpdatedCollection           is a collection that the view must update when receiving events
     * @param lengthWindowViewFactory         for copying this view in a group-by
     * @param agentInstanceViewFactoryContext context
     */
    public LengthWindowView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, LengthWindowViewFactory lengthWindowViewFactory, int size, ViewUpdatedCollection viewUpdatedCollection) {
        if (size < 1) {
            throw new IllegalArgumentException("Illegal argument for size of length window");
        }

        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.lengthWindowViewFactory = lengthWindowViewFactory;
        this.size = size;
        this.viewUpdatedCollection = viewUpdatedCollection;
    }

    /**
     * Returns true if the window is empty, or false if not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * Returns the size of the length window.
     *
     * @return size of length window
     */
    public final int getSize() {
        return size;
    }

    /**
     * Returns the (optional) collection handling random access to window contents for prior or previous events.
     *
     * @return buffer for events
     */
    public ViewUpdatedCollection getViewUpdatedCollection() {
        return viewUpdatedCollection;
    }

    public final EventType getEventType() {
        // The event type is the parent view's event type
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, lengthWindowViewFactory.getViewName(), newData, oldData);
        }

        // add data points to the window
        // we don't care about removed data from a prior view
        if (newData != null) {
            for (EventBean event : newData) {
                events.add(event);
            }
        }

        // Check for any events that get pushed out of the window
        int expiredCount = events.size() - size;
        EventBean[] expiredArr = null;
        if (expiredCount > 0) {
            expiredArr = new EventBean[expiredCount];
            for (int i = 0; i < expiredCount; i++) {
                expiredArr[i] = events.removeFirst();
            }
        }

        // update event buffer for access by expressions, if any
        if (viewUpdatedCollection != null) {
            viewUpdatedCollection.update(newData, expiredArr);
        }

        // If there are child views, call update method
        if (this.hasViews()) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qViewIndicate(this, lengthWindowViewFactory.getViewName(), newData, expiredArr);
            }
            updateChildren(newData, expiredArr);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aViewIndicate();
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public final Iterator<EventBean> iterator() {
        return events.iterator();
    }

    public final String toString() {
        return this.getClass().getName() + " size=" + size;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(events, true, lengthWindowViewFactory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return lengthWindowViewFactory;
    }
}
