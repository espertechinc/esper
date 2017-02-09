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
package com.espertech.esper.type;

/**
 * Enumeration of units in a specification of schedule, which contains elements for each of the following units:
 * minute, hour, day of month, month, day of week and seconds.
 * Notice: value ranges are the same as the "crontab" standard values not the Java Calendar field values.
 * The Java Calendar MONTH value range is 0 to 11, while in this enum the range is 1 to 12.
 */
public enum ScheduleUnit {
    /**
     * Minute.
     */
    MINUTES(0, 59),

    /**
     * Hour.
     */
    HOURS(0, 23),

    /**
     * Day of month.
     */
    DAYS_OF_MONTH(1, 31),

    /**
     * Month.
     */
    MONTHS(1, 12),

    /**
     * Day of week.
     */
    DAYS_OF_WEEK(0, 6),

    /**
     * Second.
     */
    SECONDS(0, 59);

    private final int min;
    private final int max;

    ScheduleUnit(int min, int max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Returns minimum valid value for the unit.
     *
     * @return minimum unit value
     */
    public int min() {
        return min;
    }

    /**
     * Returns minimum valid value for the unit.
     *
     * @return maximum unit value
     */
    public int max() {
        return max;
    }
}

