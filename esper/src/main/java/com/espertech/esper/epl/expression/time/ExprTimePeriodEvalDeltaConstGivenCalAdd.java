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
package com.espertech.esper.epl.expression.time;

import com.espertech.esper.core.context.util.AgentInstanceContext;

import java.util.Calendar;
import java.util.TimeZone;

public class ExprTimePeriodEvalDeltaConstGivenCalAdd implements ExprTimePeriodEvalDeltaConst, ExprTimePeriodEvalDeltaConstFactory {
    private final Calendar cal;
    private final ExprTimePeriodImpl.TimePeriodAdder[] adders;
    private final int[] added;
    private final TimeAbacus timeAbacus;
    private final int indexMicroseconds;

    public ExprTimePeriodEvalDeltaConstGivenCalAdd(ExprTimePeriodImpl.TimePeriodAdder[] adders, int[] added, TimeZone timeZone, TimeAbacus timeAbacus) {
        this.adders = adders;
        this.added = added;
        this.cal = Calendar.getInstance(timeZone);
        this.timeAbacus = timeAbacus;
        this.indexMicroseconds = ExprTimePeriodUtil.findIndexMicroseconds(adders);
    }

    public ExprTimePeriodEvalDeltaConst make(String validateMsgName, String validateMsgValue, AgentInstanceContext agentInstanceContext) {
        return this;
    }

    public boolean equalsTimePeriod(ExprTimePeriodEvalDeltaConst otherComputation) {
        if (otherComputation instanceof ExprTimePeriodEvalDeltaConstGivenCalAdd) {
            ExprTimePeriodEvalDeltaConstGivenCalAdd other = (ExprTimePeriodEvalDeltaConstGivenCalAdd) otherComputation;
            if (other.adders.length != adders.length) {
                return false;
            }
            for (int i = 0; i < adders.length; i++) {
                if (added[i] != other.added[i] || adders[i].getClass() != other.adders[i].getClass()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public synchronized long deltaAdd(long fromTime) {
        long target = addSubtract(fromTime, 1);
        return target - fromTime;
    }

    public synchronized long deltaSubtract(long fromTime) {
        long target = addSubtract(fromTime, -1);
        return fromTime - target;
    }

    public ExprTimePeriodEvalDeltaResult deltaAddWReference(long fromTime, long reference) {
        // find the next-nearest reference higher then the current time, compute delta, return reference one lower
        if (reference > fromTime) {
            while (reference > fromTime) {
                reference = reference - deltaSubtract(reference);
            }
        }

        long next = reference;
        long last;
        do {
            last = next;
            next = next + deltaAdd(last);
        }
        while (next <= fromTime);
        return new ExprTimePeriodEvalDeltaResult(next - fromTime, last);
    }

    private long addSubtract(long fromTime, int factor) {
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
