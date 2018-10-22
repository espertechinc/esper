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

public class TimePeriodUtil {
    /**
     * Delta add with reference.
     *
     * @param current   current
     * @param reference ref
     * @param msec      msec
     * @return delta
     */
    public static long deltaAddWReference(long current, long reference, long msec) {
        // Example:  current c=2300, reference r=1000, interval i=500, solution s=200
        //
        // int n = ((2300 - 1000) / 500) = 2
        // r + (n + 1) * i - c = 200
        //
        // Negative example:  current c=2300, reference r=4200, interval i=500, solution s=400
        // int n = ((2300 - 4200) / 500) = -3
        // r + (n + 1) * i - c = 4200 - 3*500 - 2300 = 400
        //
        long n = (current - reference) / msec;
        if (reference > current) { // References in the future need to deduct one window
            n--;
        }
        long solution = reference + (n + 1) * msec - current;
        if (solution == 0) {
            return msec;
        }
        return solution;
    }
}
