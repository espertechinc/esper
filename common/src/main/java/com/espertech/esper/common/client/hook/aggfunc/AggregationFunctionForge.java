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
package com.espertech.esper.common.client.hook.aggfunc;

import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

/**
 * Compile-time representation of a plug-in aggregation function.
 */
public interface AggregationFunctionForge {
    /**
     * Sets the EPL function name assigned to the factory.
     *
     * @param functionName assigned
     */
    void setFunctionName(String functionName);

    /**
     * Implemented by plug-in aggregation functions to allow such functions to validate the
     * type of values passed to the function at statement compile time and to generally
     * interrogate parameter expressions.
     *
     * @param validationContext expression information
     * @throws ExprValidationException for validation exception
     */
    void validate(AggregationFunctionValidationContext validationContext) throws ExprValidationException;

    /**
     * Returns the type of the current value.
     *
     * @return type of value returned by the aggregation methods
     */
    Class getValueType();

    /**
     * Describes to the compiler how it should manage code for the aggregation function.
     *
     * @return mode object
     */
    AggregationFunctionMode getAggregationFunctionMode();
}
