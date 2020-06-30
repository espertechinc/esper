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

import java.util.HashMap;
import java.util.Map;

public class SupportBeanParameterizedWFieldSingleMapped<T> {
    private final Map<String, T> mapProperty;
    public final Map<String, T> mapField;

    public SupportBeanParameterizedWFieldSingleMapped(T value) {
        this.mapProperty = new HashMap<>();
        this.mapProperty.put("key", value);
        this.mapField = new HashMap<>();
        this.mapField.put("key", value);
    }

    public Map<String, T> mapProperty() {
        return mapProperty;
    }

    public T mapKeyed(String key) {
        return mapProperty.get(key);
    }
}
