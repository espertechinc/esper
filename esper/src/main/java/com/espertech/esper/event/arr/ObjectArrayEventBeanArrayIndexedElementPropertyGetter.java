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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.BaseNestableEventUtil;

/**
 * Getter for an array of event bean using a nested getter.
 */
public class ObjectArrayEventBeanArrayIndexedElementPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final int index;
    private final EventPropertyGetter nestedGetter;

    /**
     * Ctor.
     *
     * @param propertyIndex property index
     * @param index         array index
     * @param nestedGetter  nested getter
     */
    public ObjectArrayEventBeanArrayIndexedElementPropertyGetter(int propertyIndex, int index, EventPropertyGetter nestedGetter) {
        this.propertyIndex = propertyIndex;
        this.index = index;
        this.nestedGetter = nestedGetter;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        EventBean[] wrapper = (EventBean[]) array[propertyIndex];
        return BaseNestableEventUtil.getArrayPropertyValue(wrapper, index, nestedGetter);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        return getObjectArray(BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean obj) {
        EventBean[] wrapper = (EventBean[]) BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj)[propertyIndex];
        return BaseNestableEventUtil.getArrayPropertyFragment(wrapper, index, nestedGetter);
    }
}