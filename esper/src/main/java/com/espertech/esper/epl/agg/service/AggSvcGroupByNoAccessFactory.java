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
import com.espertech.esper.epl.expression.core.ExprEvaluator;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByNoAccessFactory extends AggregationServiceFactoryBase {
    /**
     * Ctor.
     *
     * @param evaluators - evaluate the sub-expression within the aggregate function (ie. sum(4*myNum))
     * @param prototypes - collect the aggregation state that evaluators evaluate to, act as prototypes for new aggregations
     */
    public AggSvcGroupByNoAccessFactory(ExprEvaluator[] evaluators, AggregationMethodFactory[] prototypes) {
        super(evaluators, prototypes);
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, EngineImportService engineImportService, boolean isSubquery, Integer subqueryNumber) {
        return new AggSvcGroupByNoAccessImpl(evaluators, aggregators);
    }
}
