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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import java.util.Locale;

/**
 * Enum for aggregation multi-function state type.
 */
public enum AggregationAccessorLinearType {
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

    public static AggregationAccessorLinearType fromString(String text) {
        String compare = text.trim().toUpperCase(Locale.ENGLISH);
        for (AggregationAccessorLinearType type : AggregationAccessorLinearType.values()) {
            if (compare.equals(type.name())) {
                return type;
            }
        }
        return null;
    }
}