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
package com.espertech.esper.common.internal.event.variant;

import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A thread-safe cache for property getters per event type.
 * <p>
 * Since most often getters are used in a row for the same type, keeps a row of last used getters for
 * fast lookup based on type.
 */
public class VariantPropertyGetterCache {
    private volatile EventType[] knownTypes;
    private volatile VariantPropertyGetterRow lastUsedGetters;
    private List<String> properties;
    private Map<EventType, VariantPropertyGetterRow> allGetters;

    /**
     * Ctor.
     *
     * @param knownTypes types known at cache construction type, may be an empty list for the ANY type variance.
     */
    public VariantPropertyGetterCache(EventType[] knownTypes) {
        this.knownTypes = knownTypes;
        allGetters = new HashMap<EventType, VariantPropertyGetterRow>();
        properties = new ArrayList<String>();
    }

    /**
     * Adds the getters for a property that is identified by a property number which indexes into array of getters per type.
     *
     * @param propertyName to add
     */
    public void addGetters(String propertyName) {
        for (EventType type : knownTypes) {
            EventPropertyGetter getter = type.getGetter(propertyName);

            VariantPropertyGetterRow row = allGetters.get(type);
            if (row == null) {
                synchronized (this) {
                    row = new VariantPropertyGetterRow(type, new HashMap<>());
                    allGetters.put(type, row);
                }
            }
            row.addGetter(propertyName, getter);
        }
        properties.add(propertyName);
    }

    /**
     * Fast lookup of a getter for a property and type.
     *
     * @param propertyName property name
     * @param eventType    type of underlying event
     * @return getter
     */
    public EventPropertyGetter getGetter(String propertyName, EventType eventType) {
        VariantPropertyGetterRow lastGetters = lastUsedGetters;
        if ((lastGetters != null) && (lastGetters.eventType == eventType)) {
            EventPropertyGetter getter = lastGetters.getGetterPerProp().get(propertyName);
            if (getter == null) {
                getter = eventType.getGetter(propertyName);
                lastGetters.addGetter(propertyName, getter);
            }
            return getter;
        }

        VariantPropertyGetterRow row = allGetters.get(eventType);

        // newly seen type (Using ANY type variance or as a subtype of an existing variance type)
        // synchronized add, if added twice then that is ok too
        if (row == null) {
            synchronized (this) {
                row = allGetters.get(eventType);
                if (row == null) {
                    row = addType(eventType);
                }
            }
        }

        EventPropertyGetter getter = row.getGetterPerProp().get(propertyName);
        lastUsedGetters = row;

        if (getter == null) {
            getter = eventType.getGetter(propertyName);
            row.addGetter(propertyName, getter);
        }

        return getter;
    }

    private VariantPropertyGetterRow addType(EventType eventType) {
        EventType[] newKnownTypes = (EventType[]) resizeArray(knownTypes, knownTypes.length + 1);
        newKnownTypes[newKnownTypes.length - 1] = eventType;

        // create getters
        Map<String, EventPropertyGetter> getters = new HashMap<>(CollectionUtil.capacityHashMap(properties.size()));
        for (int i = 0; i < properties.size(); i++) {
            String propertyName = properties.get(i);
            EventPropertyGetter getter = eventType.getGetter(propertyName);
            getters.put(propertyName, getter);
        }

        VariantPropertyGetterRow row = new VariantPropertyGetterRow(eventType, getters);

        Map<EventType, VariantPropertyGetterRow> newAllGetters = new HashMap<EventType, VariantPropertyGetterRow>();
        newAllGetters.putAll(allGetters);
        newAllGetters.put(eventType, row);

        // overlay volatiles
        knownTypes = newKnownTypes;
        allGetters = newAllGetters;

        return row;
    }

    private static Object resizeArray(Object oldArray, int newSize) {
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        Class elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(
                elementType, newSize);
        int preserveLength = Math.min(oldSize, newSize);
        if (preserveLength > 0)
            System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
        return newArray;
    }

    private static class VariantPropertyGetterRow {
        private final EventType eventType;
        private final Map<String, EventPropertyGetter> getterPerProp;

        private VariantPropertyGetterRow(EventType eventType, Map<String, EventPropertyGetter> getterPerProp) {
            this.eventType = eventType;
            this.getterPerProp = getterPerProp;
        }

        public EventType getEventType() {
            return eventType;
        }

        public Map<String, EventPropertyGetter> getGetterPerProp() {
            return getterPerProp;
        }

        public void addGetter(String propertyName, EventPropertyGetter getter) {
            getterPerProp.put(propertyName, getter);
        }
    }
}
