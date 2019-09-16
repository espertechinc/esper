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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.*;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.property.IndexedProperty;
import com.espertech.esper.common.internal.event.property.MappedProperty;
import com.espertech.esper.common.internal.event.property.Property;
import com.espertech.esper.common.internal.event.property.PropertyParser;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.StringValue;

import java.util.*;

/**
 * Implementation of the {@link com.espertech.esper.common.client.EventType} interface for handling name value pairs.
 */
public abstract class BaseNestableEventType implements EventTypeSPI {
    protected EventTypeMetadata metadata;
    protected final EventType[] optionalSuperTypes;
    protected final Set<EventType> optionalDeepSupertypes;
    protected final EventTypeNestableGetterFactory getterFactory;
    protected final BeanEventTypeFactory beanEventTypeFactory;
    protected final boolean publicFields;

    // Simple (not-nested) properties are stored here
    protected String[] propertyNames;       // Cache an array of property names so not to construct one frequently
    protected EventPropertyDescriptor[] propertyDescriptors;

    protected Map<String, PropertySetDescriptorItem> propertyItems;
    protected Map<String, EventPropertyGetterSPI> propertyGetterCache; // Mapping of all property names and getters

    // Nestable definition of Map contents is here
    protected final Map<String, Object> nestableTypes;  // Deep definition of the map-type, containing nested maps and objects

    protected String startTimestampPropertyName;
    protected String endTimestampPropertyName;

    /**
     * Constructor takes a type name, map of property names and types, for
     * use with nestable Map events.
     *  @param metadata                   event type metadata
     * @param propertyTypes              is pairs of property name and type
     * @param optionalSuperTypes         the supertypes to this type if any, or null if there are no supertypes
     * @param optionalDeepSupertypes     the deep supertypes to this type if any, or null if there are no deep supertypes
     * @param startTimestampPropertyName start timestamp
     * @param endTimestampPropertyName   end timestamp
     * @param getterFactory              getter factory
     * @param beanEventTypeFactory       bean factory
     * @param publicFields true if the properties that are classes are public field default access
     */
    public BaseNestableEventType(EventTypeMetadata metadata,
                                 Map<String, Object> propertyTypes,
                                 EventType[] optionalSuperTypes,
                                 Set<EventType> optionalDeepSupertypes,
                                 String startTimestampPropertyName,
                                 String endTimestampPropertyName,
                                 EventTypeNestableGetterFactory getterFactory,
                                 BeanEventTypeFactory beanEventTypeFactory,
                                 boolean publicFields) {
        this.metadata = metadata;
        this.getterFactory = getterFactory;
        this.beanEventTypeFactory = beanEventTypeFactory;
        this.publicFields = publicFields;
        this.startTimestampPropertyName = startTimestampPropertyName;
        this.endTimestampPropertyName = endTimestampPropertyName;

        this.optionalSuperTypes = optionalSuperTypes;
        if (optionalDeepSupertypes == null) {
            this.optionalDeepSupertypes = Collections.emptySet();
        } else {
            this.optionalDeepSupertypes = optionalDeepSupertypes;
        }

        // determine property set and prepare getters
        PropertySetDescriptor propertySet = EventTypeUtility.getNestableProperties(propertyTypes, beanEventTypeFactory.getEventBeanTypedEventFactory(), getterFactory, optionalSuperTypes, beanEventTypeFactory, publicFields);

        nestableTypes = propertySet.getNestableTypes();
        propertyNames = propertySet.getPropertyNameArray();
        propertyItems = propertySet.getPropertyItems();
        propertyDescriptors = propertySet.getPropertyDescriptors().toArray(new EventPropertyDescriptor[propertySet.getPropertyDescriptors().size()]);

        EventTypeUtility.TimestampPropertyDesc desc = EventTypeUtility.validatedDetermineTimestampProps(this, startTimestampPropertyName, endTimestampPropertyName, optionalSuperTypes);
        this.startTimestampPropertyName = desc.getStart();
        this.endTimestampPropertyName = desc.getEnd();
    }

    public void setMetadataId(long publicId, long protectedId) {
        metadata = metadata.withIds(publicId, protectedId);
    }

    public String getName() {
        return metadata.getName();
    }

    public String getStartTimestampPropertyName() {
        return startTimestampPropertyName;
    }

    public String getEndTimestampPropertyName() {
        return endTimestampPropertyName;
    }

    public final Class getPropertyType(String propertyName) {
        return EventTypeUtility.getNestablePropertyType(propertyName, propertyItems, nestableTypes, beanEventTypeFactory, publicFields);
    }

    public EventPropertyGetterSPI getGetterSPI(String propertyName) {
        if (propertyGetterCache == null) {
            propertyGetterCache = new HashMap<>();
        }
        return EventTypeUtility.getNestableGetter(propertyName, propertyItems, propertyGetterCache, nestableTypes, beanEventTypeFactory.getEventBeanTypedEventFactory(), getterFactory, metadata.getApplicationType() == EventTypeApplicationType.OBJECTARR, beanEventTypeFactory, publicFields);
    }

