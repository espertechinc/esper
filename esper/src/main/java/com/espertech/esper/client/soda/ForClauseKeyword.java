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
 * Keywords for use in the for-clause.
 */
public enum ForClauseKeyword {
    /**
     * Grouped delivery - listener receives invocation per group.
     */
    GROUPED_DELIVERY("grouped_delivery"),

    /**
     * Discrete delivery - listener receives invocation per event.
     */
    DISCRETE_DELIVERY("discrete_delivery");

    private final String name;

    private ForClauseKeyword(String name) {
        this.name = name;
    }

    /**
     * Returns for-keyword.
     *
     * @return keyword
     */
    public String getName() {
        return name;
    }
}