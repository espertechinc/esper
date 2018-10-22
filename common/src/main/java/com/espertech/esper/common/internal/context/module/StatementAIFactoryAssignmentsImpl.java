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

import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.prior.PriorEvalStrategy;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPreviousStrategy;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryResult;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

import java.util.Map;

public class StatementAIFactoryAssignmentsImpl implements StatementAIFactoryAssignments {
    private final AggregationService aggregationResultFuture;
    private final PriorEvalStrategy[] priorStrategies;
    private final PreviousGetterStrategy[] previousStrategies;
    private final Map<Integer, SubSelectFactoryResult> subselects;
    private final Map<Integer, ExprTableEvalStrategy> tableAccesses;
    private final RowRecogPreviousStrategy rowRecogPreviousStrategy;

    public StatementAIFactoryAssignmentsImpl(AggregationService aggregationResultFuture, PriorEvalStrategy[] priorStrategies, PreviousGetterStrategy[] previousStrategies, Map<Integer, SubSelectFactoryResult> subselects, Map<Integer, ExprTableEvalStrategy> tableAccesses, RowRecogPreviousStrategy rowRecogPreviousStrategy) {
        this.aggregationResultFuture = aggregationResultFuture;
        this.priorStrategies = priorStrategies;
        this.previousStrategies = previousStrategies;
        this.subselects = subselects;
        this.tableAccesses = tableAccesses;
        this.rowRecogPreviousStrategy = rowRecogPreviousStrategy;
    }

    public AggregationService getAggregationResultFuture() {
        return aggregationResultFuture;
    }

    public PriorEvalStrategy[] getPriorStrategies() {
        return priorStrategies;
    }

    public PreviousGetterStrategy[] getPreviousStrategies() {
        return previousStrategies;
    }

    public Map<Integer, SubSelectFactoryResult> getSubselects() {
        return subselects;
    }

    public SubordTableLookupStrategy getSubqueryLookup(int subqueryNumber) {
        return subselects.get(subqueryNumber).getLookupStrategy();
    }

    public PriorEvalStrategy getSubqueryPrior(int subqueryNumber) {
        return subselects.get(subqueryNumber).getPriorStrategy();
    }

    public PreviousGetterStrategy getSubqueryPrevious(int subqueryNumber) {
        return subselects.get(subqueryNumber).getPreviousStrategy();
    }

    public AggregationService getSubqueryAggregation(int subqueryNumber) {
        return subselects.get(subqueryNumber).getAggregationService();
    }

    public ExprTableEvalStrategy getTableAccess(int tableAccessNumber) {
        return tableAccesses.get(tableAccessNumber);
    }

    public RowRecogPreviousStrategy getRowRecogPreviousStrategy() {
        return rowRecogPreviousStrategy;
    }
}
