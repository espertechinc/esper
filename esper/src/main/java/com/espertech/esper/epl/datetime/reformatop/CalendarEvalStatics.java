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
package com.espertech.esper.epl.datetime.reformatop;

import java.util.Calendar;

public class CalendarEvalStatics {

    public final static CalendarEval MINUTE_OF_HOUR = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.MINUTE);
        }
    };

    public final static CalendarEval MONTH_OF_YEAR = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.MONTH);
        }
    };

    public final static CalendarEval DAY_OF_MONTH = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.DATE);
        }
    };

    public final static CalendarEval DAY_OF_WEEK = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.DAY_OF_WEEK);
        }
    };

    public final static CalendarEval DAY_OF_YEAR = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.DAY_OF_YEAR);
        }
    };

    public final static CalendarEval ERA = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.ERA);
        }
    };

    public final static CalendarEval HOUR_OF_DAY = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.HOUR_OF_DAY);
        }
    };

    public final static CalendarEval MILLIS_OF_SECOND = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.MILLISECOND);
        }
    };

    public final static CalendarEval SECOND_OF_MINUTE = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.SECOND);
        }
    };

    public final static CalendarEval WEEKYEAR = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.WEEK_OF_YEAR);
        }
    };

    public final static CalendarEval YEAR = new CalendarEval() {
        public Object evaluateInternal(Calendar cal) {
            return cal.get(Calendar.YEAR);
        }
    };
}
