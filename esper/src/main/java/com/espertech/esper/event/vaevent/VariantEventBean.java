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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;

/**
 * An event bean that represents multiple potentially disparate underlying events and presents a unified face
 * across each such types or even any type.
 */
public class VariantEventBean implements EventBean, VariantEvent {
    private final VariantEventType variantEventType;
    private final EventBean underlyingEventBean;

    /**
     * Ctor.
     *
     * @param variantEventType the event type
     * @param underlying       the event
     */
    public VariantEventBean(VariantEventType variantEventType, EventBean underlying) {
        this.variantEventType = variantEventType;
        this.underlyingEventBean = underlying;
    }

    public EventType getEventType() {
        return variantEventType;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = variantEventType.getGetter(property);
        if (getter == null) {
            return null;
        }
        return getter.get(this);
    }

    public Object getUnderlying() {
        return underlyingEventBean.getUnderlying();
    }

    /**
     * Returns the underlying event.
     *
     * @return underlying event
     */
    public EventBean getUnderlyingEventBean() {
        return underlyingEventBean;
    }

    public Object getFragment(String propertyExpression) {
        EventPropertyGetter getter = variantEventType.getGetter(propertyExpression);
        if (getter == null) {
            throw PropertyAccessException.notAValidProperty(propertyExpression);
        }
        return getter.getFragment(this);
    }
}
