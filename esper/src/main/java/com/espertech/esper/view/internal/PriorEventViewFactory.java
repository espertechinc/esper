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

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.view.*;

import java.util.List;
import java.util.SortedMap;

/**
 * Factory for making {@link PriorEventView} instances.
 */
public class PriorEventViewFactory implements ViewFactory {
    private EventType eventType;

    /**
     * unbound to indicate the we are not receiving remove stream events (unbound stream, stream without child
     * views) therefore must use a different buffer.
     */
    protected boolean isUnbound;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        if (expressionParameters.size() != 1) {
            throw new ViewParameterException("View requires a single parameter indicating unbound or not");
        }
        isUnbound = (Boolean) ViewFactorySupport.validateAndEvaluate(getViewName(), viewFactoryContext.getStatementContext(), expressionParameters.get(0));
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        eventType = parentEventType;
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new PriorEventView(agentInstanceViewFactoryContext.getPriorViewUpdatedCollection());
    }

    public ViewUpdatedCollection makeViewUpdatedCollection(SortedMap<Integer, List<ExprPriorNode>> callbacksPerIndex, int agentInstanceId) {

        if (callbacksPerIndex.isEmpty()) {
            throw new IllegalStateException("No resources requested");
        }

        // Construct an array of requested prior-event indexes (such as 10th prior event, 8th prior = {10, 8})
        int[] requested = new int[callbacksPerIndex.size()];
        int count = 0;
        for (int reqIndex : callbacksPerIndex.keySet()) {
            requested[count++] = reqIndex;
        }

        // For unbound streams the buffer is strictly rolling new events
        if (isUnbound) {
            return new PriorEventBufferUnbound(callbacksPerIndex.lastKey());
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

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        return false;
    }

    public String getViewName() {
        return "Prior-Event";
    }
}
