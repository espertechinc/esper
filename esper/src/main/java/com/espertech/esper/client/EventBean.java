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
 * Interface for event representation. All events have an {@link EventType}. Events also
 * usually have one or more event properties. This interface allows the querying of event type,
 * event property values and the underlying event object.
 */
public interface EventBean {
    /**
     * Return the {@link EventType} instance that describes the set of properties available for this event.
     *
     * @return event type
     */
    public EventType getEventType();

    /**
     * Returns the value of an event property for the given property name or property expression.
     * <p>
     * Returns null if the property value is null. Throws an exception if the expression is not valid
     * against the event type.
     * <p>
     * The method takes a property name or property expression as a parameter.
     * Property expressions may include
     * indexed properties via the syntax "name[index]",
     * mapped properties via the syntax "name('key')",
     * nested properties via the syntax "outer.inner"
     * or combinations thereof.
     *
     * @param propertyExpression - name or expression of the property whose value is to be retrieved
     * @return the value of a property with the specified name.
     * @throws PropertyAccessException - if there is no property of the specified name, or the property cannot be accessed
     */
    public Object get(String propertyExpression) throws PropertyAccessException;

    /**
     * Get the underlying data object to this event wrapper.
     *
     * @return underlying data object, usually either a Map or a Java bean instance.
     */
    public Object getUnderlying();

    /**
     * Returns event beans or array of event bean for a property name or property expression.
     * <p>
     * For use with properties whose value is itself an event or whose value can be represented as
     * an event by the underlying event representation.
     * <p>
     * The {@link EventType} of the event bean instance(s) returned by this method can be determined by
     * {@link EventType#getFragmentType(String)}. Use {@link EventPropertyDescriptor} to
     * obtain a list of properties that return fragments from an event type.
     * <p>
     * Returns null if the property value is null or the property value cannot be represented as a fragment
     * by the underlying representation.
     * <p>
     * The method takes a property name or property expression as a parameter.
     * Property expressions may include
     * indexed properties via the syntax "name[index]",
     * mapped properties via the syntax "name('key')",
     * nested properties via the syntax "outer.inner"
     * or combinations thereof.
     *
     * @param propertyExpression - name or expression of the property whose value is to be presented as an EventBean or array of EventBean
     * @return the value of a property as an EventBean or array of EventBean
     * @throws PropertyAccessException - if there is no property of the specified name, or the property cannot be accessed
     */
    public Object getFragment(String propertyExpression) throws PropertyAccessException;
}
