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

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.util.PatternObjectType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains settings that apply to the compiler only (and that do not apply at runtime).
 */
public class ConfigurationCompiler implements Serializable {
    private static final long serialVersionUID = -1168467295251839905L;
    /**
     * List of configured plug-in views.
     */
    protected List<ConfigurationCompilerPlugInView> plugInViews;

    /**
     * List of configured plug-in views.
     */
    protected List<ConfigurationCompilerPlugInVirtualDataWindow> plugInVirtualDataWindows;

    /**
     * List of configured plug-in pattern objects.
     */
    protected List<ConfigurationCompilerPlugInPatternObject> plugInPatternObjects;

    /**
     * List of configured plug-in aggregation functions.
     */
    protected List<ConfigurationCompilerPlugInAggregationFunction> plugInAggregationFunctions;

    /**
     * List of configured plug-in aggregation multi-functions.
     */
    protected List<ConfigurationCompilerPlugInAggregationMultiFunction> plugInAggregationMultiFunctions;

    /**
     * List of configured plug-in single-row functions.
     */
    protected List<ConfigurationCompilerPlugInSingleRowFunction> plugInSingleRowFunctions;

    /**
     * List of configured plug-in date-time-methods.
     */
    protected List<ConfigurationCompilerPlugInDateTimeMethod> plugInDateTimeMethods;

    /**
     * List of configured plug-in enum-methods.
     */
    protected List<ConfigurationCompilerPlugInEnumMethod> plugInEnumMethods;

    private ConfigurationCompilerByteCode byteCode;
    private ConfigurationCompilerStreamSelection streamSelection;
    private ConfigurationCompilerViewResources viewResources;
    private ConfigurationCompilerLogging logging;
    private ConfigurationCompilerExpression expression;
    private ConfigurationCompilerExecution execution;
    private ConfigurationCompilerScripts scripts;
    private ConfigurationCompilerLanguage language;
    private ConfigurationCompilerSerde serde;

    /**
     * Constructs an empty configuration. The auto import values
     * are set by default to java.lang, java.math, java.text and
     * java.util.
     */
    public ConfigurationCompiler() {
        reset();
    }

    /**
     * Adds a plug-in aggregation function given a EPL function name and an aggregation forge class name.
     * <p>
     * The same function name cannot be added twice.
     *
     * @param functionName              is the new aggregation function name for use in EPL
     * @param aggregationForgeClassName is the fully-qualified class name of the class implementing the aggregation function forge interface
     * @throws ConfigurationException is thrown to indicate a problem adding the aggregation function
     */
    public void addPlugInAggregationFunctionForge(String functionName, String aggregationForgeClassName) {
        ConfigurationCompilerPlugInAggregationFunction entry = new ConfigurationCompilerPlugInAggregationFunction();
        entry.setName(functionName);
        entry.setForgeClassName(aggregationForgeClassName);
        plugInAggregationFunctions.add(entry);
    }

    /**
     * Adds a plug-in aggregation multi-function.
     *
     * @param config the configuration
     */
    public void addPlugInAggregationMultiFunction(ConfigurationCompilerPlugInAggregationMultiFunction config) {
        plugInAggregationMultiFunctions.add(config);
    }

    /**
     * Add a plug-in single-row function
     *
     * @param singleRowFunction configuration
     */
    public void addPlugInSingleRowFunction(ConfigurationCompilerPlugInSingleRowFunction singleRowFunction) {
        plugInSingleRowFunctions.add(singleRowFunction);
    }

    /**
     * Adds a plug-in single-row function given a EPL function name, a class name and a method name.
     * <p>
     * The same function name cannot be added twice.
     *
     * @param functionName is the new single-row function name for use in EPL
     * @param className    is the fully-qualified class name of the class implementing the single-row function
     * @param methodName   is the public static method provided by the class that implements the single-row function
     * @throws ConfigurationException is thrown to indicate a problem adding the single-row function
     */
    public void addPlugInSingleRowFunction(String functionName, String className, String methodName) {
        addPlugInSingleRowFunction(functionName, className, methodName, ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED);
    }

