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
package com.espertech.esper.common.client.hook.datetimemethod;

/**
 * For adding a date-time method that modifies the date-time value and that return a result of the same type
 * as the date-time value.
 * <p>
 *     long-type and Date-type values are automatically converted from and to Calendar.
 * </p>
 */
public class DateTimeMethodOpsModify implements DateTimeMethodOps {
    private DateTimeMethodMode calendarOp;
    private DateTimeMethodMode ldtOp;
    private DateTimeMethodMode zdtOp;

    /**
     * Ctor.
     */
    public DateTimeMethodOpsModify() {
    }

    /**
     * Returns the information how calendar-modify is provided
     * @return mode
     */
    public DateTimeMethodMode getCalendarOp() {
        return calendarOp;
    }

    /**
     * Sets the information how calendar-modify is provided
     * @param calendarOp mode
     */
    public void setCalendarOp(DateTimeMethodMode calendarOp) {
        this.calendarOp = calendarOp;
    }

    /**
     * Returns the information how LocalDateTime-modify is provided
     * @return mode
     */
    public DateTimeMethodMode getLdtOp() {
        return ldtOp;
    }

    /**
     * Sets the information how LocalDateTime-modify is provided
     * @param ldtOp mode
     */
    public void setLdtOp(DateTimeMethodMode ldtOp) {
        this.ldtOp = ldtOp;
    }

    /**
     * Returns the information how ZonedDateTime-modify is provided
     * @return mode
     */
    public DateTimeMethodMode getZdtOp() {
        return zdtOp;
    }

    /**
     * Sets the information how ZonedDateTime-modify is provided
     * @param zdtOp mode
     */
    public void setZdtOp(DateTimeMethodMode zdtOp) {
        this.zdtOp = zdtOp;
    }
}
