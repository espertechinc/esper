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

/**
 * A getter that works on arrays residing within a Map as an event property.
 */
public class ObjectArrayArrayPOJOEntryIndexedPropertyGetter extends BaseNativePropertyGetter implements ObjectArrayEventPropertyGetterAndIndexed {
    private final int propertyIndex;
    private final int index;

    /**
     * Ctor.
     *
     * @param index               the index to fetch the array element for
     * @param eventAdapterService factory for event beans and event types
     * @param returnType          type of the entry returned
     * @param propertyIndex       property num
     */
    public ObjectArrayArrayPOJOEntryIndexedPropertyGetter(int propertyIndex, int index, EventAdapterService eventAdapterService, Class returnType) {
        super(eventAdapterService, returnType, null);
        this.propertyIndex = propertyIndex;
        this.index = index;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        return getArrayInternal(array, index);
    }

    public Object getArrayInternal(Object[] array, int index) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = array[propertyIndex];
        return BaseNestableEventUtil.getIndexedValue(value, index);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return array.length > index;
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return getArrayInternal(array, index);
    }

    public Object get(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return getObjectArray(array);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        return array.length > index;
    }
}