    /**
     * Adds a plug-in single-row function given a EPL function name, a class name, method name and setting for value-cache behavior.
     * <p>
     * The same function name cannot be added twice.
     *
     * @param functionName is the new single-row function name for use in EPL
     * @param className    is the fully-qualified class name of the class implementing the single-row function
     * @param methodName   is the public static method provided by the class that implements the single-row function
     * @param valueCache   set the behavior for caching the return value when constant parameters are provided
     */
    public void addPlugInSingleRowFunction(String functionName, String className, String methodName, ConfigurationCompilerPlugInSingleRowFunction.ValueCache valueCache) {
        addPlugInSingleRowFunction(functionName, className, methodName, valueCache, ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED);
    }

    /**
     * Adds a plug-in single-row function given a EPL function name, a class name, method name and setting for value-cache behavior.
     * <p>
     * The same function name cannot be added twice.
     *
     * @param functionName      is the new single-row function name for use in EPL
     * @param className         is the fully-qualified class name of the class implementing the single-row function
     * @param methodName        is the public static method provided by the class that implements the single-row function
     * @param filterOptimizable whether the single-row function, when used in filters, may be subject to reverse index lookup based on the function result
     */
    public void addPlugInSingleRowFunction(String functionName, String className, String methodName, ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable filterOptimizable) {
        addPlugInSingleRowFunction(functionName, className, methodName, ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED, filterOptimizable);
    }

    /**
     * Add single-row function with configurations.
     *
     * @param functionName      EPL name of function
     * @param className         providing fully-qualified class name
     * @param methodName        providing method name
     * @param valueCache        value cache settings
     * @param filterOptimizable settings whether subject to optimizations
     */
    public void addPlugInSingleRowFunction(String functionName, String className, String methodName,
                                           ConfigurationCompilerPlugInSingleRowFunction.ValueCache valueCache,
                                           ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable filterOptimizable) {
        addPlugInSingleRowFunction(functionName, className, methodName, valueCache, filterOptimizable, false);
    }

    /**
     * Add single-row function with configurations.
     *
     * @param functionName      EPL name of function
     * @param className         providing fully-qualified class name
     * @param methodName        providing method name
     * @param valueCache        value cache settings
     * @param filterOptimizable settings whether subject to optimizations
     * @param rethrowExceptions whether to rethrow exceptions
     */
    public void addPlugInSingleRowFunction(String functionName, String className, String methodName,
                                           ConfigurationCompilerPlugInSingleRowFunction.ValueCache valueCache,
                                           ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable filterOptimizable,
                                           boolean rethrowExceptions) {
        ConfigurationCompilerPlugInSingleRowFunction entry = new ConfigurationCompilerPlugInSingleRowFunction();
        entry.setFunctionClassName(className);
        entry.setFunctionMethodName(methodName);
        entry.setName(functionName);
        entry.setValueCache(valueCache);
        entry.setFilterOptimizable(filterOptimizable);
        entry.setRethrowExceptions(rethrowExceptions);
        addPlugInSingleRowFunction(entry);
    }

    /**
     * Returns the list of plug-in views.
     *
     * @return plug-in views
     */
    public List<ConfigurationCompilerPlugInView> getPlugInViews() {
        return plugInViews;
    }

    /**
     * Returns the list of plug-in virtual data windows.
     *
     * @return plug-in virtual data windows
     */
    public List<ConfigurationCompilerPlugInVirtualDataWindow> getPlugInVirtualDataWindows() {
        return plugInVirtualDataWindows;
    }

    /**
     * Returns the list of plug-in aggregation functions.
     *
     * @return plug-in aggregation functions
     */
    public List<ConfigurationCompilerPlugInAggregationFunction> getPlugInAggregationFunctions() {
        return plugInAggregationFunctions;
    }

    /**
     * Returns the list of plug-in aggregation multi-functions.
     *
     * @return plug-in aggregation multi-functions
     */
    public List<ConfigurationCompilerPlugInAggregationMultiFunction> getPlugInAggregationMultiFunctions() {
        return plugInAggregationMultiFunctions;
    }

