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

public class ExprTimePeriodUtil {
    public static boolean validateTime(Number timeInSeconds, TimeAbacus timeAbacus) {
        return timeInSeconds != null && timeAbacus.deltaForSecondsNumber(timeInSeconds) >= 1;
    }

    public static String getTimeInvalidMsg(String validateMsgName, String validateMsgValue, Number timeInSeconds) {
        return validateMsgName + " " + validateMsgValue + " requires a size of at least 1 msec but received " + timeInSeconds;
    }

    static int findIndexMicroseconds(ExprTimePeriodAdder.TimePeriodAdder[] adders) {
        int indexMicros = -1;
        for (int i = 0; i < adders.length; i++) {
            if (adders[i].isMicroseconds()) {
                indexMicros = i;
                break;
            }
        }
        return indexMicros;
    }
}
