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
package com.espertech.esper.client;

/**
 * Get property values from an event instance for a given mapped event property by passing the map string key.
 * Instances that implement this interface are usually bound to a particular {@link EventType} and cannot
 * be used to access {@link com.espertech.esper.client.EventBean} instances of a different type.
 */
public interface EventPropertyGetterMapped {
    /**
     * Return the value for the property in the event object specified when the instance was obtained.
     * Useful for fast access to event properties. Throws a PropertyAccessException if the getter instance
     * doesn't match the EventType it was obtained from, and to indicate other property access problems.
     *
     * @param eventBean is the event to get the value of a property from
     * @param mapKey    the map key value
     * @return value of property in event
     * @throws com.espertech.esper.client.PropertyAccessException to indicate that property access failed
     */
    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException;
}
