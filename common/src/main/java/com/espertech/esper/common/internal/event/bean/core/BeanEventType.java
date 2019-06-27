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
package com.espertech.esper.common.internal.event.bean.core;

import com.espertech.esper.common.client.*;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.util.PropertyResolutionStyle;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStem;
import com.espertech.esper.common.internal.event.bean.introspect.PropertyInfo;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.property.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.MethodResolver;
import com.espertech.esper.common.internal.util.MethodResolverNoSuchMethodException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Implementation of the EventType interface for handling JavaBean-type classes.
 */
public class BeanEventType implements EventTypeSPI, NativeEventType {
    private final BeanEventTypeStem stem;
    private EventTypeMetadata metadata;
    private final BeanEventTypeFactory beanEventTypeFactory;
    private final EventType[] superTypes;
    private final Set<EventType> deepSuperTypes;
    private final String startTimestampPropertyName;
    private final String endTimestampPropertyName;
    private final Map<String, EventPropertyGetterSPI> propertyGetterCache = new HashMap<>(4);
    private EventPropertyDescriptor[] writeablePropertyDescriptors;
    private Map<String, Pair<EventPropertyDescriptor, BeanEventPropertyWriter>> writerMap;

    public BeanEventType(BeanEventTypeStem stem, EventTypeMetadata metadata, BeanEventTypeFactory beanEventTypeFactory, EventType[] superTypes, Set<EventType> deepSuperTypes, String startTimestampPropertyName, String endTimestampPropertyName) {
        this.stem = stem;
        this.metadata = metadata;
        this.beanEventTypeFactory = beanEventTypeFactory;
        this.superTypes = superTypes;
        this.deepSuperTypes = deepSuperTypes;

        EventTypeUtility.TimestampPropertyDesc desc = EventTypeUtility.validatedDetermineTimestampProps(this, startTimestampPropertyName, endTimestampPropertyName, superTypes);
        this.startTimestampPropertyName = desc.getStart();
        this.endTimestampPropertyName = desc.getEnd();
    }

    public void setMetadataId(long publicId, long protectedId) {
        metadata = metadata.withIds(publicId, protectedId);
    }

    public String getStartTimestampPropertyName() {
        return startTimestampPropertyName;
    }

    public String getEndTimestampPropertyName() {
        return endTimestampPropertyName;
    }

    public String getName() {
        return metadata.getName();
    }

    public EventPropertyDescriptor getPropertyDescriptor(String propertyName) {
        return stem.getPropertyDescriptorMap().get(propertyName);
    }

    /**
     * Returns the factory methods name, or null if none defined.
     *
     * @return factory methods name
     */
    public String getFactoryMethodName() {
        return stem.getOptionalLegacyDef() == null ? null : stem.getOptionalLegacyDef().getFactoryMethod();
    }

    public final Class getPropertyType(String propertyName) {
        PropertyInfo simpleProp = getSimplePropertyInfo(propertyName);
        if ((simpleProp != null) && (simpleProp.getClazz() != null)) {
            return simpleProp.getClazz();
        }

        Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (prop instanceof SimpleProperty) {
            // there is no such property since it wasn't in simplePropertyTypes
            return null;
        }
        return prop.getPropertyType(this, beanEventTypeFactory);
    }

    public boolean isProperty(String propertyName) {
        if (getPropertyType(propertyName) == null) {
            return false;
        }
        return true;
    }

    public final Class getUnderlyingType() {
        return stem.getClazz();
    }

    /**
     * Returns the property resolution style.
     *
     * @return property resolution style
     */
    public PropertyResolutionStyle getPropertyResolutionStyle() {
        return stem.getPropertyResolutionStyle();
    }

    public EventPropertyGetterSPI getGetterSPI(String propertyName) {
        EventPropertyGetterSPI cachedGetter = propertyGetterCache.get(propertyName);
        if (cachedGetter != null) {
            return cachedGetter;
        }

        PropertyInfo simpleProp = getSimplePropertyInfo(propertyName);
        if ((simpleProp != null) && (simpleProp.getGetterFactory() != null)) {
            EventPropertyGetterSPI getter = simpleProp.getGetterFactory().make(beanEventTypeFactory.getEventBeanTypedEventFactory(), beanEventTypeFactory);
            propertyGetterCache.put(propertyName, getter);
            return getter;
        }

        Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (prop instanceof SimpleProperty) {
            // there is no such property since it wasn't in simplePropertyGetters
            return null;
        }

        EventPropertyGetterSPI getter = prop.getGetter(this, beanEventTypeFactory.getEventBeanTypedEventFactory(), beanEventTypeFactory);
        propertyGetterCache.put(propertyName, getter);

        return getter;
    }

    public EventPropertyGetter getGetter(String propertyName) {
        return getGetterSPI(propertyName);
    }

    public EventPropertyGetterMapped getGetterMapped(String mappedPropertyName) {
        return getGetterMappedSPI(mappedPropertyName);
    }

