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
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;

import java.util.Map;

/**
 * An event that is carries multiple representations of event properties:
 * A synthetic representation that is designed for delivery as {@link EventBean} to client {@link com.espertech.esper.client.UpdateListener} code,
 * and a natural representation as a bunch of Object-type properties for fast delivery to client
 * subscriber objects via method call.
 */
public class NaturalEventBean implements EventBean, DecoratingEventBean {
    private final EventType eventBeanType;
    private final Object[] natural;
    private final EventBean optionalSynthetic;

    /**
     * Ctor.
     *
     * @param eventBeanType     the event type of the synthetic event
     * @param natural           the properties of the event
     * @param optionalSynthetic the event bean that is the synthetic event, or null if no synthetic is packed in
     */
    public NaturalEventBean(EventType eventBeanType, Object[] natural, EventBean optionalSynthetic) {
        this.eventBeanType = eventBeanType;
        this.natural = natural;
        this.optionalSynthetic = optionalSynthetic;
    }

    public EventType getEventType() {
        return eventBeanType;
    }

    public Object get(String property) throws PropertyAccessException {
        if (optionalSynthetic != null) {
            return optionalSynthetic.get(property);
        }
        throw new PropertyAccessException("Property access not allowed for natural events without the synthetic event present");
    }

    public Object getUnderlying() {
        if (optionalSynthetic != null) {
            return optionalSynthetic.getUnderlying();
        }
        return natural;
    }

    public EventBean getUnderlyingEvent() {
        return ((DecoratingEventBean) optionalSynthetic).getUnderlyingEvent();
    }

    public Map<String, Object> getDecoratingProperties() {
        return ((DecoratingEventBean) optionalSynthetic).getDecoratingProperties();
    }

    /**
     * Returns the column object result representation.
     *
     * @return select column values
     */
    public Object[] getNatural() {
        return natural;
    }

    /**
     * Returns the synthetic event that can be attached.
     *
     * @return synthetic if attached, or null if none attached
     */
    public EventBean getOptionalSynthetic() {
        return optionalSynthetic;
    }

    public Object getFragment(String propertyExpression) {
        if (optionalSynthetic != null) {
            return optionalSynthetic.getFragment(propertyExpression);
        }
        throw new PropertyAccessException("Property access not allowed for natural events without the synthetic event present");
    }
}
