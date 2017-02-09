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
 * A getter for use with Map-based events simply returns the value for the key.
 */
public class ObjectArrayEventBeanPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;

    /**
     * Ctor.
     *
     * @param propertyIndex property to get
     */
    public ObjectArrayEventBeanPropertyGetter(int propertyIndex) {
        this.propertyIndex = propertyIndex;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        Object eventBean = array[propertyIndex];
        if (eventBean == null) {
            return null;
        }

        EventBean theEvent = (EventBean) eventBean;
        return theEvent.getUnderlying();
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
        return BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj)[propertyIndex];
    }
}
