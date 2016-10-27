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

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

public class LocalDateTimeEvalStatics {

    public final static LocalDateTimeEval MinuteOfHour = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.getMinute();
            }
        };

    public final static LocalDateTimeEval MonthOfYear = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.getMonth();
            }
        };

    public final static LocalDateTimeEval DayOfMonth = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.getDayOfMonth();
            }
        };

    public final static LocalDateTimeEval DayOfWeek = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.getDayOfWeek();
            }
        };

    public final static LocalDateTimeEval DayOfYear = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.getDayOfYear();
            }
        };    

    public final static LocalDateTimeEval Era = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.get(ChronoField.ERA);
            }
        };

    public final static LocalDateTimeEval HourOfDay = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.getHour();
            }
        };

    public final static LocalDateTimeEval MillisOfSecond = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.get(ChronoField.MILLI_OF_SECOND);
            }
        };

    public final static LocalDateTimeEval SecondOfMinute = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.getSecond();
            }
        };

    public final static LocalDateTimeEval Weekyear = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
            }
        };

    public final static LocalDateTimeEval Year = new LocalDateTimeEval() {
            public Object evaluateInternal(LocalDateTime ldt) {
                return ldt.getYear();
            }
        };
}
