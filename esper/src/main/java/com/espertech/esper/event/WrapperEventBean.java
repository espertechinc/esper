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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.collection.Pair;

import java.util.Map;

/**
 * Event bean that wraps another event bean adding additional properties.
 * <p>
 * This can be useful for classes for which the statement adds derived values retaining the original class.
 * <p>
 * The event type of such events is always {@link WrapperEventType}. Additional properties are stored in a
 * Map.
 */
public class WrapperEventBean implements EventBean, DecoratingEventBean {
    private final EventBean theEvent;
    private final Map<String, Object> map;
    private final EventType eventType;

    /**
     * Ctor.
     *
     * @param theEvent   is the wrapped event
     * @param properties is zero or more property values that embellish the wrapped event
     * @param eventType  is the {@link WrapperEventType}.
     */
    public WrapperEventBean(EventBean theEvent, Map<String, Object> properties, EventType eventType) {
        this.theEvent = theEvent;
        this.map = properties;
        this.eventType = eventType;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(property);
        if (getter == null) {
            throw new PropertyAccessException("Property named '" + property + "' is not a valid property name for this type");
        }
        return eventType.getGetter(property).get(this);
    }

    public EventType getEventType() {
        return eventType;
    }

    public Object getUnderlying() {
        // If wrapper is simply for the underlyingg with no additional properties, then return the underlying type
        if (map.isEmpty()) {
            return theEvent.getUnderlying();
        } else {
            return new Pair<Object, Map>(theEvent.getUnderlying(), map);
        }
    }

    /**
     * Returns the underlying map storing the additional properties, if any.
     *
     * @return event property map
     */
    public Map getUnderlyingMap() {
        return map;
    }

    public Map<String, Object> getDecoratingProperties() {
        return map;
    }

    public EventBean getUnderlyingEvent() {
        return theEvent;
    }

    public String toString() {
        return "WrapperEventBean " +
                "[event=" + theEvent + "] " +
                "[properties=" + map + "]";
    }

    public Object getFragment(String propertyExpression) {
        EventPropertyGetter getter = eventType.getGetter(propertyExpression);
        if (getter == null) {
            throw PropertyAccessException.notAValidProperty(propertyExpression);
        }
        return getter.getFragment(this);
    }
}
