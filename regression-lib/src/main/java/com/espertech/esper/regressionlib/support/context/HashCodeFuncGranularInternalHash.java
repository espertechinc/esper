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
package com.espertech.esper.regressionlib.support.context;

public class HashCodeFuncGranularInternalHash implements HashCodeFunc {
    private int granularity;

    public HashCodeFuncGranularInternalHash(int granularity) {
        this.granularity = granularity;
    }

    public int codeFor(String key) {
        return key.hashCode() % granularity;
    }
}
