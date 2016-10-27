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

package com.espertech.esper.epl.datetime.reformatop;

import java.util.Calendar;

public class CalendarEvalStatics {

    public final static CalendarEval MinuteOfHour = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.MINUTE);
            }
        };

    public final static CalendarEval MonthOfYear = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.MONTH);
            }
        };

    public final static CalendarEval DayOfMonth = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.DATE);
            }
        };

    public final static CalendarEval DayOfWeek = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.DAY_OF_WEEK);
            }
        };

    public final static CalendarEval DayOfYear = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.DAY_OF_YEAR);
            }
        };    

    public final static CalendarEval Era = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.ERA);
            }
        };

    public final static CalendarEval HourOfDay = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.HOUR_OF_DAY);
            }
        };

    public final static CalendarEval MillisOfSecond = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.MILLISECOND);
            }
        };

    public final static CalendarEval SecondOfMinute = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.SECOND);
            }
        };

    public final static CalendarEval Weekyear = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.WEEK_OF_YEAR);
            }
        };

    public final static CalendarEval Year = new CalendarEval() {
            public Object evaluateInternal(Calendar cal) {
                return cal.get(Calendar.YEAR);
            }
        };
}
