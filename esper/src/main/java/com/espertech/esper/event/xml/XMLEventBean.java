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
package com.espertech.esper.event.xml;

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventBeanSPI;
import org.w3c.dom.Node;

/**
 * EventBean wrapper for XML documents.
 * Currently only instances of org.w3c.dom.Node can be used
 *
 * @author pablo
 */
public class XMLEventBean implements EventBeanSPI {
    private EventType eventType;
    private Node theEvent;

    /**
     * Ctor.
     *
     * @param theEvent is the node with event property information
     * @param type     is the event type for this event wrapper
     */
    public XMLEventBean(Node theEvent, EventType type) {
        this.theEvent = theEvent;
        eventType = type;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setUnderlying(Object underlying) {
        theEvent = (Node) underlying;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(property);
        if (getter == null)
            throw new PropertyAccessException("Property named '" + property + "' is not a valid property name for this type");
        return getter.get(this);
    }

    public Object getUnderlying() {
        return theEvent;
    }

    public Object getFragment(String propertyExpression) {
        EventPropertyGetter getter = eventType.getGetter(propertyExpression);
        if (getter == null) {
            throw PropertyAccessException.notAValidProperty(propertyExpression);
        }
        return getter.getFragment(this);
    }
}
