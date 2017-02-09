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
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.bean.BaseNativePropertyGetter;

import java.util.Map;

/**
 * A getter that works on arrays residing within a Map as an event property.
 */
public class MapArrayPOJOEntryIndexedPropertyGetter extends BaseNativePropertyGetter implements MapEventPropertyGetter, MapEventPropertyGetterAndIndexed {
    private final String propertyMap;
    private final int index;

    /**
     * Ctor.
     *
     * @param propertyMap         the property to use for the map lookup
     * @param index               the index to fetch the array element for
     * @param eventAdapterService factory for event beans and event types
     * @param returnType          type of the entry returned
     */
    public MapArrayPOJOEntryIndexedPropertyGetter(String propertyMap, int index, EventAdapterService eventAdapterService, Class returnType) {
        super(eventAdapterService, returnType, null);
        this.propertyMap = propertyMap;
        this.index = index;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        return getMapInternal(map, index);
    }

    public Object getMapInternal(Map<String, Object> map, int index) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = map.get(propertyMap);
        return BaseNestableEventUtil.getIndexedValue(value, index);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return map.containsKey(propertyMap);
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        Map<String, Object> map = BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean);
        return getMapInternal(map, index);
    }

    public Object get(EventBean obj) {
        return getMap(BaseNestableEventUtil.checkedCastUnderlyingMap(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Map map = BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean);
        return map.containsKey(propertyMap);
    }
}
