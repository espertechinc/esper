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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventBeanCopyMethod;
import com.espertech.esper.event.MappedEventBean;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Copy method for Map-underlying events.
 */
public class MapEventBeanCopyMethodWithArrayMap implements EventBeanCopyMethod {
    private final MapEventType mapEventType;
    private final EventAdapterService eventAdapterService;
    private final Set<String> mapPropertiesToCopy;
    private final Set<String> arrayPropertiesToCopy;

    /**
     * Ctor.
     *
     * @param mapEventType          map event type
     * @param eventAdapterService   for copying events
     * @param mapPropertiesToCopy   map props
     * @param arrayPropertiesToCopy array props
     */
    public MapEventBeanCopyMethodWithArrayMap(MapEventType mapEventType, EventAdapterService eventAdapterService, Set<String> mapPropertiesToCopy, Set<String> arrayPropertiesToCopy) {
        this.mapEventType = mapEventType;
        this.eventAdapterService = eventAdapterService;
        this.mapPropertiesToCopy = mapPropertiesToCopy;
        this.arrayPropertiesToCopy = arrayPropertiesToCopy;
    }

    public EventBean copy(EventBean theEvent) {
        MappedEventBean mapped = (MappedEventBean) theEvent;
        Map<String, Object> props = mapped.getProperties();
        HashMap<String, Object> shallowCopy = new HashMap<String, Object>(props);

        for (String name : mapPropertiesToCopy) {
            Map<String, Object> innerMap = (Map<String, Object>) props.get(name);
            if (innerMap != null) {
                Map copy = new HashMap<String, Object>(innerMap);
                shallowCopy.put(name, copy);
            }
        }

        for (String name : arrayPropertiesToCopy) {
            Object array = props.get(name);
            if (array != null && array.getClass().isArray() && Array.getLength(array) != 0) {
                Object copied = Array.newInstance(array.getClass().getComponentType(), Array.getLength(array));
                System.arraycopy(array, 0, copied, 0, Array.getLength(array));
                shallowCopy.put(name, copied);
            }
        }
        return eventAdapterService.adapterForTypedMap(shallowCopy, mapEventType);
    }
}
