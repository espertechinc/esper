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
package com.espertech.esper.epl.agg.access;

import java.util.Locale;

/**
 * Enum for aggregation multi-function state type.
 */
public enum AggregationStateType {
    /**
     * For "first" function.
     */
    FIRST,
    /**
     * For "last" function.
     */
    LAST,
    /**
     * For "window" function.
     */
    WINDOW;

    public static AggregationStateType fromString(String text) {
        String compare = text.trim().toUpperCase(Locale.ENGLISH);
        for (AggregationStateType type : AggregationStateType.values()) {
            if (compare.equals(type.name())) {
                return type;
            }
        }
        return null;
    }
}