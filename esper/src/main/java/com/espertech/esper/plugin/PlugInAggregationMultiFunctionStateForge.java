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

/**
 * State factory forge responsible for either code-generating or for returning a factory
 */
public interface PlugInAggregationMultiFunctionStateForge {
    /**
     * Return the state factory for the sharable aggregation function state.
     * <p>
     * The engine only obtains a state factory once for all shared aggregation state.
     * </p>
     *
     * @return state factory
     */
    public PlugInAggregationMultiFunctionStateFactory getStateFactory();

    default void rowMemberCodegen(PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext context) {}
    default void applyEnterCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {}
    default void applyLeaveCodegen(PlugInAggregationMultiFunctionStateForgeCodegenApplyContext context) {}
    default void clearCodegen(PlugInAggregationMultiFunctionStateForgeCodegenClearContext context) {}
}
