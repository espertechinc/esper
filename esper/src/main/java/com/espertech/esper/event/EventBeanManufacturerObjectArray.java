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
package com.espertech.esper.event;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.Map;

/**
 * Factory for ObjectArray-underlying events.
 */
public class EventBeanManufacturerObjectArray implements EventBeanManufacturer {
    private final ObjectArrayEventType eventType;
    private final EventAdapterService eventAdapterService;
    private final int[] indexPerWritable;
    private final boolean oneToOne;

    /**
     * Ctor.
     *
     * @param eventType           type to create
     * @param eventAdapterService event factory
     * @param properties          written properties
     */
    public EventBeanManufacturerObjectArray(ObjectArrayEventType eventType, EventAdapterService eventAdapterService, WriteablePropertyDescriptor[] properties) {
        this.eventAdapterService = eventAdapterService;
        this.eventType = eventType;

        Map<String, Integer> indexes = eventType.getPropertiesIndexes();
        indexPerWritable = new int[properties.length];
        boolean oneToOneMapping = true;
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();
            Integer index = indexes.get(propertyName);
            if (index == null) {
                throw new IllegalStateException("Failed to find property '" + propertyName + "' among the array indexes");
            }
            indexPerWritable[i] = index;
            if (index != i) {
                oneToOneMapping = false;
            }
        }
        oneToOne = oneToOneMapping && properties.length == eventType.getPropertyNames().length;
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
