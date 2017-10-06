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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Context for use with plug-in custom aggregation functions that implement {@link com.espertech.esper.client.hook.AggregationFunctionFactory}.
 * <p>
 * This context object provides access to the parameter expressions themselves as well
 * as information compiled from the parameter expressions for your convenience.
 */
public class AggregationValidationContext {
    private final Class[] parameterTypes;
    private final boolean[] isConstantValue;
    private final Object[] constantValues;
    private final boolean distinct;
    private final boolean windowed;
    private final ExprNode[] expressions;
    private final LinkedHashMap<String, List<ExprNode>> namedParameters;

    /**
     * Ctor.
     *
     * @param parameterTypes the type of each parameter expression.
     * @param constantValue  for each parameter expression an indicator whether the expression returns a constant result
     * @param constantValues for each parameter expression that returns a constant result this array contains the constant value
     * @param distinct       true if 'distinct' keyword was provided
     * @param windowed       true if all event properties references by parameter expressions are from streams that have data windows declared onto the stream or are from named windows
     * @param expressions    the parameter expressions themselves (positional parameters only)
     * @param namedParameters provided when there are named parameters, such as the "filter:expression" parameter
     */
    public AggregationValidationContext(Class[] parameterTypes, boolean[] constantValue, Object[] constantValues, boolean distinct, boolean windowed, ExprNode[] expressions, LinkedHashMap<String, List<ExprNode>> namedParameters) {
        this.parameterTypes = parameterTypes;
        this.isConstantValue = constantValue;
        this.constantValues = constantValues;
        this.distinct = distinct;
        this.windowed = windowed;
        this.expressions = expressions;
        this.namedParameters = namedParameters;
    }

    /**
     * The return type of each parameter expression.
     * <p>
     * This information can also be obtained by calling getType on each parameter expression.
     *
     * @return array providing result type of each parameter expression
     */
    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * A boolean indicator for each parameter expression that is true if the expression
     * returns a constant result or false if the expression result is not a constant value.
     * <p>
     * This information can also be obtained by calling isConstantResult on each parameter expression.
     *
     * @return array providing an indicator per parameter expression that the result is a constant value
     */
    public boolean[] getIsConstantValue() {
        return isConstantValue;
    }

    /**
     * If a parameter expression returns a constant value, the value of the constant it returns
     * is provided in this array.
     * <p>
     * This information can also be obtained by calling evaluate on each parameter expression
     * providing a constant value.
     *
     * @return array providing the constant return value per parameter expression that has constant result value, or null
     * if a parameter expression is deemded to not provide a constant result value
     */
    public Object[] getConstantValues() {
        return constantValues;
    }

    /**
     * Returns true to indicate that the 'distinct' keyword was specified for this aggregation function.
     *
     * @return distinct value indicator
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Returns true to indicate that all parameter expressions return event properties that originate from a stream that
     * provides a remove stream.
     *
     * @return windowed indicator
     */
    public boolean isWindowed() {
        return windowed;
    }

    /**
     * Returns the parameter expressions themselves for interrogation.
     *
     * @return parameter expressions
     */
    public ExprNode[] getExpressions() {
        return expressions;
    }

    /**
     * Returns any named parameters or null if there are no named parameters
     * @return named parameters
     */
    public LinkedHashMap<String, List<ExprNode>> getNamedParameters() {
        return namedParameters;
    }
}
