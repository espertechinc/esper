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
 * For adding a date-time method that reformats the date-time value returning a result of a different type
 * as the date-time value.
 * <p>
 *     Make sure to set a return type.
 * </p>
 */
public class DateTimeMethodOpsReformat implements DateTimeMethodOps {
    private Class returnType;
    private DateTimeMethodMode longOp;
    private DateTimeMethodMode dateOp;
    private DateTimeMethodMode calendarOp;
    private DateTimeMethodMode ldtOp;
    private DateTimeMethodMode zdtOp;

    /**
     * Ctor.
     */
    public DateTimeMethodOpsReformat() {
    }

    /**
     * Returns the information how calendar-reformat is provided
     * @return mode
     */
    public DateTimeMethodMode getCalendarOp() {
        return calendarOp;
    }

    /**
     * Sets the information how calendar-reformat is provided
     * @param calendarOp mode
     */
    public void setCalendarOp(DateTimeMethodMode calendarOp) {
        this.calendarOp = calendarOp;
    }

    /**
     * Returns the information how LocalDateTime-reformat is provided
     * @return mode
     */
    public DateTimeMethodMode getLdtOp() {
        return ldtOp;
    }

    /**
     * Sets the information how LocalDateTime-reformat is provided
     * @param ldtOp mode
     */
    public void setLdtOp(DateTimeMethodMode ldtOp) {
        this.ldtOp = ldtOp;
    }

    /**
     * Returns the information how ZonedDateTime-reformat is provided
     * @return mode
     */
    public DateTimeMethodMode getZdtOp() {
        return zdtOp;
    }

    /**
     * Sets the information how ZonedDateTime-reformat is provided
     * @param zdtOp mode
     */
    public void setZdtOp(DateTimeMethodMode zdtOp) {
        this.zdtOp = zdtOp;
    }

    /**
     * Returns the information how long-reformat is provided
     * @return mode
     */
    public DateTimeMethodMode getLongOp() {
        return longOp;
    }

    /**
     * Sets the information how long-reformat is provided
     * @param longOp mode
     */
    public void setLongOp(DateTimeMethodMode longOp) {
        this.longOp = longOp;
    }

    /**
     * Returns the information how Date-reformat is provided
     * @return mode
     */
    public DateTimeMethodMode getDateOp() {
        return dateOp;
    }

    /**
     * Sets the information how Date-reformat is provided
     * @param dateOp mode
     */
    public void setDateOp(DateTimeMethodMode dateOp) {
        this.dateOp = dateOp;
    }

    /**
     * Returns the return type.
     * @return return type
     */
    public Class getReturnType() {
        return returnType;
    }

    /**
     * Sets the return type.
     * @param returnType return type
     */
    public void setReturnType(Class returnType) {
        this.returnType = returnType;
    }
}
