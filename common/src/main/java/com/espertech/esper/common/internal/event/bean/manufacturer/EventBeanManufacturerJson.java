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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.WriteablePropertyDescriptor;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventObjectBase;

import java.util.Map;

/**
 * Factory for Json-underlying events.
 */
public class EventBeanManufacturerJson implements EventBeanManufacturer {
    private final JsonEventType jsonEventType;
    private final EventBeanTypedEventFactory eventAdapterService;
    private final int[] nativeNums;

    /**
     * Ctor.
     * @param jsonEventType type to create
     * @param eventAdapterService event factory
     * @param nativeNums native field numbers
     */
    public EventBeanManufacturerJson(JsonEventType jsonEventType, EventBeanTypedEventFactory eventAdapterService, int[] nativeNums) {
        this.eventAdapterService = eventAdapterService;
        this.jsonEventType = jsonEventType;
        this.nativeNums = nativeNums;
    }

    public EventBean make(Object[] properties) {
        Object values = makeUnderlying(properties);
        return eventAdapterService.adapterForTypedJson(values, jsonEventType);
    }

    public Object makeUnderlying(Object[] properties) {
        Object underlying = jsonEventType.getDelegateFactory().newUnderlying();
        for (int i = 0; i < properties.length; i++) {
            jsonEventType.getDelegateFactory().setValue(nativeNums[i], properties[i], underlying);
        }
        return underlying;
    }

    static int[] findPropertyIndexes(JsonEventType jsonEventType, WriteablePropertyDescriptor[] writables) {
        int[] nativeNums = new int[writables.length];
        for (int i = 0; i < writables.length; i++) {
            nativeNums[i] = findPropertyIndex(jsonEventType, writables[i].getPropertyName());
        }
        return nativeNums;
    }

    private static int findPropertyIndex(JsonEventType jsonEventType, String propertyName) {
        Map<String, Object> types = jsonEventType.getTypes();
        int index = 0;
        for (Map.Entry<String, Object> entry : types.entrySet()) {
            if (entry.getKey().equals(propertyName)) {
                return index;
            }
            index++;
        }
        throw new IllegalStateException("Failed to find writable property '" + propertyName + "'");
    }
}
