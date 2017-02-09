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
public class ConfigurationPlugInView implements Serializable {
    private String namespace;
    private String name;
    private String factoryClassName;
    private static final long serialVersionUID = 7000210715294863718L;

    /**
     * Ctor.
     */
    public ConfigurationPlugInView() {
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
}
