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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.*;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.util.MethodResolverNoSuchMethodException;
import com.espertech.esper.event.*;
import com.espertech.esper.event.property.*;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.MethodResolver;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Implementation of the EventType interface for handling JavaBean-type classes.
 */
public class BeanEventType implements EventTypeSPI, NativeEventType {
    private final EventTypeMetadata metadata;
    private final Class clazz;
    private final EventAdapterService eventAdapterService;
    private final ConfigurationEventTypeLegacy optionalLegacyDef;
    private final int eventTypeId;
    private String[] propertyNames;
    private Map<String, SimplePropertyInfo> simpleProperties;
    private Map<String, InternalEventPropDescriptor> mappedPropertyDescriptors;
    private Map<String, InternalEventPropDescriptor> indexedPropertyDescriptors;
    private EventType[] superTypes;
    private FastClass fastClass;
    private Set<EventType> deepSuperTypes;
    private Configuration.PropertyResolutionStyle propertyResolutionStyle;

    private Map<String, List<SimplePropertyInfo>> simpleSmartPropertyTable;
    private Map<String, List<SimplePropertyInfo>> indexedSmartPropertyTable;
    private Map<String, List<SimplePropertyInfo>> mappedSmartPropertyTable;

    private final Map<String, EventPropertyGetterSPI> propertyGetterCache;
    private EventPropertyDescriptor[] propertyDescriptors;
    private EventPropertyDescriptor[] writeablePropertyDescriptors;
    private Map<String, Pair<EventPropertyDescriptor, BeanEventPropertyWriter>> writerMap;
    private Map<String, EventPropertyDescriptor> propertyDescriptorMap;
    private String factoryMethodName;
    private String copyMethodName;
    private String startTimestampPropertyName;
    private String endTimestampPropertyName;
    private Map<String, EventPropertyGetter> propertyGetterCodegeneratedCache;

    /**
     * Constructor takes a java bean class as an argument.
     *
     * @param clazz               is the class of a java bean or other POJO
     * @param optionalLegacyDef   optional configuration supplying legacy event type information
     * @param eventAdapterService factory for event beans and event types
     * @param metadata            event type metadata
     * @param eventTypeId         type id
     */
    public BeanEventType(EventTypeMetadata metadata,
                         int eventTypeId,
                         Class clazz,
                         EventAdapterService eventAdapterService,
                         ConfigurationEventTypeLegacy optionalLegacyDef
    ) {
        EventTypeUtility.validateEventBeanClassVisibility(clazz);
        this.metadata = metadata;
        this.clazz = clazz;
        this.eventAdapterService = eventAdapterService;
        this.optionalLegacyDef = optionalLegacyDef;
        this.eventTypeId = eventTypeId;
        if (optionalLegacyDef != null) {
            this.factoryMethodName = optionalLegacyDef.getFactoryMethod();
            this.copyMethodName = optionalLegacyDef.getCopyMethod();
            this.propertyResolutionStyle = optionalLegacyDef.getPropertyResolutionStyle();
        } else {
            this.propertyResolutionStyle = eventAdapterService.getBeanEventTypeFactory().getDefaultPropertyResolutionStyle();
        }
        propertyGetterCache = new HashMap<String, EventPropertyGetterSPI>();

        initialize(false, eventAdapterService.getEngineImportService());

        EventTypeUtility.TimestampPropertyDesc desc = EventTypeUtility.validatedDetermineTimestampProps(this, optionalLegacyDef == null ? null : optionalLegacyDef.getStartTimestampPropertyName(), optionalLegacyDef == null ? null : optionalLegacyDef.getEndTimestampPropertyName(), superTypes);
        startTimestampPropertyName = desc.getStart();
        endTimestampPropertyName = desc.getEnd();
    }

    public String getStartTimestampPropertyName() {
        return startTimestampPropertyName;
    }

    public String getEndTimestampPropertyName() {
        return endTimestampPropertyName;
    }

    public ConfigurationEventTypeLegacy getOptionalLegacyDef() {
        return optionalLegacyDef;
    }

    public String getName() {
        return metadata.getPublicName();
    }

    public EventPropertyDescriptor getPropertyDescriptor(String propertyName) {
        return propertyDescriptorMap.get(propertyName);
    }

