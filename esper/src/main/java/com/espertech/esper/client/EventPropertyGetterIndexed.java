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
 * Get property values from an event instance for a given indexed event property by passing the array index.
 * Instances that implement this interface are usually bound to a particular {@link com.espertech.esper.client.EventType} and cannot
 * be used to access {@link EventBean} instances of a different type.
 */
public interface EventPropertyGetterIndexed {
    /**
     * Return the value for the property in the event object specified when the instance was obtained.
     * Useful for fast access to event properties. Throws a PropertyAccessException if the getter instance
     * doesn't match the EventType it was obtained from, and to indicate other property access problems.
     *
     * @param eventBean is the event to get the value of a property from
     * @param index     the index value
     * @return value of indexed property in event
     * @throws PropertyAccessException to indicate that property access failed
     */
    public Object get(EventBean eventBean, int index) throws PropertyAccessException;
}
