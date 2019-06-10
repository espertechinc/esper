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
package com.espertech.esper.common.client.configuration.common;

import com.espertech.esper.common.client.annotation.Name;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.internal.epl.dataflow.ops.BeaconSourceForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.io.Serializable;
import java.util.*;

/**
 * Contains settings that apply to both the compile-time and the runtime.
 */
public class ConfigurationCommon implements Serializable {
    /**
     * Import name of the package that hosts the annotation classes.
     */
    public static final String ANNOTATION_IMPORT = Name.class.getPackage().getName() + ".*";

    /**
     * Import package for data flow operator forges.
     */
    public static final String DATAFLOWOPERATOR_IMPORT = BeaconSourceForge.class.getPackage().getName() + ".*";
    private static final long serialVersionUID = 5944286520474247829L;

    /**
     * Map of event name and fully-qualified class name.
     */
    protected Map<String, String> eventClasses;

    /**
     * Map of event type name and XML DOM configuration.
     */
    protected Map<String, ConfigurationCommonEventTypeXMLDOM> eventTypesXMLDOM;

    /**
     * Map of event type name and XML DOM configuration.
     */
    protected Map<String, ConfigurationCommonEventTypeAvro> eventTypesAvro;

    /**
     * Map of event type name and bean-type event configuration.
     */
    protected Map<String, ConfigurationCommonEventTypeBean> eventTypesBean;

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
    protected Map<String, ConfigurationCommonEventTypeMap> mapTypeConfigurations;

    /**
     * Map event types additional configuration information.
     */
    protected Map<String, ConfigurationCommonEventTypeObjectArray> objectArrayTypeConfigurations;

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
    protected Map<String, ConfigurationCommonDBRef> databaseReferences;

    /**
     * Map of variables.
     */
    protected Map<String, ConfigurationCommonVariable> variables;

    /**
     * Map of class name and configuration for method invocations on that class.
     */
    protected Map<String, ConfigurationCommonMethodRef> methodInvocationReferences;

    /**
     * Variant streams allow events of disparate types to be treated the same.
     */
    protected Map<String, ConfigurationCommonVariantStream> variantStreams;

    /**
     * Transient configuration.
     */
    protected transient Map<String, Object> transientConfiguration;

    /**
     * Event type common configuration
     */
    protected ConfigurationCommonEventTypeMeta eventMeta;

    /**
     * Logging configuration.
     */
    protected ConfigurationCommonLogging logging;

    /**
     * Time source configuration
     */
    protected ConfigurationCommonTimeSource timeSource;

    /**
     * Execution-related configuration
     */
    protected ConfigurationCommonExecution execution;

    /**
     * Event type auto-name packages.
     */
    private Set<String> eventTypeAutoNamePackages;

