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
package com.espertech.esper.common.client.hook.aggmultifunc;

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;

/**
 * Context for use with {@link AggregationMultiFunctionForge} provides
 * information about an aggregation function at the time of declaration.
 * <p>
 * Declaration means when the aggregation function is discovered at the time
 * of parsing an EPL statement. Or when using statement object model
 * then at the time of mapping the object model to the
 * internal statement representation.
 * </p>
 */
public class AggregationMultiFunctionDeclarationContext {

    private final String functionName;
    private final boolean distinct;
    private ConfigurationCompilerPlugInAggregationMultiFunction configuration;

    /**
     * Ctor.
     *
     * @param functionName  provides the aggregation multi-function name
     * @param distinct      flag whether the "distinct" keyword was provided.
     * @param configuration the configuration provided when the aggregation multi-functions where registered
     */
    public AggregationMultiFunctionDeclarationContext(String functionName, boolean distinct, ConfigurationCompilerPlugInAggregationMultiFunction configuration) {
        this.functionName = functionName;
        this.distinct = distinct;
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
    public ConfigurationCompilerPlugInAggregationMultiFunction getConfiguration() {
        return configuration;
    }
}
