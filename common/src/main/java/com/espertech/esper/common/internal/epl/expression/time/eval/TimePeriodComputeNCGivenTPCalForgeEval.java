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

import java.util.Calendar;
import java.util.TimeZone;

public class TimePeriodComputeNCGivenTPCalForgeEval implements TimePeriodCompute {
    private ExprEvaluator[] evaluators;
    private TimePeriodAdder[] adders;
    private TimeAbacus timeAbacus;
    private TimeZone timeZone;
    private int indexMicroseconds;

    public TimePeriodComputeNCGivenTPCalForgeEval() {
    }

    public TimePeriodComputeNCGivenTPCalForgeEval(ExprEvaluator[] evaluators, TimePeriodAdder[] adders, TimeAbacus timeAbacus, TimeZone timeZone, int indexMicroseconds) {
        this.evaluators = evaluators;
        this.adders = adders;
        this.timeAbacus = timeAbacus;
        this.timeZone = timeZone;
        this.indexMicroseconds = indexMicroseconds;
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

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public void setIndexMicroseconds(int indexMicroseconds) {
        this.indexMicroseconds = indexMicroseconds;
    }

    public long deltaAdd(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return addSubtract(currentTime, 1, eventsPerStream, isNewData, context);
    }

    public long deltaSubtract(long currentTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return addSubtract(currentTime, -1, eventsPerStream, isNewData, context);
    }

    public long deltaUseRuntimeTime(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, TimeProvider timeProvider) {
        long currentTime = timeProvider.getTime();
        return addSubtract(currentTime, 1, eventsPerStream, true, exprEvaluatorContext);
    }

    public TimePeriodDeltaResult deltaAddWReference(long current, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // find the next-nearest reference higher then the current time, compute delta, return reference one lower
        while (reference > current) {
            reference = reference - deltaSubtract(reference, eventsPerStream, isNewData, context);
        }

        long next = reference;
        long last;
        do {
            last = next;
            next = next + deltaAdd(last, eventsPerStream, isNewData, context);
        }
        while (next <= current);
        return new TimePeriodDeltaResult(next - current, last);
    }

    public TimePeriodProvide getNonVariableProvide(ExprEvaluatorContext context) {
        int[] added = new int[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            added[i] = ((Number) evaluators[i].evaluate(null, true, context)).intValue();
        }
        return new TimePeriodComputeConstGivenCalAddEval(adders, added, timeAbacus, indexMicroseconds, timeZone);
    }

    private long addSubtract(long currentTime, int factor, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext context) {
        Calendar cal = Calendar.getInstance(timeZone);
        long remainder = timeAbacus.calendarSet(currentTime, cal);

        int usec = 0;
        for (int i = 0; i < adders.length; i++) {
            int value = ((Number) evaluators[i].evaluate(eventsPerStream, newData, context)).intValue();
            if (i == indexMicroseconds) {
                usec = value;
            } else {
                adders[i].add(cal, factor * value);
            }
        }

        long result = timeAbacus.calendarGet(cal, remainder);
        if (indexMicroseconds != -1) {
            result += factor * usec;
        }
        return result - currentTime;
    }
}
