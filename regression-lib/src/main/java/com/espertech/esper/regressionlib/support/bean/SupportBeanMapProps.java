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

import java.io.Serializable;
import java.util.Map;

public class SupportBeanMapProps implements Serializable {
    private final Map<String, String> properties;

    public SupportBeanMapProps(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
