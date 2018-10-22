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
package com.espertech.esper.runtime.client.plugin;

import com.espertech.esper.runtime.client.EPRuntime;

import java.util.Properties;

/**
 * Context for plugin initialization.
 */
public class PluginLoaderInitContext {
    private final String name;
    private final Properties properties;
    private final String configXml;
    private final EPRuntime runtime;

    /**
     * Initialization context for use with the adapter loader.
     *
     * @param name       is the loader name
     * @param properties is a set of properties from the configuration
     * @param runtime    is the SPI of the runtimeitself for sending events to
     * @param configXml  config xml
     */
    public PluginLoaderInitContext(String name, Properties properties, String configXml, EPRuntime runtime) {
        this.name = name;
        this.properties = properties;
        this.configXml = configXml;
        this.runtime = runtime;
    }

    /**
     * Returns plugin name.
     *
     * @return plugin name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns plugin properties.
     *
     * @return plugin properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Returns plugin configuration XML, if any.
     *
     * @return configuration XML
     */
    public String getConfigXml() {
        return configXml;
    }

    /**
     * Returns the runtimeloading the plugin.
     *
     * @return runtime
     */
    public EPRuntime getRuntime() {
        return runtime;
    }
}
