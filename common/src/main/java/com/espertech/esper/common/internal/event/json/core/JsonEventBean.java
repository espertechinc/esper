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
package com.espertech.esper.common.internal.event.json.core;

import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.core.EventBeanSPI;

public class JsonEventBean implements EventBeanSPI, JsonBackedEventBean {
    private EventType eventType;
    private Object theEvent;

    /**
     * Constructor.
     *
     * @param theEvent  is the event object
     * @param eventType is the schema information for the event object.
     */
    public JsonEventBean(Object theEvent, EventType eventType) {
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
        return "JsonEventBean" +
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
