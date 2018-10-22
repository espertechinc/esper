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
package com.espertech.esper.common.internal.view.keepall;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.view.core.*;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * This view is a keep-all data window that simply keeps all events added.
 * It in addition allows to remove events efficiently for the remove-stream events received by the view.
 */
public class KeepAllView extends ViewSupport implements DataWindowView {
    protected final AgentInstanceContext agentInstanceContext;
    private final KeepAllViewFactory keepAllViewFactory;
    protected LinkedHashSet<EventBean> indexedEvents;
    protected ViewUpdatedCollection viewUpdatedCollection;

    public KeepAllView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext, KeepAllViewFactory keepAllViewFactory, ViewUpdatedCollection viewUpdatedCollection) {
        this.agentInstanceContext = agentInstanceViewFactoryContext.getAgentInstanceContext();
        this.keepAllViewFactory = keepAllViewFactory;
        indexedEvents = new LinkedHashSet<EventBean>();
        this.viewUpdatedCollection = viewUpdatedCollection;
    }

    public ViewFactory getViewFactory() {
        return keepAllViewFactory;
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
        agentInstanceContext.getAuditProvider().view(newData, oldData, agentInstanceContext, keepAllViewFactory);
        agentInstanceContext.getInstrumentationProvider().qViewProcessIRStream(keepAllViewFactory, newData, oldData);

        if (newData != null) {
            for (EventBean newEvent : newData) {
                indexedEvents.add(newEvent);
                internalHandleAdded(newEvent);
            }
        }

        if (oldData != null) {
            for (EventBean anOldData : oldData) {
                indexedEvents.remove(anOldData);
                internalHandleRemoved(anOldData);
            }
        }

        // update event buffer for access by expressions, if any
        if (viewUpdatedCollection != null) {
            viewUpdatedCollection.update(newData, oldData);
        }

        agentInstanceContext.getInstrumentationProvider().qViewIndicate(keepAllViewFactory, newData, oldData);
        child.update(newData, oldData);
        agentInstanceContext.getInstrumentationProvider().aViewIndicate();

        agentInstanceContext.getInstrumentationProvider().aViewProcessIRStream();
    }

    public final Iterator<EventBean> iterator() {
        return indexedEvents.iterator();
    }

    public void internalHandleAdded(EventBean newEvent) {
        // no action required
    }

    public void internalHandleRemoved(EventBean oldEvent) {
        // no action required
    }

    public void visitView(ViewDataVisitor viewDataVisitor) {
        viewDataVisitor.visitPrimary(indexedEvents, true, keepAllViewFactory.getViewName(), null);
    }

    public LinkedHashSet<EventBean> getIndexedEvents() {
        return indexedEvents;
    }
}
