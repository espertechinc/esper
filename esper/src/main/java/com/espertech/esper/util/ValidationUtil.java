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

import com.espertech.esper.client.EPException;

public class ValidationUtil {
    public static void validateRequiredPropString(String value, String operatorName, String propertyName) throws EPException {
        if (value == null || value.trim().length() == 0) {
            throw getRequiredPropException(propertyName, operatorName);
        }
    }

    public static void validateRequiredProp(Object value, String operatorName, String propertyName) throws EPException {
        if (value == null) {
            throw getRequiredPropException(propertyName, operatorName);
        }
    }

    private static EPException getRequiredPropException(String propertyName, String operatorName) {
        return new EPException("Required property '" + propertyName + "' for operator " + operatorName + "' is not provided");
    }
}
