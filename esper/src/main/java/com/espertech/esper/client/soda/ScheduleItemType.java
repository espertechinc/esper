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
package com.espertech.esper.client.soda;

/**
 * Type of schedule item.
 */
public enum ScheduleItemType {
    /**
     * Wildcard means any value.
     */
    WILDCARD("*"),

    /**
     * Last day of week or month.
     */
    LASTDAY("last"),

    /**
     * Weekday (nearest to a date)
     */
    WEEKDAY("weekday"),

    /**
     * Last weekday in a month
     */
    LASTWEEKDAY("lastweekday");

    private String syntax;

    private ScheduleItemType(String s) {
        this.syntax = s;
    }

    /**
     * Returns the syntax string.
     *
     * @return syntax
     */
    public String getSyntax() {
        return syntax;
    }
}
