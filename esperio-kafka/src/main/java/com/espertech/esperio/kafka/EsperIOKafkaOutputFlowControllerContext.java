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
package com.espertech.esperio.kafka;

import com.espertech.esper.client.EPServiceProvider;

import java.util.Properties;

public class EsperIOKafkaOutputFlowControllerContext {
    private final EPServiceProvider engine;
    private final Properties properties;

    public EsperIOKafkaOutputFlowControllerContext(EPServiceProvider engine, Properties properties) {
        this.engine = engine;
        this.properties = properties;
    }

    public EPServiceProvider getEngine() {
        return engine;
    }

    public Properties getProperties() {
        return properties;
    }
}
