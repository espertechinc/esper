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

/**
 * Configuration information for plugging in a custom view.
 */
public class ConfigurationPlugInVirtualDataWindow implements Serializable {
    private static final long serialVersionUID = 2402714694249103765L;

    private String namespace;
    private String name;
    private String factoryClassName;
    private Serializable config;

    /**
     * Ctor.
     */
    public ConfigurationPlugInVirtualDataWindow() {
    }

    /**
     * Returns the namespace
     *
     * @return namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the view name.
     *
     * @return view name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the view factory class name.
     *
     * @return factory class name
     */
    public String getFactoryClassName() {
        return factoryClassName;
    }

    /**
     * Sets the view namespace.
     *
     * @param namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Sets the view name.
     *
     * @param name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the view factory class name.
     *
     * @param factoryClassName is the class name of the view factory
     */
    public void setFactoryClassName(String factoryClassName) {
        this.factoryClassName = factoryClassName;
    }

    /**
     * Returns any additional configuration passed to the factory as part of the context.
     *
     * @return optional additional configuration
     */
    public Serializable getConfig() {
        return config;
    }

    /**
     * Sets any additional configuration passed to the factory as part of the context.
     *
     * @param config optional additional configuration
     */
    public void setConfig(Serializable config) {
        this.config = config;
    }
}
