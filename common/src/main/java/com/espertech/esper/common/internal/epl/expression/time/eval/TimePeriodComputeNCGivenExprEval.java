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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriodUtil;
import com.espertech.esper.common.internal.schedule.TimeProvider;

public class TimePeriodComputeNCGivenExprEval implements TimePeriodCompute {
    private ExprEvaluator secondsEvaluator;
    private TimeAbacus timeAbacus;

    public TimePeriodComputeNCGivenExprEval() {
    }

    public TimePeriodComputeNCGivenExprEval(ExprEvaluator secondsEvaluator, TimeAbacus timeAbacus) {
        this.secondsEvaluator = secondsEvaluator;
        this.timeAbacus = timeAbacus;
    }

    public void setSecondsEvaluator(ExprEvaluator secondsEvaluator) {
        this.secondsEvaluator = secondsEvaluator;
    }

    public void setTimeAbacus(TimeAbacus timeAbacus) {
        this.timeAbacus = timeAbacus;
    }

    public long deltaAdd(long fromTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return eval(eventsPerStream, isNewData, context);
    }

    public long deltaSubtract(long fromTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return eval(eventsPerStream, isNewData, context);
    }

    public TimePeriodDeltaResult deltaAddWReference(long fromTime, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        long delta = eval(eventsPerStream, isNewData, context);
        return new TimePeriodDeltaResult(TimePeriodUtil.deltaAddWReference(fromTime, reference, delta), reference);
    }

    public long deltaUseRuntimeTime(EventBean[] eventsPerStream, ExprEvaluatorContext context, TimeProvider timeProvider) {
        return eval(eventsPerStream, true, context);
    }

    public TimePeriodProvide getNonVariableProvide(ExprEvaluatorContext context) {
        long msec = eval(null, true, context);
        return new TimePeriodComputeConstGivenDeltaEval(msec);
    }

    private long eval(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Number time = (Number) secondsEvaluator.evaluate(eventsPerStream, isNewData, context);
        if (!ExprTimePeriodUtil.validateTime(time, timeAbacus)) {
            throw new EPException(ExprTimePeriodUtil.getTimeInvalidMsg("Invalid time computation result", time == null ? "null" : time.toString(), time));
        }
        return timeAbacus.deltaForSecondsNumber(time);
    }
}
