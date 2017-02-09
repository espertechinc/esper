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
 * Getter for a dynamic indexed property for maps.
 */
public class ObjectArrayIndexedPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final int index;

    /**
     * Ctor.
     *
     * @param propertyIndex property index
     * @param index         index to get the element at
     */
    public ObjectArrayIndexedPropertyGetter(int propertyIndex, int index) {
        this.propertyIndex = propertyIndex;
        this.index = index;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        Object value = array[propertyIndex];
        return BaseNestableEventUtil.getIndexedValue(value, index);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        Object value = array[propertyIndex];
        return BaseNestableEventUtil.isExistsIndexedValue(value, index);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getObjectArray(BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isObjectArrayExistsProperty(BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean));
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }
}