    /**
     * Constructs an empty configuration. The auto import values
     * are set by default to java.lang, java.math, java.text and
     * java.util.
     */
    public ConfigurationCommon() {
        reset();
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

    /**
     * Add an name for an event type that represents java.util.Map events,
     * and for which each property may itself be a Map of further properties,
     * with unlimited nesting levels.
     * <p>
     * Each entry in the type mapping must contain the String property name as the key value,
     * and either a Class, or a further Map&lt;String, Object&gt;, or the name
     * of another previously-register Map event type (append [] for array of Map).
     *
     * @param eventTypeName is the name for the event type
     * @param typeMap       maps the name of each property in the Map event to the type
     *                      (fully qualified classname) of its value in Map event instances.
     */
    public void addEventType(String eventTypeName, Map<String, Object> typeMap) {
        nestableMapNames.put(eventTypeName, new LinkedHashMap<String, Object>(typeMap));
    }

    /**
     * Add a name for an event type that represents java.util.Map events,
     * and for which each property may itself be a Map of further properties,
     * with unlimited nesting levels.
     * <p>
     * Each entry in the type mapping must contain the String property name as the key value,
     * and either a Class, or a further Map&lt;String, Object&gt;, or the name
     * of another previously-register Map event type (append [] for array of Map).
     *
     * @param eventTypeName is the name for the event type
     * @param typeMap       maps the name of each property in the Map event to the type
     *                      (fully qualified classname) of its value in Map event instances.
     * @param superTypes    is an array of event type name of further Map types that this
     */
    public void addEventType(String eventTypeName, Map<String, Object> typeMap, String[] superTypes) {
        nestableMapNames.put(eventTypeName, new LinkedHashMap<String, Object>(typeMap));
        if (superTypes != null) {
            for (int i = 0; i < superTypes.length; i++) {
                this.addMapSuperType(eventTypeName, superTypes[i]);
            }
        }
    }

    /**
     * Add a name for an event type that represents java.util.Map events,
     * and for which each property may itself be a Map of further properties,
     * with unlimited nesting levels.
     * <p>
     * Each entry in the type mapping must contain the String property name as the key value,
     * and either a Class, or a further Map&lt;String, Object&gt;, or the name
     * of another previously-register Map event type (append [] for array of Map).
     *
     * @param eventTypeName is the name for the event type
     * @param typeMap       maps the name of each property in the Map event to the type
     *                      (fully qualified classname) of its value in Map event instances.
     * @param mapConfig     is the Map-event type configuration that may defined super-types, timestamp-property-name etc.
     */
    public void addEventType(String eventTypeName, Map<String, Object> typeMap, ConfigurationCommonEventTypeMap mapConfig) {
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
        ConfigurationCommonEventTypeMap current = mapTypeConfigurations.get(mapeventTypeName);
        if (current == null) {
            current = new ConfigurationCommonEventTypeMap();
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
        ConfigurationCommonEventTypeObjectArray current = objectArrayTypeConfigurations.get(eventTypeName);
        if (current == null) {
            current = new ConfigurationCommonEventTypeObjectArray();
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
    public void addMapConfiguration(String mapeventTypeName, ConfigurationCommonEventTypeMap config) {
        mapTypeConfigurations.put(mapeventTypeName, config);
    }

    /**
     * Add configuration for a object array event type.
     *
     * @param objectArrayeventTypeName configuration to add
     * @param config                   map type configuration
     */
    public void addObjectArrayConfiguration(String objectArrayeventTypeName, ConfigurationCommonEventTypeObjectArray config) {
        objectArrayTypeConfigurations.put(objectArrayeventTypeName, config);
    }

    /**
     * Add an name for an event type that represents org.w3c.dom.Node events.
     *
     * @param eventTypeName       is the name for the event type
     * @param xmlDOMEventTypeDesc descriptor containing property and mapping information for XML-DOM events
     */
    public void addEventType(String eventTypeName, ConfigurationCommonEventTypeXMLDOM xmlDOMEventTypeDesc) {
        eventTypesXMLDOM.put(eventTypeName, xmlDOMEventTypeDesc);
    }

    /**
     * Add an event type that represents Object-array (Object[]) events.
     *
     * @param eventTypeName is the name for the event type
     * @param propertyNames name of each property, length must match number of types
     * @param propertyTypes type of each property, length must match number of names
     * @throws ConfigurationException if the name is already in used for a different type
     */
    public void addEventType(String eventTypeName, String[] propertyNames, Object[] propertyTypes) {
        LinkedHashMap<String, Object> propertyTypesMap = EventTypeUtility.validateObjectArrayDef(propertyNames, propertyTypes);
        nestableObjectArrayNames.put(eventTypeName, propertyTypesMap);
    }

    /**
     * Add an event type that represents Object-array (Object[]) events.
     *
     * @param eventTypeName         is the name for the event type
     * @param propertyNames         name of each property, length must match number of types
     * @param propertyTypes         type of each property, length must match number of names
     * @param optionalConfiguration object-array type configuration
     * @throws ConfigurationException if the name is already in used for a different type
     */
    public void addEventType(String eventTypeName, String[] propertyNames, Object[] propertyTypes, ConfigurationCommonEventTypeObjectArray optionalConfiguration) throws ConfigurationException {
        LinkedHashMap<String, Object> propertyTypesMap = EventTypeUtility.validateObjectArrayDef(propertyNames, propertyTypes);
        nestableObjectArrayNames.put(eventTypeName, propertyTypesMap);
        objectArrayTypeConfigurations.put(eventTypeName, optionalConfiguration);
        if (optionalConfiguration.getSuperTypes() != null && optionalConfiguration.getSuperTypes().size() > 1) {
            throw new ConfigurationException(ConfigurationCommonEventTypeObjectArray.SINGLE_SUPERTYPE_MSG);
        }
    }

    /**
     * Add a database reference with a given database name.
     *
     * @param name               is the database name
     * @param configurationDBRef descriptor containing database connection and access policy information
     */
    public void addDatabaseReference(String name, ConfigurationCommonDBRef configurationDBRef) {
        databaseReferences.put(name, configurationDBRef);
    }

    /**
     * Add an name for an event type that represents bean Java type (non-JavaBean style) events.
     * Note that when adding multiple names for the same Java class the names represent an
     * alias to the same event type since event type identity for Java classes is per Java class.
     *
     * @param eventTypeName     is the name for the event type
     * @param eventClass        fully-qualified class name of the event type
     * @param beanEventTypeDesc descriptor containing property and mapping information for Legacy Java type events
     */
    public void addEventType(String eventTypeName, String eventClass, ConfigurationCommonEventTypeBean beanEventTypeDesc) {
        eventClasses.put(eventTypeName, eventClass);
        eventTypesBean.put(eventTypeName, beanEventTypeDesc);
    }

    /**
     * Adds a package or class to the list of automatically-imported classes and packages.
     * <p>
     * To import a single class offering a static method, simply supply the fully-qualified name of the class
     * and use the syntax {@code classname.methodname(...)}
     * <p>
     * To import a whole package and use the {@code classname.methodname(...)} syntax, specify a package
     * with wildcard, such as {@code com.mycompany.staticlib.*}.
     *
     * @param autoImport is a fully-qualified class name or a package name with wildcard
     */
    public void addImport(String autoImport) {
        imports.add(autoImport);
    }

    /**
     * Adds a class to the list of automatically-imported classes.
     * <p>
     * Use #addImport(String) to import a package.
     *
     * @param autoImport is a class to import
     */
    public void addImport(Class autoImport) {
        addImport(autoImport.getName());
    }

    /**
     * Adds a package or class to the list of automatically-imported classes and packages for use by annotations only.
     *
     * @param importName import such as package name, class name, or package with ".*".
     * @throws ConfigurationException if incorrect package or class names are encountered
     */
    public void addAnnotationImport(String importName) {
        annotationImports.add(importName);
    }

    /**
     * Add a class to the imports available for annotations only
     *
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
    public void addMethodRef(String className, ConfigurationCommonMethodRef methodInvocationConfig) {
        this.methodInvocationReferences.put(className, methodInvocationConfig);
    }

    /**
     * Adds a cache configuration for a class providing methods for use in the from-clause.
     *
     * @param clazz                  is the class providing methods
     * @param methodInvocationConfig is the cache configuration
     */
    public void addMethodRef(Class clazz, ConfigurationCommonMethodRef methodInvocationConfig) {
        this.methodInvocationReferences.put(clazz.getName(), methodInvocationConfig);
    }

    /**
     * Returns the mapping of event type name to Java class name.
     *
     * @return event type names for Java class names
     */
    public Map<String, String> getEventTypeNames() {
        return eventClasses;
    }

    /**
     * Returns a map keyed by event type name, and values being the definition for the
     * Map event type of the property names and types that make up the event.
     *
     * @return map of event type name and definition of event properties
     */
    public Map<String, Properties> getEventTypesMapEvents() {
        return mapNames;
    }

    /**
     * Returns a map keyed by event type name, and values being the definition for the
     * event type of the property names and types that make up the event,
     * for nestable, strongly-typed Map-based event representations.
     *
     * @return map of event type name and definition of event properties
     */
    public Map<String, Map<String, Object>> getEventTypesNestableMapEvents() {
        return nestableMapNames;
    }

    /**
     * Returns the object-array event types.
     *
     * @return object-array event types
     */
    public Map<String, Map<String, Object>> getEventTypesNestableObjectArrayEvents() {
        return nestableObjectArrayNames;
    }

    /**
     * Returns the mapping of event type name to XML DOM event type information.
     *
     * @return event type name mapping to XML DOM configs
     */
    public Map<String, ConfigurationCommonEventTypeXMLDOM> getEventTypesXMLDOM() {
        return eventTypesXMLDOM;
    }

    /**
     * Returns the Avro event types.
     *
     * @return Avro event types
     */
    public Map<String, ConfigurationCommonEventTypeAvro> getEventTypesAvro() {
        return eventTypesAvro;
    }

    /**
     * Returns the mapping of event type name to legacy java event type information.
     *
     * @return event type name mapping to legacy java class configs
     */
    public Map<String, ConfigurationCommonEventTypeBean> getEventTypesBean() {
        return eventTypesBean;
    }

    /**
     * Returns the imports
     *
     * @return imports
     */
    public List<String> getImports() {
        return imports;
    }

    /**
     * Returns the annotation imports
     *
     * @return annotation imports
     */
    public List<String> getAnnotationImports() {
        return annotationImports;
    }

    /**
     * Returns the database names
     *
     * @return database names
     */
    public Map<String, ConfigurationCommonDBRef> getDatabaseReferences() {
        return databaseReferences;
    }

    /**
     * Returns the object-array event type configurations.
     *
     * @return type configs
     */
    public Map<String, ConfigurationCommonEventTypeObjectArray> getObjectArrayTypeConfigurations() {
        return objectArrayTypeConfigurations;
    }

    /**
     * Returns the preconfigured variables
     *
     * @return variables
     */
    public Map<String, ConfigurationCommonVariable> getVariables() {
        return variables;
    }

    /**
     * Returns the method-invocation-names for use in joins
     *
     * @return method-invocation-names
     */
    public Map<String, ConfigurationCommonMethodRef> getMethodInvocationReferences() {
        return methodInvocationReferences;
    }

    /**
     * Returns for each Map event type name the set of supertype event type names (Map types only).
     *
     * @return map of name to set of supertype names
     */
    public Map<String, ConfigurationCommonEventTypeMap> getMapTypeConfigurations() {
        return mapTypeConfigurations;
    }

    /**
     * Add a global variable.
     * <p>
     * Use the runtime API to set variable values or EPL statements to change variable values.
     *
     * @param variableName        name of the variable to add
     * @param type                the type name of the variable, must be a primitive or boxed Java-builtin scalar type or "object" for any
     *                            value or an event type name or a class name or fully-qualified class name.  Append "[]" for array.
     * @param initializationValue is the first assigned value.
     *                            For static initialization the value can be string-typed and will be parsed.
     *                            For static initialization the initialization value, if provided, must implement {@link java.io.Serializable} or {@link java.io.Externalizable}.
     * @throws ConfigurationException if the type and initialization value don't match or the variable name
     *                                is already in use
     */
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

    /**
     * Add a global variable.
     * <p>
     * Use the runtime API to set variable values or EPL statements to change variable values.
     *
     * @param variableName        name of the variable to add
     * @param type                the type name of the variable, must be a primitive or boxed Java-builtin scalar type or "object" for any
     *                            value or an event type name or a class name or fully-qualified class name.  Append "[]" for array.
     * @param initializationValue is the first assigned value
     *                            The value can be string-typed and will be parsed.
     *                            For static initialization the initialization value, if provided, must implement {@link java.io.Serializable} or {@link java.io.Externalizable}.
     * @throws ConfigurationException if the type and initialization value don't match or the variable name
     *                                is already in use
     */
    public void addVariable(String variableName, String type, Object initializationValue) {
        addVariable(variableName, type, initializationValue, false);
    }

    /**
     * Add a global variable, allowing constants.
     * <p>
     * Use the runtime API to set variable values or EPL statements to change variable values.
     *
     * @param variableName        name of the variable to add
     * @param type                the type name of the variable, must be a primitive or boxed Java-builtin scalar type or "object" for any
     *                            value or an event type name or a class name or fully-qualified class name.  Append "[]" for array.
     * @param initializationValue is the first assigned value
     *                            For static initialization the value can be string-typed and will be parsed.
     *                            For static initialization the initialization value, if provided, must implement {@link java.io.Serializable} or {@link java.io.Externalizable}.
     * @param constant            true to identify the variable as a constant
     * @throws ConfigurationException if the type and initialization value don't match or the variable name
     *                                is already in use
     */
    public void addVariable(String variableName, String type, Object initializationValue, boolean constant) {
        ConfigurationCommonVariable configVar = new ConfigurationCommonVariable();
        configVar.setType(type);
        configVar.setInitializationValue(initializationValue);
        configVar.setConstant(constant);
        variables.put(variableName, configVar);
    }

    /**
     * Adds a new variant stream. Variant streams allow events of disparate types to be treated the same.
     *
     * @param variantStreamName   is the name of the variant stream
     * @param variantStreamConfig the configuration such as variant type names and any-type setting
     */
    public void addVariantStream(String variantStreamName, ConfigurationCommonVariantStream variantStreamConfig) {
        variantStreams.put(variantStreamName, variantStreamConfig);
    }

    /**
     * Returns a map of variant stream name and variant configuration information. Variant streams allows handling
     * events of all sorts of different event types the same way.
     *
     * @return map of name and variant stream config
     */
    public Map<String, ConfigurationCommonVariantStream> getVariantStreams() {
        return variantStreams;
    }

    /**
     * Returns true if a variant stream by the name has been declared, or false if not.
     *
     * @param name of variant stream
     * @return indicator whether the variant stream by that name exists
     */
    public boolean isVariantStreamExists(String name) {
        return variantStreams.containsKey(name);
    }

    /**
     * Adds an Avro event type
     *
     * @param eventTypeName type name
     * @param avro          configs
     */
    public void addEventTypeAvro(String eventTypeName, ConfigurationCommonEventTypeAvro avro) {
        eventTypesAvro.put(eventTypeName, avro);
    }

    /**
     * Returns transient configuration, i.e. information that is passed along as a reference and not as a value
     *
     * @return map of transients
     */
    public Map<String, Object> getTransientConfiguration() {
        return transientConfiguration;
    }

    /**
     * Sets transient configuration, i.e. information that is passed along as a reference and not as a value
     *
     * @param transientConfiguration map of transients
     */
    public void setTransientConfiguration(Map<String, Object> transientConfiguration) {
        this.transientConfiguration = transientConfiguration;
    }

    /**
     * Returns event representation default settings.
     *
     * @return event representation default settings
     */
    public ConfigurationCommonEventTypeMeta getEventMeta() {
        return eventMeta;
    }

    /**
     * Returns the time source configuration.
     *
     * @return time source enum
     */
    public ConfigurationCommonTimeSource getTimeSource() {
        return timeSource;
    }

    /**
     * Reset to an empty configuration.
     */
    protected void reset() {
        eventClasses = new LinkedHashMap<>();
        mapNames = new LinkedHashMap<>();
        nestableMapNames = new LinkedHashMap<>();
        nestableObjectArrayNames = new LinkedHashMap<>();
        eventTypesXMLDOM = new LinkedHashMap<>();
        eventTypesAvro = new LinkedHashMap<>();
        eventTypesBean = new LinkedHashMap<>();
        databaseReferences = new HashMap<>();
        imports = new ArrayList<>();
        annotationImports = new ArrayList<>(2);
        addDefaultImports();
        variables = new HashMap<>();
        methodInvocationReferences = new HashMap<>();
        variantStreams = new HashMap<>();
        mapTypeConfigurations = new HashMap<>();
        objectArrayTypeConfigurations = new HashMap<>();
        eventMeta = new ConfigurationCommonEventTypeMeta();
        logging = new ConfigurationCommonLogging();
        timeSource = new ConfigurationCommonTimeSource();
        transientConfiguration = new HashMap<>(2);
        eventTypeAutoNamePackages = new HashSet<>(2);
        execution = new ConfigurationCommonExecution();
    }

    /**
     * Returns logging settings applicable to common.
     *
     * @return logging settings
     */
    public ConfigurationCommonLogging getLogging() {
        return logging;
    }

    /**
     * Returns the execution settings.
     *
     * @return execution settings
     */
    public ConfigurationCommonExecution getExecution() {
        return execution;
    }

    /**
     * Adds a Java package name of a package that Java event classes reside in.
     * <p>
     * This setting allows an application to place all it's events into one or more Java packages
     * and then declare these packages via this method. The runtime
     * attempts to resolve an event type name to a Java class residing in each declared package.
     * <p>
     * For example, in the statement "select * from MyEvent" the runtime attempts to load class "javaPackageName.MyEvent"
     * and if successful, uses that class as the event type.
     *
     * @param packageName is the fully-qualified Java package name of the Java package that event classes reside in
     */
    public void addEventTypeAutoName(String packageName) {
        eventTypeAutoNamePackages.add(packageName);
    }

    /**
     * Returns a set of Java package names that Java event classes reside in.
     * <p>
     * This setting allows an application to place all it's events into one or more Java packages
     * and then declare these packages via this method. The runtime
     * attempts to resolve an event type name to a Java class residing in each declared package.
     * <p>
     * For example, in the statement "select * from MyEvent" the runtime attempts to load class "javaPackageName.MyEvent"
     * and if successful, uses that class as the event type.
     *
     * @return set of Java package names to look for events types when encountering a new event type name
     */
    public Set<String> getEventTypeAutoNamePackages() {
        return eventTypeAutoNamePackages;
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
        imports.add(DATAFLOWOPERATOR_IMPORT);
    }
}
