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
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategy;
import com.espertech.esper.common.internal.view.previous.PreviousGetterStrategy;

public interface StatementAIFactoryAssignments {
    AggregationService getAggregationResultFuture();

    PriorEvalStrategy[] getPriorStrategies();

    PreviousGetterStrategy[] getPreviousStrategies();

    SubordTableLookupStrategy getSubqueryLookup(int subqueryNumber);

    PriorEvalStrategy getSubqueryPrior(int subqueryNumber);

    PreviousGetterStrategy getSubqueryPrevious(int subqueryNumber);

    AggregationService getSubqueryAggregation(int subqueryNumber);

    ExprTableEvalStrategy getTableAccess(int tableAccessNumber);

    RowRecogPreviousStrategy getRowRecogPreviousStrategy();
}
