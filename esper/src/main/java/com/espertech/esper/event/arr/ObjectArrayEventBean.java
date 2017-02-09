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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventBeanSPI;
import com.espertech.esper.event.ObjectArrayBackedEventBean;

public class ObjectArrayEventBean implements EventBeanSPI, ObjectArrayBackedEventBean {

    private Object[] propertyValues;
    private EventType eventType;

    public ObjectArrayEventBean(Object[] propertyValues, EventType eventType) {
        this.propertyValues = propertyValues;
        this.eventType = eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Object[] getProperties() {
        return propertyValues;
    }

    public void setPropertyValues(Object[] propertyValues) {
        this.propertyValues = propertyValues;
    }

    public void setUnderlying(Object underlying) {
        this.propertyValues = (Object[]) underlying;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(property);
        if (getter == null) {
            throw new PropertyAccessException("Property named '" + property + "' is not a valid property name for this type");
        }
        return getter.get(this);
    }

    public Object getUnderlying() {
        return propertyValues;
    }

    public Object getFragment(String propertyExpression) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(propertyExpression);
        if (getter == null) {
            throw PropertyAccessException.notAValidProperty(propertyExpression);
        }
        return getter.getFragment(this);
    }
}
