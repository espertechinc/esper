/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.epl.expression.time;

import java.util.Calendar;
import java.util.TimeZone;

public class ExprTimePeriodEvalDeltaConstCalAdd implements ExprTimePeriodEvalDeltaConst
{
    private final Calendar cal;
    private final ExprTimePeriodImpl.TimePeriodAdder[] adders;
    private final int[] added;

    public ExprTimePeriodEvalDeltaConstCalAdd(ExprTimePeriodImpl.TimePeriodAdder[] adders, int[] added, TimeZone timeZone) {
        this.adders = adders;
        this.added = added;
        this.cal = Calendar.getInstance(timeZone);
    }

    public boolean equalsTimePeriod(ExprTimePeriodEvalDeltaConst otherComputation) {
        if (otherComputation instanceof ExprTimePeriodEvalDeltaConstCalAdd) {
            ExprTimePeriodEvalDeltaConstCalAdd other = (ExprTimePeriodEvalDeltaConstCalAdd) otherComputation;
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

    public synchronized long deltaMillisecondsAdd(long fromTime) {
        cal.setTimeInMillis(fromTime);
        addSubtract(adders, added, cal, 1);
        return cal.getTimeInMillis() - fromTime;
    }

    public synchronized long deltaMillisecondsSubtract(long fromTime) {
        cal.setTimeInMillis(fromTime);
        addSubtract(adders, added, cal, -1);
        return fromTime - cal.getTimeInMillis();
    }

    public synchronized ExprTimePeriodEvalDeltaResult deltaMillisecondsAddWReference(long fromTime, long reference) {
        // find the next-nearest reference higher then the current time, compute delta, return reference one lower
        if (reference > fromTime) {
            while(reference > fromTime) {
                reference = reference - deltaMillisecondsSubtract(reference);
            }
        }

        long next = reference;
        long last;
        do {
            last = next;
            next = next + deltaMillisecondsAdd(last);
        }
        while (next <= fromTime);
        return new ExprTimePeriodEvalDeltaResult(next - fromTime, last);
    }

    private static void addSubtract(ExprTimePeriodImpl.TimePeriodAdder[] adders, int[] added, Calendar cal, int factor) {
        for (int i = 0; i < adders.length; i++) {
            adders[i].add(cal, factor * added[i]);
        }
    }
}
