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
package com.espertech.esper.common.client.configuration.runtime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Contains settings that apply to the runtime only (and that do not apply to the compiler).
 */
public class ConfigurationRuntime implements Serializable {
    private static final long serialVersionUID = -3207891291764278914L;
    /**
     * Optional classname to use for constructing services context.
     */
    protected String epServicesContextFactoryClassName;

    /**
     * List of adapter loaders.
     */
    protected List<ConfigurationRuntimePluginLoader> pluginLoaders;

    private ConfigurationRuntimeMetricsReporting metricsReporting;

    private ConfigurationRuntimeExceptionHandling exceptionHandling;
    private ConfigurationRuntimeConditionHandling conditionHandling;
    private ConfigurationRuntimeThreading threading;
    private ConfigurationRuntimeMatchRecognize matchRecognize;
    private ConfigurationRuntimePatterns patterns;
    private ConfigurationRuntimeVariables variables;
    private ConfigurationRuntimeLogging logging;
    private ConfigurationRuntimeTimeSource timeSource;
    private ConfigurationRuntimeExpression expression;
    private ConfigurationRuntimeExecution execution;

    /**
     * Ctor.
     */
    public ConfigurationRuntime() {
        reset();
    }

    /**
     * Sets the class name of the services context factory class to use.
     *
     * @param epServicesContextFactoryClassName service context factory class name
     */
    public void setEpServicesContextFactoryClassName(String epServicesContextFactoryClassName) {
        this.epServicesContextFactoryClassName = epServicesContextFactoryClassName;
    }

    /**
     * Returns the class name of the services context factory class to use.
     *
     * @return class name
     */
    public String getEPServicesContextFactoryClassName() {
        return epServicesContextFactoryClassName;
    }

    /**
     * Returns the plug-in loaders.
     *
     * @return plug-in loaders
     */
    public List<ConfigurationRuntimePluginLoader> getPluginLoaders() {
        return pluginLoaders;
    }

    /**
     * Add a plugin loader (f.e. an input/output adapter loader).
     * <p>The class is expected to implement the PluginLoader interface.</p>.
     *
     * @param loaderName    is the name of the loader
     * @param className     is the fully-qualified classname of the loader class
     * @param configuration is loader configuration entries
     */
    public void addPluginLoader(String loaderName, String className, Properties configuration) {
        addPluginLoader(loaderName, className, configuration, null);
    }

    /**
     * Add a plugin loader (f.e. an input/output adapter loader) without any additional loader configuration
     * <p>The class is expected to implement the PluginLoader interface.</p>.
     *
     * @param loaderName is the name of the loader
     * @param className  is the fully-qualified classname of the loader class
     */
    public void addPluginLoader(String loaderName, String className) {
        addPluginLoader(loaderName, className, null, null);
    }

    /**
     * Add a plugin loader (f.e. an input/output adapter loader).
     * <p>The class is expected to implement the PluginLoader interface.</p>.
     *
     * @param loaderName       is the name of the loader
     * @param className        is the fully-qualified classname of the loader class
     * @param configuration    is loader configuration entries
     * @param configurationXML config xml if any
     */
    public void addPluginLoader(String loaderName, String className, Properties configuration, String configurationXML) {
        ConfigurationRuntimePluginLoader pluginLoader = new ConfigurationRuntimePluginLoader();
        pluginLoader.setLoaderName(loaderName);
        pluginLoader.setClassName(className);
        pluginLoader.setConfigProperties(configuration);
        pluginLoader.setConfigurationXML(configurationXML);
        pluginLoaders.add(pluginLoader);
    }

    /**
     * Returns the metrics reporting configuration.
     *
     * @return metrics reporting config
     */
    public ConfigurationRuntimeMetricsReporting getMetricsReporting() {
        return metricsReporting;
    }

    /**
     * Sets the list of plug-in loaders
     *
     * @param pluginLoaders list of loaders
     */
    public void setPluginLoaders(List<ConfigurationRuntimePluginLoader> pluginLoaders) {
        this.pluginLoaders = pluginLoaders;
    }

