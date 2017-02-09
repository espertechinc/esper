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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventBeanSPI;

/**
 * Wrapper for Java bean (POJO or regular) Java objects the represent events.
 * Allows access to event properties, which is done through the getter supplied by the event type.
 * {@link EventType} instances containing type information are obtained from {@link BeanEventTypeFactory}.
 * Two BeanEventBean instances are equal if they have the same event type and refer to the same instance of event object.
 * Clients that need to compute equality between Java beans wrapped by this class need to obtain the underlying object.
 */
public class BeanEventBean implements EventBeanSPI {
    private EventType eventType;
    private Object theEvent;

    /**
     * Constructor.
     *
     * @param theEvent  is the event object
     * @param eventType is the schema information for the event object.
     */
    public BeanEventBean(Object theEvent, EventType eventType) {
        this.eventType = eventType;
        this.theEvent = theEvent;
    }

    public Object getUnderlying() {
        return theEvent;
    }

    public void setUnderlying(Object underlying) {
        theEvent = underlying;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(property);
        if (getter == null) {
            throw new PropertyAccessException("Property named '" + property + "' is not a valid property name for this type");
        }
        return getter.get(this);
    }

    public String toString() {
        return "BeanEventBean" +
                " eventType=" + eventType +
                " bean=" + theEvent;
    }

    public Object getFragment(String propertyExpression) {
        EventPropertyGetter getter = eventType.getGetter(propertyExpression);
        if (getter == null) {
            throw PropertyAccessException.notAValidProperty(propertyExpression);
        }
        return getter.getFragment(this);
    }
}
