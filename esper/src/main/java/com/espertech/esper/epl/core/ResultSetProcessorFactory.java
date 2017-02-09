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
 * Processor prototype for result sets for instances that apply the select-clause, group-by-clause and having-clauses
 * as supplied.
 */
public interface ResultSetProcessorFactory {
    /**
     * Returns the event type of processed results.
     *
     * @return event type of the resulting events posted by the processor.
     */
    public EventType getResultEventType();

    public boolean hasAggregation();

    public ResultSetProcessor instantiate(OrderByProcessor orderByProcessor, AggregationService aggregationService, AgentInstanceContext agentInstanceContext);

    /**
     * Returns the type of result set processor.
     *
     * @return result set processor type
     */
    public ResultSetProcessorType getResultSetProcessorType();
}
