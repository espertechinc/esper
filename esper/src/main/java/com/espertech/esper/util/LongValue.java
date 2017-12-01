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
package com.espertech.esper.util;

public final class LongValue {
    /**
     * Parse the string containing a long value.
     *
     * @param value is the textual long value
     * @return long value
     */
    public static long parseString(String value) {
        if ((value.endsWith("L")) || ((value.endsWith("l")))) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.startsWith("+")) {
            value = value.substring(1);
        }
        return Long.parseLong(value);
    }
}
