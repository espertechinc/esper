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
import com.espertech.esper.event.map.MapEventType;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for Map-underlying events.
 */
public class EventBeanManufacturerMap implements EventBeanManufacturer {
    private final MapEventType mapEventType;
    private final EventAdapterService eventAdapterService;
    private final WriteablePropertyDescriptor[] writables;

    /**
     * Ctor.
     *
     * @param mapEventType        type to create
     * @param eventAdapterService event factory
     * @param properties          written properties
     */
    public EventBeanManufacturerMap(MapEventType mapEventType, EventAdapterService eventAdapterService, WriteablePropertyDescriptor[] properties) {
        this.eventAdapterService = eventAdapterService;
        this.mapEventType = mapEventType;
        this.writables = properties;
    }

    public EventBean make(Object[] properties) {
        Map<String, Object> values = makeUnderlying(properties);
        return eventAdapterService.adapterForTypedMap(values, mapEventType);
    }

    public Map<String, Object> makeUnderlying(Object[] properties) {
        Map<String, Object> values = new HashMap<String, Object>();
        for (int i = 0; i < properties.length; i++) {
            values.put(writables[i].getPropertyName(), properties[i]);
        }
        return values;
    }
}