    public EventPropertyGetterMappedSPI getGetterMappedSPI(String propertyName) {
        EventPropertyDescriptor desc = stem.getPropertyDescriptorMap().get(propertyName);
        if (desc == null || !desc.isMapped()) {
            return null;
        }
        MappedProperty mappedProperty = new MappedProperty(propertyName);
        return mappedProperty.getGetter(this, beanEventTypeFactory.getEventBeanTypedEventFactory(), beanEventTypeFactory);
    }

    public EventPropertyGetterIndexed getGetterIndexed(String indexedPropertyName) {
        return getGetterIndexedSPI(indexedPropertyName);
    }

    public EventPropertyGetterIndexedSPI getGetterIndexedSPI(String indexedPropertyName) {
        EventPropertyDescriptor desc = stem.getPropertyDescriptorMap().get(indexedPropertyName);
        if (desc == null || !desc.isIndexed()) {
            return null;
        }
        IndexedProperty indexedProperty = new IndexedProperty(indexedPropertyName);
        return indexedProperty.getGetter(this, beanEventTypeFactory.getEventBeanTypedEventFactory(), beanEventTypeFactory);
    }

    /**
     * Looks up and returns a cached simple property's descriptor.
     *
     * @param propertyName to look up
     * @return property descriptor
     */
    public final PropertyStem getSimpleProperty(String propertyName) {
        PropertyInfo simpleProp = getSimplePropertyInfo(propertyName);
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
    public final PropertyStem getMappedProperty(String propertyName) {
        if (this.getPropertyResolutionStyle().equals(PropertyResolutionStyle.CASE_SENSITIVE)) {
            return stem.getMappedPropertyDescriptors().get(propertyName);
        }
        if (this.getPropertyResolutionStyle().equals(PropertyResolutionStyle.CASE_INSENSITIVE)) {
            List<PropertyInfo> propertyInfos = stem.getMappedSmartPropertyTable().get(propertyName.toLowerCase(Locale.ENGLISH));
            return propertyInfos != null
                    ? propertyInfos.get(0).getDescriptor()
                    : null;
        }
        if (this.getPropertyResolutionStyle().equals(PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE)) {
            List<PropertyInfo> propertyInfos = stem.getMappedSmartPropertyTable().get(propertyName.toLowerCase(Locale.ENGLISH));
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
    public final PropertyStem getIndexedProperty(String propertyName) {
        if (this.getPropertyResolutionStyle().equals(PropertyResolutionStyle.CASE_SENSITIVE)) {
            return stem.getIndexedPropertyDescriptors().get(propertyName);
        }
        if (this.getPropertyResolutionStyle().equals(PropertyResolutionStyle.CASE_INSENSITIVE)) {
            List<PropertyInfo> propertyInfos = stem.getIndexedSmartPropertyTable().get(propertyName.toLowerCase(Locale.ENGLISH));
            return propertyInfos != null
                    ? propertyInfos.get(0).getDescriptor()
                    : null;
        }
        if (this.getPropertyResolutionStyle().equals(PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE)) {
            List<PropertyInfo> propertyInfos = stem.getIndexedSmartPropertyTable().get(propertyName.toLowerCase(Locale.ENGLISH));
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
        return stem.getPropertyNames();
    }

    public EventType[] getSuperTypes() {
        return superTypes;
    }

    public Iterator<EventType> getDeepSuperTypes() {
        return deepSuperTypes.iterator();
    }

    public String toString() {
        return "BeanEventType" +
                " name=" + getName() +
                " clazz=" + stem.getClazz().getName();
    }


    private PropertyInfo getSimplePropertyInfo(String propertyName) {
        PropertyInfo propertyInfo;
        List<PropertyInfo> simplePropertyInfoList;

        if (this.getPropertyResolutionStyle().equals(PropertyResolutionStyle.CASE_SENSITIVE)) {
            return stem.getSimpleProperties().get(propertyName);
        }
        if (this.getPropertyResolutionStyle().equals(PropertyResolutionStyle.CASE_INSENSITIVE)) {
            propertyInfo = stem.getSimpleProperties().get(propertyName);
            if (propertyInfo != null) {
                return propertyInfo;
            }

            simplePropertyInfoList = stem.getSimpleSmartPropertyTable().get(propertyName.toLowerCase(Locale.ENGLISH));
            return
                    simplePropertyInfoList != null
                            ? simplePropertyInfoList.get(0)
                            : null;
        }
        if (this.getPropertyResolutionStyle().equals(PropertyResolutionStyle.DISTINCT_CASE_INSENSITIVE)) {
            propertyInfo = stem.getSimpleProperties().get(propertyName);
            if (propertyInfo != null) {
                return propertyInfo;
            }

            simplePropertyInfoList = stem.getSimpleSmartPropertyTable().get(propertyName.toLowerCase(Locale.ENGLISH));
            if (simplePropertyInfoList != null) {
                if (simplePropertyInfoList.size() != 1) {
                    throw new EPException("Unable to determine which property to use for \"" + propertyName + "\" because more than one property matched");
                }

                return simplePropertyInfoList.get(0);
            }
        }

        return null;
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        return stem.getPropertyDescriptors();
    }

    public FragmentEventType getFragmentType(String propertyExpression) {
        PropertyInfo simpleProp = getSimplePropertyInfo(propertyExpression);
        if ((simpleProp != null) && (simpleProp.getClazz() != null)) {
            GenericPropertyDesc genericProp = simpleProp.getDescriptor().getReturnTypeGeneric();
            return EventBeanUtility.createNativeFragmentType(genericProp.getType(), genericProp.getGeneric(), beanEventTypeFactory, stem.isPublicFields());
        }

        Property prop = PropertyParser.parseAndWalkLaxToSimple(propertyExpression);
        if (prop instanceof SimpleProperty) {
            // there is no such property since it wasn't in simplePropertyTypes
            return null;
        }

        GenericPropertyDesc genericProp = prop.getPropertyTypeGeneric(this, beanEventTypeFactory);
        if (genericProp == null) {
            return null;
        }
        return EventBeanUtility.createNativeFragmentType(genericProp.getType(), genericProp.getGeneric(), beanEventTypeFactory, stem.isPublicFields());
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
                setterMethod = MethodResolver.resolveMethod(stem.getClazz(), methodName, new Class[]{String.class, Object.class}, true, new boolean[2], new boolean[2]);
            } catch (MethodResolverNoSuchMethodException e) {
                log.info("Failed to find mapped property setter method '" + methodName + "' for writing to property '" + propertyName + "' taking {String, Object} as parameters");
                return null;
            }
            if (setterMethod == null) {
                return null;
            }
            return new BeanEventPropertyWriterMapProp(stem.getClazz(), setterMethod, mapProp.getKey());
        }

        if (property instanceof IndexedProperty) {
            IndexedProperty indexedProp = (IndexedProperty) property;
            String methodName = PropertyHelper.getSetterMethodName(indexedProp.getPropertyNameAtomic());
            Method setterMethod;
            try {
                setterMethod = MethodResolver.resolveMethod(stem.getClazz(), methodName, new Class[]{int.class, Object.class}, true, new boolean[2], new boolean[2]);
            } catch (MethodResolverNoSuchMethodException e) {
                log.info("Failed to find indexed property setter method '" + methodName + "' for writing to property '" + propertyName + "' taking {int, Object} as parameters");
                return null;
            }
            if (setterMethod == null) {
                return null;
            }
            return new BeanEventPropertyWriterIndexedProp(stem.getClazz(), setterMethod, indexedProp.getIndex());
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

    public Set<EventType> getDeepSuperTypesAsSet() {
        return deepSuperTypes;
    }

    public EventBeanCopyMethodForge getCopyMethodForge(String[] properties) {
        String copyMethodName = stem.getOptionalLegacyDef() == null ? null : stem.getOptionalLegacyDef().getCopyMethod();
        if (copyMethodName == null) {
            if (JavaClassHelper.isImplementsInterface(stem.getClazz(), Serializable.class)) {
                return new BeanEventBeanSerializableCopyMethodForge(this);
            }
            return null;
        }
        Method method = null;
        try {
            method = stem.getClazz().getMethod(copyMethodName);
        } catch (NoSuchMethodException e) {
            log.error("Configured copy-method for class '" + stem.getClazz().getName() + " not found by name '" + copyMethodName + "': " + e.getMessage());
        }
        if (method == null) {
            if (JavaClassHelper.isImplementsInterface(stem.getClazz(), Serializable.class)) {
                return new BeanEventBeanSerializableCopyMethodForge(this);
            }
            throw new EPException("Configured copy-method for class '" + stem.getClazz().getName() + " not found by name '" + copyMethodName + "' and class does not implement Serializable");
        }
        return new BeanEventBeanConfiguredCopyMethodForge(this, method);
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

    public BeanEventTypeStem getStem() {
        return stem;
    }

    public ExprValidationException equalsCompareType(EventType eventType) {
        if (this != eventType) {
            return new ExprValidationException("Bean event types mismatch");
        }
        return null;
    }

    private void initializeWriters() {
        Set<WriteablePropertyDescriptor> writables = PropertyHelper.getWritableProperties(stem.getClazz());
        EventPropertyDescriptor[] desc = new EventPropertyDescriptor[writables.size()];
        Map<String, Pair<EventPropertyDescriptor, BeanEventPropertyWriter>> writers = new HashMap<String, Pair<EventPropertyDescriptor, BeanEventPropertyWriter>>();

        int count = 0;
        for (final WriteablePropertyDescriptor writable : writables) {
            EventPropertyDescriptor propertyDesc = new EventPropertyDescriptor(writable.getPropertyName(), writable.getType(), null, false, false, false, false, false);
            desc[count++] = propertyDesc;
            writers.put(writable.getPropertyName(), new Pair<>(propertyDesc, new BeanEventPropertyWriter(stem.getClazz(), writable.getWriteMethod())));
        }

        writerMap = writers;
        writeablePropertyDescriptors = desc;
    }

    private static final Logger log = LoggerFactory.getLogger(BeanEventType.class);
}
