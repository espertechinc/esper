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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Properties;

public class ExecEventMapPropertyConfig implements RegressionExecution {
    public void configure(Configuration configuration) {
        Properties properties = new Properties();
        properties.put("myInt", int.class.getName());
        properties.put("byteArr", byte[].class.getName());
        properties.put("myInt2", "int");
        properties.put("double", "double");
        properties.put("boolean", "boolean");
        properties.put("long", "long");
        properties.put("astring", "string");
        configuration.addEventType("MyPrimMapEvent", properties);
    }

    public void run(EPServiceProvider epService) {
        // no assertions required
    }
}
