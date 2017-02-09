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
package com.espertech.esperio.representation.axiom;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import org.apache.axiom.om.OMNode;

/**
 * EventBean wrapper for XML documents. Currently only instances of OMNode can
 * be used
 *
 * @author Paul Fremantle
 */
public class AxiomEventBean implements EventBean {
    private EventType eventType;
    private OMNode theEvent;

    /**
     * Ctor.
     *
     * @param theEvent is the node with event property information
     * @param type     is the event type for this event wrapper
     */
    public AxiomEventBean(OMNode theEvent, EventType type) {
        this.theEvent = theEvent;
        eventType = type;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = eventType.getGetter(property);
        if (getter == null) {
            throw new PropertyAccessException("Property named '" + property
                    + "' is not a valid property name for this type");
        }
        return getter.get(this);
    }

    public Object getUnderlying() {
        return theEvent;
    }

    public Object getFragment(String propertyExpression) throws PropertyAccessException {
        return null;
    }
}
