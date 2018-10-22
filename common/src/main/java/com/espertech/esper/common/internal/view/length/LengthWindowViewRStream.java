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
package com.espertech.esper.common.internal.view.length;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * This view is a moving window extending the specified number of elements into the past,
 * allowing in addition to remove events efficiently for remove-stream events received by the view.
 */
public class LengthWindowViewRStream extends ViewSupport implements DataWindowView {
    private final AgentInstanceContext agentInstanceContext;
    private final LengthWindowViewFactory lengthWindowViewFactory;
    private final int size;
    private LinkedHashSet<EventBean> indexedEvents;

    /**
     * Constructor creates a moving window extending the specified number of elements into the past.
     *
     * @param size                    is the specified number of elements into the past
     * @param lengthWindowViewFactory for copying this view in a group-by
     * @param agentInstanceContext    context
     */
    public LengthWindowViewRStream(AgentInstanceViewFactoryChainContext agentInstanceContext, LengthWindowViewFactory lengthWindowViewFactory, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Illegal argument for size of length window");
        }

        this.agentInstanceContext = agentInstanceContext.getAgentInstanceContext();
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
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, lengthWindowViewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(lengthWindowViewFactory, newData, oldData);

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
        if (child != null) {
            agentInstanceContext.getInstrumentationProvider().qViewIndicate(lengthWindowViewFactory, newData, expiredArr);
            child.update(newData, expiredArr);
            agentInstanceContext.getInstrumentationProvider().aViewIndicate();
        }

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
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
