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
 * A getter that works on EventBean events residing within a Map as an event property.
 */
public class ObjectArrayEventBeanEntryPropertyGetter implements ObjectArrayEventPropertyGetter {

    private final int propertyIndex;
    private final EventPropertyGetter eventBeanEntryGetter;

    /**
     * Ctor.
     *
     * @param propertyIndex        the property to look at
     * @param eventBeanEntryGetter the getter for the map entry
     */
    public ObjectArrayEventBeanEntryPropertyGetter(int propertyIndex, EventPropertyGetter eventBeanEntryGetter) {
        this.propertyIndex = propertyIndex;
        this.eventBeanEntryGetter = eventBeanEntryGetter;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = array[propertyIndex];

        if (value == null) {
            return null;
        }

        // Object within the map
        EventBean theEvent = (EventBean) value;
        return eventBeanEntryGetter.get(theEvent);
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
        // If the map does not contain the key, this is allowed and represented as null
        Object value = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj)[propertyIndex];

        if (value == null) {
            return null;
        }

        // Object within the map
        EventBean theEvent = (EventBean) value;
        return eventBeanEntryGetter.getFragment(theEvent);
    }
}
