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
package com.espertech.esper.core.service;

import java.util.Map;

public class ConfiguratorContext {
    private final String engineURI;
    private final Map<String, EPServiceProviderSPI> runtimes;

    public ConfiguratorContext(String engineURI, Map<String, EPServiceProviderSPI> runtimes) {
        this.engineURI = engineURI;
        this.runtimes = runtimes;
    }

    public String getEngineURI() {
        return engineURI;
    }

    public Map<String, EPServiceProviderSPI> getRuntimes() {
        return runtimes;
    }
}
