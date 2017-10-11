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
package com.espertech.esper.client.hook;

/**
 * Type of byte code generation.
 */
public enum AggregationFunctionFactoryCodegenType {
    /**
     * Use when not generating code.
     * <p>
     *     Methods to generate code do not need to be implemented.
     * </p>
     */
    CODEGEN_NONE,

    /**
     * Use when the generated code takes care of "distinct" and "filter" functionaltity.
     * <p>
     *     Methods to generate code must be implemented (except for apply-managed) and the apply-unmanaged methods must provide code to apply.
     * </p>
     */
    CODEGEN_UNMANAGED,

    /**
     * Use when the generated code receives a single "value" reference that contains the value object (or object-array for multivalue).
     * The container takes care of "distinct" and "filter" but not null-checks.
     * <p>
     *     Methods to generate code must be implemented (except for apply-unmanaged) and the apply-managed methods must provide code to apply.
     * </p>
     */
    CODEGEN_MANAGED
}
