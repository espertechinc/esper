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

import java.util.Iterator;

/**
 * This interface provides metadata on events.
 * <p>
 * The interface exposes events as organizations of named values.
 * The contract is that any event in the system must have a name-based way of accessing sub-data within its
 * event type. A simple example is a Java bean: the names can be property names, and those properties can have still
 * more properties beneath them. Another example is a Map structure. Here string names can refer to data objects.
 * <p>
 * The interface presents an immutable view of events. There are no methods to change property values.
 * Events by definition are an observation of a past occurrance or state change and may not be modified.
 * <p>
 * Information on the super-types (superclass and interfaces implemented by JavaBean events) is also available,
 * for Java POJO events as well as for Map event types that has supertypes.
 * <p>
 * Implementations provide metadata on the properties that an implemenation itself provides.
 * <p>
 * Implementations also allow property expressioms that may use nested, indexed, mapped or a combination
 * of these as a syntax to access property types and values.
 * <p>
 * Implementations in addition may provide a means to access property values as event fragments, which
 * are typed events themselves.
 * <p>
 * The order of property names depends on the underlying event type and may be platform-specific.
 * When the underlying class is object-array the order of property names is always as-provided.
 * When the underlying class is map the order of property names is defined only when LinkedHashMap was used to register the type.
 * When the underlying class is bean the order of property names is depends on the order of the methods returned by reflection.
 */
public interface EventType {
    /**
     * Get the type of an event property.
     * <p>
     * Returns null if the property name or property expression is not valid against the event type.
     * Can also return null if a select-clause selects a constant null value.
     * <p>
     * The method takes a property name or property expression as a parameter.
     * Property expressions may include
     * indexed properties via the syntax "name[index]",
     * mapped properties via the syntax "name('key')",
     * nested properties via the syntax "outer.inner"
     * or combinations thereof.
     * <p>
     * Returns unboxed (such as 'int.class') as well as boxed (java.lang.Integer) type.
     *
     * @param propertyExpression is the property name or property expression
     * @return type of the property, the unboxed or the boxed type.
     */
    public Class getPropertyType(String propertyExpression);

    /**
     * Check that the given property name or property expression is valid for this event type, ie. that the property exists on the event type.
     * <p>
     * The method takes a property name or property expression as a parameter.
     * Property expressions may include
     * indexed properties via the syntax "name[index]",
     * mapped properties via the syntax "name('key')",
     * nested properties via the syntax "outer.inner"
     * or combinations thereof.
     *
     * @param propertyExpression is the property name or property expression to check
     * @return true if exists, false if not
     */
    public boolean isProperty(String propertyExpression);

    /**
     * Get the getter of an event property or property expression: Getters are useful when an application
     * receives events of the same event type multiple times and requires fast access
     * to an event property or nested, indexed or mapped property.
     * <p>
     * Returns null if the property name or property expression is not valid against the event type.
     * <p>
     * The method takes a property name or property expression as a parameter.
     * Property expressions may include
     * indexed properties via the syntax "name[index]",
     * mapped properties via the syntax "name('key')",
     * nested properties via the syntax "outer.inner"
     * or combinations thereof.
     *
     * @param propertyExpression is the property name or property expression
     * @return a getter that can be used to obtain property values for event instances of the same event type
     */
    public EventPropertyGetter getGetter(String propertyExpression);

    /**
     * Returns the event type of the fragment that is the value of a property name or property expression.
     * <p>
     * Returns null if the property name or property expression is not valid or does not return
     * a fragment for the event type.
     * <p>
     * The {@link EventPropertyDescriptor} provides a flag that indicates which properties
     * provide fragment events.
     * <p>
     * This is useful for navigating properties that are itself events or other well-defined types
     * that the underlying event representation may represent as an event type. It is up to each
     * event representation to determine what properties can be represented as event types themselves.
     * <p>
     * The method takes a property name or property expression as a parameter.
     * Property expressions may include
     * indexed properties via the syntax "name[index]",
     * mapped properties via the syntax "name('key')",
     * nested properties via the syntax "outer.inner"
     * or combinations thereof.
     * <p>
     * The underlying event representation may not support providing fragments or therefore fragment event types for any or all properties,
     * in which case the method returns null.
     * <p>
     * Use the {@link #getPropertyDescriptors} method to obtain a list of properties for which a fragment event type
     * may be retrieved by this method.
     *
     * @param propertyExpression is the name of the property to return the fragment event type
     * @return fragment event type of the property
     */
    public FragmentEventType getFragmentType(String propertyExpression);

