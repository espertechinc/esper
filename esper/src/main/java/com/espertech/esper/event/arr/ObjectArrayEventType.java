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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.ConfigurationEventTypeObjectArray;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.event.*;
import com.espertech.esper.event.property.IndexedProperty;
import com.espertech.esper.event.property.MappedProperty;
import com.espertech.esper.event.property.Property;
import com.espertech.esper.event.property.PropertyParser;
import com.espertech.esper.util.CollectionUtil;

import java.util.*;

public class ObjectArrayEventType extends BaseNestableEventType {

    protected Map<String, Pair<EventPropertyDescriptor, ObjectArrayEventBeanPropertyWriter>> propertyWriters;
    protected EventPropertyDescriptor[] writablePropertyDescriptors;

    public ObjectArrayEventType(EventTypeMetadata metadata, String eventTypeName, int eventTypeId, EventAdapterService eventAdapterService, Map<String, Object> properyTypes, ConfigurationEventTypeObjectArray typeDef, EventType[] optionalSuperTypes, Set<EventType> optionalDeepSupertypes) {
        super(metadata, eventTypeName, eventTypeId, eventAdapterService, properyTypes, optionalSuperTypes, optionalDeepSupertypes, typeDef, getGetterFactory(eventTypeName, properyTypes, optionalSuperTypes));
    }

    protected void postUpdateNestableTypes() {
        EventTypeNestableGetterFactoryObjectArray factory = (EventTypeNestableGetterFactoryObjectArray) super.getterFactory;
        Map<String, Integer> indexPerProperty = factory.getPropertiesIndex();
        int index = findMax(indexPerProperty) + 1;
        for (Map.Entry<String, Object> entry : nestableTypes.entrySet()) {
            if (indexPerProperty.containsKey(entry.getKey())) {
                continue;
            }
            indexPerProperty.put(entry.getKey(), index);
            index++;
        }
    }

    public Map<String, Integer> getPropertiesIndexes() {
        return ((EventTypeNestableGetterFactoryObjectArray) super.getterFactory).getPropertiesIndex();
    }

    public final Class getUnderlyingType() {
        return Object[].class;
    }

    public EventBeanCopyMethod getCopyMethod(String[] properties) {
        BaseNestableEventUtil.MapIndexedPropPair pair = BaseNestableEventUtil.getIndexedAndMappedProps(properties);

        if (pair.getMapProperties().isEmpty() && pair.getArrayProperties().isEmpty()) {
            return new ObjectArrayEventBeanCopyMethod(this, eventAdapterService);
        } else {
            return new ObjectArrayEventBeanCopyMethodWithArrayMap(this, eventAdapterService, pair.getMapProperties(), pair.getArrayProperties(), getPropertiesIndexes());
        }
    }

    public EventBeanReader getReader() {
        return null;
    }

