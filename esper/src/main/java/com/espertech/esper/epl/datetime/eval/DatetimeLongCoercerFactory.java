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

package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.util.JavaClassHelper;

import java.util.Calendar;
import java.util.Date;

public class DatetimeLongCoercerFactory {

    private static DatetimeLongCoercerLong datetimeLongCoercerLong = new DatetimeLongCoercerLong();
    private static DatetimeLongCoercerDate datetimeLongCoercerDate = new DatetimeLongCoercerDate();
    private static DatetimeLongCoercerCal datetimeLongCoercerCal = new DatetimeLongCoercerCal();

    public static DatetimeLongCoercer getCoercer(Class clazz) {
        if (JavaClassHelper.isSubclassOrImplementsInterface(clazz, Date.class)) {
            return datetimeLongCoercerDate;
        }
        if (JavaClassHelper.isSubclassOrImplementsInterface(clazz, Calendar.class)) {
            return datetimeLongCoercerCal;
        }
        return datetimeLongCoercerLong;
    }
}
