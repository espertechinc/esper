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
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;

public abstract class ObjectArrayNestedEntryPropertyGetterBase implements ObjectArrayEventPropertyGetter {

    protected final int propertyIndex;
    protected final EventType fragmentType;
    protected final EventAdapterService eventAdapterService;

    /**
     * Ctor.
     *
     * @param propertyIndex       the property to look at
     * @param eventAdapterService factory for event beans and event types
     * @param fragmentType        type of the entry returned
     */
    public ObjectArrayNestedEntryPropertyGetterBase(int propertyIndex, EventType fragmentType, EventAdapterService eventAdapterService) {
        this.propertyIndex = propertyIndex;
        this.fragmentType = fragmentType;
        this.eventAdapterService = eventAdapterService;
    }

    public abstract Object handleNestedValue(Object value);

    public abstract boolean handleNestedValueExists(Object value);

    public abstract Object handleNestedValueFragment(Object value);

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        Object value = array[propertyIndex];
        if (value == null) {
            return null;
        }
        return handleNestedValue(value);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        return getObjectArray(BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        Object value = array[propertyIndex];
        if (value == null) {
            return false;
        }
        return handleNestedValueExists(value);
    }

    public Object getFragment(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        Object value = array[propertyIndex];
        if (value == null) {
            return null;
        }
        return handleNestedValueFragment(value);
    }
}