    public EventPropertyGetter getGetter(final String propertyName) {
        return getGetterSPI(propertyName);
    }

    public EventPropertyGetterMapped getGetterMapped(String mappedPropertyName) {
        return getGetterMappedSPI(mappedPropertyName);
    }

    public EventPropertyGetterMappedSPI getGetterMappedSPI(String mappedPropertyName) {
        PropertySetDescriptorItem item = propertyItems.get(mappedPropertyName);
        if (item == null || !item.getPropertyDescriptor().isMapped()) {
            return null;
        }
        MappedProperty mappedProperty = new MappedProperty(mappedPropertyName);
        return getterFactory.getPropertyProvidedGetterMap(nestableTypes, mappedPropertyName, mappedProperty, beanEventTypeFactory.getEventBeanTypedEventFactory(), beanEventTypeFactory);
    }

    public EventPropertyGetterIndexed getGetterIndexed(String indexedPropertyName) {
        return getGetterIndexedSPI(indexedPropertyName);
    }

    public EventPropertyGetterIndexedSPI getGetterIndexedSPI(String indexedPropertyName) {
        PropertySetDescriptorItem item = propertyItems.get(indexedPropertyName);
        if (item == null || !item.getPropertyDescriptor().isIndexed()) {
            return null;
        }
        IndexedProperty indexedProperty = new IndexedProperty(indexedPropertyName);
        return getterFactory.getPropertyProvidedGetterIndexed(nestableTypes, indexedPropertyName, indexedProperty, beanEventTypeFactory.getEventBeanTypedEventFactory(), beanEventTypeFactory);
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public boolean isProperty(String propertyName) {
        Class propertyType = getPropertyType(propertyName);
        if (propertyType == null) {
            // Could be a native null type, such as "insert into A select null as field..."
            if (propertyItems.containsKey(StringValue.unescapeDot(propertyName))) {
                return true;
            }

        }
        return propertyType != null;
    }

    public EventType[] getSuperTypes() {
        return optionalSuperTypes;
    }

    public Iterator<EventType> getDeepSuperTypes() {
        return optionalDeepSupertypes.iterator();
    }

    public Set<EventType> getDeepSuperTypesAsSet() {
        return optionalDeepSupertypes;
    }

    /**
     * Returns the name-type map of map properties, each value in the map
     * can be a Class or a Map&lt;String, Object&gt; (for nested maps).
     *
     * @return is the property name and types
     */
    public Map<String, Object> getTypes() {
        return this.nestableTypes;
    }

    public EventPropertyDescriptor[] getPropertyDescriptors() {
        return propertyDescriptors;
    }

    /**
     * Compares two sets of properties and determines if they are the same, allowing for
     * boxed/unboxed types, and nested map types.
     *
     * @param setOne    is the first set of properties
     * @param setTwo    is the second set of properties
     * @param otherName name of the type compared to
     * @return null if the property set is equivalent or message if not
     */
    public static ExprValidationException isDeepEqualsProperties(String otherName, Map<String, Object> setOne, Map<String, Object> setTwo) {
        // Should have the same number of properties
        if (setOne.size() != setTwo.size()) {
            return new ExprValidationException("Type by name '" + otherName + "' expects " + setOne.size() + " properties but receives " + setTwo.size() + " properties");
        }

        // Compare property by property
        for (Map.Entry<String, Object> entry : setOne.entrySet()) {
            String propName = entry.getKey();
            Object setTwoType = setTwo.get(entry.getKey());
            boolean setTwoTypeFound = setTwo.containsKey(entry.getKey());
            Object setOneType = entry.getValue();

            ExprValidationException message = BaseNestableEventUtil.comparePropType(propName, setOneType, setTwoType, setTwoTypeFound, otherName);
            if (message != null) {
                return message;
            }
        }

        return null;
    }

    public EventPropertyDescriptor getPropertyDescriptor(String propertyName) {
        PropertySetDescriptorItem item = propertyItems.get(propertyName);
        if (item == null) {
            return null;
        }
        return item.getPropertyDescriptor();
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public FragmentEventType getFragmentType(String propertyName) {
        PropertySetDescriptorItem item = propertyItems.get(propertyName);
        if (item != null) {
            // may contain null values
            return item.getFragmentEventType();
        }

        // see if this is a nested property
        int index = StringValue.unescapedIndexOfDot(propertyName);
        if (index == -1) {
            // dynamic simple property
            if (propertyName.endsWith("?")) {
                return null;
            }

            // parse, can be an indexed property
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
            if (property instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) property;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                } else if (type instanceof EventType[]) {
                    EventType eventType = ((EventType[]) type)[0];
                    return new FragmentEventType(eventType, false, false);
                } else if (type instanceof TypeBeanOrUnderlying[]) {
                    EventType innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                    if (!(innerType instanceof BaseNestableEventType)) {
                        return null;
                    }
                    return new FragmentEventType(innerType, false, false);  // false since an index is present
                }
                if (!(type instanceof Class)) {
                    return null;
                }
                if (!((Class) type).isArray()) {
                    return null;
                }
                // its an array
                return EventBeanUtility.createNativeFragmentType(((Class) type).getComponentType(), null, beanEventTypeFactory, false);
            } else if (property instanceof MappedProperty) {
                // No type information available for the inner event
                return null;
            } else {
                return null;
            }
        }

        // Map event types allow 2 types of properties inside:
        //   - a property that is a Java object is interrogated via bean property getters and BeanEventType
        //   - a property that is a Map itself is interrogated via map property getters
        // The property getters therefore act on

        // Take apart the nested property into a map key and a nested value class property name
        String propertyMap = StringValue.unescapeDot(propertyName.substring(0, index));
        String propertyNested = propertyName.substring(index + 1, propertyName.length());

        // If the property is dynamic, it cannot be a fragment
        if (propertyMap.endsWith("?")) {
            return null;
        }

        Object nestedType = nestableTypes.get(propertyMap);
        if (nestedType == null) {
            // parse, can be an indexed property
            Property property = PropertyParser.parseAndWalkLaxToSimple(propertyMap);
            if (property instanceof IndexedProperty) {
                IndexedProperty indexedProp = (IndexedProperty) property;
                Object type = nestableTypes.get(indexedProp.getPropertyNameAtomic());
                if (type == null) {
                    return null;
                }
                // handle map-in-map case
                if (type instanceof TypeBeanOrUnderlying[]) {
                    EventType innerType = ((TypeBeanOrUnderlying[]) type)[0].getEventType();
                    if (!(innerType instanceof BaseNestableEventType)) {
                        return null;
                    }
                    return innerType.getFragmentType(propertyNested);
                } else if (type instanceof EventType[]) {
                    // handle eventtype[] in map
                    EventType innerType = ((EventType[]) type)[0];
                    return innerType.getFragmentType(propertyNested);
                } else {
                    // handle array class in map case
                    if (!(type instanceof Class)) {
                        return null;
                    }
                    if (!((Class) type).isArray()) {
                        return null;
                    }
                    FragmentEventType fragmentParent = EventBeanUtility.createNativeFragmentType((Class) type, null, beanEventTypeFactory, false);
                    if (fragmentParent == null) {
                        return null;
                    }
                    return fragmentParent.getFragmentType().getFragmentType(propertyNested);
                }
            } else if (property instanceof MappedProperty) {
                // No type information available for the property's map value
                return null;
            } else {
                return null;
            }
        }

        // If there is a map value in the map, return the Object value if this is a dynamic property
        if (nestedType == Map.class) {
            return null;
        } else if (nestedType instanceof Map) {
            return null;
        } else if (nestedType instanceof Class) {
            Class simpleClass = (Class) nestedType;
            if (!JavaClassHelper.isFragmentableType(simpleClass)) {
                return null;
            }
            EventType nestedEventType = beanEventTypeFactory.getCreateBeanType(simpleClass, false);
            return nestedEventType.getFragmentType(propertyNested);
        } else if (nestedType instanceof EventType) {
            EventType innerType = (EventType) nestedType;
            return innerType.getFragmentType(propertyNested);
        } else if (nestedType instanceof EventType[]) {
            EventType[] innerType = (EventType[]) nestedType;
            return innerType[0].getFragmentType(propertyNested);
        } else if (nestedType instanceof TypeBeanOrUnderlying) {
            EventType innerType = ((TypeBeanOrUnderlying) nestedType).getEventType();
            if (!(innerType instanceof BaseNestableEventType)) {
                return null;
            }
            return innerType.getFragmentType(propertyNested);
        } else if (nestedType instanceof TypeBeanOrUnderlying[]) {
            EventType innerType = ((TypeBeanOrUnderlying[]) nestedType)[0].getEventType();
            if (!(innerType instanceof BaseNestableEventType)) {
                return null;
            }
            return innerType.getFragmentType(propertyNested);
        } else {
            String message = "Nestable map type configuration encountered an unexpected value type of '"
                    + nestedType.getClass() + " for property '" + propertyName + "', expected Class, Map.class or Map<String, Object> as value type";
            throw new PropertyAccessException(message);
        }
    }

    /**
     * Returns a message if the type, compared to this type, is not compatible in regards to the property numbers
     * and types.
     *
     * @param otherType to compare to
     * @return message
     */
    public ExprValidationException compareEquals(EventType otherType) {
        if (!(otherType instanceof BaseNestableEventType)) {
            return new ExprValidationException("Type by name '" + otherType.getName() + "' is not a compatible type (target type underlying is '" + otherType.getUnderlyingType().getName() + "')");
        }

        BaseNestableEventType other = (BaseNestableEventType) otherType;
        return isDeepEqualsProperties(otherType.getName(), other.nestableTypes, this.nestableTypes);
    }

    public ExprValidationException equalsCompareType(EventType otherEventType) {
        if (this == otherEventType) {
            return null;
        }

        return compareEquals(otherEventType);
    }
}
