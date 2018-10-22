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
package com.espertech.esper.common.client.configuration.compiler;

import com.espertech.esper.common.client.util.PatternObjectType;

import java.io.Serializable;

/**
 * Configuration information for plugging in a custom view.
 */
public class ConfigurationCompilerPlugInPatternObject implements Serializable {
    private String namespace;
    private String name;
    private String forgeClassName;
    private PatternObjectType patternObjectType;
    private static final long serialVersionUID = -9206572934368025423L;

    /**
     * Ctor.
     */
    public ConfigurationCompilerPlugInPatternObject() {
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
    public String getForgeClassName() {
        return forgeClassName;
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
     * @param forgeClassName is the class name of the view factory
     */
    public void setForgeClassName(String forgeClassName) {
        this.forgeClassName = forgeClassName;
    }

    /**
     * Returns an object type of the pattern object plug-in.
     *
     * @return pattern object type
     */
    public PatternObjectType getPatternObjectType() {
        return patternObjectType;
    }

    /**
     * Set the type of pattern object for plug-in.
     *
     * @param patternObjectType is the object type to set
     */
    public void setPatternObjectType(PatternObjectType patternObjectType) {
        this.patternObjectType = patternObjectType;
    }

}
