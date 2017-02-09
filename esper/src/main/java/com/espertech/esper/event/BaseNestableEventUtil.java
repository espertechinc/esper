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
package com.espertech.esper.event;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.arr.ObjectArrayEventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventPropertyGetter;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.event.property.IndexedProperty;
import com.espertech.esper.event.property.MappedProperty;
import com.espertech.esper.event.property.Property;
import com.espertech.esper.event.property.PropertyParser;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BaseNestableEventUtil {
    public static Map<String, Object> checkedCastUnderlyingMap(EventBean theEvent) throws PropertyAccessException {
        return (Map<String, Object>) theEvent.getUnderlying();
    }

    public static Object[] checkedCastUnderlyingObjectArray(EventBean theEvent) throws PropertyAccessException {
        return (Object[]) theEvent.getUnderlying();
    }

    public static Object handleNestedValueArrayWithMap(Object value, int index, MapEventPropertyGetter getter) {
        if (!value.getClass().isArray()) {
            return null;
        }
        if (Array.getLength(value) <= index) {
            return null;
        }
        Object valueMap = Array.get(value, index);
        if (!(valueMap instanceof Map)) {
            if (valueMap instanceof EventBean) {
                return getter.get((EventBean) valueMap);
            }
            return null;
        }
        return getter.getMap((Map<String, Object>) valueMap);
    }

    public static boolean handleNestedValueArrayWithMapExists(Object value, int index, MapEventPropertyGetter getter) {
        if (!value.getClass().isArray()) {
            return false;
        }
        if (Array.getLength(value) <= index) {
            return false;
        }
        Object valueMap = Array.get(value, index);
        if (!(valueMap instanceof Map)) {
            if (valueMap instanceof EventBean) {
                return getter.isExistsProperty((EventBean) valueMap);
            }
            return false;
        }
        return getter.isMapExistsProperty((Map<String, Object>) valueMap);
    }

    public static Object handleNestedValueArrayWithMapFragment(Object value, int index, MapEventPropertyGetter getter, EventAdapterService eventAdapterService, EventType fragmentType) {
        if (!value.getClass().isArray()) {
            return null;
        }
        if (Array.getLength(value) <= index) {
            return null;
        }
        Object valueMap = Array.get(value, index);
        if (!(valueMap instanceof Map)) {
            if (value instanceof EventBean) {
                return getter.getFragment((EventBean) value);
            }
            return null;
        }

        // If the map does not contain the key, this is allowed and represented as null
        EventBean eventBean = eventAdapterService.adapterForTypedMap((Map<String, Object>) valueMap, fragmentType);
        return getter.getFragment(eventBean);
    }

    public static Object handleNestedValueArrayWithObjectArray(Object value, int index, ObjectArrayEventPropertyGetter getter) {
        if (!value.getClass().isArray()) {
            return null;
        }
        if (Array.getLength(value) <= index) {
            return null;
        }
        Object valueArray = Array.get(value, index);
        if (!(valueArray instanceof Object[])) {
            if (valueArray instanceof EventBean) {
                return getter.get((EventBean) valueArray);
            }
            return null;
        }
        return getter.getObjectArray((Object[]) valueArray);
    }

    public static boolean handleNestedValueArrayWithObjectArrayExists(Object value, int index, ObjectArrayEventPropertyGetter getter) {
        if (!value.getClass().isArray()) {
            return false;
        }
        if (Array.getLength(value) <= index) {
            return false;
        }
        Object valueArray = Array.get(value, index);
        if (!(valueArray instanceof Object[])) {
            if (valueArray instanceof EventBean) {
                return getter.isExistsProperty((EventBean) valueArray);
            }
            return false;
        }
        return getter.isObjectArrayExistsProperty((Object[]) valueArray);
    }

    public static Object handleNestedValueArrayWithObjectArrayFragment(Object value, int index, ObjectArrayEventPropertyGetter getter, EventType fragmentType, EventAdapterService eventAdapterService) {
        if (!value.getClass().isArray()) {
            return null;
        }
        if (Array.getLength(value) <= index) {
            return null;
        }
        Object valueArray = Array.get(value, index);
        if (!(valueArray instanceof Object[])) {
            if (value instanceof EventBean) {
                return getter.getFragment((EventBean) value);
            }
            return null;
        }

        // If the map does not contain the key, this is allowed and represented as null
        EventBean eventBean = eventAdapterService.adapterForTypedObjectArray((Object[]) valueArray, fragmentType);
        return getter.getFragment(eventBean);
    }

    public static Object handleCreateFragmentMap(Object value, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        if (!(value instanceof Map)) {
            if (value instanceof EventBean) {
                return value;
            }
            return null;
        }
        Map subEvent = (Map) value;
        return eventAdapterService.adapterForTypedMap(subEvent, fragmentEventType);
    }

    public static Object handleCreateFragmentObjectArray(Object value, EventType fragmentEventType, EventAdapterService eventAdapterService) {
        if (!(value instanceof Object[])) {
            if (value instanceof EventBean) {
                return value;
            }
            return null;
        }
        Object[] subEvent = (Object[]) value;
        return eventAdapterService.adapterForTypedObjectArray(subEvent, fragmentEventType);
    }

    public static Object getMappedPropertyValue(Object value, String key) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            return null;
        }
        Map innerMap = (Map) value;
        return innerMap.get(key);
    }

    public static boolean getMappedPropertyExists(Object value, String key) {
        if (value == null) {
            return false;
        }
        if (!(value instanceof Map)) {
            return false;
        }
        Map innerMap = (Map) value;
        return innerMap.containsKey(key);
    }

    public static MapIndexedPropPair getIndexedAndMappedProps(String[] properties) {
        Set<String> mapPropertiesToCopy = new HashSet<String>();
        Set<String> arrayPropertiesToCopy = new HashSet<String>();
        for (int i = 0; i < properties.length; i++) {
            Property prop = PropertyParser.parseAndWalkLaxToSimple(properties[i]);
            if (prop instanceof MappedProperty) {
                MappedProperty mappedProperty = (MappedProperty) prop;
                mapPropertiesToCopy.add(mappedProperty.getPropertyNameAtomic());
            }
            if (prop instanceof IndexedProperty) {
                IndexedProperty indexedProperty = (IndexedProperty) prop;
                arrayPropertiesToCopy.add(indexedProperty.getPropertyNameAtomic());
            }
        }
        return new MapIndexedPropPair(mapPropertiesToCopy, arrayPropertiesToCopy);
    }

    public static Object getIndexedValue(Object value, int index) {
        if (value == null) {
            return null;
        }
        if (!value.getClass().isArray()) {
            return null;
        }
        if (index >= Array.getLength(value)) {
            return null;
        }
        return Array.get(value, index);
    }

    public static boolean isExistsIndexedValue(Object value, int index) {
        if (value == null) {
            return false;
        }
        if (!value.getClass().isArray()) {
            return false;
        }
        if (index >= Array.getLength(value)) {
            return false;
        }
        return true;
    }

    public static EventBean getFragmentNonPojo(EventAdapterService eventAdapterService, Object fragmentUnderlying, EventType fragmentEventType) {
        if (fragmentUnderlying == null) {
            return null;
        }
        if (fragmentEventType instanceof MapEventType) {
            return eventAdapterService.adapterForTypedMap((Map<String, Object>) fragmentUnderlying, fragmentEventType);
        }
        return eventAdapterService.adapterForTypedObjectArray((Object[]) fragmentUnderlying, fragmentEventType);
    }

    public static Object getFragmentArray(EventAdapterService eventAdapterService, Object value, EventType fragmentEventType) {
        if (value instanceof Object[]) {
            Object[] subEvents = (Object[]) value;

            int countNull = 0;
            for (Object subEvent : subEvents) {
                if (subEvent != null) {
                    countNull++;
                }
            }

            EventBean[] outEvents = new EventBean[countNull];
            int count = 0;
            for (Object item : subEvents) {
                if (item != null) {
                    outEvents[count++] = BaseNestableEventUtil.getFragmentNonPojo(eventAdapterService, item, fragmentEventType);
                }
            }

            return outEvents;
        }

        if (!(value instanceof Map[])) {
            return null;
        }
        Map[] mapTypedSubEvents = (Map[]) value;

        int countNull = 0;
        for (Map map : mapTypedSubEvents) {
            if (map != null) {
                countNull++;
            }
        }

        EventBean[] mapEvents = new EventBean[countNull];
        int count = 0;
        for (Map map : mapTypedSubEvents) {
            if (map != null) {
                mapEvents[count++] = eventAdapterService.adapterForTypedMap(map, fragmentEventType);
            }
        }

        return mapEvents;
    }

    public static Object getBeanArrayValue(BeanEventPropertyGetter nestedGetter, Object value, int index) {

        if (value == null) {
            return null;
        }
        if (!value.getClass().isArray()) {
            return null;
        }
        if (Array.getLength(value) <= index) {
            return null;
        }
        Object arrayItem = Array.get(value, index);
        if (arrayItem == null) {
            return null;
        }

        return nestedGetter.getBeanProp(arrayItem);
    }

    public static Object getFragmentPojo(Object result, BeanEventType eventType, EventAdapterService eventAdapterService) {
        if (result == null) {
            return null;
        }
        if (result instanceof EventBean[]) {
            return result;
        }
        if (result instanceof EventBean) {
            return result;
        }
        if (result.getClass().isArray()) {
            int len = Array.getLength(result);
            EventBean[] events = new EventBean[len];
            for (int i = 0; i < events.length; i++) {
                events[i] = eventAdapterService.adapterForTypedBean(Array.get(result, i), eventType);
            }
            return events;
        }
        return eventAdapterService.adapterForTypedBean(result, eventType);
    }

    public static Object getArrayPropertyValue(EventBean[] wrapper, int index, EventPropertyGetter nestedGetter) {
        if (wrapper == null) {
            return null;
        }
        if (wrapper.length <= index) {
            return null;
        }
        EventBean innerArrayEvent = wrapper[index];
        return nestedGetter.get(innerArrayEvent);
    }

    public static Object getArrayPropertyFragment(EventBean[] wrapper, int index, EventPropertyGetter nestedGetter) {
        if (wrapper == null) {
            return null;
        }
        if (wrapper.length <= index) {
            return null;
        }
        EventBean innerArrayEvent = wrapper[index];
        return nestedGetter.getFragment(innerArrayEvent);
    }

    public static Object getArrayPropertyUnderlying(EventBean[] wrapper, int index) {
        if (wrapper == null) {
            return null;
        }
        if (wrapper.length <= index) {
            return null;
        }

        return wrapper[index].getUnderlying();
    }

    public static Object getArrayPropertyBean(EventBean[] wrapper, int index) {
        if (wrapper == null) {
            return null;
        }
        if (wrapper.length <= index) {
            return null;
        }

        return wrapper[index];
    }

    public static Object getArrayPropertyAsUnderlyingsArray(Class underlyingType, EventBean[] wrapper) {
        if (wrapper != null) {
            Object array = Array.newInstance(underlyingType, wrapper.length);
            for (int i = 0; i < wrapper.length; i++) {
                Array.set(array, i, wrapper[i].getUnderlying());
            }
            return array;
        }

        return null;
    }

    public static String comparePropType(String propName, Object setOneType, Object setTwoType, boolean setTwoTypeFound, String otherName) {
        // allow null for nested event types
        if ((setOneType instanceof String || setOneType instanceof EventType) && setTwoType == null) {
            return null;
        }
        if ((setTwoType instanceof String || setTwoType instanceof EventType) && setOneType == null) {
            return null;
        }
        if (!setTwoTypeFound) {
            return "The property '" + propName + "' is not provided but required";
        }
        if (setTwoType == null) {
            return null;
        }
        if (setOneType == null) {
            return "Type by name '" + otherName + "' in property '" + propName + "' incompatible with null-type or property name not found in target";
        }

        if ((setTwoType instanceof Class) && (setOneType instanceof Class)) {
            Class boxedOther = JavaClassHelper.getBoxedType((Class) setTwoType);
            Class boxedThis = JavaClassHelper.getBoxedType((Class) setOneType);
            if (!boxedOther.equals(boxedThis)) {
                if (!JavaClassHelper.isSubclassOrImplementsInterface(boxedOther, boxedThis)) {
                    return "Type by name '" + otherName + "' in property '" + propName + "' expected " + boxedThis + " but receives " + boxedOther;
                }
            }
        } else if ((setTwoType instanceof BeanEventType) && (setOneType instanceof Class)) {
            Class boxedOther = JavaClassHelper.getBoxedType(((BeanEventType) setTwoType).getUnderlyingType());
            Class boxedThis = JavaClassHelper.getBoxedType((Class) setOneType);
            if (!boxedOther.equals(boxedThis)) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected " + boxedThis + " but receives " + boxedOther;
            }
        } else if (setTwoType instanceof EventType[] && ((EventType[]) setTwoType)[0] instanceof BeanEventType && setOneType instanceof Class && ((Class) setOneType).isArray()) {
            Class boxedOther = JavaClassHelper.getBoxedType((((EventType[]) setTwoType)[0]).getUnderlyingType());
            Class boxedThis = JavaClassHelper.getBoxedType(((Class) setOneType).getComponentType());
            if (!boxedOther.equals(boxedThis)) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected " + boxedThis + " but receives " + boxedOther;
            }
        } else if ((setTwoType instanceof Map) && (setOneType instanceof Map)) {
            String messageIsDeepEquals = BaseNestableEventType.isDeepEqualsProperties(propName, (Map<String, Object>) setOneType, (Map<String, Object>) setTwoType);
            if (messageIsDeepEquals != null) {
                return messageIsDeepEquals;
            }
        } else if ((setTwoType instanceof EventType) && (setOneType instanceof EventType)) {
            boolean mismatch;
            if (setTwoType instanceof EventTypeSPI && setOneType instanceof EventTypeSPI) {
                mismatch = !((EventTypeSPI) setOneType).equalsCompareType((EventTypeSPI) setTwoType);
            } else {
                mismatch = !setOneType.equals(setTwoType);
            }
            if (mismatch) {
                EventType setOneEventType = (EventType) setOneType;
                EventType setTwoEventType = (EventType) setTwoType;
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneEventType.getName() + "' but receives event type '" + setTwoEventType.getName() + "'";
            }
        } else if ((setTwoType instanceof String) && (setOneType instanceof EventType)) {
            EventType setOneEventType = (EventType) setOneType;
            String setTwoEventType = (String) setTwoType;
            if (!EventTypeUtility.isTypeOrSubTypeOf(setTwoEventType, setOneEventType)) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneEventType.getName() + "' but receives event type '" + setTwoEventType + "'";
            }
        } else if ((setTwoType instanceof EventType) && (setOneType instanceof String)) {
            EventType setTwoEventType = (EventType) setTwoType;
            String setOneEventType = (String) setOneType;
            if (!EventTypeUtility.isTypeOrSubTypeOf(setOneEventType, setTwoEventType)) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneEventType + "' but receives event type '" + setTwoEventType.getName() + "'";
            }
        } else if ((setTwoType instanceof String) && (setOneType instanceof String)) {
            if (!setTwoType.equals(setOneType)) {
                String setOneEventType = (String) setOneType;
                String setTwoEventType = (String) setTwoType;
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneEventType + "' but receives event type '" + setTwoEventType + "'";
            }
        } else if ((setTwoType instanceof EventType[]) && (setOneType instanceof String)) {
            EventType[] setTwoTypeArr = (EventType[]) setTwoType;
            EventType setTwoFragmentType = setTwoTypeArr[0];
            String setOneTypeString = (String) setOneType;
            if (!(setOneTypeString.endsWith("[]"))) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneType + "' but receives event type '" + setTwoFragmentType.getName() + "[]'";
            }
            String setOneTypeNoArray = setOneTypeString.replaceAll("\\[\\]", "");
            if (!(setTwoFragmentType.getName().equals(setOneTypeNoArray))) {
                return "Type by name '" + otherName + "' in property '" + propName + "' expected event type '" + setOneTypeNoArray + "[]' but receives event type '" + setTwoFragmentType.getName() + "'";
            }
        } else {
            String typeOne = getTypeName(setOneType);
            String typeTwo = getTypeName(setTwoType);
            if (typeOne.equals(typeTwo)) {
                return null;
            }
            return "Type by name '" + otherName + "' in property '" + propName + "' expected " + typeOne + " but receives " + typeTwo;
        }

        return null;
    }

    private static String getTypeName(Object type) {
        if (type == null) {
            return "null";
        }
        if (type instanceof Class) {
            return ((Class) type).getName();
        }
        if (type instanceof EventType) {
            return "event type '" + ((EventType) type).getName() + "'";
        }
        if (type instanceof String) {
            Class boxedType = JavaClassHelper.getBoxedType(JavaClassHelper.getPrimitiveClassForName((String) type));
            if (boxedType != null) {
                return boxedType.getName();
            }
            return (String) type;
        }
        return type.getClass().getName();
    }

    public static class MapIndexedPropPair {
        private final Set<String> mapProperties;
        private final Set<String> arrayProperties;

        public MapIndexedPropPair(Set<String> mapProperties, Set<String> arrayProperties) {
            this.mapProperties = mapProperties;
            this.arrayProperties = arrayProperties;
        }

        public Set<String> getMapProperties() {
            return mapProperties;
        }

        public Set<String> getArrayProperties() {
            return arrayProperties;
        }
    }
}
