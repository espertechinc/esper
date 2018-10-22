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

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionState;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateFactory;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateFactoryContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class CycleDetectorAggregationStateFactory implements AggregationMultiFunctionStateFactory {

    private ExprEvaluator from;
    private ExprEvaluator to;

    public AggregationMultiFunctionState newState(AggregationMultiFunctionStateFactoryContext ctx) {
        return new CycleDetectorAggregationState(this);
    }

    public void setFrom(ExprEvaluator from) {
        this.from = from;
    }

    public void setTo(ExprEvaluator to) {
        this.to = to;
    }

    public ExprEvaluator getFrom() {
        return from;
    }

    public ExprEvaluator getTo() {
        return to;
    }
}
