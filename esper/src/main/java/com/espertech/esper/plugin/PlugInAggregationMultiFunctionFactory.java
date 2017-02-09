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

import java.io.Serializable;

/**
 * Entry point for the extension API for aggregation multi-functions.
 * <p>
 * This API allows adding one or more related aggregation functions that can share state,
 * share parameters or exhibit related behavior.
 * </p>
 * <p>
 * Please use {@link com.espertech.esper.client.ConfigurationPlugInAggregationMultiFunction}
 * to register this factory class in the engine together with one or more function names.
 * </p>
 * <p>
 * The engine instantiates a single instance of this class at the time it encounters the first
 * aggregation multi-function in a given statement at the time of statement parsing or
 * compilation from statement object model.
 * </p>
 * <p>
 * At the time of statement parsing, each aggregation multi-function encountered during parsing
 * of EPL statement text results in an invocation to {@link #addAggregationFunction(PlugInAggregationMultiFunctionDeclarationContext)}. The same
 * upon statement compilation for statement object model.
 * For multiple aggregation functions, the order in which such calls occur is not well defined
 * and should not be relied on by the implementation.
 * </p>
 * <p>
 * The engine invokes {@link #validateGetHandler(PlugInAggregationMultiFunctionValidationContext)}
 * at the time of expression node validation. Validation occurs after statement parsing
 * and when type information is established.
 * For multiple aggregation functions, the order in which such calls occur is not well defined
 * and should not be relied on by the implementation.
 * </p>
 * <p>
 * Usually a single {@link PlugInAggregationMultiFunctionHandler} handler class can handle the needs
 * of all related aggregation functions.
 * Usually you have a single handler class and return one handler object for each
 * aggregation function expression, where the handler object takes the validation context as a parameter.
 * Use multiple different handler classes when your aggregation
 * functions have sufficiently different execution contexts or behaviors. Your application may want to use the
 * expression and type information available in
 * {@link PlugInAggregationMultiFunctionValidationContext} to decide what behavior to provide.
 * </p>
 * <p>
 * The function class must be Serializable only when used with EsperHA.
 * </p>
 */
public interface PlugInAggregationMultiFunctionFactory extends Serializable {
    /**
     * Called for each instance of use of any of the aggregation functions at declaration discovery time
     * and before any expression validation takes place.
     *
     * @param declarationContext context
     */
    public void addAggregationFunction(PlugInAggregationMultiFunctionDeclarationContext declarationContext);

    /**
     * Called for each instance of use of any of the aggregation functions at validation time
     * after all declared aggregation have been added.
     *
     * @param validationContext validationContext
     * @return handler for providing type information, accessor and provider factory
     */
    public PlugInAggregationMultiFunctionHandler validateGetHandler(PlugInAggregationMultiFunctionValidationContext validationContext);
}