    /**
     * Get the class that represents the Java type of the event type.
     * Returns a Java bean event class if the schema represents a Java bean event type.
     * Returns java.util.Map is the schema represents a collection of values in a Map.
     *
     * @return type of the event object
     */
    public Class getUnderlyingType();

    /**
     * Get the property names for the event type.
     * <p>
     * Note that the order of property names depends on the underlying event type.
     * <p>
     * The method does not return property names of inner or nested types.
     *
     * @return A string array containing the property names of this typed event data object.
     */
    public String[] getPropertyNames();

    /**
     * Get property descriptors for the event type.
     * <p>
     * Note that the order of property names depends on the underlying event type.
     * <p>
     * The method does not return property information of inner or nested types.
     *
     * @return descriptors for all known properties of the event type.
     */
    public EventPropertyDescriptor[] getPropertyDescriptors();

    /**
     * Get the property descriptor for a given property of the event, or null
     * if a property by that name was not found.
     * <p>
     * The property name parameter does accept a property expression. It therefore does not allow the indexed, mapped or nested property expression syntax
     * and only returns the descriptor for the event type's known properties.
     * <p>
     * The method does not return property information of inner or nested types.
     * <p>
     * For returning a property descriptor for nested, indexed or mapped properties
     * use {@link com.espertech.esper.event.EventTypeUtility}.
     * </p>
     *
     * @param propertyName property name
     * @return descriptor for the named property
     */
    public EventPropertyDescriptor getPropertyDescriptor(String propertyName);

    /**
     * Returns an array of event types that are super to this event type, from which this event type inherited event properties.
     * <p>For Java bean instances underlying the event this method returns the event types for all
     * superclasses extended by the Java bean and all interfaces implemented by the Java bean.
     *
     * @return an array of event types
     */
    public EventType[] getSuperTypes();

    /**
     * Returns iterator over all super types to event type, going up the hierarchy and including all
     * Java interfaces (and their extended interfaces) and superclasses as EventType instances.
     *
     * @return iterator of event types represeting all superclasses and implemented interfaces, all the way up to
     * java.lang.Object but excluding java.lang.Object itself
     */
    public Iterator<EventType> getDeepSuperTypes();

    /**
     * Returns the type name or null if no type name is assigned.
     * <p>
     * A type name is available for application-configured event types
     * and for event types that represent events of a stream populated by insert-into.
     * <p>
     * No type name is available for anonymous statement-specific event type.
     *
     * @return type name or null if none assigned
     */
    public String getName();

    /**
     * Get the getter of an event property that is a mapped event property: Getters are useful when an application
     * receives events of the same event type multiple times and requires fast access
     * to a mapped property.
     * <p>
     * Returns null if the property name is not valid against the event type or the property is not a mapped property.
     * <p>
     * The method takes a mapped property name (and not a property expression) as a parameter.
     *
     * @param mappedPropertyName is the property name
     * @return a getter that can be used to obtain property values for event instances of the same event type
     */
    public EventPropertyGetterMapped getGetterMapped(String mappedPropertyName);

    /**
     * Get the getter of an event property that is a indexed event property: Getters are useful when an application
     * receives events of the same event type multiple times and requires fast access
     * to a indexed property.
     * <p>
     * Returns null if the property name is not valid against the event type or the property is not an indexed property.
     * <p>
     * The method takes a indexed property name (and not a property expression) as a parameter.
     *
     * @param indexedPropertyName is the property name
     * @return a getter that can be used to obtain property values for event instances of the same event type
     */
    public EventPropertyGetterIndexed getGetterIndexed(String indexedPropertyName);

    /**
     * Returns the event type id assigned to the event type.
     *
     * @return event type id
     */
    public int getEventTypeId();

    /**
     * Returns the property name of the property providing the start timestamp value.
     *
     * @return start timestamp property name
     */
    public String getStartTimestampPropertyName();

    /**
     * Returns the property name of the property providing the end timestamp value.
     *
     * @return end timestamp property name
     */
    public String getEndTimestampPropertyName();
}
