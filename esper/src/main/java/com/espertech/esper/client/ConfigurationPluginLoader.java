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
package com.espertech.esper.client;

import java.io.Serializable;
import java.util.Properties;

/**
 * Holds configuration for a plugin such as an input/output adapter loader.
 */
public class ConfigurationPluginLoader implements Serializable {
    private String loaderName;
    private String className;
    private Properties configProperties;
    private String configurationXML;
    private static final long serialVersionUID = 6053550897594738083L;

    /**
     * Ctor.
     */
    public ConfigurationPluginLoader() {
    }

    /**
     * Returns the loader class name.
     *
     * @return class name of loader
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the loader classname.
     *
     * @param className of loader
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns loader configuration properties.
     *
     * @return config entries
     */
    public Properties getConfigProperties() {
        return configProperties;
    }

    /**
     * Sets the loader configuration.
     *
     * @param configProperties is the configuration of the loader
     */
    public void setConfigProperties(Properties configProperties) {
        this.configProperties = configProperties;
    }

    /**
     * Returns the loader name.
     *
     * @return loader name
     */
    public String getLoaderName() {
        return loaderName;
    }

    /**
     * Sets the loader name.
     *
     * @param loaderName is the loader name
     */
    public void setLoaderName(String loaderName) {
        this.loaderName = loaderName;
    }

    /**
     * Returns configuration XML for the plugin.
     *
     * @return xml
     */
    public String getConfigurationXML() {
        return configurationXML;
    }

    /**
     * Sets configuration XML for the plugin.
     *
     * @param configurationXML xml to set
     */
    public void setConfigurationXML(String configurationXML) {
        this.configurationXML = configurationXML;
    }

    public String toString() {
        return "ConfigurationPluginLoader name '" + loaderName + "' class '" + className + " ' properties '" + configProperties + "'";
    }
}
