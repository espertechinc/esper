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

import com.espertech.esper.client.annotation.Name;
import com.espertech.esper.event.EventTypeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * An instance of <tt>Configuration</tt> allows the application
 * to specify properties to be used when
 * creating a <tt>EPServiceProvider</tt>. Usually an application will create
 * a single <tt>Configuration</tt>, then get one or more instances of
 * {@link EPServiceProvider} via {@link EPServiceProviderManager}.
 * The <tt>Configuration</tt> is meant
 * only as an initialization-time object. <tt>EPServiceProvider</tt>s are
 * immutable and do not retain any association back to the
 * <tt>Configuration</tt>.
 * <br>
 * The format of an Esper XML configuration file is defined in
 * <tt>esper-configuration-(version).xsd</tt>.
 */
public class Configuration implements ConfigurationOperations, ConfigurationInformation, Serializable {
    private static final long serialVersionUID = -220881974438617882L;
    private final static Logger log = LoggerFactory.getLogger(Configuration.class);

    /**
     * Import name of the package that hosts the annotation classes.
     */
    public static final String ANNOTATION_IMPORT = Name.class.getPackage().getName() + ".*";

    /**
     * Default name of the configuration file.
     */
    protected static final String ESPER_DEFAULT_CONFIG = "esper.cfg.xml";

    /**
     * Map of event name and fully-qualified class name.
     */
    protected Map<String, String> eventClasses;

    /**
     * Map of event type name and XML DOM configuration.
     */
    protected Map<String, ConfigurationEventTypeXMLDOM> eventTypesXMLDOM;

    /**
     * Map of event type name and XML DOM configuration.
     */
    protected Map<String, ConfigurationEventTypeAvro> eventTypesAvro;

    /**
     * Map of event type name and Legacy-type event configuration.
     */
    protected Map<String, ConfigurationEventTypeLegacy> eventTypesLegacy;

    /**
     * The type names for events that are backed by java.util.Map,
     * not containing strongly-typed nested maps.
     */
    protected Map<String, Properties> mapNames;

    /**
     * The type names for events that are backed by java.util.Map,
     * possibly containing strongly-typed nested maps.
     * <p>
     * Each entrie's value must be either a Class or a Map&lt;String,Object&gt; to
     * define nested maps.
     */
    protected Map<String, Map<String, Object>> nestableMapNames;

    /**
     * The type names for events that are backed by java.util.Map,
     * possibly containing strongly-typed nested maps.
     * <p>
     * Each entrie's value must be either a Class or a Map&lt;String,Object&gt; to
     * define nested maps.
     */
    protected Map<String, Map<String, Object>> nestableObjectArrayNames;

    /**
     * Map event types additional configuration information.
     */
    protected Map<String, ConfigurationEventTypeMap> mapTypeConfigurations;

    /**
     * Map event types additional configuration information.
     */
    protected Map<String, ConfigurationEventTypeObjectArray> objectArrayTypeConfigurations;

    /**
     * The class and package name imports that
     * will be used to resolve partial class names.
     */
    protected List<String> imports;

    /**
     * For annotations only, the class and package name imports that
     * will be used to resolve partial class names (not available in EPL statements unless used in an annotation).
     */
    protected List<String> annotationImports;

    /**
     * The class and package name imports that
     * will be used to resolve partial class names.
     */
    protected Map<String, ConfigurationDBRef> databaseReferences;

    /**
     * Optional classname to use for constructing services context.
     */
    protected String epServicesContextFactoryClassName;

    /**
     * List of configured plug-in views.
     */
    protected List<ConfigurationPlugInView> plugInViews;

    /**
     * List of configured plug-in views.
     */
    protected List<ConfigurationPlugInVirtualDataWindow> plugInVirtualDataWindows;

    /**
     * List of configured plug-in pattern objects.
     */
    protected List<ConfigurationPlugInPatternObject> plugInPatternObjects;

    /**
     * List of configured plug-in aggregation functions.
     */
    protected List<ConfigurationPlugInAggregationFunction> plugInAggregationFunctions;

    /**
     * List of configured plug-in aggregation multi-functions.
     */
    protected List<ConfigurationPlugInAggregationMultiFunction> plugInAggregationMultiFunctions;

    /**
     * List of configured plug-in single-row functions.
     */
    protected List<ConfigurationPlugInSingleRowFunction> plugInSingleRowFunctions;

    /**
     * List of adapter loaders.
     */
    protected List<ConfigurationPluginLoader> pluginLoaders;

    /**
     * Saves engine default configs such as threading settings
     */
    protected ConfigurationEngineDefaults engineDefaults;

    /**
     * Saves the packages to search to resolve event type names.
     */
    protected Set<String> eventTypeAutoNamePackages;

    /**
     * Map of variables.
     */
    protected Map<String, ConfigurationVariable> variables;

    /**
     * Map of class name and configuration for method invocations on that class.
     */
    protected Map<String, ConfigurationMethodRef> methodInvocationReferences;

    /**
     * Map of plug-in event representation name and configuration
     */
    protected Map<URI, ConfigurationPlugInEventRepresentation> plugInEventRepresentation;

    /**
     * Map of plug-in event types.
     */
    protected Map<String, ConfigurationPlugInEventType> plugInEventTypes;

