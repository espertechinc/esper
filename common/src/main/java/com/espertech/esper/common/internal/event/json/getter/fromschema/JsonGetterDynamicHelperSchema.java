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

import java.util.Map;

public class JsonGetterDynamicHelperSchema {
    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns the json object prop.
     *
     * @param propertyName property name
     * @param object       json object
     * @return value
     */
    public static Object getJsonPropertySimpleValue(String propertyName, Map<String, Object> object) {
        return object.get(propertyName);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns flag whether the json object prop exists.
     *
     * @param propertyName property name
     * @param object       json object
     * @return value
     */
    public static boolean getJsonPropertySimpleExists(String propertyName, Map<String, Object> object) {
        return object.containsKey(propertyName);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns the json object prop.
     *
     * @param propertyName property name
     * @param index        index
     * @param object       json object
     * @return value
     */
    public static Object getJsonPropertyIndexedValue(String propertyName, int index, Map<String, Object> object) {
        Object value = object.get(propertyName);
        if (!(value instanceof Object[])) {
            return null;
        }
        Object[] array = (Object[]) value;
        if (index >= array.length) {
            return null;
        }
        return array[index];
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns the json object prop.
     *
     * @param propertyName property name
     * @param index        index
     * @param object       json object
     * @return value
     */
    public static boolean getJsonPropertyIndexedExists(String propertyName, int index, Map<String, Object> object) {
        Object value = object.get(propertyName);
        if (!(value instanceof Object[])) {
            return false;
        }
        Object[] array = (Object[]) value;
        return index < array.length;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns the json object prop.
     *
     * @param propertyName property name
     * @param key          key
     * @param object       json object
     * @return value
     */
    public static Object getJsonPropertyMappedValue(String propertyName, String key, Map<String, Object> object) {
        Object value = object.get(propertyName);
        if (!(value instanceof Map)) {
            return null;
        }
        Map<String, Object> map = (Map<String, Object>) value;
        return map.get(key);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * Returns the json object prop.
     *
     * @param propertyName property name
     * @param key          key
     * @param object       json object
     * @return value
     */
    public static boolean getJsonPropertyMappedExists(String propertyName, String key, Map<String, Object> object) {
        Object value = object.get(propertyName);
        if (!(value instanceof Map)) {
            return false;
        }
        Map<String, Object> map = (Map<String, Object>) value;
        return map.containsKey(key);
    }
}
