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
import com.espertech.esper.epl.expression.accessagg.*;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.methodagg.*;
import com.espertech.esper.epl.expression.time.TimeAbacus;
import com.espertech.esper.schedule.TimeProvider;

import java.math.MathContext;

public class AggregationFactoryFactoryDefault implements AggregationFactoryFactory {

    public final static AggregationFactoryFactoryDefault INSTANCE = new AggregationFactoryFactoryDefault();

    private AggregationFactoryFactoryDefault() {
    }

    public AggregationMethodFactory makeCount(StatementExtensionSvcContext statementExtensionSvcContext, ExprCountNode exprCountNode, boolean ignoreNulls, Class countedValueType) {
        return new AggregationMethodFactoryCount(exprCountNode, ignoreNulls, countedValueType);
    }

    public AggregationMethodFactory makeSum(StatementExtensionSvcContext statementExtensionSvcContext, ExprSumNode exprSumNode, Class childType) {
        return new AggregationMethodFactorySum(exprSumNode, childType);
    }

    public AggregationMethodFactory makeAvedev(StatementExtensionSvcContext statementExtensionSvcContext, ExprAvedevNode exprAvedevNode, Class childType, ExprNode[] positionalParams) {
        return new AggregationMethodFactoryAvedev(exprAvedevNode, childType, positionalParams);
    }

    public AggregationMethodFactory makeAvg(StatementExtensionSvcContext statementExtensionSvcContext, ExprAvgNode exprAvgNode, Class childType, MathContext optionalMathContext) {
        return new AggregationMethodFactoryAvg(exprAvgNode, childType, optionalMathContext);
    }

    public AggregationMethodFactory makeCountEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprCountEverNode exprCountEverNode, boolean ignoreNulls) {
        return new AggregationMethodFactoryCountEver(exprCountEverNode, ignoreNulls);
    }

    public AggregationMethodFactory makeFirstEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprFirstEverNode exprFirstEverNode, Class type) {
        return new AggregationMethodFactoryFirstEver(exprFirstEverNode, type);
    }

    public AggregationMethodFactory makeLastEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprLastEverNode exprLastEverNode, Class type) {
        return new AggregationMethodFactoryLastEver(exprLastEverNode, type);
    }

    public AggregationMethodFactory makeLeaving(StatementExtensionSvcContext statementExtensionSvcContext, ExprLeavingAggNode exprLeavingAggNode) {
        return new AggregationMethodFactoryLeaving(exprLeavingAggNode);
    }

    public AggregationMethodFactory makeMedian(StatementExtensionSvcContext statementExtensionSvcContext, ExprMedianNode exprMedianNode, Class childType) {
        return new AggregationMethodFactoryMedian(exprMedianNode, childType);
    }

    public AggregationMethodFactory makeMinMax(StatementExtensionSvcContext statementExtensionSvcContext, ExprMinMaxAggrNode exprMinMaxAggrNode, Class type, boolean hasDataWindows) {
        return new AggregationMethodFactoryMinMax(exprMinMaxAggrNode, type, hasDataWindows);
    }

    public AggregationMethodFactory makeNth(StatementExtensionSvcContext statementExtensionSvcContext, ExprNthAggNode exprNthAggNode, Class type, int size) {
        return new AggregationMethodFactoryNth(exprNthAggNode, type, size);
    }

    public AggregationMethodFactory makePlugInMethod(StatementExtensionSvcContext statementExtensionSvcContext, ExprPlugInAggNode expr, AggregationFunctionFactory factory, Class childType) {
        return new AggregationMethodFactoryPlugIn(expr, factory, childType);
    }

    public AggregationMethodFactory makeRate(StatementExtensionSvcContext statementExtensionSvcContext, ExprRateAggNode exprRateAggNode, boolean isEver, long intervalTime, TimeProvider timeProvider, TimeAbacus timeAbacus) {
        return new AggregationMethodFactoryRate(exprRateAggNode, isEver, intervalTime, timeProvider, timeAbacus);
    }

    public AggregationMethodFactory makeStddev(StatementExtensionSvcContext statementExtensionSvcContext, ExprStddevNode exprStddevNode, Class childType) {
        return new AggregationMethodFactoryStddev(exprStddevNode, childType);
    }

    public AggregationMethodFactory makeLinearUnbounded(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionLinearAccessNode parent, EventType containedType, Class accessorResultType, int streamNum) {
        return new AggregationMethodFactoryFirstLastUnbound(parent, containedType, accessorResultType, streamNum);
    }

    public AggregationStateFactory makeLinear(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionLinearAccessNode expr, int streamNum) {
        return new AggregationStateFactoryLinear(expr, streamNum);
    }

    public AggregationStateFactoryCountMinSketch makeCountMinSketch(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggCountMinSketchNode expr, CountMinSketchSpec specification) {
        return new AggregationStateFactoryCountMinSketch(expr, specification);
    }

    public AggregationStateFactory makeMinMaxEver(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionSortedMinMaxByNode expr, AggregationStateMinMaxByEverSpec spec) {
        return new AggregationStateFactoryMinMaxByEver(expr, spec);
    }

    public AggregationStateFactory makePlugInAccess(StatementExtensionSvcContext statementExtensionSvcContext, ExprPlugInAggMultiFunctionNodeFactory factory) {
        return new AggregationStateFactoryPlugin(factory);
    }

    public AggregationStateFactory makeSorted(StatementExtensionSvcContext statementExtensionSvcContext, ExprAggMultiFunctionSortedMinMaxByNode expr, AggregationStateSortedSpec spec) {
        return new AggregationStateFactorySorted(expr, spec);
    }
}
