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
import java.math.MathContext;

/**
 * Expression evaluation settings in the runtime are for results of expressions.
 */
public class ConfigurationCompilerExpression implements Serializable {
    private static final long serialVersionUID = 3247294506900217035L;
    private boolean integerDivision;
    private boolean divisionByZeroReturnsNull;
    private boolean udfCache;
    private boolean extendedAggregation;
    private boolean duckTyping;
    private MathContext mathContext;

    /**
     * Ctor.
     */
    public ConfigurationCompilerExpression() {
        integerDivision = false;
        divisionByZeroReturnsNull = false;
        udfCache = true;
        extendedAggregation = true;
    }

    /**
     * Returns false (the default) for integer division returning double values.
     * <p>
     * Returns true to signal that Java-convention integer division semantics
     * are used for divisions, whereas the division between two non-FP numbers
     * returns only the whole number part of the result and any fractional part is dropped.
     *
     * @return indicator
     */
    public boolean isIntegerDivision() {
        return integerDivision;
    }

    /**
     * Set to false (default) for integer division returning double values.
     * Set to true to signal the Java-convention integer division semantics
     * are used for divisions, whereas the division between two non-FP numbers
     * returns only the whole number part of the result and any fractional part is dropped.
     *
     * @param integerDivision true for integer division returning integer, false (default) for
     */
    public void setIntegerDivision(boolean integerDivision) {
        this.integerDivision = integerDivision;
    }

    /**
     * Returns false (default) when division by zero returns Double.Infinity.
     * Returns true when division by zero return null.
     * <p>
     * If integer devision is set, then division by zero for non-FP operands also returns null.
     *
     * @return indicator for division-by-zero results
     */
    public boolean isDivisionByZeroReturnsNull() {
        return divisionByZeroReturnsNull;
    }

    /**
     * Set to false (default) to have division by zero return Double.Infinity.
     * Set to true to have division by zero return null.
     * <p>
     * If integer division is set, then division by zero for non-FP operands also returns null.
     *
     * @param divisionByZeroReturnsNull indicator for division-by-zero results
     */
    public void setDivisionByZeroReturnsNull(boolean divisionByZeroReturnsNull) {
        this.divisionByZeroReturnsNull = divisionByZeroReturnsNull;
    }

    /**
     * By default true, indicates that user-defined functions cache return results
     * if the parameter set is empty or has constant-only return values.
     *
     * @return cache flag
     */
    public boolean isUdfCache() {
        return udfCache;
    }

    /**
     * Set to true (the default) to indicate that user-defined functions cache return results
     * if the parameter set is empty or has constant-only return values.
     *
     * @param udfCache cache flag
     */
    public void setUdfCache(boolean udfCache) {
        this.udfCache = udfCache;
    }

    /**
     * Enables or disables non-SQL standard builtin aggregation functions.
     *
     * @return indicator
     */
    public boolean isExtendedAggregation() {
        return extendedAggregation;
    }

    /**
     * Enables or disables non-SQL standard builtin aggregation functions.
     *
     * @param extendedAggregation indicator
     */
    public void setExtendedAggregation(boolean extendedAggregation) {
        this.extendedAggregation = extendedAggregation;
    }

    /**
     * Returns true to indicate that duck typing is enable for the specific syntax where it is allowed (check the documentation).
     *
     * @return indicator
     */
    public boolean isDuckTyping() {
        return duckTyping;
    }

    /**
     * Set to true to indicate that duck typing is enable for the specific syntax where it is allowed (check the documentation).
     *
     * @param duckTyping indicator
     */
    public void setDuckTyping(boolean duckTyping) {
        this.duckTyping = duckTyping;
    }

    /**
     * Returns the math context for big decimal operations, or null to leave the math context undefined.
     *
     * @return math context or null
     */
    public MathContext getMathContext() {
        return mathContext;
    }

    /**
     * Sets the math context for big decimal operations, or null to leave the math context undefined.
     *
     * @param mathContext math context or null
     */
    public void setMathContext(MathContext mathContext) {
        this.mathContext = mathContext;
    }
}
