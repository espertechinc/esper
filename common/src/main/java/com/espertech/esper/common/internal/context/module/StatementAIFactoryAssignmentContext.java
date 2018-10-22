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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.internal.context.airegistry.StatementAIResourceRegistry;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

public class StatementAIFactoryAssignmentContext implements StatementAIFactoryAssignments {
    private final StatementAIResourceRegistry registry;

    public StatementAIFactoryAssignmentContext(StatementAIResourceRegistry registry) {
        this.registry = registry;
    }

    public AggregationService getAggregationResultFuture() {
        return registry.getAgentInstanceAggregationService();
    }

    public PriorEvalStrategy[] getPriorStrategies() {
        return registry.getAgentInstancePriorEvalStrategies();
    }

    public PreviousGetterStrategy[] getPreviousStrategies() {
        return registry.getAgentInstancePreviousGetterStrategies();
    }

    public SubordTableLookupStrategy getSubqueryLookup(int subqueryNumber) {
        return registry.getAgentInstanceSubselects().get(subqueryNumber).getLookupStrategies();
    }

    public PriorEvalStrategy getSubqueryPrior(int subqueryNumber) {
        return registry.getAgentInstanceSubselects().get(subqueryNumber).getPriorEvalStrategies();
    }

    public PreviousGetterStrategy getSubqueryPrevious(int subqueryNumber) {
        return registry.getAgentInstanceSubselects().get(subqueryNumber).getPreviousGetterStrategies();
    }

    public AggregationService getSubqueryAggregation(int subqueryNumber) {
        return registry.getAgentInstanceSubselects().get(subqueryNumber).getAggregationServices();
    }

    public ExprTableEvalStrategy getTableAccess(int tableAccessNumber) {
        return registry.getAgentInstanceTableAccesses().get(tableAccessNumber);
    }

    public RowRecogPreviousStrategy getRowRecogPreviousStrategy() {
        return registry.getAgentInstanceRowRecogPreviousStrategy();
    }
}
