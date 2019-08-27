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
package com.espertech.esper.common.internal.epl.util;

public enum EPLExpressionParamType {
    /**
     * Boolean-type parameter.
     */
    BOOLEAN,

    /**
     * Any numeric value.
     */
    NUMERIC,

    /**
     * Any value, ie. Object
     */
    ANY,

    /**
     * A specific class as indicated by a separate Class
     */
    SPECIFIC,

    /**
     * Time-period or number of seconds
     */
    TIME_PERIOD_OR_SEC,

    /**
     * Date-time value.
     */
    DATETIME;

    public Class getMethodParamType() {
        if (this == BOOLEAN) {
            return Boolean.class;
        }
        return Object.class;
    }
}