    /**
     * Returns the list of plug-in single-row functions.
     *
     * @return plug-in single-row functions
     */
    public List<ConfigurationCompilerPlugInSingleRowFunction> getPlugInSingleRowFunctions() {
        return plugInSingleRowFunctions;
    }

    /**
     * Returns the list of plug-in pattern objects.
     *
     * @return plug-in pattern objects
     */
    public List<ConfigurationCompilerPlugInPatternObject> getPlugInPatternObjects() {
        return plugInPatternObjects;
    }

    /**
     * Add a view for plug-in.
     *
     * @param namespace      is the namespace the view should be available under
     * @param name           is the name of the view
     * @param viewForgeClass is the view forge class to use
     */
    public void addPlugInView(String namespace, String name, String viewForgeClass) {
        ConfigurationCompilerPlugInView configurationPlugInView = new ConfigurationCompilerPlugInView();
        configurationPlugInView.setNamespace(namespace);
        configurationPlugInView.setName(name);
        configurationPlugInView.setForgeClassName(viewForgeClass);
        plugInViews.add(configurationPlugInView);
    }

    /**
     * Add a virtual data window for plug-in.
     *
     * @param namespace  is the namespace the virtual data window should be available under
     * @param name       is the name of the data window
     * @param forgeClass is the view forge class to use
     */
    public void addPlugInVirtualDataWindow(String namespace, String name, String forgeClass) {
        addPlugInVirtualDataWindow(namespace, name, forgeClass, null);
    }

    /**
     * Add a virtual data window for plug-in.
     *
     * @param namespace                 is the namespace the virtual data window should be available under
     * @param name                      is the name of the data window
     * @param forgeClass                is the view forge class to use
     * @param customConfigurationObject additional configuration to be passed along
     */
    public void addPlugInVirtualDataWindow(String namespace, String name, String forgeClass, Serializable customConfigurationObject) {
        ConfigurationCompilerPlugInVirtualDataWindow configurationPlugInVirtualDataWindow = new ConfigurationCompilerPlugInVirtualDataWindow();
        configurationPlugInVirtualDataWindow.setNamespace(namespace);
        configurationPlugInVirtualDataWindow.setName(name);
        configurationPlugInVirtualDataWindow.setForgeClassName(forgeClass);
        configurationPlugInVirtualDataWindow.setConfig(customConfigurationObject);
        plugInVirtualDataWindows.add(configurationPlugInVirtualDataWindow);
    }

    /**
     * Add a pattern event observer for plug-in.
     *
     * @param namespace          is the namespace the observer should be available under
     * @param name               is the name of the observer
     * @param observerForgeClass is the observer forge class to use
     */
    public void addPlugInPatternObserver(String namespace, String name, String observerForgeClass) {
        ConfigurationCompilerPlugInPatternObject entry = new ConfigurationCompilerPlugInPatternObject();
        entry.setNamespace(namespace);
        entry.setName(name);
        entry.setForgeClassName(observerForgeClass);
        entry.setPatternObjectType(PatternObjectType.OBSERVER);
        plugInPatternObjects.add(entry);
    }

    /**
     * Add a pattern guard for plug-in.
     *
     * @param namespace       is the namespace the guard should be available under
     * @param name            is the name of the guard
     * @param guardForgeClass is the guard forge class to use
     */
    public void addPlugInPatternGuard(String namespace, String name, String guardForgeClass) {
        ConfigurationCompilerPlugInPatternObject entry = new ConfigurationCompilerPlugInPatternObject();
        entry.setNamespace(namespace);
        entry.setName(name);
        entry.setForgeClassName(guardForgeClass);
        entry.setPatternObjectType(PatternObjectType.GUARD);
        plugInPatternObjects.add(entry);
    }

    /**
     * Returns code generation settings
     *
     * @return code generation settings
     */
    public ConfigurationCompilerByteCode getByteCode() {
        return byteCode;
    }

    /**
     * Sets code generation settings
     *
     * @param byteCode settings
     */
    public void setByteCode(ConfigurationCompilerByteCode byteCode) {
        this.byteCode = byteCode;
    }

    /**
     * Returns settings applicable to streams (insert and remove, insert only or remove only) selected for a statement.
     *
     * @return stream selection defaults
     */
    public ConfigurationCompilerStreamSelection getStreamSelection() {
        return streamSelection;
    }

