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
package com.espertech.esper.regressionlib.support.bean;

import java.util.Map;

public class SupportEventTypeErasure {

    private String key;
    private int subkey;
    private Map<String, SupportEventInnerTypeWGetIds> innerTypes;
    private SupportEventInnerTypeWGetIds[] innerTypesArray;

    public SupportEventTypeErasure(String key, int subkey, Map<String, SupportEventInnerTypeWGetIds> innerTypes, SupportEventInnerTypeWGetIds[] innerTypesArray) {
        this.key = key;
        this.subkey = subkey;
        this.innerTypes = innerTypes;
        this.innerTypesArray = innerTypesArray;
    }

    public Map<String, SupportEventInnerTypeWGetIds> getInnerTypes() {
        return innerTypes;
    }

    public String getKey() {
        return key;
    }

    public int getSubkey() {
        return subkey;
    }

    public SupportEventInnerTypeWGetIds[] getInnerTypesArray() {
        return innerTypesArray;
    }
}
