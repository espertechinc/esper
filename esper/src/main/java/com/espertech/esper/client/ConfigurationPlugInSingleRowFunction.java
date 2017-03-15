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
package com.espertech.esper.client;

import java.io.Serializable;

/**
 * Configuration information for plugging in a custom single-row function.
 */
public class ConfigurationPlugInSingleRowFunction implements Serializable {
    private static final long serialVersionUID = 4096734947283212246L;

    private String name;
    private String functionClassName;
    private String functionMethodName;
    private ValueCache valueCache = ValueCache.DISABLED;
    private FilterOptimizable filterOptimizable = FilterOptimizable.ENABLED;
    private boolean rethrowExceptions = false;
    private String eventTypeName;

    /**
     * Ctor.
     * @param name UDF name
     * @param functionClassName class name
     * @param functionMethodName method name
     * @param valueCache value cache
     * @param filterOptimizable optimizable setting
     * @param rethrowExceptions rethrow setting
     * @param eventTypeName optional event type name
     */
    public ConfigurationPlugInSingleRowFunction(String name, String functionClassName, String functionMethodName, ValueCache valueCache, FilterOptimizable filterOptimizable, boolean rethrowExceptions, String eventTypeName) {
        this.name = name;
        this.functionClassName = functionClassName;
        this.functionMethodName = functionMethodName;
        this.valueCache = valueCache;
        this.filterOptimizable = filterOptimizable;
        this.rethrowExceptions = rethrowExceptions;
        this.eventTypeName = eventTypeName;
    }

    /**
     * Ctor.
     */
    public ConfigurationPlugInSingleRowFunction() {
    }

    /**
     * Returns the single-row function name for use in EPL.
     *
     * @return single-row function name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the single-row function name for use in EPL.
     *
     * @param name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the single-row function name.
     *
     * @return name
     */
    public String getFunctionClassName() {
        return functionClassName;
    }

    /**
     * Sets the single-row function's implementation class name.
     *
     * @param functionClassName is the implementation class name
     */
    public void setFunctionClassName(String functionClassName) {
        this.functionClassName = functionClassName;
    }

    /**
     * Returns the name of the single-row function.
     *
     * @return function name
     */
    public String getFunctionMethodName() {
        return functionMethodName;
    }

    /**
     * Sets the name of the single-row function.
     *
     * @param functionMethodName single-row function name
     */
    public void setFunctionMethodName(String functionMethodName) {
        this.functionMethodName = functionMethodName;
    }

    /**
     * Returns the setting for the cache behavior.
     *
     * @return cache behavior
     */
    public ValueCache getValueCache() {
        return valueCache;
    }

    /**
     * Sets the cache behavior.
     *
     * @param valueCache cache behavior
     */
    public void setValueCache(ValueCache valueCache) {
        this.valueCache = valueCache;
    }

    /**
     * Returns filter optimization settings.
     *
     * @return filter optimization settings
     */
    public FilterOptimizable getFilterOptimizable() {
        return filterOptimizable;
    }

    /**
     * Sets filter optimization settings.
     *
     * @param filterOptimizable filter optimization settings
     */
    public void setFilterOptimizable(FilterOptimizable filterOptimizable) {
        this.filterOptimizable = filterOptimizable;
    }

    /**
     * Returns indicator whether the engine re-throws exceptions
     * thrown by the single-row function. The default is false
     * therefore the engine by default does not rethrow exceptions.
     *
     * @return indicator
     */
    public boolean isRethrowExceptions() {
        return rethrowExceptions;
    }

    /**
     * Sets indicator whether the engine re-throws exceptions
     * thrown by the single-row function. The default is false
     * therefore the engine by default does not rethrow exceptions.
     *
     * @param rethrowExceptions indicator
     */
    public void setRethrowExceptions(boolean rethrowExceptions) {
        this.rethrowExceptions = rethrowExceptions;
    }

    /**
     * Returns the event type name for functions that return {@link EventBean} instances.
     * @return event type name
     */
    public String getEventTypeName() {
        return eventTypeName;
    }

    /**
     * Sets the event type name for functions that return {@link EventBean} instances.
     * @param eventTypeName event type name
     */
    public void setEventTypeName(String eventTypeName) {
        this.eventTypeName = eventTypeName;
    }

    /**
     * Enum for single-row function value cache setting.
     */
    public enum ValueCache {
        /**
         * The default, the result of a single-row function is always computed anew.
         */
        DISABLED,

        /**
         * Causes the engine to not actually invoke the single-row function and instead return a cached precomputed value
         * when all parameters are constants or there are no parameters.
         */
        ENABLED,

        /**
         * Causes the engine to follow the engine-wide policy as configured for user-defined functions.
         */
        CONFIGURED
    }

    /**
     * Controls whether a single-row function is eligible for optimization if it occurs in a filter expression.
     */
    public enum FilterOptimizable {
        /**
         * The engine does not consider the single-row function for optimizing evaluation: The function gets evaluated for each event possibly multiple times.
         */
        DISABLED,

        /**
         * The engine considers the single-row function for optimizing evaluation: The function gets evaluated only once per event.
         */
        ENABLED
    }
}
