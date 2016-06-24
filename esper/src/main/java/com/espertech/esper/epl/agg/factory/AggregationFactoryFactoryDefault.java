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
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.methodagg.*;
import com.espertech.esper.schedule.TimeProvider;

import java.math.MathContext;

public class AggregationFactoryFactoryDefault implements AggregationFactoryFactory {

    public final static AggregationFactoryFactoryDefault INSTANCE = new AggregationFactoryFactoryDefault();

    private AggregationFactoryFactoryDefault() {
    }

    public AggregationMethodFactory makeCount(ExprCountNode exprCountNode, boolean ignoreNulls, Class countedValueType) {
        return new AggregationMethodFactoryCount(exprCountNode, ignoreNulls, countedValueType);
    }

    public AggregationMethodFactory makeSum(ExprSumNode exprSumNode, Class childType) {
        return new AggregationMethodFactorySum(exprSumNode, childType);
    }

    public AggregationMethodFactory makeAvedev(ExprAvedevNode exprAvedevNode, Class childType, ExprNode[] positionalParams) {
        return new AggregationMethodFactoryAvedev(exprAvedevNode, childType, positionalParams);
    }

    public AggregationMethodFactory makeAvg(ExprAvgNode exprAvgNode, Class childType, MathContext optionalMathContext) {
        return new AggregationMethodFactoryAvg(exprAvgNode, childType, optionalMathContext);
    }

    public AggregationMethodFactory makeCountEver(ExprCountEverNode exprCountEverNode, boolean ignoreNulls) {
        return new AggregationMethodFactoryCountEver(exprCountEverNode, ignoreNulls);
    }

    public AggregationMethodFactory makeFirstEver(ExprFirstEverNode exprFirstEverNode, Class type) {
        return new AggregationMethodFactoryFirstEver(exprFirstEverNode, type);
    }

    public AggregationMethodFactory makeLastEver(ExprLastEverNode exprLastEverNode, Class type) {
        return new AggregationMethodFactoryLastEver(exprLastEverNode, type);
    }

    public AggregationMethodFactory makeLeaving(ExprLeavingAggNode exprLeavingAggNode) {
        return new AggregationMethodFactoryLeaving(exprLeavingAggNode);
    }

    public AggregationMethodFactory makeMedian(ExprMedianNode exprMedianNode, Class childType) {
        return new AggregationMethodFactoryMedian(exprMedianNode, childType);
    }

    public AggregationMethodFactory makeMinMax(ExprMinMaxAggrNode exprMinMaxAggrNode, Class type, boolean hasDataWindows) {
        return new AggregationMethodFactoryMinMax(exprMinMaxAggrNode, type, hasDataWindows);
    }

    public AggregationMethodFactory makeNth(ExprNthAggNode exprNthAggNode, Class type, int size) {
        return new AggregationMethodFactoryNth(exprNthAggNode, type, size);
    }

    public AggregationMethodFactory makePlugIn(ExprPlugInAggNode expr, AggregationFunctionFactory factory, Class childType) {
        return new AggregationMethodFactoryPlugIn(expr, factory, childType);
    }

    public AggregationMethodFactory makeRate(ExprRateAggNode exprRateAggNode, boolean isEver, long intervalMsec, TimeProvider timeProvider) {
        return new AggregationMethodFactoryRate(exprRateAggNode, isEver, intervalMsec, timeProvider);
    }

    public AggregationMethodFactory makeStddev(ExprStddevNode exprStddevNode, Class childType) {
        return new AggregationMethodFactoryStddev(exprStddevNode, childType);
    }
}
