/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;

/**
 * Result set processor prototype for the simplest case: no aggregation functions used in the select clause, and no group-by.
 */
public class ResultSetProcessorSimpleFactory implements ResultSetProcessorFactory
{
    private final boolean isSelectRStream;
    private final SelectExprProcessor selectExprProcessor;
    private final ExprEvaluator optionalHavingExpr;

    /**
     * Ctor.
     * @param selectExprProcessor - for processing the select expression and generting the final output rows
     * @param optionalHavingNode - having clause expression node
     * @param isSelectRStream - true if remove stream events should be generated
     */
    public ResultSetProcessorSimpleFactory(SelectExprProcessor selectExprProcessor,
                                           ExprEvaluator optionalHavingNode,
                                           boolean isSelectRStream)
    {
        this.selectExprProcessor = selectExprProcessor;
        this.optionalHavingExpr = optionalHavingNode;
        this.isSelectRStream = isSelectRStream;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorSimple(this, selectExprProcessor, orderByProcessor, agentInstanceContext);
    }

    public EventType getResultEventType()
    {
        return selectExprProcessor.getResultEventType();
    }

    public boolean hasAggregation() {
        return false;
    }

    public boolean isSelectRStream() {
        return isSelectRStream;
    }

    public ExprEvaluator getOptionalHavingExpr() {
        return optionalHavingExpr;
    }
}
