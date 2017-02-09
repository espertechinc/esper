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

/**
 * Type widner that coerces from String to char if required.
 */
public class TypeWidenerStringToCharCoercer implements TypeWidener {
    public Object widen(Object input) {
        String result = input.toString();
        if ((result != null) && (result.length() > 0)) {
            return result.charAt(0);
        }
        return null;
    }
}
