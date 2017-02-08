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
package com.espertech.esper.epl.agg.service;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.core.EngineImportService;

/**
 * A null object implementation of the AggregationService
 * interface.
 */
public class AggregationServiceNullFactory implements AggregationServiceFactory {

    public final static AggregationServiceNullFactory AGGREGATION_SERVICE_NULL_FACTORY = new AggregationServiceNullFactory();

    private final static AggregationServiceNull AGGREGATION_SERVICE_NULL = new AggregationServiceNull();

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, EngineImportService engineImportService, boolean isSubquery, Integer subqueryNumber) {
        return AGGREGATION_SERVICE_NULL;
    }
}
