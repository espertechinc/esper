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
package com.espertech.esper.common.internal.event.arr;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethod;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

/**
 * Copy method for Map-underlying events.
 */
public class ObjectArrayEventBeanCopyMethodWithArrayMap implements EventBeanCopyMethod {
    private final ObjectArrayEventType eventType;
    private final EventBeanTypedEventFactory eventAdapterService;
    private final int[] mapIndexesToCopy;
    private final int[] arrayIndexesToCopy;

    public ObjectArrayEventBeanCopyMethodWithArrayMap(ObjectArrayEventType eventType, EventBeanTypedEventFactory eventAdapterService, int[] mapIndexesToCopy, int[] arrayIndexesToCopy) {
        this.eventType = eventType;
        this.eventAdapterService = eventAdapterService;
        this.mapIndexesToCopy = mapIndexesToCopy;
        this.arrayIndexesToCopy = arrayIndexesToCopy;
    }

    public EventBean copy(EventBean theEvent) {
        ObjectArrayBackedEventBean arrayBacked = (ObjectArrayBackedEventBean) theEvent;
        Object[] props = arrayBacked.getProperties();
        Object[] shallowCopy = new Object[props.length];
        System.arraycopy(props, 0, shallowCopy, 0, props.length);

        for (int index : mapIndexesToCopy) {
            Map<String, Object> innerMap = (Map<String, Object>) shallowCopy[index];
            if (innerMap != null) {
                Map copy = new HashMap<String, Object>(innerMap);
                shallowCopy[index] = copy;
            }
        }

        for (int index : arrayIndexesToCopy) {
            Object array = shallowCopy[index];
            if (array != null && array.getClass().isArray() && Array.getLength(array) != 0) {
                Object copied = Array.newInstance(array.getClass().getComponentType(), Array.getLength(array));
                System.arraycopy(array, 0, copied, 0, Array.getLength(array));
                shallowCopy[index] = copied;
            }
        }
        return eventAdapterService.adapterForTypedObjectArray(shallowCopy, eventType);
    }
}
