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
package com.espertech.esper.example.cycledetect;

import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateContext;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionStateFactory;

public class CycleDetectorAggregationStateFactory implements PlugInAggregationMultiFunctionStateFactory {

    private final ExprEvaluator fromEvaluator;
    private final ExprEvaluator toEvaluator;

    public CycleDetectorAggregationStateFactory(ExprEvaluator fromEvaluator, ExprEvaluator toEvaluator) {
        this.fromEvaluator = fromEvaluator;
        this.toEvaluator = toEvaluator;
    }

    public AggregationState makeAggregationState(PlugInAggregationMultiFunctionStateContext stateContext) {
        return new CycleDetectorAggregationState(this);
    }

    public ExprEvaluator getFromEvaluator() {
        return fromEvaluator;
    }

    public ExprEvaluator getToEvaluator() {
        return toEvaluator;
    }
}
