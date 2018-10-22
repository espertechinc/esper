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

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

/**
 * Copy method for Map-underlying events.
 */
public class MapEventBeanCopyMethodWithArrayMap implements EventBeanCopyMethod {
    private final MapEventType mapEventType;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final String[] mapPropertiesToCopy;
    private final String[] arrayPropertiesToCopy;

    /**
     * Ctor.
     *
     * @param mapEventType               map event type
     * @param eventBeanTypedEventFactory for copying events
     * @param mapPropertiesToCopy        map props
     * @param arrayPropertiesToCopy      array props
     */
    public MapEventBeanCopyMethodWithArrayMap(MapEventType mapEventType, EventBeanTypedEventFactory eventBeanTypedEventFactory, String[] mapPropertiesToCopy, String[] arrayPropertiesToCopy) {
        this.mapEventType = mapEventType;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
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

        return eventBeanTypedEventFactory.adapterForTypedMap(shallowCopy, mapEventType);
    }
}