    /**
     * URIs that point to plug-in event representations that are given a chance to dynamically resolve an event type name to an
     * event type, as it occurs in a new EPL statement.
     */
    protected URI[] plugInEventTypeResolutionURIs;

    /**
     * All revision event types which allow updates to past events.
     */
    protected Map<String, ConfigurationRevisionEventType> revisionEventTypes;

    /**
     * Variant streams allow events of disparate types to be treated the same.
     */
    protected Map<String, ConfigurationVariantStream> variantStreams;


    protected transient Map<String, Object> transientConfiguration;

    /**
     * Constructs an empty configuration. The auto import values
     * are set by default to java.lang, java.math, java.text and
     * java.util.
     */
    public Configuration() {
        reset();
    }

    /**
     * Sets the class name of the services context factory class to use.
     *
     * @param epServicesContextFactoryClassName service context factory class name
     */
    public void setEPServicesContextFactoryClassName(String epServicesContextFactoryClassName) {
        this.epServicesContextFactoryClassName = epServicesContextFactoryClassName;
    }

    public String getEPServicesContextFactoryClassName() {
        return epServicesContextFactoryClassName;
    }

    public void addPlugInAggregationFunctionFactory(String functionName, String aggregationFactoryClassName) throws ConfigurationException {
        ConfigurationPlugInAggregationFunction entry = new ConfigurationPlugInAggregationFunction();
        entry.setName(functionName);
        entry.setFactoryClassName(aggregationFactoryClassName);
        plugInAggregationFunctions.add(entry);
    }

    public void addPlugInAggregationMultiFunction(ConfigurationPlugInAggregationMultiFunction config) throws ConfigurationException {
        plugInAggregationMultiFunctions.add(config);
    }

    public void addPlugInSingleRowFunction(ConfigurationPlugInSingleRowFunction singleRowFunction) {
        plugInSingleRowFunctions.add(singleRowFunction);
    }

    public void addPlugInSingleRowFunction(String functionName, String className, String methodName) throws ConfigurationException {
        addPlugInSingleRowFunction(functionName, className, methodName, ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED);
    }

    public void addPlugInSingleRowFunction(String functionName, String className, String methodName, ConfigurationPlugInSingleRowFunction.ValueCache valueCache) throws ConfigurationException {
        addPlugInSingleRowFunction(functionName, className, methodName, valueCache, ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);
    }

    public void addPlugInSingleRowFunction(String functionName, String className, String methodName, ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable) throws ConfigurationException {
        addPlugInSingleRowFunction(functionName, className, methodName, ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED, filterOptimizable);
    }

    /**
     * Returns transient configuration, i.e. information that is passed along as a reference and not as a value
     * @return map of transients
     */
    public Map<String, Object> getTransientConfiguration() {
        return transientConfiguration;
    }

    /**
     * Sets transient configuration, i.e. information that is passed along as a reference and not as a value
     * @param transientConfiguration map of transients
     */
    public void setTransientConfiguration(Map<String, Object> transientConfiguration) {
        this.transientConfiguration = transientConfiguration;
    }

