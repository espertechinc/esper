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

import java.util.Arrays;

/**
 * Type widner that coerces from String to char if required.
 */
public class TypeWidenerObjectArrayToCollectionCoercer implements TypeWidener {
    public Object widen(Object input) {
        return input == null ? null : Arrays.asList((Object[]) input);
    }
}