    public int getEventTypeId() {
        return eventTypeId;
    }

    /**
     * Returns the factory methods name, or null if none defined.
     *
     * @return factory methods name
     */
    public String getFactoryMethodName() {
        return factoryMethodName;
    }

    public final Class getPropertyType(String propertyName) {
        SimplePropertyInfo simpleProp = getSimplePropertyInfo(propertyName);
        if ((simpleProp != null) && (simpleProp.getClazz() != null)) {
            return simpleProp.getClazz();
        }

        Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (prop instanceof SimpleProperty) {
            // there is no such property since it wasn't in simplePropertyTypes
            return null;
        }
        return prop.getPropertyType(this, eventAdapterService);
    }

    public boolean isProperty(String propertyName) {
        if (getPropertyType(propertyName) == null) {
            return false;
        }
        return true;
    }

    public final Class getUnderlyingType() {
        return clazz;
    }

    /**
     * Returns the property resolution style.
     *
     * @return property resolution style
     */
    public Configuration.PropertyResolutionStyle getPropertyResolutionStyle() {
        return propertyResolutionStyle;
    }

    public EventPropertyGetterSPI getGetterSPI(String propertyName) {
        EventPropertyGetterSPI cachedGetter = propertyGetterCache.get(propertyName);
        if (cachedGetter != null) {
            return cachedGetter;
        }

        SimplePropertyInfo simpleProp = getSimplePropertyInfo(propertyName);
        if ((simpleProp != null) && (simpleProp.getter != null)) {
            EventPropertyGetterSPI getter = simpleProp.getGetter();
            propertyGetterCache.put(propertyName, getter);
            return getter;
        }

        Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (prop instanceof SimpleProperty) {
            // there is no such property since it wasn't in simplePropertyGetters
            return null;
        }

        EventPropertyGetterSPI getter = prop.getGetter(this, eventAdapterService);
        propertyGetterCache.put(propertyName, getter);

        return getter;
    }

    public EventPropertyGetter getGetter(String propertyName) {
        if (!eventAdapterService.getEngineImportService().isCodegenEventPropertyGetters()) {
            return getGetterSPI(propertyName);
        }
        if (propertyGetterCodegeneratedCache == null) {
            propertyGetterCodegeneratedCache = new HashMap<>();
        }

        EventPropertyGetter getter = propertyGetterCodegeneratedCache.get(propertyName);
        if (getter != null) {
            return getter;
        }

        EventPropertyGetterSPI getterSPI = getGetterSPI(propertyName);
        if (getterSPI == null) {
            return null;
        }

        EventPropertyGetter getterCode = eventAdapterService.getEngineImportService().codegenGetter(getterSPI, metadata.getPublicName(), propertyName);
        propertyGetterCodegeneratedCache.put(propertyName, getterCode);
        return getterCode;
    }

    public EventPropertyGetterMapped getGetterMapped(String mappedPropertyName) {
        EventPropertyGetterMappedSPI getter = getGetterMappedSPI(mappedPropertyName);
        if (getter == null) {
            return null;
        }
        if (!eventAdapterService.getEngineImportService().isCodegenEventPropertyGetters()) {
            return getter;
        }
        return eventAdapterService.getEngineImportService().codegenGetter(getter, metadata.getPublicName(), mappedPropertyName);
    }

    public EventPropertyGetterMappedSPI getGetterMappedSPI(String propertyName) {
        EventPropertyDescriptor desc = this.propertyDescriptorMap.get(propertyName);
        if (desc == null || !desc.isMapped()) {
            return null;
        }
        MappedProperty mappedProperty = new MappedProperty(propertyName);
        return mappedProperty.getGetter(this, eventAdapterService);
    }

    public EventPropertyGetterIndexed getGetterIndexed(String indexedPropertyName) {
        EventPropertyGetterIndexedSPI getter = getGetterIndexedSPI(indexedPropertyName);
        if (getter == null) {
            return null;
        }
        if (!eventAdapterService.getEngineImportService().isCodegenEventPropertyGetters()) {
            return getter;
        }
        return eventAdapterService.getEngineImportService().codegenGetter(getter, metadata.getPublicName(), indexedPropertyName);
    }

