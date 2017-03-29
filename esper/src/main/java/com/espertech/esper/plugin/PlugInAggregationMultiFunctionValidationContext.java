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
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprNamedParameterNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationContext;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Context for use with {@link PlugInAggregationMultiFunctionFactory} provides
 * information about an aggregation function at the time of validation.
 * <p>
 * At validation time the event type information, parameter expressions
 * and other statement-specific services are available.
 * </p>
 */
public class PlugInAggregationMultiFunctionValidationContext {
    private final String functionName;
    private final EventType[] eventTypes;
    private final ExprNode[] parameterExpressions;
    private final String engineURI;
    private final String statementName;
    private final ExprValidationContext validationContext;
    private final ConfigurationPlugInAggregationMultiFunction config;
    private final TableMetadataColumnAggregation optionalTableColumnAccessed;
    private final ExprNode[] allParameterExpressions;

    public PlugInAggregationMultiFunctionValidationContext(String functionName, EventType[] eventTypes, ExprNode[] parameterExpressions, String engineURI, String statementName, ExprValidationContext validationContext, ConfigurationPlugInAggregationMultiFunction config, TableMetadataColumnAggregation optionalTableColumnAccessed, ExprNode[] allParameterExpressions) {
        this.functionName = functionName;
        this.eventTypes = eventTypes;
        this.parameterExpressions = parameterExpressions;
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.validationContext = validationContext;
        this.config = config;
        this.optionalTableColumnAccessed = optionalTableColumnAccessed;
        this.allParameterExpressions = allParameterExpressions;
    }

    /**
     * Returns the aggregation function name
     *
     * @return aggregation function name
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Returns the event types of all events in the select clause
     *
     * @return types
     */
    public EventType[] getEventTypes() {
        return eventTypes;
    }

    /**
     * Returns positional parameters expressions to this aggregation function.
     * Use {@link #getAllParameterExpressions()} for a list of all parameters including non-positional parameters.
     *
     * @return positional parameter expressions
     */
    public ExprNode[] getParameterExpressions() {
        return parameterExpressions;
    }

    /**
     * Returns the engine URI.
     *
     * @return engine URI.
     */
    public String getEngineURI() {
        return engineURI;
    }

    /**
     * Returns the statement name.
     *
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns additional validation contextual services.
     *
     * @return validation context
     */
    public ExprValidationContext getValidationContext() {
        return validationContext;
    }

    /**
     * Returns the original configuration object for the aggregation multi-function
     *
     * @return config
     */
    public ConfigurationPlugInAggregationMultiFunction getConfig() {
        return config;
    }

    public TableMetadataColumnAggregation getOptionalTableColumnAccessed() {
        return optionalTableColumnAccessed;
    }

    /**
     * Returns positional and non-positional parameters.
     *
     * @return all parameters
     */
    public ExprNode[] getAllParameterExpressions() {
        return allParameterExpressions;
    }

    /**
     * Gets the named parameters as a list
     * @return named params
     */
    public LinkedHashMap<String, List<ExprNode>> getNamedParameters() {
        LinkedHashMap<String, List<ExprNode>> named = new LinkedHashMap<>();
        for (ExprNode node : allParameterExpressions) {
            if (node instanceof ExprNamedParameterNode) {
                ExprNamedParameterNode namedNode = (ExprNamedParameterNode) node;
                named.put(namedNode.getParameterName(), Arrays.asList(namedNode.getChildNodes()));
            }
        }
        return named;
    }
}
