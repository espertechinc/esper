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
 * Getter for a dynamic indexed property for maps.
 */
public class MapIndexedPropertyGetter implements MapEventPropertyGetter {
    private final int index;
    private final String fieldName;

    /**
     * Ctor.
     *
     * @param fieldName property name
     * @param index     index to get the element at
     */
    public MapIndexedPropertyGetter(String fieldName, int index) {
        this.index = index;
        this.fieldName = fieldName;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object value = map.get(fieldName);
        return BaseNestableEventUtil.getIndexedValue(value, index);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        Object value = map.get(fieldName);
        return BaseNestableEventUtil.isExistsIndexedValue(value, index);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getMap(BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isMapExistsProperty(BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean));
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }
}
