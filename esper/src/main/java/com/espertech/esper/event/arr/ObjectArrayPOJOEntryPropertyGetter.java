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
public class ObjectArrayPOJOEntryPropertyGetter extends BaseNativePropertyGetter implements ObjectArrayEventPropertyGetter {
    private final int propertyIndex;
    private final BeanEventPropertyGetter entryGetter;

    /**
     * Ctor.
     *
     * @param entryGetter         the getter for the map entry
     * @param eventAdapterService for producing wrappers to objects
     * @param returnType          type of the entry returned
     * @param propertyIndex       index
     * @param nestedComponentType nested component type
     */
    public ObjectArrayPOJOEntryPropertyGetter(int propertyIndex, BeanEventPropertyGetter entryGetter, EventAdapterService eventAdapterService, Class returnType, Class nestedComponentType) {
        super(eventAdapterService, returnType, nestedComponentType);
        this.propertyIndex = propertyIndex;
        this.entryGetter = entryGetter;
    }

    public Object getObjectArray(Object[] array) throws PropertyAccessException {
        // If the map does not contain the key, this is allowed and represented as null
        Object value = array[propertyIndex];

        if (value == null) {
            return null;
        }

        // Object within the map
        if (value instanceof EventBean) {
            return entryGetter.get((EventBean) value);
        }
        return entryGetter.getBeanProp(value);
    }

    public boolean isObjectArrayExistsProperty(Object[] array) {
        return true; // Property exists as the property is not dynamic (unchecked)
    }

    public Object get(EventBean obj) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(obj);
        return getObjectArray(array);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object[] array = BaseNestableEventUtil.checkedCastUnderlyingObjectArray(eventBean);
        Object value = array[propertyIndex];

        if (value == null) {
            return false;
        }

        // Object within the map
        if (value instanceof EventBean) {
            return entryGetter.isExistsProperty((EventBean) value);
        }
        return entryGetter.isBeanExistsProperty(value);
    }
}
