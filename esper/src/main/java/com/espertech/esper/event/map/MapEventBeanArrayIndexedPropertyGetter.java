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
 * Getter for array events.
 */
public class MapEventBeanArrayIndexedPropertyGetter implements MapEventPropertyGetter {
    private final String propertyName;
    private final int index;

    /**
     * Ctor.
     *
     * @param propertyName property name
     * @param index        array index
     */
    public MapEventBeanArrayIndexedPropertyGetter(String propertyName, int index) {
        this.propertyName = propertyName;
        this.index = index;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        EventBean[] wrapper = (EventBean[]) map.get(propertyName);
        return BaseNestableEventUtil.getArrayPropertyUnderlying(wrapper, index);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return true;
    }

    public Object get(EventBean obj) {
        return getMap(BaseNestableEventUtil.checkedCastUnderlyingMap(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean obj) {
        Map<String, Object> map = BaseNestableEventUtil.checkedCastUnderlyingMap(obj);
        EventBean[] wrapper = (EventBean[]) map.get(propertyName);
        return BaseNestableEventUtil.getArrayPropertyBean(wrapper, index);
    }
}