    /**
     * Add single-row function with configurations.
     *
     * @param functionName      EPL name of function
     * @param className         providing fully-qualified class name
     * @param methodName        providing method name
     * @param valueCache        value cache settings
     * @param filterOptimizable settings whether subject to optimizations
     * @throws ConfigurationException thrown to indicate that the configuration is invalid
     */
    public void addPlugInSingleRowFunction(String functionName, String className, String methodName,
                                           ConfigurationPlugInSingleRowFunction.ValueCache valueCache,
                                           ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable) throws ConfigurationException {
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
     * @throws ConfigurationException thrown to indicate that the configuration is invalid
     */
    public void addPlugInSingleRowFunction(String functionName, String className, String methodName,
                                           ConfigurationPlugInSingleRowFunction.ValueCache valueCache,
                                           ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable,
                                           boolean rethrowExceptions) throws ConfigurationException {
        ConfigurationPlugInSingleRowFunction entry = new ConfigurationPlugInSingleRowFunction();
        entry.setFunctionClassName(className);
        entry.setFunctionMethodName(methodName);
        entry.setName(functionName);
        entry.setValueCache(valueCache);
        entry.setFilterOptimizable(filterOptimizable);
        entry.setRethrowExceptions(rethrowExceptions);
        addPlugInSingleRowFunction(entry);
    }

    /**
     * Checks if an event type has already been registered for that name.
     *
     * @param eventTypeName the name
     * @return true if already registered
     * @since 2.1
     */
    public boolean isEventTypeExists(String eventTypeName) {
        return eventClasses.containsKey(eventTypeName)
                || mapNames.containsKey(eventTypeName)
                || nestableMapNames.containsKey(eventTypeName)
                || nestableObjectArrayNames.containsKey(eventTypeName)
                || eventTypesXMLDOM.containsKey(eventTypeName)
                || eventTypesAvro.containsKey(eventTypeName);
        //note: no need to check legacy as they get added as class event type
    }

    /**
     * Add an name for an event type represented by Java-bean plain-old Java object events.
     * Note that when adding multiple names for the same Java class the names represent an
     * alias to the same event type since event type identity for Java classes is per Java class.
     *
     * @param eventTypeName  is the name for the event type
     * @param eventClassName fully-qualified class name of the event type
     */
    public void addEventType(String eventTypeName, String eventClassName) {
        eventClasses.put(eventTypeName, eventClassName);
    }

    /**
     * Add an name for an event type represented by Java-bean plain-old Java object events.
     * Note that when adding multiple names for the same Java class the names represent an
     * alias to the same event type since event type identity for Java classes is per Java class.
     *
     * @param eventTypeName is the name for the event type
     * @param eventClass    is the Java event class for which to add the name
     */
    public void addEventType(String eventTypeName, Class eventClass) {
        addEventType(eventTypeName, eventClass.getName());
    }

    /**
     * Add an name for an event type represented by Java-bean plain-old Java object events,
     * and the name is the simple class name of the class.
     *
     * @param eventClass is the Java event class for which to add the name
     */
    public void addEventType(Class eventClass) {
        addEventType(eventClass.getSimpleName(), eventClass.getName());
    }

    /**
     * Add an name for an event type that represents java.util.Map events.
     * <p>
     * Each entry in the type map is the property name and the fully-qualified
     * Java class name or primitive type name.
     *
     * @param eventTypeName is the name for the event type
     * @param typeMap       maps the name of each property in the Map event to the type
     *                      (fully qualified classname) of its value in Map event instances.
     */
    public void addEventType(String eventTypeName, Properties typeMap) {
        mapNames.put(eventTypeName, typeMap);
    }

    public void addEventType(String eventTypeName, Map<String, Object> typeMap) {
        nestableMapNames.put(eventTypeName, new LinkedHashMap<String, Object>(typeMap));
    }

    public void addEventType(String eventTypeName, Map<String, Object> typeMap, String[] superTypes) {
        nestableMapNames.put(eventTypeName, new LinkedHashMap<String, Object>(typeMap));
        if (superTypes != null) {
            for (int i = 0; i < superTypes.length; i++) {
                this.addMapSuperType(eventTypeName, superTypes[i]);
            }
        }
    }

    public void addEventType(String eventTypeName, Map<String, Object> typeMap, ConfigurationEventTypeMap mapConfig) throws ConfigurationException {
        nestableMapNames.put(eventTypeName, new LinkedHashMap<String, Object>(typeMap));
        mapTypeConfigurations.put(eventTypeName, mapConfig);
    }

    /**
     * Add, for a given Map event type identified by the first parameter, the supertype (by its event type name).
     * <p>
     * Each Map event type may have any number of supertypes, each supertype must also be of a Map-type event.
     *
     * @param mapeventTypeName the name of a Map event type, that is to have a supertype
     * @param mapSupertypeName the name of a Map event type that is the supertype
     */
    public void addMapSuperType(String mapeventTypeName, String mapSupertypeName) {
        ConfigurationEventTypeMap current = mapTypeConfigurations.get(mapeventTypeName);
        if (current == null) {
            current = new ConfigurationEventTypeMap();
            mapTypeConfigurations.put(mapeventTypeName, current);
        }
        Set<String> superTypes = current.getSuperTypes();
        superTypes.add(mapSupertypeName);
    }

    /**
     * Add, for a given Object-array event type identified by the first parameter, the supertype (by its event type name).
     * <p>
     * Each Object array event type may have any number of supertypes, each supertype must also be of a Object-array-type event.
     *
     * @param eventTypeName the name of a Map event type, that is to have a supertype
     * @param supertypeName the name of a Map event type that is the supertype
     */
    public void addObjectArraySuperType(String eventTypeName, String supertypeName) {
        ConfigurationEventTypeObjectArray current = objectArrayTypeConfigurations.get(eventTypeName);
        if (current == null) {
            current = new ConfigurationEventTypeObjectArray();
            objectArrayTypeConfigurations.put(eventTypeName, current);
        }
        Set<String> superTypes = current.getSuperTypes();
        if (!superTypes.isEmpty()) {
            throw new ConfigurationException("Object-array event types may not have multiple supertypes");
        }
        superTypes.add(supertypeName);
    }

    /**
     * Add configuration for a map event type.
     *
     * @param mapeventTypeName configuration to add
     * @param config           map type configuration
     */
    public void addMapConfiguration(String mapeventTypeName, ConfigurationEventTypeMap config) {
        mapTypeConfigurations.put(mapeventTypeName, config);
    }

    /**
     * Add configuration for a object array event type.
     *
     * @param objectArrayeventTypeName configuration to add
     * @param config                   map type configuration
     */
    public void addObjectArrayConfiguration(String objectArrayeventTypeName, ConfigurationEventTypeObjectArray config) {
        objectArrayTypeConfigurations.put(objectArrayeventTypeName, config);
    }

    /**
     * Add an name for an event type that represents org.w3c.dom.Node events.
     *
     * @param eventTypeName       is the name for the event type
     * @param xmlDOMEventTypeDesc descriptor containing property and mapping information for XML-DOM events
     */
    public void addEventType(String eventTypeName, ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc) {
        eventTypesXMLDOM.put(eventTypeName, xmlDOMEventTypeDesc);
    }

    public void addEventType(String eventTypeName, String[] propertyNames, Object[] propertyTypes) throws ConfigurationException {
        LinkedHashMap<String, Object> propertyTypesMap = EventTypeUtility.validateObjectArrayDef(propertyNames, propertyTypes);
        nestableObjectArrayNames.put(eventTypeName, propertyTypesMap);
    }

    public void addEventType(String eventTypeName, String[] propertyNames, Object[] propertyTypes, ConfigurationEventTypeObjectArray config) throws ConfigurationException {
        LinkedHashMap<String, Object> propertyTypesMap = EventTypeUtility.validateObjectArrayDef(propertyNames, propertyTypes);
        nestableObjectArrayNames.put(eventTypeName, propertyTypesMap);
        objectArrayTypeConfigurations.put(eventTypeName, config);
        if (config.getSuperTypes() != null && config.getSuperTypes().size() > 1) {
            throw new ConfigurationException(ConfigurationEventTypeObjectArray.SINGLE_SUPERTYPE_MSG);
        }
    }

    public void addRevisionEventType(String revisioneventTypeName, ConfigurationRevisionEventType revisionEventTypeConfig) {
        revisionEventTypes.put(revisioneventTypeName, revisionEventTypeConfig);
    }

    /**
     * Add a database reference with a given database name.
     *
     * @param name               is the database name
     * @param configurationDBRef descriptor containing database connection and access policy information
     */
    public void addDatabaseReference(String name, ConfigurationDBRef configurationDBRef) {
        databaseReferences.put(name, configurationDBRef);
    }

    /**
     * Add an name for an event type that represents legacy Java type (non-JavaBean style) events.
     * Note that when adding multiple names for the same Java class the names represent an
     * alias to the same event type since event type identity for Java classes is per Java class.
     *
     * @param eventTypeName       is the name for the event type
     * @param eventClass          fully-qualified class name of the event type
     * @param legacyEventTypeDesc descriptor containing property and mapping information for Legacy Java type events
     */
    public void addEventType(String eventTypeName, String eventClass, ConfigurationEventTypeLegacy legacyEventTypeDesc) {
        eventClasses.put(eventTypeName, eventClass);
        eventTypesLegacy.put(eventTypeName, legacyEventTypeDesc);
    }

    public void addImport(String autoImport) {
        imports.add(autoImport);
    }

    public void addImport(Class autoImport) {
        addImport(autoImport.getName());
    }

    public void addAnnotationImport(String autoImport) {
        annotationImports.add(autoImport);
    }

    /**
     * Add a class to the imports available for annotations only
     * @param autoImport class to add
     */
    public void addAnnotationImport(Class autoImport) {
        addAnnotationImport(autoImport.getName());
    }

    /**
     * Remove an import.
     *
     * @param name to remove
     */
    public void removeImport(String name) {
        imports.remove(name);
    }

    /**
     * Adds a cache configuration for a class providing methods for use in the from-clause.
     *
     * @param className              is the class name (simple or fully-qualified) providing methods
     * @param methodInvocationConfig is the cache configuration
     */
    public void addMethodRef(String className, ConfigurationMethodRef methodInvocationConfig) {
        this.methodInvocationReferences.put(className, methodInvocationConfig);
    }

    /**
     * Adds a cache configuration for a class providing methods for use in the from-clause.
     *
     * @param clazz                  is the class providing methods
     * @param methodInvocationConfig is the cache configuration
     */
    public void addMethodRef(Class clazz, ConfigurationMethodRef methodInvocationConfig) {
        this.methodInvocationReferences.put(clazz.getName(), methodInvocationConfig);
    }

    public Map<String, String> getEventTypeNames() {
        return eventClasses;
    }

    public Map<String, Properties> getEventTypesMapEvents() {
        return mapNames;
    }

    public Map<String, Map<String, Object>> getEventTypesNestableMapEvents() {
        return nestableMapNames;
    }

    public Map<String, Map<String, Object>> getEventTypesNestableObjectArrayEvents() {
        return nestableObjectArrayNames;
    }

    public Map<String, ConfigurationEventTypeXMLDOM> getEventTypesXMLDOM() {
        return eventTypesXMLDOM;
    }

    public Map<String, ConfigurationEventTypeAvro> getEventTypesAvro() {
        return eventTypesAvro;
    }

    public Map<String, ConfigurationEventTypeLegacy> getEventTypesLegacy() {
        return eventTypesLegacy;
    }

    public List<String> getImports() {
        return imports;
    }

    public List<String> getAnnotationImports() {
        return annotationImports;
    }

    public Map<String, ConfigurationDBRef> getDatabaseReferences() {
        return databaseReferences;
    }

    public List<ConfigurationPlugInView> getPlugInViews() {
        return plugInViews;
    }

    public Map<String, ConfigurationEventTypeObjectArray> getObjectArrayTypeConfigurations() {
        return objectArrayTypeConfigurations;
    }

    public List<ConfigurationPlugInVirtualDataWindow> getPlugInVirtualDataWindows() {
        return plugInVirtualDataWindows;
    }

    public List<ConfigurationPluginLoader> getPluginLoaders() {
        return pluginLoaders;
    }

    public List<ConfigurationPlugInAggregationFunction> getPlugInAggregationFunctions() {
        return plugInAggregationFunctions;
    }

    public List<ConfigurationPlugInAggregationMultiFunction> getPlugInAggregationMultiFunctions() {
        return plugInAggregationMultiFunctions;
    }

    public List<ConfigurationPlugInSingleRowFunction> getPlugInSingleRowFunctions() {
        return plugInSingleRowFunctions;
    }

    public List<ConfigurationPlugInPatternObject> getPlugInPatternObjects() {
        return plugInPatternObjects;
    }

    public Map<String, ConfigurationVariable> getVariables() {
        return variables;
    }

    public Map<String, ConfigurationMethodRef> getMethodInvocationReferences() {
        return methodInvocationReferences;
    }

    public Map<String, ConfigurationRevisionEventType> getRevisionEventTypes() {
        return revisionEventTypes;
    }

    public Map<String, ConfigurationEventTypeMap> getMapTypeConfigurations() {
        return mapTypeConfigurations;
    }

    /**
     * Add a plugin loader (f.e. an input/output adapter loader).
     * <p>The class is expected to implement {@link com.espertech.esper.plugin.PluginLoader}</p>.
     *
     * @param loaderName    is the name of the loader
     * @param className     is the fully-qualified classname of the loader class
     * @param configuration is loader cofiguration entries
     */
    public void addPluginLoader(String loaderName, String className, Properties configuration) {
        addPluginLoader(loaderName, className, configuration, null);
    }

    /**
     * Add a plugin loader (f.e. an input/output adapter loader) without any additional loader configuration
     * <p>The class is expected to implement {@link com.espertech.esper.plugin.PluginLoader}</p>.
     *
     * @param loaderName is the name of the loader
     * @param className  is the fully-qualified classname of the loader class
     */
    public void addPluginLoader(String loaderName, String className) {
        addPluginLoader(loaderName, className, null, null);
    }

    /**
     * Add a plugin loader (f.e. an input/output adapter loader).
     * <p>The class is expected to implement {@link com.espertech.esper.plugin.PluginLoader}</p>.
     *
     * @param loaderName       is the name of the loader
     * @param className        is the fully-qualified classname of the loader class
     * @param configuration    is loader cofiguration entries
     * @param configurationXML config xml if any
     */
    public void addPluginLoader(String loaderName, String className, Properties configuration, String configurationXML) {
        ConfigurationPluginLoader pluginLoader = new ConfigurationPluginLoader();
        pluginLoader.setLoaderName(loaderName);
        pluginLoader.setClassName(className);
        pluginLoader.setConfigProperties(configuration);
        pluginLoader.setConfigurationXML(configurationXML);
        pluginLoaders.add(pluginLoader);
    }

    /**
     * Add a view for plug-in.
     *
     * @param namespace        is the namespace the view should be available under
     * @param name             is the name of the view
     * @param viewFactoryClass is the view factory class to use
     */
    public void addPlugInView(String namespace, String name, String viewFactoryClass) {
        ConfigurationPlugInView configurationPlugInView = new ConfigurationPlugInView();
        configurationPlugInView.setNamespace(namespace);
        configurationPlugInView.setName(name);
        configurationPlugInView.setFactoryClassName(viewFactoryClass);
        plugInViews.add(configurationPlugInView);
    }

    /**
     * Add a virtual data window for plug-in.
     *
     * @param namespace    is the namespace the virtual data window should be available under
     * @param name         is the name of the data window
     * @param factoryClass is the view factory class to use
     */
    public void addPlugInVirtualDataWindow(String namespace, String name, String factoryClass) {
        addPlugInVirtualDataWindow(namespace, name, factoryClass, null);
    }

    /**
     * Add a virtual data window for plug-in.
     *
     * @param namespace                 is the namespace the virtual data window should be available under
     * @param name                      is the name of the data window
     * @param factoryClass              is the view factory class to use
     * @param customConfigurationObject additional configuration to be passed along
     */
    public void addPlugInVirtualDataWindow(String namespace, String name, String factoryClass, Serializable customConfigurationObject) {
        ConfigurationPlugInVirtualDataWindow configurationPlugInVirtualDataWindow = new ConfigurationPlugInVirtualDataWindow();
        configurationPlugInVirtualDataWindow.setNamespace(namespace);
        configurationPlugInVirtualDataWindow.setName(name);
        configurationPlugInVirtualDataWindow.setFactoryClassName(factoryClass);
        configurationPlugInVirtualDataWindow.setConfig(customConfigurationObject);
        plugInVirtualDataWindows.add(configurationPlugInVirtualDataWindow);
    }

    /**
     * Add a pattern event observer for plug-in.
     *
     * @param namespace            is the namespace the observer should be available under
     * @param name                 is the name of the observer
     * @param observerFactoryClass is the observer factory class to use
     */
    public void addPlugInPatternObserver(String namespace, String name, String observerFactoryClass) {
        ConfigurationPlugInPatternObject entry = new ConfigurationPlugInPatternObject();
        entry.setNamespace(namespace);
        entry.setName(name);
        entry.setFactoryClassName(observerFactoryClass);
        entry.setPatternObjectType(ConfigurationPlugInPatternObject.PatternObjectType.OBSERVER);
        plugInPatternObjects.add(entry);
    }

    /**
     * Add a pattern guard for plug-in.
     *
     * @param namespace         is the namespace the guard should be available under
     * @param name              is the name of the guard
     * @param guardFactoryClass is the guard factory class to use
     */
    public void addPlugInPatternGuard(String namespace, String name, String guardFactoryClass) {
        ConfigurationPlugInPatternObject entry = new ConfigurationPlugInPatternObject();
        entry.setNamespace(namespace);
        entry.setName(name);
        entry.setFactoryClassName(guardFactoryClass);
        entry.setPatternObjectType(ConfigurationPlugInPatternObject.PatternObjectType.GUARD);
        plugInPatternObjects.add(entry);
    }

    public void addEventTypeAutoName(String packageName) {
        eventTypeAutoNamePackages.add(packageName);
    }

    public void addVariable(String variableName, Class type, Object initializationValue) {
        addVariable(variableName, type.getName(), initializationValue, false);
    }

    /**
     * Add variable that can be a constant.
     *
     * @param variableName        name of variable
     * @param type                variable type
     * @param initializationValue initial value
     * @param constant            constant indicator
     */
    public void addVariable(String variableName, Class type, Object initializationValue, boolean constant) {
        addVariable(variableName, type.getName(), initializationValue, constant);
    }

    public void addVariable(String variableName, String type, Object initializationValue) throws ConfigurationException {
        addVariable(variableName, type, initializationValue, false);
    }

    public void addVariable(String variableName, String type, Object initializationValue, boolean constant) throws ConfigurationException {
        ConfigurationVariable configVar = new ConfigurationVariable();
        configVar.setType(type);
        configVar.setInitializationValue(initializationValue);
        configVar.setConstant(constant);
        variables.put(variableName, configVar);
    }

    /**
     * Adds an event representation responsible for creating event types (event metadata) and event bean instances (events) for
     * a certain kind of object representation that holds the event property values.
     *
     * @param eventRepresentationRootURI   uniquely identifies the event representation and acts as a parent
     *                                     for child URIs used in resolving
     * @param eventRepresentationClassName is the name of the class implementing {@link com.espertech.esper.plugin.PlugInEventRepresentation}.
     * @param initializer                  is optional configuration or initialization information, or null if none required
     */
    public void addPlugInEventRepresentation(URI eventRepresentationRootURI, String eventRepresentationClassName, Serializable initializer) {
        ConfigurationPlugInEventRepresentation config = new ConfigurationPlugInEventRepresentation();
        config.setEventRepresentationClassName(eventRepresentationClassName);
        config.setInitializer(initializer);
        this.plugInEventRepresentation.put(eventRepresentationRootURI, config);
    }

    /**
     * Adds an event representation responsible for creating event types (event metadata) and event bean instances (events) for
     * a certain kind of object representation that holds the event property values.
     *
     * @param eventRepresentationRootURI uniquely identifies the event representation and acts as a parent
     *                                   for child URIs used in resolving
     * @param eventRepresentationClass   is the class implementing {@link com.espertech.esper.plugin.PlugInEventRepresentation}.
     * @param initializer                is optional configuration or initialization information, or null if none required
     */
    public void addPlugInEventRepresentation(URI eventRepresentationRootURI, Class eventRepresentationClass, Serializable initializer) {
        addPlugInEventRepresentation(eventRepresentationRootURI, eventRepresentationClass.getName(), initializer);
    }

    public void addPlugInEventType(String eventTypeName, URI[] resolutionURIs, Serializable initializer) {
        ConfigurationPlugInEventType config = new ConfigurationPlugInEventType();
        config.setEventRepresentationResolutionURIs(resolutionURIs);
        config.setInitializer(initializer);
        plugInEventTypes.put(eventTypeName, config);
    }

    public void setPlugInEventTypeResolutionURIs(URI[] urisToResolveName) {
        plugInEventTypeResolutionURIs = urisToResolveName;
    }

    public URI[] getPlugInEventTypeResolutionURIs() {
        return plugInEventTypeResolutionURIs;
    }

    public Map<URI, ConfigurationPlugInEventRepresentation> getPlugInEventRepresentation() {
        return plugInEventRepresentation;
    }

    public Map<String, ConfigurationPlugInEventType> getPlugInEventTypes() {
        return plugInEventTypes;
    }

    public Set<String> getEventTypeAutoNamePackages() {
        return eventTypeAutoNamePackages;
    }

    public ConfigurationEngineDefaults getEngineDefaults() {
        return engineDefaults;
    }

    public void addVariantStream(String varianteventTypeName, ConfigurationVariantStream variantStreamConfig) {
        variantStreams.put(varianteventTypeName, variantStreamConfig);
    }

    public Map<String, ConfigurationVariantStream> getVariantStreams() {
        return variantStreams;
    }

    public void updateMapEventType(String mapeventTypeName, Map<String, Object> typeMap) throws ConfigurationException {
        throw new UnsupportedOperationException("Map type update is only available in runtime configuration");
    }

    public void updateObjectArrayEventType(String myEvent, String[] namesNew, Object[] typesNew) {
        throw new UnsupportedOperationException("Object-array type update is only available in runtime configuration");
    }

    public void replaceXMLEventType(String xmlEventTypeName, ConfigurationEventTypeXMLDOM config) throws ConfigurationException {
        throw new UnsupportedOperationException("XML type update is only available in runtime configuration");
    }

    public Set<String> getEventTypeNameUsedBy(String name) {
        throw new UnsupportedOperationException("Get event type by name is only available in runtime configuration");
    }

    public boolean isVariantStreamExists(String name) {
        return variantStreams.containsKey(name);
    }

    public void setMetricsReportingInterval(String stmtGroupName, long newInterval) {
        this.getEngineDefaults().getMetricsReporting().setStatementGroupInterval(stmtGroupName, newInterval);
    }

    public void setMetricsReportingStmtEnabled(String statementName) {
        throw new UnsupportedOperationException("Statement metric reporting can only be enabled or disabled at runtime");
    }

    public void setMetricsReportingStmtDisabled(String statementName) {
        throw new UnsupportedOperationException("Statement metric reporting can only be enabled or disabled at runtime");
    }

    public EventType getEventType(String eventTypeName) {
        throw new UnsupportedOperationException("Obtaining an event type by name is only available at runtime");
    }

    public EventType[] getEventTypes() {
        throw new UnsupportedOperationException("Obtaining event types is only available at runtime");
    }

    public void setMetricsReportingEnabled() {
        this.getEngineDefaults().getMetricsReporting().setEnableMetricsReporting(true);
    }

    public void setMetricsReportingDisabled() {
        this.getEngineDefaults().getMetricsReporting().setEnableMetricsReporting(false);
    }

    public void setPatternMaxSubexpressions(Long maxSubexpressions) {
        this.getEngineDefaults().getPatterns().setMaxSubexpressions(maxSubexpressions);
    }

    public void setMatchRecognizeMaxStates(Long maxStates) {
        this.getEngineDefaults().getMatchRecognize().setMaxStates(maxStates);
    }

    /**
     * Use the configuration specified in an application
     * resource named <tt>esper.cfg.xml</tt>.
     *
     * @return Configuration initialized from the resource
     * @throws EPException thrown to indicate error reading configuration
     */
    public Configuration configure() throws EPException {
        configure('/' + ESPER_DEFAULT_CONFIG);
        return this;
    }

    /**
     * Use the configuration specified in the given application
     * resource. The format of the resource is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     * <p>
     * The resource is found via <tt>getConfigurationInputStream(resource)</tt>.
     * That method can be overridden to implement an arbitrary lookup strategy.
     * </p>
     * <p>
     * See <tt>getResourceAsStream</tt> for information on how the resource name is resolved.
     * </p>
     *
     * @param resource if the file name of the resource
     * @return Configuration initialized from the resource
     * @throws EPException thrown to indicate error reading configuration
     */
    public Configuration configure(String resource) throws EPException {
        if (log.isDebugEnabled()) {
            log.debug("Configuring from resource: " + resource);
        }
        InputStream stream = getConfigurationInputStream(resource);
        ConfigurationParser.doConfigure(this, stream, resource);
        return this;
    }

    /**
     * Get the configuration file as an <tt>InputStream</tt>. Might be overridden
     * by subclasses to allow the configuration to be located by some arbitrary
     * mechanism.
     * <p>
     * See <tt>getResourceAsStream</tt> for information on how the resource name is resolved.
     *
     * @param resource is the resource name
     * @return input stream for resource
     * @throws EPException thrown to indicate error reading configuration
     */
    protected static InputStream getConfigurationInputStream(String resource) throws EPException {
        return getResourceAsStream(resource);
    }


    /**
     * Use the configuration specified by the given URL.
     * The format of the document obtained from the URL is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     *
     * @param url URL from which you wish to load the configuration
     * @return A configuration configured via the file
     * @throws EPException is thrown when the URL could not be access
     */
    public Configuration configure(URL url) throws EPException {
        if (log.isDebugEnabled()) {
            log.debug("configuring from url: " + url.toString());
        }
        try {
            ConfigurationParser.doConfigure(this, url.openStream(), url.toString());
            return this;
        } catch (IOException ioe) {
            throw new EPException("could not configure from URL: " + url, ioe);
        }
    }

    /**
     * Use the configuration specified in the given application
     * file. The format of the file is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     *
     * @param configFile <tt>File</tt> from which you wish to load the configuration
     * @return A configuration configured via the file
     * @throws EPException when the file could not be found
     */
    public Configuration configure(File configFile) throws EPException {
        if (log.isDebugEnabled()) {
            log.debug("configuring from file: " + configFile.getName());
        }

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
            ConfigurationParser.doConfigure(this, inputStream, configFile.toString());
        } catch (FileNotFoundException fnfe) {
            throw new EPException("could not find file: " + configFile, fnfe);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.debug("Error closing input stream", e);
                }
            }
        }
        return this;
    }

    public boolean removeEventType(String eventTypeName, boolean force) throws ConfigurationException {
        eventClasses.remove(eventTypeName);
        eventTypesXMLDOM.remove(eventTypeName);
        eventTypesAvro.remove(eventTypeName);
        eventTypesLegacy.remove(eventTypeName);
        mapNames.remove(eventTypeName);
        nestableMapNames.remove(eventTypeName);
        mapTypeConfigurations.remove(eventTypeName);
        plugInEventTypes.remove(eventTypeName);
        revisionEventTypes.remove(eventTypeName);
        variantStreams.remove(eventTypeName);
        return true;
    }

    public Set<String> getVariableNameUsedBy(String variableName) {
        throw new UnsupportedOperationException("Get variable use information is only available in runtime configuration");
    }

    public boolean removeVariable(String name, boolean force) throws ConfigurationException {
        return this.variables.remove(name) != null;
    }

    public void addEventTypeAvro(String eventTypeName, ConfigurationEventTypeAvro avro) {
        eventTypesAvro.put(eventTypeName, avro);
    }

    /**
     * Use the mappings and properties specified in the given XML document.
     * The format of the file is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     *
     * @param document an XML document from which you wish to load the configuration
     * @return A configuration configured via the <tt>Document</tt>
     * @throws EPException if there is problem in accessing the document.
     */
    public Configuration configure(Document document) throws EPException {
        if (log.isDebugEnabled()) {
            log.debug("configuring from XML document");
        }
        ConfigurationParser.doConfigure(this, document);
        return this;
    }

    /**
     * Returns an input stream from an application resource in the classpath.
     * <p>
     * The method first removes the '/' character from the resource name if
     * the first character is '/'.
     * <p>
     * The lookup order is as follows:
     * <p>
     * If a thread context class loader exists, use <tt>Thread.currentThread().getResourceAsStream</tt>
     * to obtain an InputStream.
     * <p>
     * If no input stream was returned, use the <tt>Configuration.class.getResourceAsStream</tt>.
     * to obtain an InputStream.
     * <p>
     * If no input stream was returned, use the <tt>Configuration.class.getClassLoader().getResourceAsStream</tt>.
     * to obtain an InputStream.
     * <p>
     * If no input stream was returned, throw an Exception.
     *
     * @param resource to get input stream for
     * @return input stream for resource
     */
    protected static InputStream getResourceAsStream(String resource) {
        String stripped = resource.startsWith("/") ?
                resource.substring(1) : resource;

        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(stripped);
        }
        if (stream == null) {
            stream = Configuration.class.getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = Configuration.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (stream == null) {
            throw new EPException(resource + " not found");
        }
        return stream;
    }

    /**
     * Reset to an empty configuration.
     */
    protected void reset() {
        eventClasses = new LinkedHashMap<String, String>();
        mapNames = new LinkedHashMap<String, Properties>();
        nestableMapNames = new LinkedHashMap<String, Map<String, Object>>();
        nestableObjectArrayNames = new LinkedHashMap<String, Map<String, Object>>();
        eventTypesXMLDOM = new LinkedHashMap<String, ConfigurationEventTypeXMLDOM>();
        eventTypesAvro = new LinkedHashMap<String, ConfigurationEventTypeAvro>();
        eventTypesLegacy = new LinkedHashMap<String, ConfigurationEventTypeLegacy>();
        databaseReferences = new HashMap<String, ConfigurationDBRef>();
        imports = new ArrayList<String>();
        annotationImports = new ArrayList<String>(2);
        addDefaultImports();
        plugInViews = new ArrayList<ConfigurationPlugInView>();
        plugInVirtualDataWindows = new ArrayList<ConfigurationPlugInVirtualDataWindow>();
        pluginLoaders = new ArrayList<ConfigurationPluginLoader>();
        plugInAggregationFunctions = new ArrayList<ConfigurationPlugInAggregationFunction>();
        plugInAggregationMultiFunctions = new ArrayList<ConfigurationPlugInAggregationMultiFunction>();
        plugInSingleRowFunctions = new ArrayList<ConfigurationPlugInSingleRowFunction>();
        plugInPatternObjects = new ArrayList<ConfigurationPlugInPatternObject>();
        engineDefaults = new ConfigurationEngineDefaults();
        eventTypeAutoNamePackages = new LinkedHashSet<String>();
        variables = new HashMap<String, ConfigurationVariable>();
        methodInvocationReferences = new HashMap<String, ConfigurationMethodRef>();
        plugInEventRepresentation = new LinkedHashMap<URI, ConfigurationPlugInEventRepresentation>();
        plugInEventTypes = new LinkedHashMap<String, ConfigurationPlugInEventType>();
        revisionEventTypes = new LinkedHashMap<String, ConfigurationRevisionEventType>();
        variantStreams = new HashMap<String, ConfigurationVariantStream>();
        mapTypeConfigurations = new HashMap<String, ConfigurationEventTypeMap>();
        objectArrayTypeConfigurations = new HashMap<String, ConfigurationEventTypeObjectArray>();
        transientConfiguration = new HashMap<>();
    }

    /**
     * Use these imports until the user specifies something else.
     */
    private void addDefaultImports() {
        imports.add("java.lang.*");
        imports.add("java.math.*");
        imports.add("java.text.*");
        imports.add("java.util.*");
        imports.add(ANNOTATION_IMPORT);
        imports.add("com.espertech.esper.dataflow.ops.*");
    }

    /**
     * Enumeration of different resolution styles for resolving property names.
     */
    public static enum PropertyResolutionStyle {
        /**
         * Properties are only matched if the names are identical in name
         * and case to the original property name.
         */
        CASE_SENSITIVE,

        /**
         * Properties are matched if the names are identical.  A case insensitive
         * search is used and will choose the first property that matches
         * the name exactly or the first property that matches case insensitively
         * should no match be found.
         */
        CASE_INSENSITIVE,

        /**
         * Properties are matched if the names are identical.  A case insensitive
         * search is used and will choose the first property that matches
         * the name exactly case insensitively.  If more than one 'name' can be
         * mapped to the property an exception is thrown.
         */
        DISTINCT_CASE_INSENSITIVE;

        /**
         * Returns the default property resolution style.
         *
         * @return is the case-sensitive resolution
         */
        public static PropertyResolutionStyle getDefault() {
            return CASE_SENSITIVE;
        }
    }

}