    public ObjectArrayEventBeanPropertyWriter getWriter(String propertyName) {
        if (writablePropertyDescriptors == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, ObjectArrayEventBeanPropertyWriter> pair = propertyWriters.get(propertyName);
        if (pair != null) {
            return pair.getSecond();
        }

        Property property = PropertyParser.parseAndWalkLaxToSimple(propertyName);
        if (property instanceof MappedProperty) {
            MappedProperty mapProp = (MappedProperty) property;
            Integer index = getPropertiesIndexes().get(mapProp.getPropertyNameAtomic());
            if (index == null) {
                return null;
            }
            return new ObjectArrayEventBeanPropertyWriterMapProp(index, mapProp.getKey());
        }

        if (property instanceof IndexedProperty) {
            IndexedProperty indexedProp = (IndexedProperty) property;
            Integer index = getPropertiesIndexes().get(indexedProp.getPropertyNameAtomic());
            if (index == null) {
                return null;
            }
            return new ObjectArrayEventBeanPropertyWriterIndexedProp(index, indexedProp.getIndex());
        }

        return null;
    }

    public EventPropertyDescriptor getWritableProperty(String propertyName) {
        if (writablePropertyDescriptors == null) {
            initializeWriters();
        }
        Pair<EventPropertyDescriptor, ? extends EventPropertyWriter> pair = propertyWriters.get(propertyName);
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
        if (writablePropertyDescriptors == null) {
            initializeWriters();
        }
        return writablePropertyDescriptors;
    }

    public EventBeanWriter getWriter(String[] properties) {
        if (writablePropertyDescriptors == null) {
            initializeWriters();
        }

        boolean allSimpleProps = true;
        ObjectArrayEventBeanPropertyWriter[] writers = new ObjectArrayEventBeanPropertyWriter[properties.length];
        List<Integer> indexes = new ArrayList<Integer>();
        Map<String, Integer> indexesPerProperty = getPropertiesIndexes();

        for (int i = 0; i < properties.length; i++) {
            Pair<EventPropertyDescriptor, ObjectArrayEventBeanPropertyWriter> writerPair = propertyWriters.get(properties[i]);
            if (writerPair != null) {
                writers[i] = writerPair.getSecond();
                indexes.add(indexesPerProperty.get(writerPair.getFirst().getPropertyName()));
            } else {
                writers[i] = getWriter(properties[i]);
                if (writers[i] == null) {
                    return null;
                }
                allSimpleProps = false;
            }
        }

        if (allSimpleProps) {
            int[] propertyIndexes = CollectionUtil.intArray(indexes);
            return new ObjectArrayEventBeanWriterSimpleProps(propertyIndexes);
        } else {
            return new ObjectArrayEventBeanWriterPerProp(writers);
        }
    }

    private void initializeWriters() {
        List<EventPropertyDescriptor> writeableProps = new ArrayList<EventPropertyDescriptor>();
        Map<String, Pair<EventPropertyDescriptor, ObjectArrayEventBeanPropertyWriter>> propertWritersMap = new HashMap<String, Pair<EventPropertyDescriptor, ObjectArrayEventBeanPropertyWriter>>();
        for (EventPropertyDescriptor prop : propertyDescriptors) {
            writeableProps.add(prop);
            final String propertyName = prop.getPropertyName();
            Integer index = getPropertiesIndexes().get(prop.getPropertyName());
            if (index == null) {
                continue;
            }
            ObjectArrayEventBeanPropertyWriter eventPropertyWriter = new ObjectArrayEventBeanPropertyWriter(index);
            propertWritersMap.put(propertyName, new Pair<EventPropertyDescriptor, ObjectArrayEventBeanPropertyWriter>(prop, eventPropertyWriter));
        }

        propertyWriters = propertWritersMap;
        writablePropertyDescriptors = writeableProps.toArray(new EventPropertyDescriptor[writeableProps.size()]);
    }

    private static EventTypeNestableGetterFactory getGetterFactory(String eventTypeName, Map<String, Object> propertyTypes, EventType[] optionalSupertypes) {
        Map<String, Integer> indexPerProperty = new HashMap<String, Integer>();

        int index = 0;
        if (optionalSupertypes != null) {
            for (EventType superType : optionalSupertypes) {
                ObjectArrayEventType objectArraySuperType = (ObjectArrayEventType) superType;
                for (String propertyName : objectArraySuperType.getPropertyNames()) {
                    if (indexPerProperty.containsKey(propertyName)) {
                        continue;
                    }
                    indexPerProperty.put(propertyName, index);
                    index++;
                }
            }
        }

        for (Map.Entry<String, Object> entry : propertyTypes.entrySet()) {
            indexPerProperty.put(entry.getKey(), index);
            index++;
        }
        return new EventTypeNestableGetterFactoryObjectArray(eventTypeName, indexPerProperty);
    }

    private static int findMax(Map<String, Integer> indexPerProperty) {
        int max = -1;
        for (Map.Entry<String, Integer> entry : indexPerProperty.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
            }
        }
        return max;
    }

    public static Object[] convertEvent(EventBean theEvent, ObjectArrayEventType targetType) {
        Map<String, Integer> indexesTarget = targetType.getPropertiesIndexes();
        Map<String, Integer> indexesSource = ((ObjectArrayEventType) theEvent.getEventType()).getPropertiesIndexes();
        Object[] dataTarget = new Object[indexesTarget.size()];
        Object[] dataSource = (Object[]) theEvent.getUnderlying();
        for (Map.Entry<String, Integer> sourceEntry : indexesSource.entrySet()) {
            String propertyName = sourceEntry.getKey();
            Integer targetIndex = indexesTarget.get(propertyName);
            if (targetIndex == null) {
                continue;
            }
            Object value = dataSource[sourceEntry.getValue()];
            dataTarget[targetIndex] = value;
        }
        return dataTarget;
    }

    public boolean isDeepEqualsConsiderOrder(ObjectArrayEventType other) {
        EventTypeNestableGetterFactoryObjectArray factoryOther = (EventTypeNestableGetterFactoryObjectArray) other.getterFactory;
        EventTypeNestableGetterFactoryObjectArray factoryMe = (EventTypeNestableGetterFactoryObjectArray) getterFactory;

        if (factoryOther.getPropertiesIndex().size() != factoryMe.getPropertiesIndex().size()) {
            return false;
        }

        for (Map.Entry<String, Integer> propMeEntry : factoryMe.getPropertiesIndex().entrySet()) {
            Integer otherIndex = factoryOther.getPropertiesIndex().get(propMeEntry.getKey());
            if (otherIndex == null || !otherIndex.equals(propMeEntry.getValue())) {
                return false;
            }

            String propName = propMeEntry.getKey();
            Object setOneType = this.nestableTypes.get(propName);
            Object setTwoType = other.nestableTypes.get(propName);
            boolean setTwoTypeFound = other.nestableTypes.containsKey(propName);

            String comparedMessage = BaseNestableEventUtil.comparePropType(propName, setOneType, setTwoType, setTwoTypeFound, other.getName());
            if (comparedMessage != null) {
                return false;
            }
        }

        return true;
    }
}
