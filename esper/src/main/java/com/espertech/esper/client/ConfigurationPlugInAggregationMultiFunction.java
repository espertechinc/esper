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
import java.util.Map;

/**
 * Configuration information for plugging in a custom aggregation multi-function.
 */
public class ConfigurationPlugInAggregationMultiFunction implements Serializable {
    private static final long serialVersionUID = -1126332072916978240L;
    private String[] functionNames;
    private String multiFunctionFactoryClassName;
    private Map<String, Object> additionalConfiguredProperties;

    /**
     * Ctor.
     */
    public ConfigurationPlugInAggregationMultiFunction() {
    }

    /**
     * Ctor.
     *
     * @param functionNames                 the aggregation function names
     * @param multiFunctionFactoryClassName the factory class name
     */
    public ConfigurationPlugInAggregationMultiFunction(String[] functionNames, String multiFunctionFactoryClassName) {
        this.functionNames = functionNames;
        this.multiFunctionFactoryClassName = multiFunctionFactoryClassName;
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
    public String getMultiFunctionFactoryClassName() {
        return multiFunctionFactoryClassName;
    }

    /**
     * Sets the factory class name.
     *
     * @param multiFunctionFactoryClassName class name
     */
    public void setMultiFunctionFactoryClassName(String multiFunctionFactoryClassName) {
        this.multiFunctionFactoryClassName = multiFunctionFactoryClassName;
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
