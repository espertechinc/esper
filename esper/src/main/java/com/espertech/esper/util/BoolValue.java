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

import java.util.Locale;

public final class BoolValue {
    /**
     * Parse the boolean string.
     *
     * @param value is a bool value
     * @return parsed boolean
     */
    public static boolean parseString(String value) {
        if (!(value.toLowerCase(Locale.ENGLISH).equals("true")) && (!(value.toLowerCase(Locale.ENGLISH).equals("false")))) {
            throw new IllegalArgumentException("Boolean value '" + value + "' cannot be converted to boolean");
        }
        return Boolean.parseBoolean(value);
    }
}
