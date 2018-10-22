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
package com.espertech.esper.common.client.util;

/**
 * Enumeration of different resolution styles for resolving property names.
 */
public enum PropertyResolutionStyle {
    /**
     * Properties are only matched if the names are identical in name
     * and case to the original property name.
     */
    CASE_SENSITIVE,

    /**
     * Properties are matched if the names are identical.  A case insensitive
     * search is used and will choose the first property that matches
     * the name exactly or the first property that matches case insensitively
     * should no match be found.
     */
    CASE_INSENSITIVE,

    /**
     * Properties are matched if the names are identical.  A case insensitive
     * search is used and will choose the first property that matches
     * the name exactly case insensitively.  If more than one 'name' can be
     * mapped to the property an exception is thrown.
     */
    DISTINCT_CASE_INSENSITIVE;

    /**
     * Returns the default property resolution style.
     *
     * @return is the case-sensitive resolution
     */
    public static PropertyResolutionStyle getDefault() {
        return CASE_SENSITIVE;
    }
}
