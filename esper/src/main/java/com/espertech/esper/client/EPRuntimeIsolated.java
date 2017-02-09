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

import java.net.URI;
import java.util.Map;

/**
 * Runtime interface for the isolated service provider, for controlling event visibility and scheduling
 * for the statements contained within the isolated service.
 */
public interface EPRuntimeIsolated {
    /**
     * Send an event represented by a plain Java object to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     *
     * @param object is the event to sent to the runtime
     * @throws com.espertech.esper.client.EPException is thrown when the processing of the event lead to an error
     */
    public void sendEvent(Object object) throws EPException;

    /**
     * Send a map containing event property values to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     *
     * @param map           - map that contains event property values. Keys are expected to be of type String while values
     *                      can be of any type. Keys and values should match those declared via Configuration for the given eventTypeName.
     * @param eventTypeName - the name for the Map event type that was previously configured
     * @throws com.espertech.esper.client.EPException - when the processing of the event leads to an error
     */
    public void sendEvent(Map map, String eventTypeName) throws EPException;

    /**
     * Send an object array containing event property values to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     *
     * @param objectarray              - array that contains event property values. Your application must ensure that property values
     *                                 match the exact same order that the property names and types have been declared, and that the array length matches the number of properties declared.
     * @param objectArrayEventTypeName - the name for the Object-array event type that was previously configured
     * @throws EPException - when the processing of the event leads to an error
     */
    public void sendEvent(Object[] objectarray, String objectArrayEventTypeName);

    /**
     * Send an event represented by a DOM node to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     *
     * @param node is the DOM node as an event
     * @throws com.espertech.esper.client.EPException is thrown when the processing of the event lead to an error
     */
    public void sendEvent(org.w3c.dom.Node node) throws EPException;

    /**
     * Returns current engine time.
     * <p>
     * If time is provided externally via timer events, the function returns current time as externally provided.
     *
     * @return current engine time
     */
    public long getCurrentTime();

    /**
     * Returns the time at which the next schedule execution is expected, returns null if no schedule execution is
     * outstanding.
     *
     * @return time of next schedule if any
     */
    public Long getNextScheduledTime();

    /**
     * Returns a facility to process event objects that are of a known type.
     * <p>
     * Given an event type name this method returns a sender that allows to send in
     * event objects of that type. The event objects send in via the event sender
     * are expected to match the event type, thus the event sender does
     * not inspect the event object other then perform basic checking.
     * <p>
     * For events backed by a Java class (JavaBean events), the sender ensures that the
     * object send in matches in class, or implements or extends the class underlying the event type
     * for the given event type name.
     * <p>
     * For events backed by a Object[] (Object-array events), the sender does not perform any checking other
     * then checking that the event object indeed is an array of object.
     * <p>
     * For events backed by a java.util.Map (Map events), the sender does not perform any checking other
     * then checking that the event object indeed implements Map.
     * <p>
     * For events backed by a org.w3c.Node (XML DOM events), the sender checks that the root element name
     * indeed does match the root element name for the event type name.
     *
     * @param eventTypeName is the name of the event type
     * @return sender for fast-access processing of event objects of known type (and content)
     * @throws EventTypeException thrown to indicate that the name does not exist
     */
    public EventSender getEventSender(String eventTypeName) throws EventTypeException;

    /**
     * For use with plug-in event representations, returns a facility to process event objects that are of one of a number of types
     * that one or more of the registered plug-in event representation extensions can reflect upon and provide an
     * event for.
     *
     * @param uris is the URIs that specify which plug-in event representations may process an event object.
     *             <p>URIs do not need to match event representation URIs exactly, a child (hierarchical) match is enough
     *             for an event representation to participate.
     *             <p>The order of URIs is relevant as each event representation's factory is asked in turn to
     *             process the event, until the first factory processes the event.
     * @return sender for processing of event objects of one of the plug-in event representations
     * @throws EventTypeException thrown to indicate that the URI list was invalid
     */
    public EventSender getEventSender(URI[] uris) throws EventTypeException;
}