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
package com.espertech.esper.plugin;

import com.espertech.esper.client.ConfigurationPlugInAggregationMultiFunction;

/**
 * Context for use with {@link PlugInAggregationMultiFunctionFactory} provides
 * information about an aggregation function at the time of declaration.
 * <p>
 * Declaration means when the aggregation function is discovered at the time
 * of parsing an EPL statement. Or when using statement object model
 * then at the time of mapping the object model to the
 * internal statement representation.
 * </p>
 */
public class PlugInAggregationMultiFunctionDeclarationContext {

    private final String functionName;
    private final boolean distinct;
    private final String engineURI;
    private ConfigurationPlugInAggregationMultiFunction configuration;

    /**
     * Ctor.
     *
     * @param functionName  provides the aggregation multi-function name
     * @param distinct      flag whether the "distinct" keyword was provided.
     * @param engineURI     the engine URI
     * @param configuration the configuration provided when the aggregation multi-functions where registered
     */
    public PlugInAggregationMultiFunctionDeclarationContext(String functionName, boolean distinct, String engineURI, ConfigurationPlugInAggregationMultiFunction configuration) {
        this.functionName = functionName;
        this.distinct = distinct;
        this.engineURI = engineURI;
        this.configuration = configuration;
    }

    /**
     * Returns a flag whether the "distinct" keyword was provided.
     *
     * @return distinct flag
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Returns the engine uri.
     *
     * @return engine uri
     */
    public String getEngineURI() {
        return engineURI;
    }

    /**
     * Returns the aggregation function name.
     *
     * @return function name
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Returns the configuration provided when the aggregation multi-functions where registered.
     *
     * @return configuration
     */
    public ConfigurationPlugInAggregationMultiFunction getConfiguration() {
        return configuration;
    }
}
