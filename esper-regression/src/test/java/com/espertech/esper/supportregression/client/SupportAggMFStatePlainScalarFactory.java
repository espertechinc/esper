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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.plugin.*;

public class SupportAggMFStatePlainScalarFactory implements PlugInAggregationMultiFunctionStateFactory {

    private final ExprEvaluator evaluator;

    public SupportAggMFStatePlainScalarFactory(ExprEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public AggregationState makeAggregationState(PlugInAggregationMultiFunctionStateContext stateContext) {
        return new SupportAggMFStatePlainScalar(this);
    }

    public ExprEvaluator getEvaluator() {
        return evaluator;
    }

    public static void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {
        SupportAggMFStatePlainScalar.rowMemberCodegen(context);
    }

    public static void applyEnterCodegen(ExprForge forge, PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        SupportAggMFStatePlainScalar.applyEnterCodegen(forge, context);
    }

    public static void applyLeaveCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {
        // no code
    }

    public static void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {
        SupportAggMFStatePlainScalar.clearCodegen(context);
    }
}
