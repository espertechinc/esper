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
package com.espertech.esper.epl.spec;

import java.io.Serializable;
import java.util.Map;

public class GraphOperatorDetail implements Serializable {
    private static final long serialVersionUID = 8897181288959361822L;
    private final Map<String, Object> configs;

    public GraphOperatorDetail(Map<String, Object> configs) {
        this.configs = configs;
    }

    public Map<String, Object> getConfigs() {
        return configs;
    }
}
