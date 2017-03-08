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
package com.espertech.esper.epl.db;

import com.espertech.esper.collection.MultiKeyUntyped;

public class DataCacheUtil {
    public static Object getLookupKey(Object[] methodParams, int numInputParameters) {
        if (numInputParameters == 0) {
            return Object.class;
        } else if (numInputParameters == 1) {
            return methodParams[0];
        } else {
            if (methodParams.length == numInputParameters) {
                return new MultiKeyUntyped(methodParams);
            }
            Object[] lookupKeys = new Object[numInputParameters];
            System.arraycopy(methodParams, 0, lookupKeys, 0, numInputParameters);
            return new MultiKeyUntyped(lookupKeys);
        }
    }
}
