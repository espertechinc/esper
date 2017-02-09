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
 * Get property values from an event instance for a given event property.
 * Instances that implement this interface are usually bound to a particular {@link com.espertech.esper.client.EventType} and cannot
 * be used to access {@link EventBean} instances of a different type.
 */
public interface EventPropertyGetter {
    /**
     * Return the value for the property in the event object specified when the instance was obtained.
     * Useful for fast access to event properties. Throws a PropertyAccessException if the getter instance
     * doesn't match the EventType it was obtained from, and to indicate other property access problems.
     *
     * @param eventBean is the event to get the value of a property from
     * @return value of property in event
     * @throws PropertyAccessException to indicate that property access failed
     */
    public Object get(EventBean eventBean) throws PropertyAccessException;

    /**
     * Returns true if the property exists, or false if the type does not have such a property.
     * <p>
     * Useful for dynamic properties of the syntax "property?" and the dynamic nested/indexed/mapped versions.
     * Dynamic nested properties follow the syntax "property?.nested" which is equivalent to "property?.nested?".
     * If any of the properties in the path of a dynamic nested property return null, the dynamic nested property
     * does not exists and the method returns false.
     * <p>
     * For non-dynamic properties, this method always returns true since a getter would not be available
     * unless
     *
     * @param eventBean is the event to check if the dynamic property exists
     * @return indictor whether the property exists, always true for non-dynamic (default) properties
     */
    public boolean isExistsProperty(EventBean eventBean);

    /**
     * Returns {@link EventBean} or array of {@link EventBean} for a property name or property expression.
     * <p>
     * For use with properties whose value is itself an event or whose value can be represented as
     * an event by the underlying event representation.
     * <p>
     * The {@link EventType} of the {@link EventBean} instance(s) returned by this method can be determined by
     * {@link EventType#getFragmentType(String)}. Use {@link EventPropertyDescriptor} to
     * obtain a list of properties that return fragments from an event type.
     * <p>
     * Returns null if the property value is null or the property value cannot be represented as a fragment
     * by the underlying representation.
     *
     * @param eventBean is the event to get the fragment value of a property
     * @return the value of a property as an EventBean or array of EventBean
     * @throws PropertyAccessException - if there is no property of the specified name, or the property cannot be accessed
     */
    public Object getFragment(EventBean eventBean) throws PropertyAccessException;
}
