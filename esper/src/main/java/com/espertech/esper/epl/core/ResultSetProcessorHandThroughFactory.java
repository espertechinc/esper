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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.AggregationService;

/**
 * Result set processor prototye for the hand-through case:
 * no aggregation functions used in the select clause, and no group-by, no having and ordering.
 */
public class ResultSetProcessorHandThroughFactory implements ResultSetProcessorFactory {
    private final EventType resultEventType;
    private final SelectExprProcessor selectExprProcessor;
    private final boolean isSelectRStream;

    public ResultSetProcessorHandThroughFactory(EventType resultEventType, SelectExprProcessor selectExprProcessor, boolean selectRStream) {
        this.resultEventType = resultEventType;
        this.selectExprProcessor = selectExprProcessor;
        isSelectRStream = selectRStream;
    }

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext) {
        return new ResultSetProcessorHandThrough(this, selectExprProcessor, agentInstanceContext);
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public boolean hasAggregation() {
        return false;
    }

    public boolean isSelectRStream() {
        return isSelectRStream;
    }

    public ResultSetProcessorType getResultSetProcessorType() {
        return ResultSetProcessorType.HANDTHROUGH;
    }
}
