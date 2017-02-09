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
import com.espertech.esper.event.EventBeanWriter;
import com.espertech.esper.event.MappedEventBean;

import java.util.Map;

/**
 * Writer method for writing to Map-type events.
 */
public class MapEventBeanWriterSimpleProps implements EventBeanWriter {
    private final String[] properties;

    /**
     * Ctor.
     *
     * @param properties names of properties to write
     */
    public MapEventBeanWriterSimpleProps(String[] properties) {
        this.properties = properties;
    }

    /**
     * Write values to an event.
     *
     * @param values   to write
     * @param theEvent to write to
     */
    public void write(Object[] values, EventBean theEvent) {
        MappedEventBean mappedEvent = (MappedEventBean) theEvent;
        Map<String, Object> map = mappedEvent.getProperties();

        for (int i = 0; i < properties.length; i++) {
            map.put(properties[i], values[i]);
        }
    }
}
