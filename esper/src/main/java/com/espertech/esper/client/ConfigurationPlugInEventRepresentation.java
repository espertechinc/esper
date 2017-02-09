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
 * Configuration object for plug-in event representations.
 */
public class ConfigurationPlugInEventRepresentation implements Serializable {
    private String eventRepresentationClassName;
    private Serializable initializer;
    private static final long serialVersionUID = 4548096417918791735L;

    /**
     * Returns the class name of the class providing the pluggable event representation.
     *
     * @return class name of class implementing {@link com.espertech.esper.plugin.PlugInEventRepresentation}
     */
    public String getEventRepresentationClassName() {
        return eventRepresentationClassName;
    }

    /**
     * Sets the class name of the class providing the pluggable event representation.
     *
     * @param factoryClassName class name of class implementing {@link com.espertech.esper.plugin.PlugInEventRepresentation}
     */
    public void setEventRepresentationClassName(String factoryClassName) {
        this.eventRepresentationClassName = factoryClassName;
    }

    /**
     * Returns the optional initialization or configuration information for the plug-in event representation.
     *
     * @return any configuration object specific to the event representation, or a XML string
     * if supplied via configuration XML file, or null if none supplied
     */
    public Serializable getInitializer() {
        return initializer;
    }

    /**
     * Sets the optional initialization or configuration information for the plug-in event representation.
     *
     * @param initializer any configuration object specific to the event representation, or a XML string
     *                    if supplied via configuration XML file, or null if none to supply
     */
    public void setInitializer(Serializable initializer) {
        this.initializer = initializer;
    }
}
