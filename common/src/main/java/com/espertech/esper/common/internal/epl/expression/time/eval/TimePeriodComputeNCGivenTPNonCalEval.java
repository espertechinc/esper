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
package com.espertech.esper.common.internal.epl.expression.time.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.adder.TimePeriodAdder;
import com.espertech.esper.common.internal.schedule.TimeProvider;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriodForge.makeTimePeriodParamNullException;

public class TimePeriodComputeNCGivenTPNonCalEval implements TimePeriodCompute {

    private ExprEvaluator[] evaluators;
    private TimePeriodAdder[] adders;
    private TimeAbacus timeAbacus;

    public TimePeriodComputeNCGivenTPNonCalEval() {
    }

    public TimePeriodComputeNCGivenTPNonCalEval(ExprEvaluator[] evaluators, TimePeriodAdder[] adders, TimeAbacus timeAbacus) {
        this.evaluators = evaluators;
        this.adders = adders;
        this.timeAbacus = timeAbacus;
    }

    public void setEvaluators(ExprEvaluator[] evaluators) {
        this.evaluators = evaluators;
    }

    public void setAdders(TimePeriodAdder[] adders) {
        this.adders = adders;
    }

    public void setTimeAbacus(TimeAbacus timeAbacus) {
        this.timeAbacus = timeAbacus;
    }

    public long deltaAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return evaluate(eventsPerStream, isNewData, context);
    }

    public long deltaSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return evaluate(eventsPerStream, isNewData, context);
    }

    public long deltaUseRuntimeTime(EventBean[] eventsPerStream, ExprEvaluatorContext context, TimeProvider timeProvider) {
        return evaluate(eventsPerStream, true, context);
    }

    public TimePeriodDeltaResult deltaAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        long timeDelta = evaluate(eventsPerStream, isNewData, context);
        return new TimePeriodDeltaResult(TimePeriodUtil.deltaAddWReference(current, reference, timeDelta), reference);
    }

    public TimePeriodProvide getNonVariableProvide(ExprEvaluatorContext context) {
        long delta = evaluate(null, true, context);
        return new TimePeriodComputeConstGivenDeltaEval(delta);
    }

    private long evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        double seconds = 0;
        for (int i = 0; i < adders.length; i++) {
            Double result = eval(evaluators[i], eventsPerStream, isNewData, context);
            if (result == null) {
                throw makeTimePeriodParamNullException("Received null value evaluating time period");
            }
            seconds += adders[i].compute(result);
        }
        return timeAbacus.deltaForSecondsDouble(seconds);
    }

    private Double eval(ExprEvaluator expr, EventBean[] events, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object value = expr.evaluate(events, isNewData, exprEvaluatorContext);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof BigInteger) {
            return ((Number) value).doubleValue();
        }
        return ((Number) value).doubleValue();
    }
}
