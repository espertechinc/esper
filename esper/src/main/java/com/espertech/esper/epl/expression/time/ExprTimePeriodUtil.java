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

import com.espertech.esper.util.JavaClassHelper;

public class ExprTimePeriodUtil {
    public static boolean validateTime(Number timeInSeconds) {
        if (timeInSeconds == null) {
            return false;
        }
        long millisecondsBeforeExpiry = computeTimeMSec(timeInSeconds);
        return millisecondsBeforeExpiry >= 1;
    }

    public static String getTimeInvalidMsg(String validateMsgName, String validateMsgValue, Number timeInSeconds) {
        return validateMsgName + " " + validateMsgValue + " requires a size of at least 1 msec but received " + timeInSeconds;
    }

    public  static long computeTimeMSec(Number timeInSeconds) {
        if (JavaClassHelper.isFloatingPointNumber(timeInSeconds)) {
            return Math.round(1000d * timeInSeconds.doubleValue());
        }
        else {
            return 1000 * timeInSeconds.longValue();
        }
    }
}
