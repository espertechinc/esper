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

/**
 * Getter for a dynamic mappeds property for maps.
 */
public class ObjectArrayMappedPropertyGetter implements ObjectArrayEventPropertyGetterAndMapped {
    private final int propertyIndex;
    private final String key;

    /**
     * Ctor.
     *
     * @param propertyIndex property index
     * @param key           get the element at
     */
    public ObjectArrayMappedPropertyGetter(int propertyIndex, String key) {
        this.propertyIndex = propertyIndex;
        this.key = key;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        return getObjectArrayInternal(array, key);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        Object value = array[propertyIndex];
        return BaseNestableEventUtil.getMappedPropertyExists(value, key);
    }

    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException {
        Object[] data = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return getObjectArrayInternal(data, mapKey);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object[] data = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return getObjectArray(data);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] data = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return isObjectArrayExistsProperty(data);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    private Object getObjectArrayInternal(Object[] objectArray, String providedKey) throws PropertyAccessException {
        Object value = objectArray[propertyIndex];
        return BaseNestableEventUtil.getMappedPropertyValue(value, providedKey);
    }
}
