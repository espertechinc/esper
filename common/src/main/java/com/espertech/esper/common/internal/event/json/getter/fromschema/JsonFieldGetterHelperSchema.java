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
package com.espertech.esper.common.internal.event.json.getter.fromschema;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.lang.reflect.Array;

import static com.espertech.esper.common.internal.util.CollectionUtil.*;

public class JsonFieldGetterHelperSchema {
    static Object getJsonSimpleProp(JsonUnderlyingField field, Object object) {
        JsonEventObjectBase und = (JsonEventObjectBase) object;
        return und.getNativeValue(field.getPropertyNumber());
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param object         object
     * @param propertyNumber field number
     * @param index          index
     * @return value
     * @throws PropertyAccessException property access exceptions
     */
    public static Object getJsonIndexedProp(Object object, int propertyNumber, int index) throws PropertyAccessException {
        JsonEventObjectBase und = (JsonEventObjectBase) object;
        Object array = und.getNativeValue(propertyNumber);
        return arrayValueAtIndex(array, index);
    }

    static boolean getJsonIndexedPropExists(Object object, JsonUnderlyingField field, int index) throws PropertyAccessException {
        JsonEventObjectBase und = (JsonEventObjectBase) object;
        Object array = und.getNativeValue(field.getPropertyNumber());
        return CollectionUtil.arrayExistsAtIndex(array, index);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param object         object
     * @param propertyNumber field number
     * @param key            key
     * @return value
     * @throws PropertyAccessException property access exceptions
     */
    public static Object getJsonMappedProp(Object object, int propertyNumber, String key) throws PropertyAccessException {
        JsonEventObjectBase und = (JsonEventObjectBase) object;
        Object result = und.getNativeValue(propertyNumber);
        return getMapValueChecked(result, key);
    }

    public static boolean getJsonMappedExists(Object object, int propertyNumber, String key) throws PropertyAccessException {
        JsonEventObjectBase und = (JsonEventObjectBase) object;
        Object result = und.getNativeValue(propertyNumber);
        return getMapKeyExistsChecked(result, key);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param und          underlying
     * @param propNumber   property number
     * @param fragmentType event type
     * @param factory      factory
     * @return event bean or null
     */
    public static EventBean handleJsonCreateFragmentSimple(JsonEventObjectBase und, int propNumber, EventType fragmentType, EventBeanTypedEventFactory factory) {
        Object prop = und.getNativeValue(propNumber);
        if (prop == null) {
            return null;
        }
        return factory.adapterForTypedJson(prop, fragmentType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param und          underlying
     * @param propNumber   property number
     * @param fragmentType event type
     * @param factory      factory
     * @param index        index
     * @return event bean or null
     */
    public static EventBean handleJsonCreateFragmentIndexed(JsonEventObjectBase und, int propNumber, int index, EventType fragmentType, EventBeanTypedEventFactory factory) {
        Object prop = und.getNativeValue(propNumber);
        prop = CollectionUtil.arrayValueAtIndex(prop, index);
        if (prop == null) {
            return null;
        }
        return factory.adapterForTypedJson(prop, fragmentType);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param und          underlying
     * @param propNumber   property number
     * @param fragmentType event type
     * @param factory      factory
     * @return event bean or null
     */
    public static EventBean[] handleJsonCreateFragmentArray(JsonEventObjectBase und, int propNumber, EventType fragmentType, EventBeanTypedEventFactory factory) throws PropertyAccessException {
        Object value = und.getNativeValue(propNumber);
        if (value == null) {
            return null;
        }
        int len = Array.getLength(value);
        EventBean[] events = new EventBean[len];
        for (int i = 0; i < len; i++) {
            Object item = Array.get(value, i);
            events[i] = factory.adapterForTypedJson(item, fragmentType);
        }
        return events;
    }
}
