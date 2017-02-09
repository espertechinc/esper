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

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

public class LocalDateTimeEvalStatics {

    public final static LocalDateTimeEval MINUTE_OF_HOUR = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.getMinute();
        }
    };

    public final static LocalDateTimeEval MONTH_OF_YEAR = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.getMonth();
        }
    };

    public final static LocalDateTimeEval DAY_OF_MONTH = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.getDayOfMonth();
        }
    };

    public final static LocalDateTimeEval DAY_OF_WEEK = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.getDayOfWeek();
        }
    };

    public final static LocalDateTimeEval DAY_OF_YEAR = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.getDayOfYear();
        }
    };

    public final static LocalDateTimeEval ERA = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.get(ChronoField.ERA);
        }
    };

    public final static LocalDateTimeEval HOUR_OF_DAY = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.getHour();
        }
    };

    public final static LocalDateTimeEval MILLIS_OF_SECOND = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.get(ChronoField.MILLI_OF_SECOND);
        }
    };

    public final static LocalDateTimeEval SECOND_OF_MINUTE = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.getSecond();
        }
    };

    public final static LocalDateTimeEval WEEKYEAR = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
        }
    };

    public final static LocalDateTimeEval YEAR = new LocalDateTimeEval() {
        public Object evaluateInternal(LocalDateTime ldt) {
            return ldt.getYear();
        }
    };
}