    /**
     * Returns view resources defaults.
     *
     * @return view resources defaults
     */
    public ConfigurationCompilerViewResources getViewResources() {
        return viewResources;
    }

    /**
     * Returns logging settings applicable to compiler.
     *
     * @return logging settings
     */
    public ConfigurationCompilerLogging getLogging() {
        return logging;
    }

    /**
     * Returns the expression-related settings for compiler.
     *
     * @return expression-related settings
     */
    public ConfigurationCompilerExpression getExpression() {
        return expression;
    }

    /**
     * Returns statement execution-related settings, settings that
     * influence event/schedule to statement processing.
     *
     * @return execution settings
     */
    public ConfigurationCompilerExecution getExecution() {
        return execution;
    }

    /**
     * Returns script settings.
     *
     * @return script settings
     */
    public ConfigurationCompilerScripts getScripts() {
        return scripts;
    }

    /**
     * Sets script settings.
     *
     * @param scripts script settings
     */
    public void setScripts(ConfigurationCompilerScripts scripts) {
        this.scripts = scripts;
    }

    /**
     * Returns the language-related settings.
     *
     * @return language-related settings
     */
    public ConfigurationCompilerLanguage getLanguage() {
        return language;
    }

    /**
     * Returns the serializer and de-serializer -related settings
     * @return serde settings
     */
    public ConfigurationCompilerSerde getSerde() {
        return serde;
    }

    /**
     * Sets the serializer and de-serializer -related settings
     * @param serde serde settings
     */
    public void setSerde(ConfigurationCompilerSerde serde) {
        this.serde = serde;
    }

    /**
     * Add a plug-in date-time method
     * @param dateTimeMethodName method name
     * @param dateTimeMethodForgeFactoryClassName fully-qualified forge class name
     */
    public void addPlugInDateTimeMethod(String dateTimeMethodName, String dateTimeMethodForgeFactoryClassName) {
        plugInDateTimeMethods.add(new ConfigurationCompilerPlugInDateTimeMethod(dateTimeMethodName, dateTimeMethodForgeFactoryClassName));
    }

    /**
     * Add a plug-in enum method
     * @param enumMethodName method name
     * @param enumMethodForgeFactoryClassName fully-qualified forge class name
     */
    public void addPlugInEnumMethod(String enumMethodName, String enumMethodForgeFactoryClassName) {
        plugInEnumMethods.add(new ConfigurationCompilerPlugInEnumMethod(enumMethodName, enumMethodForgeFactoryClassName));
    }

    /**
     * Returns the list of plug-in date-time methods
     * @return plug-in date-time methods
     */
    public List<ConfigurationCompilerPlugInDateTimeMethod> getPlugInDateTimeMethods() {
        return plugInDateTimeMethods;
    }

    /**
     * Returns the list of plug-in enum-methods
     * @return plug-in enum methods
     */
    public List<ConfigurationCompilerPlugInEnumMethod> getPlugInEnumMethods() {
        return plugInEnumMethods;
    }

    /**
     * Reset to an empty configuration.
     */
    protected void reset() {
        plugInViews = new ArrayList<>();
        plugInVirtualDataWindows = new ArrayList<>();
        plugInAggregationFunctions = new ArrayList<>();
        plugInAggregationMultiFunctions = new ArrayList<>();
        plugInSingleRowFunctions = new ArrayList<>();
        plugInDateTimeMethods = new ArrayList<>();
        plugInEnumMethods = new ArrayList<>();
        plugInPatternObjects = new ArrayList<>();
        byteCode = new ConfigurationCompilerByteCode();
        streamSelection = new ConfigurationCompilerStreamSelection();
        viewResources = new ConfigurationCompilerViewResources();
        logging = new ConfigurationCompilerLogging();
        expression = new ConfigurationCompilerExpression();
        execution = new ConfigurationCompilerExecution();
        scripts = new ConfigurationCompilerScripts();
        language = new ConfigurationCompilerLanguage();
        serde = new ConfigurationCompilerSerde();
    }
}
