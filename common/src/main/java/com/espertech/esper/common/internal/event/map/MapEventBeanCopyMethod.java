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
package com.espertech.esper.common.internal.event.map;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethod;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.core.MappedEventBean;

import java.util.HashMap;
import java.util.Map;

/**
 * Copy method for Map-underlying events.
 */
public class MapEventBeanCopyMethod implements EventBeanCopyMethod {
    private final MapEventType mapEventType;
    private final EventBeanTypedEventFactory eventAdapterService;

    /**
     * Ctor.
     *
     * @param mapEventType        map event type
     * @param eventAdapterService for copying events
     */
    public MapEventBeanCopyMethod(MapEventType mapEventType, EventBeanTypedEventFactory eventAdapterService) {
        this.mapEventType = mapEventType;
        this.eventAdapterService = eventAdapterService;
    }

    public EventBean copy(EventBean theEvent) {
        MappedEventBean mapped = (MappedEventBean) theEvent;
        Map<String, Object> props = mapped.getProperties();
        return eventAdapterService.adapterForTypedMap(new HashMap<>(props), mapEventType);
    }
}
