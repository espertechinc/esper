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

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventBeanSPI;
import com.espertech.esper.event.MappedEventBean;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for events represented by a Map of key-value pairs that are the event properties.
 * MapEventBean instances are equal if they have the same {@link EventType} and all property names
 * and values are reference-equal.
 */
public class MapEventBean implements EventBeanSPI, MappedEventBean {
    private EventType eventType;
    private Map<String, Object> properties;

    /**
     * Constructor for initialization with existing values.
     * Makes a shallow copy of the supplied values to not be surprised by changing property values.
     *
     * @param properties are the event property values
     * @param eventType  is the type of the event, i.e. describes the map entries
     */
    public MapEventBean(Map<String, Object> properties, EventType eventType) {
        this.properties = properties;
        this.eventType = eventType;
    }

    /**
     * Constructor for the mutable functions, e.g. only the type of values is known but not the actual values.
     *
     * @param eventType is the type of the event, i.e. describes the map entries
     */
    public MapEventBean(EventType eventType) {
        this.properties = new HashMap<String, Object>();
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setUnderlying(Object underlying) {
        properties = (Map<String, Object>) underlying;
    }

    /**
     * Returns the properties.
     *
     * @return properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(property);
        if (getter == null) {
            throw new PropertyAccessException("Property named '" + property + "' is not a valid property name for this type");
        }
        return getter.get(this);
    }

    public Object getUnderlying() {
        return properties;
    }

    public String toString() {
        return "MapEventBean " +
                "eventType=" + eventType;
    }

    public Object getFragment(String propertyExpression) {
        EventPropertyGetter getter = eventType.getGetter(propertyExpression);
        if (getter == null) {
            throw PropertyAccessException.notAValidProperty(propertyExpression);
        }
        return getter.getFragment(this);
    }
}
