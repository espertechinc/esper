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
import com.espertech.esper.epl.agg.access.AggregationStateMinMaxByEverSpecForge;
import com.espertech.esper.epl.agg.access.AggregationStateSortedSpecForge;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.approx.CountMinSketchSpec;
import com.espertech.esper.epl.expression.accessagg.ExprAggCountMinSketchNode;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionSortedMinMaxByNode;
import com.espertech.esper.epl.expression.accessagg.ExprPlugInAggMultiFunctionNodeFactory;
import com.espertech.esper.epl.expression.core.ExprForge;
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

    AggregationMethodFactory makeFirstLastEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprFirstLastEverNode exprFirstLastEverNode, Class type);

    AggregationMethodFactory makeLeaving(StatementExtensionSvcContext statementExtensionSvcContext, ExprLeavingAggNode exprLeavingAggNode);

    AggregationMethodFactory makeMedian(StatementExtensionSvcContext statementExtensionSvcContext, ExprMedianNode exprMedianNode, Class childType);

    AggregationMethodFactory makeMinMax(StatementExtensionSvcContext statementExtensionSvcContext, ExprMinMaxAggrNode exprMinMaxAggrNode, Class type, boolean hasDataWindows);

    AggregationMethodFactory makeNth(StatementExtensionSvcContext statementExtensionSvcContext, ExprNthAggNode exprNthAggNode, Class type, int size);

    AggregationMethodFactory makePlugInMethod(StatementExtensionSvcContext statementExtensionSvcContext, ExprPlugInAggNode expr, AggregationFunctionFactory factory, Class childType);

    AggregationMethodFactory makeRate(StatementExtensionSvcContext statementExtensionSvcContext, ExprRateAggNode exprRateAggNode, boolean isEver, long intervalMsec, TimeProvider timeProvider, TimeAbacus timeAbacus);

    AggregationMethodFactory makeStddev(StatementExtensionSvcContext statementExtensionSvcContext, ExprStddevNode exprStddevNode, Class childType);

    AggregationMethodFactory makeLinearUnbounded(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionLinearAccessNode exprAggMultiFunctionLinearAccessNode, EventType containedType, Class accessorResultType, int streamNum, boolean hasFilter);

    AggregationStateFactoryForge makeLinear(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionLinearAccessNode expr, int streamNum, ExprForge optionalFilter, boolean join);

    AggregationStateFactoryCountMinSketch makeCountMinSketch(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggCountMinSketchNode expr, CountMinSketchSpec specification);

    AggregationStateFactoryForge makeMinMaxEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionSortedMinMaxByNode expr, AggregationStateMinMaxByEverSpecForge spec);

    AggregationStateFactoryForge makePlugInAccess(StatementExtensionSvcContext statementExtensionSvcContext, ExprPlugInAggMultiFunctionNodeFactory factory);

    AggregationStateFactoryForge makeSorted(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionSortedMinMaxByNode expr, AggregationStateSortedSpecForge spec);
}
