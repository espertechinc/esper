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
package com.espertech.esper.epl.agg.factory;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.epl.agg.access.AggregationStateMinMaxByEverSpec;
import com.espertech.esper.epl.agg.access.AggregationStateSortedSpec;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.approx.CountMinSketchSpec;
import com.espertech.esper.epl.expression.accessagg.ExprAggCountMinSketchNode;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionSortedMinMaxByNode;
import com.espertech.esper.epl.expression.accessagg.ExprPlugInAggMultiFunctionNodeFactory;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.methodagg.*;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.schedule.TimeProvider;

import java.math.MathContext;

public interface AggregationFactoryFactory {
    AggregationMethodFactory makeCount(StatementExtensionSvcContext statementExtensionSvcContext, ExprCountNode exprCountNode, boolean ignoreNulls, Class countedValueType);

    AggregationMethodFactory makeSum(StatementExtensionSvcContext statementExtensionSvcContext, ExprSumNode exprSumNode, Class childType);

    AggregationMethodFactory makeAvedev(StatementExtensionSvcContext statementExtensionSvcContext, ExprAvedevNode exprAvedevNode, Class childType, ExprNode[] positionalParams);

    AggregationMethodFactory makeAvg(StatementExtensionSvcContext statementExtensionSvcContext, ExprAvgNode exprAvgNode, Class childType, MathContext optionalMathContext);

    AggregationMethodFactory makeCountEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprCountEverNode exprCountEverNode, boolean ignoreNulls);

    AggregationMethodFactory makeFirstEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprFirstEverNode exprFirstEverNode, Class type);

    AggregationMethodFactory makeLastEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprLastEverNode exprLastEverNode, Class type);

    AggregationMethodFactory makeLeaving(StatementExtensionSvcContext statementExtensionSvcContext, ExprLeavingAggNode exprLeavingAggNode);

    AggregationMethodFactory makeMedian(StatementExtensionSvcContext statementExtensionSvcContext, ExprMedianNode exprMedianNode, Class childType);

    AggregationMethodFactory makeMinMax(StatementExtensionSvcContext statementExtensionSvcContext, ExprMinMaxAggrNode exprMinMaxAggrNode, Class type, boolean hasDataWindows);

    AggregationMethodFactory makeNth(StatementExtensionSvcContext statementExtensionSvcContext, ExprNthAggNode exprNthAggNode, Class type, int size);

    AggregationMethodFactory makePlugInMethod(StatementExtensionSvcContext statementExtensionSvcContext, ExprPlugInAggNode expr, AggregationFunctionFactory factory, Class childType);

    AggregationMethodFactory makeRate(StatementExtensionSvcContext statementExtensionSvcContext, ExprRateAggNode exprRateAggNode, boolean isEver, long intervalMsec, TimeProvider timeProvider, TimeAbacus timeAbacus);

    AggregationMethodFactory makeStddev(StatementExtensionSvcContext statementExtensionSvcContext, ExprStddevNode exprStddevNode, Class childType);

    AggregationMethodFactory makeLinearUnbounded(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionLinearAccessNode exprAggMultiFunctionLinearAccessNode, EventType containedType, Class accessorResultType, int streamNum);

    AggregationStateFactory makeLinear(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionLinearAccessNode expr, int streamNum);

    AggregationStateFactoryCountMinSketch makeCountMinSketch(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggCountMinSketchNode expr, CountMinSketchSpec specification);

    AggregationStateFactory makeMinMaxEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionSortedMinMaxByNode expr, AggregationStateMinMaxByEverSpec spec);

    AggregationStateFactory makePlugInAccess(StatementExtensionSvcContext statementExtensionSvcContext, ExprPlugInAggMultiFunctionNodeFactory factory);

    AggregationStateFactory makeSorted(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionSortedMinMaxByNode expr, AggregationStateSortedSpec spec);
}
