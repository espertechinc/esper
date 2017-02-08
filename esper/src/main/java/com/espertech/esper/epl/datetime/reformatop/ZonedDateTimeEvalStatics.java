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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

public class ZonedDateTimeEvalStatics {

    public final static ZonedDateTimeEval MinuteOfHour = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.getMinute();
            }
        };

    public final static ZonedDateTimeEval MonthOfYear = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.getMonth();
            }
        };

    public final static ZonedDateTimeEval DayOfMonth = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.getDayOfMonth();
            }
        };

    public final static ZonedDateTimeEval DayOfWeek = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.getDayOfWeek();
            }
        };

    public final static ZonedDateTimeEval DayOfYear = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.getDayOfYear();
            }
        };    

    public final static ZonedDateTimeEval Era = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.get(ChronoField.ERA);
            }
        };

    public final static ZonedDateTimeEval HourOfDay = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.getHour();
            }
        };

    public final static ZonedDateTimeEval MillisOfSecond = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.get(ChronoField.MILLI_OF_SECOND);
            }
        };

    public final static ZonedDateTimeEval SecondOfMinute = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.getSecond();
            }
        };

    public final static ZonedDateTimeEval Weekyear = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
            }
        };

    public final static ZonedDateTimeEval Year = new ZonedDateTimeEval() {
            public Object evaluateInternal(ZonedDateTime ldt) {
                return ldt.getYear();
            }
        };
}
