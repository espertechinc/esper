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
package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.client.util.TimePeriod;
import com.espertech.esper.epl.methodbase.DotMethodFP;
import com.espertech.esper.epl.methodbase.DotMethodFPInputEnum;
import com.espertech.esper.epl.methodbase.DotMethodFPParam;
import com.espertech.esper.epl.methodbase.DotMethodFPParamTypeEnum;

public class DatetimeMethodEnumParams {

    public static final DotMethodFP[] WITHTIME = new DotMethodFP[] {
                new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                        new DotMethodFPParam("an integer-type hour", DotMethodFPParamTypeEnum.SPECIFIC, Integer.class),
                        new DotMethodFPParam("an integer-type minute", DotMethodFPParamTypeEnum.SPECIFIC, Integer.class),
                        new DotMethodFPParam("an integer-type second", DotMethodFPParamTypeEnum.SPECIFIC, Integer.class),
                        new DotMethodFPParam("an integer-type millis", DotMethodFPParamTypeEnum.SPECIFIC, Integer.class))
            };

    public static final DotMethodFP[] WITHDATE = new DotMethodFP[] {
                new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                        new DotMethodFPParam("an integer-type year", DotMethodFPParamTypeEnum.SPECIFIC, Integer.class),
                        new DotMethodFPParam("an integer-type month", DotMethodFPParamTypeEnum.SPECIFIC, Integer.class),
                        new DotMethodFPParam("an integer-type day", DotMethodFPParamTypeEnum.SPECIFIC, Integer.class))
            };

    public static final DotMethodFP[] PLUSMINUS = new DotMethodFP[] {
                new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                        new DotMethodFPParam(0, "a numeric-type millisecond", DotMethodFPParamTypeEnum.NUMERIC)),
                new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                        new DotMethodFPParam("a time period", DotMethodFPParamTypeEnum.SPECIFIC, TimePeriod.class))
            };

    public static final DotMethodFP[] CALFIELD = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam("a string-type calendar field name", DotMethodFPParamTypeEnum.SPECIFIC, String.class)),
            };

    public static final DotMethodFP[] CALFIELD_PLUS_INT = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam("a string-type calendar field name", DotMethodFPParamTypeEnum.SPECIFIC, String.class),
                            new DotMethodFPParam("an integer-type value", DotMethodFPParamTypeEnum.SPECIFIC, Integer.class)),
            };

    public static final DotMethodFP[] NOPARAM = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY)
            };

    public static final DotMethodFP[] BETWEEN = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam("a date-time type", DotMethodFPParamTypeEnum.DATETIME, null),
                            new DotMethodFPParam("a date-time type", DotMethodFPParamTypeEnum.DATETIME, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam("a date-time type", DotMethodFPParamTypeEnum.DATETIME, null),
                            new DotMethodFPParam("a date-time type", DotMethodFPParamTypeEnum.DATETIME, null),
                            new DotMethodFPParam("boolean", DotMethodFPParamTypeEnum.BOOLEAN, null),
                            new DotMethodFPParam("boolean", DotMethodFPParamTypeEnum.BOOLEAN, null)),
            };

    /**
     * Interval.
     */

    public static final String INPUT_INTERVAL = "timestamp or timestamped-event";
    public static final String INPUT_INTERVAL_START = "interval start value";
    public static final String INPUT_INTERVAL_FINISHES = "interval finishes value";

    public static final DotMethodFP[] INTERVAL_BEFORE_AFTER = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                        new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                        new DotMethodFPParam(INPUT_INTERVAL_START, DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                        new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                        new DotMethodFPParam(INPUT_INTERVAL_START, DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null),
                        new DotMethodFPParam(INPUT_INTERVAL_FINISHES, DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null))
                    };

    public static final DotMethodFP[] INTERVAL_COINCIDES = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                        new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                        new DotMethodFPParam("threshold for start and end value", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                        new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                        new DotMethodFPParam("threshold for start value", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null),
                        new DotMethodFPParam("threshold for end value", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null))
                    };

    public static final DotMethodFP[] INTERVAL_DURING_INCLUDES = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                            new DotMethodFPParam("maximum distance interval both start and end", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                            new DotMethodFPParam("minimum distance interval both start and end", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null),
                            new DotMethodFPParam("maximum distance interval both start and end", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                            new DotMethodFPParam("minimum distance start", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null),
                            new DotMethodFPParam("maximum distance start", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null),
                            new DotMethodFPParam("minimum distance end", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null),
                            new DotMethodFPParam("maximum distance end", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    };

    public static final DotMethodFP[] INTERVAL_DURING_OVERLAPS_OVERLAPBY = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                            new DotMethodFPParam("maximum distance interval both start and end", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                            new DotMethodFPParam("minimum distance interval both start and end", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null),
                            new DotMethodFPParam("maximum distance interval both start and end", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    };

    public static final DotMethodFP[] INTERVAL_FINISHES_FINISHEDBY = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                            new DotMethodFPParam("maximum distance between end timestamps", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    };

    public static final DotMethodFP[] INTERVAL_STARTS_STARTEDBY = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                            new DotMethodFPParam("maximum distance between start timestamps", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    };

    public static final DotMethodFP[] INTERVAL_MEETS_METBY = new DotMethodFP[] {
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null)),
                    new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY,
                            new DotMethodFPParam(INPUT_INTERVAL, DotMethodFPParamTypeEnum.ANY, null),
                            new DotMethodFPParam("maximum distance between start and end timestamps", DotMethodFPParamTypeEnum.TIME_PERIOD_OR_SEC, null)),
                    };
}
