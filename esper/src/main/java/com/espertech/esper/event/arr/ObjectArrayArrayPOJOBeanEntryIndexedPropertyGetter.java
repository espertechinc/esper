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
import com.espertech.esper.event.bean.BaseNativePropertyGetter;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;

/**
 * A getter that works on POJO events residing within a Map as an event property.
 */
public class ObjectArrayArrayPOJOBeanEntryIndexedPropertyGetter extends BaseNativePropertyGetter implements ObjectArrayEventPropertyGetter {

    private final int propertyIndex;
    private final int index;
    private final BeanEventPropertyGetter nestedGetter;

    /**
     * Ctor.
     *
     * @param propertyIndex       the property to look at
     * @param nestedGetter        the getter for the map entry
     * @param eventAdapterService for producing wrappers to objects
     * @param index               the index to fetch the array element for
     * @param returnType          type of the entry returned
     */
    public ObjectArrayArrayPOJOBeanEntryIndexedPropertyGetter(int propertyIndex, int index, BeanEventPropertyGetter nestedGetter, EventAdapterService eventAdapterService, Class returnType) {
        super(eventAdapterService, returnType, null);
        this.propertyIndex = propertyIndex;
        this.index = index;
        this.nestedGetter = nestedGetter;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = array[propertyIndex];
        return BaseNestableEventUtil.getBeanArrayValue(nestedGetter, value, index);
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
}
