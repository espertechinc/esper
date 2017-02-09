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

/**
 * Getter for a dynamic property (syntax field.inner?), using vanilla reflection.
 */
public class ObjectArrayDynamicPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final String propertyName;

    public ObjectArrayDynamicPropertyGetter(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        return null;
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return false;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        ObjectArrayEventType objectArrayEventType = (ObjectArrayEventType) eventBean.getEventType();
        Integer index = objectArrayEventType.getPropertiesIndexes().get(propertyName);
        if (index == null) {
            return null;
        }
        Object[] theEvent = (Object[]) eventBean.getUnderlying();
        return theEvent[index];
    }

    public boolean isExistsProperty(EventBean eventBean) {
        ObjectArrayEventType objectArrayEventType = (ObjectArrayEventType) eventBean.getEventType();
        Integer index = objectArrayEventType.getPropertiesIndexes().get(propertyName);
        return index != null;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }
}
