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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.map.MapEventPropertyGetter;

import java.util.Map;

public class ObjectArrayMapPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int index;
    private final MapEventPropertyGetter getter;

    /**
     * Ctor.
     *
     * @param getter is the getter to use to interrogate the property in the map
     * @param index  index
     */
    public ObjectArrayMapPropertyGetter(int index, MapEventPropertyGetter getter) {
        if (getter == null) {
            throw new IllegalArgumentException("Getter is a required parameter");
        }
        this.index = index;
        this.getter = getter;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        Object valueTopObj = array[index];
        if (!(valueTopObj instanceof Map)) {
            return null;
        }
        return getter.getMap((Map) valueTopObj);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        Object valueTopObj = array[index];
        if (!(valueTopObj instanceof Map)) {
            return false;
        }

        return getter.isMapExistsProperty((Map) valueTopObj);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return getObjectArray(array);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return isObjectArrayExistsProperty(array);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }
}
