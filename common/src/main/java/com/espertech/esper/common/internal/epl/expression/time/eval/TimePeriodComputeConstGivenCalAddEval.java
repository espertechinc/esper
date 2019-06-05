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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.adder.TimePeriodAdder;
import com.espertech.esper.common.internal.schedule.TimeProvider;

import java.util.Calendar;
import java.util.TimeZone;

public class TimePeriodComputeConstGivenCalAddEval implements TimePeriodCompute, TimePeriodProvide {
    private TimePeriodAdder[] adders;
    private int[] added;
    private TimeAbacus timeAbacus;
    private int indexMicroseconds;
    private TimeZone timeZone;

    public TimePeriodComputeConstGivenCalAddEval() {
    }

    public TimePeriodComputeConstGivenCalAddEval(TimePeriodAdder[] adders, int[] added, TimeAbacus timeAbacus, int indexMicroseconds, TimeZone timeZone) {
        this.adders = adders;
        this.added = added;
        this.timeAbacus = timeAbacus;
        this.indexMicroseconds = indexMicroseconds;
        this.timeZone = timeZone;
    }

    public void setAdders(TimePeriodAdder[] adders) {
        this.adders = adders;
    }

    public void setAdded(int[] added) {
        this.added = added;
    }

    public void setTimeAbacus(TimeAbacus timeAbacus) {
        this.timeAbacus = timeAbacus;
    }

    public void setIndexMicroseconds(int indexMicroseconds) {
        this.indexMicroseconds = indexMicroseconds;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public long deltaAdd(long fromTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        long target = addSubtract(fromTime, 1);
        return target - fromTime;
    }

    public long deltaSubtract(long fromTime, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        long target = addSubtract(fromTime, -1);
        return fromTime - target;
    }

    public long deltaUseRuntimeTime(EventBean[] eventsPerStream, ExprEvaluatorContext context, TimeProvider timeProvider) {
        return deltaAdd(timeProvider.getTime(), eventsPerStream, true, context);
    }

    public TimePeriodDeltaResult deltaAddWReference(long fromTime, long reference, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        // find the next-nearest reference higher then the current time, compute delta, return reference one lower
        while (reference > fromTime) {
            reference = reference - deltaSubtract(reference, eventsPerStream, isNewData, context);
        }

        long next = reference;
        long last;
        do {
            last = next;
            next = next + deltaAdd(last, eventsPerStream, isNewData, context);
        }
        while (next <= fromTime);
        return new TimePeriodDeltaResult(next - fromTime, last);
    }

    public TimePeriodProvide getNonVariableProvide(ExprEvaluatorContext context) {
        return this;
    }

    private long addSubtract(long fromTime, int factor) {
        Calendar cal = Calendar.getInstance(timeZone);
        long remainder = timeAbacus.calendarSet(fromTime, cal);
        for (int i = 0; i < adders.length; i++) {
            adders[i].add(cal, factor * added[i]);
        }
        long result = timeAbacus.calendarGet(cal, remainder);
        if (indexMicroseconds != -1) {
            result += factor * added[indexMicroseconds];
        }
        return result;
    }
}