    public EventPropertyGetterIndexedSPI getGetterIndexedSPI(String indexedPropertyName) {
        EventPropertyDescriptor desc = this.propertyDescriptorMap.get(indexedPropertyName);
        if (desc == null || !desc.isIndexed()) {
            return null;
        }
        IndexedProperty indexedProperty = new IndexedProperty(indexedPropertyName);
        return indexedProperty.getGetter(this, eventAdapterService);
    }

    /**
     * Looks up and returns a cached simple property's descriptor.
     *
     * @param propertyName to look up
     * @return property descriptor
     */
    public final InternalEventPropDescriptor getSimpleProperty(String propertyName) {
        SimplePropertyInfo simpleProp = getSimplePropertyInfo(propertyName);
        if (simpleProp != null) {
            return simpleProp.getDescriptor();
        }
        return null;
    }

    /**
     * Looks up and returns a cached mapped property's descriptor.
     *
     * @param propertyName to look up
     * @return property descriptor
     */
    public final InternalEventPropDescriptor getMappedProperty(String propertyName) {
        if (this.getPropertyResolutionStyle().equals(Configuration.PropertyResolutionStyle.CASE_SENSITIVE)) {
            return mappedPropertyDescriptors.get(propertyName);
        }
        if (this.getPropertyResolutionStyle().equals(Configuration.PropertyResolutionStyle.CASE_INSENSITIVE)) {
            List<SimplePropertyInfo> propertyInfos = mappedSmartPropertyTable.get(propertyName.toLowerCase(Locale.ENGLISH));
            return propertyInfos != null
                    ? propertyInfos.get(0).getDescriptor()
                    : null;
        }
        if (this.getPropertyResolutionStyle().equals(Configuration.PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE)) {
            List<SimplePropertyInfo> propertyInfos = mappedSmartPropertyTable.get(propertyName.toLowerCase(Locale.ENGLISH));
            if (propertyInfos != null) {
                if (propertyInfos.size() != 1) {
                    throw new EPException("Unable to determine which property to use for \"" + propertyName + "\" because more than one property matched");
                }

                return propertyInfos.get(0).getDescriptor();
            }
        }
        return null;
    }

