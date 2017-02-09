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
 * Returns the event bean or the underlying array.
 */
public class ObjectArrayEventBeanArrayPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final Class underlyingType;

    /**
     * Ctor.
     *
     * @param propertyIndex  property to get
     * @param underlyingType type of property
     */
    public ObjectArrayEventBeanArrayPropertyGetter(int propertyIndex, Class underlyingType) {
        this.propertyIndex = propertyIndex;
        this.underlyingType = underlyingType;
    }

    public Object getObjectArray(Object[] arrayEvent) throws PropertyAccessException {
        Object innerValue = arrayEvent[propertyIndex];
        return BaseNestableEventUtil.getArrayPropertyAsUnderlyingsArray(underlyingType, (EventBean[]) innerValue);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return getObjectArray(array);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return array[propertyIndex];
    }
}
