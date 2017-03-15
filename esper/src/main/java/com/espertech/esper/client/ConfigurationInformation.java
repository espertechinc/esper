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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Provides configurations for an engine instance.
 */
public interface ConfigurationInformation {
    /**
     * Returns the service context factory class name
     *
     * @return class name
     */
    public String getEPServicesContextFactoryClassName();

    /**
     * Returns the mapping of event type name to Java class name.
     *
     * @return event type names for Java class names
     */
    public Map<String, String> getEventTypeNames();

    /**
     * Returns a map keyed by event type name, and values being the definition for the
     * Map event type of the property names and types that make up the event.
     *
     * @return map of event type name and definition of event properties
     */
    public Map<String, Properties> getEventTypesMapEvents();

    /**
     * Returns a map keyed by event type name, and values being the definition for the
     * event type of the property names and types that make up the event,
     * for nestable, strongly-typed Map-based event representations.
     *
     * @return map of event type name and definition of event properties
     */
    public Map<String, Map<String, Object>> getEventTypesNestableMapEvents();

    /**
     * Returns the mapping of event type name to XML DOM event type information.
     *
     * @return event type name mapping to XML DOM configs
     */
    public Map<String, ConfigurationEventTypeXMLDOM> getEventTypesXMLDOM();

    /**
     * Returns the Avro event types.
     * @return Avro event types
     */
    public Map<String, ConfigurationEventTypeAvro> getEventTypesAvro();

    /**
     * Returns the mapping of event type name to legacy java event type information.
     *
     * @return event type name mapping to legacy java class configs
     */
    public Map<String, ConfigurationEventTypeLegacy> getEventTypesLegacy();

    /**
     * Returns the class and package imports.
     *
     * @return imported names
     */
    public List<String> getImports();

    /**
     * Returns the class and package imports for annotation-only use.
     *
     * @return imported names
     */
    public List<String> getAnnotationImports();

    /**
     * Returns a map of string database names to database configuration options.
     *
     * @return map of database configurations
     */
    public Map<String, ConfigurationDBRef> getDatabaseReferences();

    /**
     * Returns a list of configured plug-in views.
     *
     * @return list of plug-in view configs
     */
    public List<ConfigurationPlugInView> getPlugInViews();

    /**
     * Returns a list of configured plug-in virtual data windows.
     *
     * @return list of plug-in virtual data windows
     */
    public List<ConfigurationPlugInVirtualDataWindow> getPlugInVirtualDataWindows();

    /**
     * Returns a list of configured plugin loaders.
     *
     * @return adapter loaders
     */
    public List<ConfigurationPluginLoader> getPluginLoaders();

    /**
     * Returns a list of configured plug-in aggregation functions.
     *
     * @return list of configured aggregations
     */
    public List<ConfigurationPlugInAggregationFunction> getPlugInAggregationFunctions();

    /**
     * Returns a list of configured plug-in multi-function aggregation functions.
     *
     * @return list of configured multi-function aggregations
     */
    public List<ConfigurationPlugInAggregationMultiFunction> getPlugInAggregationMultiFunctions();

    /**
     * Returns a list of configured plug-in single-row functions.
     *
     * @return list of configured single-row functions
     */
    public List<ConfigurationPlugInSingleRowFunction> getPlugInSingleRowFunctions();

    /**
     * Returns a list of configured plug-ins for pattern observers and guards.
     *
     * @return list of pattern plug-ins
     */
    public List<ConfigurationPlugInPatternObject> getPlugInPatternObjects();

    /**
     * Returns engine default settings.
     *
     * @return engine defaults
     */
    public ConfigurationEngineDefaults getEngineDefaults();

    /**
     * Returns the global variables by name as key and type plus initialization value as value
     *
     * @return map of variable name and variable configuration
     */
    public Map<String, ConfigurationVariable> getVariables();

    /**
     * Returns a map of class name and cache configurations, for use in
     * method invocations in the from-clause of methods provided by the class.
     *
     * @return map of fully-qualified or simple class name and cache configuration
     */
    public Map<String, ConfigurationMethodRef> getMethodInvocationReferences();

    /**
     * Returns a set of Java package names that Java event classes reside in.
     * <p>
     * This setting allows an application to place all it's events into one or more Java packages
     * and then declare these packages via this method. The engine
     * attempts to resolve an event type name to a Java class residing in each declared package.
     * <p>
     * For example, in the statement "select * from MyEvent" the engine attempts to load class "javaPackageName.MyEvent"
     * and if successful, uses that class as the event type.
     *
     * @return set of Java package names to look for events types when encountering a new event type name
     */
    public Set<String> getEventTypeAutoNamePackages();

    /**
     * Returns a map of plug-in event representation URI and their event representation class and initializer.
     *
     * @return map of URI keys and event representation configuration
     */
    public Map<URI, ConfigurationPlugInEventRepresentation> getPlugInEventRepresentation();

    /**
     * Returns a map of event type name of those event types that will be supplied by a plug-in event representation,
     * and their configuration.
     *
     * @return map of names to plug-in event type config
     */
    public Map<String, ConfigurationPlugInEventType> getPlugInEventTypes();

    /**
     * Returns the URIs that point to plug-in event representations that are given a chance to dynamically resolve an event
     * type name to an event type, when a new (unseen) event type name occurs in a new EPL statement.
     * <p>
     * The order of the URIs matters as event representations are asked in turn, to accept the name.
     * <p>
     * URIs can be child URIs of plug-in event representations and can add additional parameters or fragments
     * for use by the event representation.
     *
     * @return URIs for resolving an event type name
     */
    public URI[] getPlugInEventTypeResolutionURIs();

    /**
     * Returns a map of revision event type name and revision event type configuration. Revision event types handle updates (new versions)
     * for past events.
     *
     * @return map of name and revision event type config
     */
    public Map<String, ConfigurationRevisionEventType> getRevisionEventTypes();

    /**
     * Returns a map of variant stream name and variant configuration information. Variant streams allows handling
     * events of all sorts of different event types the same way.
     *
     * @return map of name and variant stream config
     */
    public Map<String, ConfigurationVariantStream> getVariantStreams();

    /**
     * Returns for each Map event type name the set of supertype event type names (Map types only).
     *
     * @return map of name to set of supertype names
     */
    public Map<String, ConfigurationEventTypeMap> getMapTypeConfigurations();

    /**
     * Returns the object-array event type configurations.
     *
     * @return type configs
     */
    public Map<String, ConfigurationEventTypeObjectArray> getObjectArrayTypeConfigurations();

    /**
     * Returns the object-array event types.
     *
     * @return object-array event types
     */
    public Map<String, Map<String, Object>> getEventTypesNestableObjectArrayEvents();

    /**
     * Returns the transient configuration, which are configuration values that are passed by reference (and not by value)
     * @return transient configuration
     */
    public Map<String, Object> getTransientConfiguration();
}




