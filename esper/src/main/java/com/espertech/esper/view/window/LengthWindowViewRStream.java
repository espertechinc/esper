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
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * This view is a moving window extending the specified number of elements into the past,
 * allowing in addition to remove events efficiently for remove-stream events received by the view.
 */
public class LengthWindowViewRStream extends ViewSupport implements DataWindowView {
    protected final AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext;
    private final LengthWindowViewFactory lengthWindowViewFactory;
    private final int size;
    protected LinkedHashSet<EventBean> indexedEvents;

    /**
     * Constructor creates a moving window extending the specified number of elements into the past.
     *
     * @param size                            is the specified number of elements into the past
     * @param lengthWindowViewFactory         for copying this view in a group-by
     * @param agentInstanceViewFactoryContext context
     */
    public LengthWindowViewRStream(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, LengthWindowViewFactory lengthWindowViewFactory, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Illegal argument for size of length window");
        }

        this.agentInstanceViewFactoryContext = agentInstanceViewFactoryContext;
        this.lengthWindowViewFactory = lengthWindowViewFactory;
        this.size = size;
        indexedEvents = new LinkedHashSet<EventBean>();
    }

    /**
     * Returns true if the window is empty, or false if not empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return indexedEvents.isEmpty();
    }

    /**
     * Returns the size of the length window.
     *
     * @return size of length window
     */
    public final int getSize() {
        return size;
    }

    public final EventType getEventType() {
        // The event type is the parent view's event type
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, lengthWindowViewFactory.getViewName(), newData, oldData);
        }

        EventBean[] expiredArr = null;
        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                indexedEvents.remove(anOldData);
                internalHandleRemoved(anOldData);
            }
            expiredArr = oldData;
        }

        // add data points to the window
        // we don't care about removed data from a prior view
        if (newData != null) {
            for (EventBean newEvent : newData) {
                indexedEvents.add(newEvent);
                internalHandleAdded(newEvent);
            }
        }

        // Check for any events that get pushed out of the window
        int expiredCount = indexedEvents.size() - size;
        if (expiredCount > 0) {
            expiredArr = new EventBean[expiredCount];
            Iterator<EventBean> it = indexedEvents.iterator();
            for (int i = 0; i < expiredCount; i++) {
                expiredArr[i] = it.next();
            }
            for (EventBean anExpired : expiredArr) {
                indexedEvents.remove(anExpired);
                internalHandleExpired(anExpired);
            }
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

    public void internalHandleExpired(EventBean oldData) {
        // no action required
    }

    public void internalHandleRemoved(EventBean expiredData) {
        // no action required
    }

    public void internalHandleAdded(EventBean newData) {
        // no action required
    }

    public final Iterator<EventBean> iterator() {
        return indexedEvents.iterator();
    }

    public final String toString() {
        return this.getClass().getName() + " size=" + size;
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(indexedEvents, true, lengthWindowViewFactory.getViewName(), null);
    }

    public ViewFactory getViewFactory() {
        return lengthWindowViewFactory;
    }
}
