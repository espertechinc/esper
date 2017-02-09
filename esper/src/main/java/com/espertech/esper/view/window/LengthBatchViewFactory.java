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

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.ViewUpdatedCollection;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.view.*;

import java.util.List;

/**
 * Factory for {@link com.espertech.esper.view.window.TimeBatchView}.
 */
public class LengthBatchViewFactory implements DataWindowViewFactory, DataWindowViewWithPrevious, DataWindowBatchingViewFactory {
    /**
     * The length window size.
     */
    protected ExprEvaluator sizeEvaluator;

    private EventType eventType;

    public void setViewParameters(ViewFactoryContext viewFactoryContext, List<ExprNode> expressionParameters) throws ViewParameterException {
        sizeEvaluator = ViewFactorySupport.validateSizeSingleParam(getViewName(), viewFactoryContext, expressionParameters);
    }

    public void attach(EventType parentEventType, StatementContext statementContext, ViewFactory optionalParentFactory, List<ViewFactory> parentViewFactories) throws ViewParameterException {
        this.eventType = parentEventType;
    }

    public Object makePreviousGetter() {
        return new RelativeAccessByEventNIndexGetterImpl();
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        int size = ViewFactorySupport.evaluateSizeParam(getViewName(), sizeEvaluator, agentInstanceViewFactoryContext.getAgentInstanceContext());
        ViewUpdatedCollection viewUpdatedCollection = agentInstanceViewFactoryContext.getStatementContext().getViewServicePreviousFactory().getOptPreviousExprRelativeAccess(agentInstanceViewFactoryContext);
        if (agentInstanceViewFactoryContext.isRemoveStream()) {
            return new LengthBatchViewRStream(agentInstanceViewFactoryContext, this, size);
        } else {
            return new LengthBatchView(agentInstanceViewFactoryContext, this, size, viewUpdatedCollection);
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean canReuse(View view, AgentInstanceContext agentInstanceContext) {
        if (!(view instanceof LengthBatchView)) {
            return false;
        }

        LengthBatchView myView = (LengthBatchView) view;
        int size = ViewFactorySupport.evaluateSizeParam(getViewName(), sizeEvaluator, agentInstanceContext);
        return myView.getSize() == size && myView.isEmpty();

    }

    public String getViewName() {
        return "Length-Batch";
    }
}
