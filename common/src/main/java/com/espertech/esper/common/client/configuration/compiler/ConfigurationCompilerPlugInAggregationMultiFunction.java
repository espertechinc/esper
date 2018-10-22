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

import java.io.Serializable;
import java.util.Map;

/**
 * Configuration information for plugging in a custom aggregation multi-function.
 */
public class ConfigurationCompilerPlugInAggregationMultiFunction implements Serializable {
    private static final long serialVersionUID = -1126332072916978240L;
    private String[] functionNames;
    private String multiFunctionForgeClassName;
    private Map<String, Object> additionalConfiguredProperties;

    /**
     * Ctor.
     */
    public ConfigurationCompilerPlugInAggregationMultiFunction() {
    }

    /**
     * Ctor.
     *
     * @param functionNames               the aggregation function names
     * @param multiFunctionForgeClassName the factory class name
     */
    public ConfigurationCompilerPlugInAggregationMultiFunction(String[] functionNames, String multiFunctionForgeClassName) {
        this.functionNames = functionNames;
        this.multiFunctionForgeClassName = multiFunctionForgeClassName;
    }

    /**
     * Returns aggregation function names.
     *
     * @return names
     */
    public String[] getFunctionNames() {
        return functionNames;
    }

    /**
     * Sets aggregation function names.
     *
     * @param functionNames names to set
     */
    public void setFunctionNames(String[] functionNames) {
        this.functionNames = functionNames;
    }

    /**
     * Returns the factory class name.
     *
     * @return class name
     */
    public String getMultiFunctionForgeClassName() {
        return multiFunctionForgeClassName;
    }

    /**
     * Sets the factory class name.
     *
     * @param multiFunctionForgeClassName class name
     */
    public void setMultiFunctionForgeClassName(String multiFunctionForgeClassName) {
        this.multiFunctionForgeClassName = multiFunctionForgeClassName;
    }

    /**
     * Returns a map of optional configuration properties, or null if none provided.
     *
     * @return additional optional properties
     */
    public Map<String, Object> getAdditionalConfiguredProperties() {
        return additionalConfiguredProperties;
    }

    /**
     * Sets a map of optional configuration properties, or null if none provided.
     *
     * @param additionalConfiguredProperties additional optional properties
     */
    public void setAdditionalConfiguredProperties(Map<String, Object> additionalConfiguredProperties) {
        this.additionalConfiguredProperties = additionalConfiguredProperties;
    }
}
