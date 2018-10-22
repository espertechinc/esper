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
package com.espertech.esper.common.internal.view.prior;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.view.core.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.common.internal.view.core.View;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.core.ViewFactoryContext;

import java.util.SortedSet;

/**
 * Factory for making {@link PriorEventView} instances.
 */
public class PriorEventViewFactory implements ViewFactory {
    protected EventType eventType;

    /**
     * unbound to indicate the we are not receiving remove stream events (unbound stream, stream without child
     * views) therefore must use a different buffer.
     */
    protected boolean isUnbound;

    public void setUnbound(boolean unbound) {
        isUnbound = unbound;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new PriorEventView(agentInstanceViewFactoryContext.getPriorViewUpdatedCollection());
    }

    public ViewUpdatedCollection makeViewUpdatedCollection(SortedSet<Integer> priorRequests, AgentInstanceContext agentInstanceContext) {

        if (priorRequests.isEmpty()) {
            throw new IllegalStateException("No resources requested");
        }

        // Construct an array of requested prior-event indexes (such as 10th prior event, 8th prior = {10, 8})
        int[] requested = new int[priorRequests.size()];
        int count = 0;
        for (int reqIndex : priorRequests) {
            requested[count++] = reqIndex;
        }

        // For unbound streams the buffer is strictly rolling new events
        if (isUnbound) {
            return new PriorEventBufferUnbound(priorRequests.last());
        } else if (requested.length == 1) {
            // For bound streams (with views posting old and new data), and if only one prior index requested
            return new PriorEventBufferSingle(requested[0]);
        } else {
            // For bound streams (with views posting old and new data)
            // Multiple prior event indexes requested, such as "prior(2, price), prior(8, price)"
            // Sharing a single viewUpdatedCollection for multiple prior-event indexes
            return new PriorEventBufferMulti(requested);
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getViewName() {
        return "prior";
    }
}
