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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.internal.epl.expression.core.ExprNamedParameterNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationContext;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Context for use with {@link com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionForge} provides
 * information about an aggregation function at the time of validation.
 * <p>
 * At validation time the event type information, parameter expressions
 * and other statement-specific services are available.
 * </p>
 */
public class AggregationMultiFunctionValidationContext {
    private final String functionName;
    private final EventType[] eventTypes;
    private final ExprNode[] parameterExpressions;
    private final String statementName;
    private final ExprValidationContext validationContext;
    private final ConfigurationCompilerPlugInAggregationMultiFunction config;
    private final TableMetadataColumnAggregation optionalTableColumnRead;
    private final ExprNode[] allParameterExpressions;
    private final ExprNode optionalFilterExpression;

    /**
     * Ctor.
     *
     * @param functionName             function name
     * @param eventTypes               event types
     * @param parameterExpressions     expressions
     * @param statementName            statement name
     * @param validationContext        validation context
     * @param config                   configuration
     * @param optionalTableColumnRead  optional table column name
     * @param allParameterExpressions  all parameters
     * @param optionalFilterExpression optional filter parameter
     */
    public AggregationMultiFunctionValidationContext(String functionName, EventType[] eventTypes, ExprNode[] parameterExpressions, String statementName, ExprValidationContext validationContext, ConfigurationCompilerPlugInAggregationMultiFunction config, TableMetadataColumnAggregation optionalTableColumnRead, ExprNode[] allParameterExpressions, ExprNode optionalFilterExpression) {
        this.functionName = functionName;
        this.eventTypes = eventTypes;
        this.parameterExpressions = parameterExpressions;
        this.statementName = statementName;
        this.validationContext = validationContext;
        this.config = config;
        this.optionalTableColumnRead = optionalTableColumnRead;
        this.allParameterExpressions = allParameterExpressions;
        this.optionalFilterExpression = optionalFilterExpression;
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
    public ConfigurationCompilerPlugInAggregationMultiFunction getConfig() {
        return config;
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
     * Returns the filter expression when provided
     *
     * @return filter expression
     */
    public ExprNode getOptionalFilterExpression() {
        return optionalFilterExpression;
    }

    /**
     * Returns table column information when used with tables
     *
     * @return table column
     */
    public TableMetadataColumnAggregation getOptionalTableColumnRead() {
        return optionalTableColumnRead;
    }

    /**
     * Gets the named parameters as a list
     *
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
