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
package com.espertech.esper.regressionlib.support.extend.aggmultifunc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionState;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

public class SupportAggMFMultiRTPlainScalarState implements AggregationMultiFunctionState {
    private final SupportAggMFMultiRTPlainScalarStateFactory factory;

    private Object lastValue;

    public SupportAggMFMultiRTPlainScalarState(SupportAggMFMultiRTPlainScalarStateFactory factory) {
        this.factory = factory;
    }

    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        lastValue = factory.getParam().evaluate(eventsPerStream, true, exprEvaluatorContext);
    }

    public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        // ever semantics
    }

    public void clear() {
        lastValue = null;
    }

    public int size() {
        return lastValue == null ? 0 : 1;
    }

    public Object getLastValue() {
        return lastValue;
    }
}
