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
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.bean.BeanEventType;

/**
 * A getter for use with Map-based events simply returns the value for the key.
 */
public class ObjectArrayEntryPropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final EventAdapterService eventAdapterService;
    private final BeanEventType eventType;

    /**
     * Ctor.
     *
     * @param propertyIndex       index
     * @param eventType           type of the entry returned
     * @param eventAdapterService factory for event beans and event types
     */
    public ObjectArrayEntryPropertyGetter(int propertyIndex, BeanEventType eventType, EventAdapterService eventAdapterService) {
        this.propertyIndex = propertyIndex;
        this.eventAdapterService = eventAdapterService;
        this.eventType = eventType;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        return array[propertyIndex];
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        Object[] arr = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return getObjectArray(arr);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object getFragment(EventBean eventBean) {
        if (eventType == null) {
            return null;
        }
        Object result = get(eventBean);
        return BaseNestableEventUtil.getFragmentPojo(result, eventType, eventAdapterService);
    }
}
