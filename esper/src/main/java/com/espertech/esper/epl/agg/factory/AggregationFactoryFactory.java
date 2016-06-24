/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.agg.factory;

import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.epl.expression.methodagg.ExprPlugInAggNode;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.methodagg.*;
import com.espertech.esper.schedule.TimeProvider;

import java.math.MathContext;

public interface AggregationFactoryFactory {
    AggregationMethodFactory makeCount(ExprCountNode exprCountNode, boolean ignoreNulls, Class countedValueType);
    AggregationMethodFactory makeSum(ExprSumNode exprSumNode, Class childType);
    AggregationMethodFactory makeAvedev(ExprAvedevNode exprAvedevNode, Class childType, ExprNode[] positionalParams);
    AggregationMethodFactory makeAvg(ExprAvgNode exprAvgNode, Class childType, MathContext optionalMathContext);
    AggregationMethodFactory makeCountEver(ExprCountEverNode exprCountEverNode, boolean ignoreNulls);
    AggregationMethodFactory makeFirstEver(ExprFirstEverNode exprFirstEverNode, Class type);
    AggregationMethodFactory makeLastEver(ExprLastEverNode exprLastEverNode, Class type);
    AggregationMethodFactory makeLeaving(ExprLeavingAggNode exprLeavingAggNode);
    AggregationMethodFactory makeMedian(ExprMedianNode exprMedianNode, Class childType);
    AggregationMethodFactory makeMinMax(ExprMinMaxAggrNode exprMinMaxAggrNode, Class type, boolean hasDataWindows);
    AggregationMethodFactory makeNth(ExprNthAggNode exprNthAggNode, Class type, int size);
    AggregationMethodFactory makePlugIn(ExprPlugInAggNode expr, AggregationFunctionFactory factory, Class childType);
    AggregationMethodFactory makeRate(ExprRateAggNode exprRateAggNode, boolean isEver, long intervalMsec, TimeProvider timeProvider);
    AggregationMethodFactory makeStddev(ExprStddevNode exprStddevNode, Class childType);
}