    /**
     * Looks up and returns a cached indexed property's descriptor.
     *
     * @param propertyName to look up
     * @return property descriptor
     */
    public final InternalEventPropDescriptor getIndexedProperty(String propertyName) {
        if (this.getPropertyResolutionStyle().equals(Configuration.PropertyResolutionStyle.CASE_SENSITIVE)) {
            return indexedPropertyDescriptors.get(propertyName);
        }
        if (this.getPropertyResolutionStyle().equals(Configuration.PropertyResolutionStyle.CASE_INSENSITIVE)) {
            List<SimplePropertyInfo> propertyInfos = indexedSmartPropertyTable.get(propertyName.toLowerCase(Locale.ENGLISH));
            return propertyInfos != null
                    ? propertyInfos.get(0).getDescriptor()
                    : null;
        }
        if (this.getPropertyResolutionStyle().equals(Configuration.PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE)) {
            List<SimplePropertyInfo> propertyInfos = indexedSmartPropertyTable.get(propertyName.toLowerCase(Locale.ENGLISH));
            if (propertyInfos != null) {
                if (propertyInfos.size() != 1) {
                    throw new EPException("Unable to determine which property to use for \"" + propertyName + "\" because more than one property matched");
                }

                return propertyInfos.get(0).getDescriptor();
            }
        }
        return null;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public EventType[] getSuperTypes() {
        return superTypes;
    }

    public Iterator<EventType> getDeepSuperTypes() {
        return deepSuperTypes.iterator();
    }

    /**
     * Returns the fast class reference, if code generation is used for this type, else null.
     *
     * @return fast class, or null if no code generation
     */
    public FastClass getFastClass() {
        return fastClass;
    }

    public String toString() {
        return "BeanEventType" +
                " name=" + getName() +
                " clazz=" + clazz.getName();
    }

    private void initialize(boolean isConfigured, EngineImportService engineImportService) {
        PropertyListBuilder propertyListBuilder = PropertyListBuilderFactory.createBuilder(optionalLegacyDef);
        List<InternalEventPropDescriptor> properties = propertyListBuilder.assessProperties(clazz);

        this.propertyDescriptors = new EventPropertyDescriptor[properties.size()];
        this.propertyDescriptorMap = new HashMap<String, EventPropertyDescriptor>();
        this.propertyNames = new String[properties.size()];
        this.simpleProperties = new HashMap<String, SimplePropertyInfo>();
        this.mappedPropertyDescriptors = new HashMap<String, InternalEventPropDescriptor>();
        this.indexedPropertyDescriptors = new HashMap<String, InternalEventPropDescriptor>();

        if (usesSmartResolutionStyle()) {
            simpleSmartPropertyTable = new HashMap<String, List<SimplePropertyInfo>>();
            mappedSmartPropertyTable = new HashMap<String, List<SimplePropertyInfo>>();
            indexedSmartPropertyTable = new HashMap<String, List<SimplePropertyInfo>>();
        }

        if ((optionalLegacyDef == null) ||
                (optionalLegacyDef.getCodeGeneration() != ConfigurationEventTypeLegacy.CodeGeneration.DISABLED)) {
            // get CGLib fast class using current thread class loader
            fastClass = null;
            try {
                fastClass = FastClass.create(engineImportService.getFastClassClassLoader(clazz), clazz);
            } catch (Throwable exWithThreadClassLoader) {
                // get CGLib fast class based on given class (for OSGI support)
                try {
                    fastClass = FastClass.create(clazz);
                } catch (Throwable exWithoutThreadClassLoader) {
                    log.warn(".initialize Unable to obtain CGLib fast class and/or method implementation for class " +
                            clazz.getName() + ", error msg is " + exWithThreadClassLoader.getMessage(), exWithThreadClassLoader);
                    log.warn(".initialize Not using the provided class loader, the error msg is: " + exWithoutThreadClassLoader.getMessage(), exWithoutThreadClassLoader);
                    fastClass = null;
                }
            }
        }

        int count = 0;
        for (InternalEventPropDescriptor desc : properties) {
            String propertyName = desc.getPropertyName();
            Class underlyingType;
            Class componentType;
            boolean isRequiresIndex;
            boolean isRequiresMapkey;
            boolean isIndexed;
            boolean isMapped;
            boolean isFragment;

            if (desc.getPropertyType().equals(EventPropertyType.SIMPLE)) {
                EventPropertyGetterSPI getter;
                Class type;
                if (desc.getReadMethod() != null) {
                    getter = PropertyHelper.getGetter(desc.getReadMethod(), fastClass, eventAdapterService);
                    type = desc.getReadMethod().getReturnType();
                } else {
                    if (desc.getAccessorField() == null) {
                        // Ignore property
                        continue;
                    }
                    getter = new ReflectionPropFieldGetter(desc.getAccessorField(), eventAdapterService);
                    type = desc.getAccessorField().getType();
                }

                underlyingType = type;
                componentType = null;
                isRequiresIndex = false;
                isRequiresMapkey = false;
                isIndexed = false;
                isMapped = false;
                if (JavaClassHelper.isImplementsInterface(type, Map.class)) {
                    isMapped = true;
                    // We do not yet allow to fragment maps entries.
                    // Class genericType = JavaClassHelper.getGenericReturnTypeMap(desc.getReadMethod(), desc.getAccessorField());
                    isFragment = false;

                    if (desc.getReadMethod() != null) {
                        componentType = JavaClassHelper.getGenericReturnTypeMap(desc.getReadMethod(), false);
                    } else if (desc.getAccessorField() != null) {
                        componentType = JavaClassHelper.getGenericFieldTypeMap(desc.getAccessorField(), false);
                    } else {
                        componentType = Object.class;
                    }
                } else if (type.isArray()) {
                    isIndexed = true;
                    isFragment = JavaClassHelper.isFragmentableType(type.getComponentType());
                    componentType = type.getComponentType();
                } else if (JavaClassHelper.isImplementsInterface(type, Iterable.class)) {
                    isIndexed = true;
                    Class genericType = JavaClassHelper.getGenericReturnType(desc.getReadMethod(), desc.getAccessorField(), true);
                    isFragment = JavaClassHelper.isFragmentableType(genericType);
                    if (genericType != null) {
                        componentType = genericType;
                    } else {
                        componentType = Object.class;
                    }
                } else {
                    isMapped = false;
                    isFragment = JavaClassHelper.isFragmentableType(type);
                }
                simpleProperties.put(propertyName, new SimplePropertyInfo(type, getter, desc));

                // Recognize that there may be properties with overlapping case-insentitive names
                if (usesSmartResolutionStyle()) {
                    // Find the property in the smart property table
                    String smartPropertyName = propertyName.toLowerCase(Locale.ENGLISH);
                    List<SimplePropertyInfo> propertyInfoList = simpleSmartPropertyTable.get(smartPropertyName);
                    if (propertyInfoList == null) {
                        propertyInfoList = new ArrayList<SimplePropertyInfo>();
                        simpleSmartPropertyTable.put(smartPropertyName, propertyInfoList);
                    }

                    // Enter the property into the smart property list
                    SimplePropertyInfo propertyInfo = new SimplePropertyInfo(type, getter, desc);
                    propertyInfoList.add(propertyInfo);
                }
            } else if (desc.getPropertyType().equals(EventPropertyType.MAPPED)) {
                mappedPropertyDescriptors.put(propertyName, desc);

                underlyingType = desc.getReturnType();
                componentType = Object.class;
                isRequiresIndex = false;
                isRequiresMapkey = desc.getReadMethod().getParameterTypes().length > 0;
                isIndexed = false;
                isMapped = true;
                isFragment = false;

                // Recognize that there may be properties with overlapping case-insentitive names
                if (usesSmartResolutionStyle()) {
                    // Find the property in the smart property table
                    String smartPropertyName = propertyName.toLowerCase(Locale.ENGLISH);
                    List<SimplePropertyInfo> propertyInfoList = mappedSmartPropertyTable.get(smartPropertyName);
                    if (propertyInfoList == null) {
                        propertyInfoList = new ArrayList<SimplePropertyInfo>();
                        mappedSmartPropertyTable.put(smartPropertyName, propertyInfoList);
                    }

                    // Enter the property into the smart property list
                    SimplePropertyInfo propertyInfo = new SimplePropertyInfo(desc.getReturnType(), null, desc);
                    propertyInfoList.add(propertyInfo);
                }
            } else if (desc.getPropertyType().equals(EventPropertyType.INDEXED)) {
                indexedPropertyDescriptors.put(propertyName, desc);

                underlyingType = desc.getReturnType();
                componentType = null;
                isRequiresIndex = desc.getReadMethod().getParameterTypes().length > 0;
                isRequiresMapkey = false;
                isIndexed = true;
                isMapped = false;
                isFragment = JavaClassHelper.isFragmentableType(desc.getReturnType());

                if (usesSmartResolutionStyle()) {
                    // Find the property in the smart property table
                    String smartPropertyName = propertyName.toLowerCase(Locale.ENGLISH);
                    List<SimplePropertyInfo> propertyInfoList = indexedSmartPropertyTable.get(smartPropertyName);
                    if (propertyInfoList == null) {
                        propertyInfoList = new ArrayList<SimplePropertyInfo>();
                        indexedSmartPropertyTable.put(smartPropertyName, propertyInfoList);
                    }

                    // Enter the property into the smart property list
                    SimplePropertyInfo propertyInfo = new SimplePropertyInfo(desc.getReturnType(), null, desc);
                    propertyInfoList.add(propertyInfo);
                }
            } else {
                continue;
            }

            propertyNames[count] = desc.getPropertyName();
            EventPropertyDescriptor descriptor = new EventPropertyDescriptor(desc.getPropertyName(),
                    underlyingType, componentType, isRequiresIndex, isRequiresMapkey, isIndexed, isMapped, isFragment);
            propertyDescriptors[count++] = descriptor;
            propertyDescriptorMap.put(descriptor.getPropertyName(), descriptor);
        }

        // Determine event type super types
        superTypes = getSuperTypes(clazz, eventAdapterService.getBeanEventTypeFactory());
        if (superTypes != null && superTypes.length == 0) {
            superTypes = null;
        }
        if (metadata != null && metadata.getTypeClass() == EventTypeMetadata.TypeClass.NAMED_WINDOW) {
            superTypes = null;
        }

        // Determine deep supertypes
        // Get Java super types (superclasses and interfaces), deep get of all in the tree
        Set<Class> supers = new HashSet<Class>();
        getSuper(clazz, supers);
        removeJavaLibInterfaces(supers);    // Remove "java." super types

        // Cache the supertypes of this event type for later use
        deepSuperTypes = new HashSet<EventType>();
        for (Class superClass : supers) {
            EventType superType = eventAdapterService.getBeanEventTypeFactory().createBeanType(superClass.getName(), superClass, false, false, isConfigured);
            deepSuperTypes.add(superType);
        }
    }

    private static EventType[] getSuperTypes(Class clazz, BeanEventTypeFactory beanEventTypeFactory) {
        List<Class> superclasses = new LinkedList<Class>();

        // add superclass
        Class superClass = clazz.getSuperclass();
        if (superClass != null) {
            superclasses.add(superClass);
        }

        // add interfaces
        Class[] interfaces = clazz.getInterfaces();
        superclasses.addAll(Arrays.asList(interfaces));

        // Build event types, ignoring java language types
        List<EventType> superTypes = new LinkedList<EventType>();
        for (Class superclass : superclasses) {
            if (!superclass.getName().startsWith("java")) {
                EventType superType = beanEventTypeFactory.createBeanType(superclass.getName(), superclass, false, false, false);
                superTypes.add(superType);
            }
        }

        return superTypes.toArray(new EventType[superTypes.size()]);
    }

    /**
     * Add the given class's implemented interfaces and superclasses to the result set of classes.
     *
     * @param clazz  to introspect
     * @param result to add classes to
     */
    protected static void getSuper(Class clazz, Set<Class> result) {
        getSuperInterfaces(clazz, result);
        getSuperClasses(clazz, result);
    }

    private static void getSuperInterfaces(Class clazz, Set<Class> result) {
        Class[] interfaces = clazz.getInterfaces();

        for (int i = 0; i < interfaces.length; i++) {
            result.add(interfaces[i]);
            getSuperInterfaces(interfaces[i], result);
        }
    }

    private static void getSuperClasses(Class clazz, Set<Class> result) {
        Class superClass = clazz.getSuperclass();
        if (superClass == null) {
            return;
        }

        result.add(superClass);
        getSuper(superClass, result);
    }

    private static void removeJavaLibInterfaces(Set<Class> classes) {
        for (Class clazz : classes.toArray(new Class[0])) {
            if (clazz.getName().startsWith("java")) {
                classes.remove(clazz);
            }
        }
    }

    private boolean usesSmartResolutionStyle() {
        return propertyResolutionStyle.equals(Configuration.PropertyResolutionStyle.CASE_INSENSITIVE) ||
                propertyResolutionStyle.equals(Configuration.PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE);
    }

    private SimplePropertyInfo getSimplePropertyInfo(String propertyName) {
        SimplePropertyInfo propertyInfo;
        List<SimplePropertyInfo> simplePropertyInfoList;

        if (this.getPropertyResolutionStyle().equals(Configuration.PropertyResolutionStyle.CASE_SENSITIVE)) {
            return simpleProperties.get(propertyName);
        }
        if (this.getPropertyResolutionStyle().equals(Configuration.PropertyResolutionStyle.CASE_INSENSITIVE)) {
            propertyInfo = simpleProperties.get(propertyName);
            if (propertyInfo != null) {
                return propertyInfo;
            }

            simplePropertyInfoList = simpleSmartPropertyTable.get(propertyName.toLowerCase(Locale.ENGLISH));
            return
                    simplePropertyInfoList != null
                            ? simplePropertyInfoList.get(0)
                            : null;
        }
        if (this.getPropertyResolutionStyle().equals(Configuration.PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE)) {
            propertyInfo = simpleProperties.get(propertyName);
            if (propertyInfo != null) {
                return propertyInfo;
            }

            simplePropertyInfoList = simpleSmartPropertyTable.get(propertyName.toLowerCase(Locale.ENGLISH));
            if (simplePropertyInfoList != null) {
                if (simplePropertyInfoList.size() != 1) {
                    throw new EPException("Unable to determine which property to use for \"" + propertyName + "\" because more than one property matched");
                }

                return simplePropertyInfoList.get(0);
            }
        }

        return null;
    }

    /**
     * Descriptor caching the getter, class and property info.
     */
    public static class SimplePropertyInfo {
        private Class clazz;
        private EventPropertyGetterSPI getter;
        private InternalEventPropDescriptor descriptor;

        /**
         * Ctor.
         *
         * @param clazz      is the class
         * @param getter     is the getter
         * @param descriptor is the property info
         */
        public SimplePropertyInfo(Class clazz, EventPropertyGetterSPI getter, InternalEventPropDescriptor descriptor) {
            this.clazz = clazz;
            this.getter = getter;
            this.descriptor = descriptor;
        }

        /**
         * Returns the return type.
         *
         * @return return type
         */
        public Class getClazz() {
            return clazz;
        }

        /**
         * Returns the getter.
         *
         * @return getter
         */
        public EventPropertyGetterSPI getGetter() {
            return getter;
        }

        /**
         * Returns the property info.
         *
         * @return property info
         */
        public InternalEventPropDescriptor getDescriptor() {
            return descriptor;
        }
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public FragmentEventType getFragmentType(String propertyExpression) {
        SimplePropertyInfo simpleProp = getSimplePropertyInfo(propertyExpression);
        if ((simpleProp != null) && (simpleProp.getClazz() != null)) {
            GenericPropertyDesc genericProp = simpleProp.getDescriptor().getReturnTypeGeneric();
            return EventBeanUtility.createNativeFragmentType(genericProp.getType(), genericProp.getGeneric(), eventAdapterService);
        }

        Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyExpression);
        if (prop instanceof SimpleProperty) {
            // there is no such property since it wasn't in simplePropertyTypes
            return null;
        }

        GenericPropertyDesc genericProp = prop.getPropertyTypeGeneric(this, eventAdapterService);
        if (genericProp == null) {
            return null;
        }
        return EventBeanUtility.createNativeFragmentType(genericProp.getType(), genericProp.getGeneric(), eventAdapterService);
    }

    public BeanEventPropertyWriter getWriter(String propertyName) {
        if (writeablePropertyDescriptors == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, BeanEventPropertyWriter> pair = writerMap.get(propertyName);
        if (pair != null) {
            return pair.getSecond();
        }

        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (property instanceof MappedProperty) {
            MappedProperty mapProp = (MappedProperty) property;
            String methodName = PropertyHelper.getSetterMethodName(mapProp.getPropertyNameAtomic());
            Method setterMethod;
            try {
                setterMethod = MethodResolver.resolveMethod(clazz, methodName, new Class[]{String.class, Object.class}, true, new boolean[2], new boolean[2]);
            } catch (MethodResolverNoSuchMethodException e) {
                log.info("Failed to find mapped property setter method '" + methodName + "' for writing to property '" + propertyName + "' taking {String, Object} as parameters");
                return null;
            }
            if (setterMethod == null) {
                return null;
            }
            final FastMethod fastMethod = fastClass.getMethod(setterMethod);
            return new BeanEventPropertyWriterMapProp(clazz, fastMethod, mapProp.getKey());
        }

        if (property instanceof IndexedProperty) {
            IndexedProperty indexedProp = (IndexedProperty) property;
            String methodName = PropertyHelper.getSetterMethodName(indexedProp.getPropertyNameAtomic());
            Method setterMethod;
            try {
                setterMethod = MethodResolver.resolveMethod(clazz, methodName, new Class[]{int.class, Object.class}, true, new boolean[2], new boolean[2]);
            } catch (MethodResolverNoSuchMethodException e) {
                log.info("Failed to find indexed property setter method '" + methodName + "' for writing to property '" + propertyName + "' taking {int, Object} as parameters");
                return null;
            }
            if (setterMethod == null) {
                return null;
            }
            final FastMethod fastMethod = fastClass.getMethod(setterMethod);
            return new BeanEventPropertyWriterIndexedProp(clazz, fastMethod, indexedProp.getIndex());
        }

        return null;
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
        if (writeablePropertyDescriptors == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, BeanEventPropertyWriter> pair = writerMap.get(propertyName);
        if (pair != null) {
            return pair.getFirst();
        }

        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (property instanceof MappedProperty) {
            EventPropertyWriter writer = getWriter(propertyName);
            if (writer == null) {
                return null;
            }
            MappedProperty mapProp = (MappedProperty) property;
            return new EventPropertyDescriptor(mapProp.getPropertyNameAtomic(), Object.class, null, false, true, false, true, false);
        }
        if (property instanceof IndexedProperty) {
            EventPropertyWriter writer = getWriter(propertyName);
            if (writer == null) {
                return null;
            }
            IndexedProperty indexedProp = (IndexedProperty) property;
            return new EventPropertyDescriptor(indexedProp.getPropertyNameAtomic(), Object.class, null, true, false, true, false, false);
        }
        return null;
    }

    public EventPropertyDescriptor[] getWriteableProperties() {
        if (writeablePropertyDescriptors == null) {
            initializeWriters();
        }

        return writeablePropertyDescriptors;
    }

    public EventBeanReader getReader() {
        return new BeanEventBeanReader(this);
    }

    public EventBeanCopyMethod getCopyMethod(String[] properties) {
        if (copyMethodName == null) {
            if (JavaClassHelper.isImplementsInterface(clazz, Serializable.class)) {
                return new BeanEventBeanSerializableCopyMethod(this, eventAdapterService);
            }
            return null;
        }
        Method method = null;
        try {
            method = clazz.getMethod(copyMethodName);
        } catch (NoSuchMethodException e) {
            log.error("Configured copy-method for class '" + clazz.getName() + " not found by name '" + copyMethodName + "': " + e.getMessage());
        }
        if (method == null) {
            if (JavaClassHelper.isImplementsInterface(clazz, Serializable.class)) {
                return new BeanEventBeanSerializableCopyMethod(this, eventAdapterService);
            }
            throw new EPException("Configured copy-method for class '" + clazz.getName() + " not found by name '" + copyMethodName + "' and class does not implement Serializable");
        }
        return new BeanEventBeanConfiguredCopyMethod(this, eventAdapterService, fastClass.getMethod(method));
    }

    public EventBeanWriter getWriter(String[] properties) {
        if (writeablePropertyDescriptors == null) {
            initializeWriters();
        }

        BeanEventPropertyWriter[] writers = new BeanEventPropertyWriter[properties.length];
        for (int i = 0; i < properties.length; i++) {
            Pair<EventPropertyDescriptor, BeanEventPropertyWriter> pair = writerMap.get(properties[i]);
            if (pair != null) {
                writers[i] = pair.getSecond();
            } else {
                writers[i] = getWriter(properties[i]);
            }

        }
        return new BeanEventBeanWriter(writers);
    }

    public boolean equalsCompareType(EventType eventType) {
        return this == eventType;
    }

    private void initializeWriters() {
        Set<WriteablePropertyDescriptor> writables = PropertyHelper.getWritableProperties(fastClass.getJavaClass());
        EventPropertyDescriptor[] desc = new EventPropertyDescriptor[writables.size()];
        Map<String, Pair<EventPropertyDescriptor, BeanEventPropertyWriter>> writers = new HashMap<String, Pair<EventPropertyDescriptor, BeanEventPropertyWriter>>();

        int count = 0;
        for (final WriteablePropertyDescriptor writable : writables) {
            EventPropertyDescriptor propertyDesc = new EventPropertyDescriptor(writable.getPropertyName(), writable.getType(), null, false, false, false, false, false);
            desc[count++] = propertyDesc;

            final FastMethod fastMethod = fastClass.getMethod(writable.getWriteMethod());
            writers.put(writable.getPropertyName(), new Pair<EventPropertyDescriptor, BeanEventPropertyWriter>(propertyDesc, new BeanEventPropertyWriter(clazz, fastMethod)));
        }

        writerMap = writers;
        writeablePropertyDescriptors = desc;
    }

    private static final Logger log = LoggerFactory.getLogger(BeanEventType.class);
}
