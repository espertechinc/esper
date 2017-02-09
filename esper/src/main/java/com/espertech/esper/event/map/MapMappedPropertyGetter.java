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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.BaseNestableEventUtil;

import java.util.Map;

/**
 * Getter for a dynamic mappeds property for maps.
 */
public class MapMappedPropertyGetter implements MapEventPropertyGetter, MapEventPropertyGetterAndMapped {
    private final String key;
    private final String fieldName;

    /**
     * Ctor.
     *
     * @param fieldName property name
     * @param key       get the element at
     */
    public MapMappedPropertyGetter(String fieldName, String key) {
        this.key = key;
        this.fieldName = fieldName;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        return getMapInternal(map, key);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        Object value = map.get(fieldName);
        return BaseNestableEventUtil.getMappedPropertyExists(value, key);
    }

    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException {
        Map<String, Object> data = BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean);
        return getMapInternal(data, mapKey);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Map<String, Object> data = BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean);
        return getMap(data);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Map<String, Object> data = BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean);
        return isMapExistsProperty(data);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    private Object getMapInternal(Map<String, Object> map, String providedKey) throws PropertyAccessException {
        Object value = map.get(fieldName);
        return BaseNestableEventUtil.getMappedPropertyValue(value, providedKey);
    }
}
