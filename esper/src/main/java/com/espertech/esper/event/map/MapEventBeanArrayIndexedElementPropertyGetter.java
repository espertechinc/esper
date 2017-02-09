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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.BaseNestableEventUtil;

import java.util.Map;

/**
 * Getter for an array of event bean using a nested getter.
 */
public class MapEventBeanArrayIndexedElementPropertyGetter implements MapEventPropertyGetter {
    private final String propertyName;
    private final int index;
    private final EventPropertyGetter nestedGetter;

    /**
     * Ctor.
     *
     * @param propertyName property name
     * @param index        array index
     * @param nestedGetter nested getter
     */
    public MapEventBeanArrayIndexedElementPropertyGetter(String propertyName, int index, EventPropertyGetter nestedGetter) {
        this.propertyName = propertyName;
        this.index = index;
        this.nestedGetter = nestedGetter;
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        EventBean[] wrapper = (EventBean[]) map.get(propertyName);
        return BaseNestableEventUtil.getArrayPropertyValue(wrapper, index, nestedGetter);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        return true; // Property exists as the property is not dynamic (unchecked)
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
        return BaseNestableEventUtil.getArrayPropertyFragment(wrapper, index, nestedGetter);
    }
}