    /**
     * Sets the metrics reporting settings
     *
     * @param metricsReporting metrics reporting settings
     */
    public void setMetricsReporting(ConfigurationRuntimeMetricsReporting metricsReporting) {
        this.metricsReporting = metricsReporting;
    }

    /**
     * Returns the exception handling configuration.
     *
     * @return exception handling configuration
     */
    public ConfigurationRuntimeExceptionHandling getExceptionHandling() {
        return exceptionHandling;
    }

    /**
     * Sets the exception handling configuration.
     *
     * @param exceptionHandling exception handling configuration
     */
    public void setExceptionHandling(ConfigurationRuntimeExceptionHandling exceptionHandling) {
        this.exceptionHandling = exceptionHandling;
    }

    /**
     * Returns the condition handling configuration.
     *
     * @return condition handling configuration
     */
    public ConfigurationRuntimeConditionHandling getConditionHandling() {
        return conditionHandling;
    }

    /**
     * Sets the condition handling configuration.
     *
     * @param conditionHandling exception handling configuration
     */
    public void setConditionHandling(ConfigurationRuntimeConditionHandling conditionHandling) {
        this.conditionHandling = conditionHandling;
    }

    /**
     * Returns threading settings.
     *
     * @return threading settings object
     */
    public ConfigurationRuntimeThreading getThreading() {
        return threading;
    }

    /**
     * Sets the threading settings
     *
     * @param threading settings
     */
    public void setThreading(ConfigurationRuntimeThreading threading) {
        this.threading = threading;
    }

    /**
     * Return match-recognize settings.
     *
     * @return match-recognize settings
     */
    public ConfigurationRuntimeMatchRecognize getMatchRecognize() {
        return matchRecognize;
    }

    /**
     * Sets match-recognize settings.
     *
     * @param matchRecognize settings to set
     */
    public void setMatchRecognize(ConfigurationRuntimeMatchRecognize matchRecognize) {
        this.matchRecognize = matchRecognize;
    }

    /**
     * Return pattern settings.
     *
     * @return pattern settings
     */
    public ConfigurationRuntimePatterns getPatterns() {
        return patterns;
    }

    /**
     * Sets pattern settings.
     *
     * @param patterns settings to set
     */
    public void setPatterns(ConfigurationRuntimePatterns patterns) {
        this.patterns = patterns;
    }

    /**
     * Returns defaults applicable to variables.
     *
     * @return variable defaults
     */
    public ConfigurationRuntimeVariables getVariables() {
        return variables;
    }

    /**
     * Returns logging settings applicable to runtime.
     *
     * @return logging settings
     */
    public ConfigurationRuntimeLogging getLogging() {
        return logging;
    }

    /**
     * Returns the time source configuration.
     *
     * @return time source enum
     */
    public ConfigurationRuntimeTimeSource getTimeSource() {
        return timeSource;
    }

    /**
     * Returns the expression-related settings for common.
     *
     * @return expression-related settings
     */
    public ConfigurationRuntimeExpression getExpression() {
        return expression;
    }

    /**
     * Returns statement execution-related settings, settings that
     * influence event/schedule to statement processing.
     *
     * @return execution settings
     */
    public ConfigurationRuntimeExecution getExecution() {
        return execution;
    }

    /**
     * Reset to an empty configuration.
     */
    protected void reset() {
        pluginLoaders = new ArrayList<>();
        metricsReporting = new ConfigurationRuntimeMetricsReporting();
        exceptionHandling = new ConfigurationRuntimeExceptionHandling();
        conditionHandling = new ConfigurationRuntimeConditionHandling();
        threading = new ConfigurationRuntimeThreading();
        matchRecognize = new ConfigurationRuntimeMatchRecognize();
        patterns = new ConfigurationRuntimePatterns();
        variables = new ConfigurationRuntimeVariables();
        logging = new ConfigurationRuntimeLogging();
        timeSource = new ConfigurationRuntimeTimeSource();
        expression = new ConfigurationRuntimeExpression();
        execution = new ConfigurationRuntimeExecution();
    }
}
