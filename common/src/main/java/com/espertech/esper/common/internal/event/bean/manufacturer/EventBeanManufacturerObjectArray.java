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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.arr.ObjectArrayEventType;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;

/**
 * Factory for ObjectArray-underlying events.
 */
public class EventBeanManufacturerObjectArray implements EventBeanManufacturer {
    private final ObjectArrayEventType eventType;
    private final EventBeanTypedEventFactory eventAdapterService;
    private final int[] indexPerWritable;
    private final boolean oneToOne;

    public EventBeanManufacturerObjectArray(ObjectArrayEventType eventType, EventBeanTypedEventFactory eventAdapterService, int[] indexPerWritable, boolean oneToOne) {
        this.eventType = eventType;
        this.eventAdapterService = eventAdapterService;
        this.indexPerWritable = indexPerWritable;
        this.oneToOne = oneToOne;
    }

    public EventBean make(Object[] properties) {
        Object[] cols = makeUnderlying(properties);
        return eventAdapterService.adapterForTypedObjectArray(cols, eventType);
    }

    public Object[] makeUnderlying(Object[] properties) {
        if (oneToOne) {
            return properties;
        }
        Object[] cols = new Object[eventType.getPropertyNames().length];
        for (int i = 0; i < properties.length; i++) {
            int indexToWrite = indexPerWritable[i];
            cols[indexToWrite] = properties[i];
        }
        return cols;
    }
}
