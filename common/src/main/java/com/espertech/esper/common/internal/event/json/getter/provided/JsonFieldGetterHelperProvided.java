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
package com.espertech.esper.common.internal.event.json.getter.provided;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.util.CollectionUtil.getMapKeyExistsChecked;
import static com.espertech.esper.common.internal.util.CollectionUtil.getMapValueChecked;

public class JsonFieldGetterHelperProvided {
    public static Object getJsonProvidedMappedProp(Object underlying, Field field, String key) throws PropertyAccessException {
        Object result = getJsonProvidedSimpleProp(underlying, field);
        return getMapValueChecked(result, key);
    }

    public static Object getJsonProvidedIndexedProp(Object underlying, Field field, int index) {
        Object result = getJsonProvidedSimpleProp(underlying, field);
        return CollectionUtil.arrayValueAtIndex(result, index);
    }

    public static Object handleJsonProvidedCreateFragmentSimple(Object underlying, Field field, EventType fragmentType, EventBeanTypedEventFactory factory) {
        Object prop = getJsonProvidedSimpleProp(underlying, field);
        if (prop == null) {
            return null;
        }
        if (fragmentType instanceof JsonEventType) {
            return factory.adapterForTypedJson(prop, fragmentType);
        }
        return factory.adapterForTypedBean(prop, fragmentType);
    }

    public static Object getJsonProvidedSimpleProp(Object object, Field field) throws PropertyAccessException {
        try {
            return field.get(object);
        } catch (IllegalAccessException ex) {
            throw new PropertyAccessException("Failed to access field '" + field.getName() + "' of class '" + field.getDeclaringClass().getName() + "': " + ex.getMessage(), ex);
        }
    }

    public static Object handleJsonProvidedCreateFragmentArray(Object value, EventType fragmentType, EventBeanTypedEventFactory factory) {
        if (value == null) {
            return null;
        }
        int len = Array.getLength(value);
        EventBean[] events = new EventBean[len];
        if (fragmentType instanceof JsonEventType) {
            for (int i = 0; i < len; i++) {
                Object item = Array.get(value, i);
                events[i] = factory.adapterForTypedJson(item, fragmentType);
            }
        } else {
            for (int i = 0; i < len; i++) {
                Object item = Array.get(value, i);
                events[i] = factory.adapterForTypedBean(item, fragmentType);
            }
        }
        return events;
    }

    public static boolean getJsonProvidedMappedExists(Object underlying, Field field, String key) throws PropertyAccessException {
        Object result = getJsonProvidedSimpleProp(underlying, field);
        return getMapKeyExistsChecked(result, key);
    }

    public static boolean getJsonProvidedIndexedPropExists(Object object, Field field, int index) {
        Object array = getJsonProvidedSimpleProp(object, field);
        return CollectionUtil.arrayExistsAtIndex(array, index);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param prop         value
     * @param fragmentType event type
     * @param factory      factory
     * @param index        index
     * @return event bean or null
     */
    public static EventBean handleJsonProvidedCreateFragmentIndexed(Object prop, int index, EventType fragmentType, EventBeanTypedEventFactory factory) {
        prop = CollectionUtil.arrayValueAtIndex(prop, index);
        if (prop == null) {
            return null;
        }
        return factory.adapterForTypedJson(prop, fragmentType);
    }
}
