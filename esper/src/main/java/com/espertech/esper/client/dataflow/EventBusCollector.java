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
package com.espertech.esper.client.dataflow;

import com.espertech.esper.client.EPException;

import java.util.Map;

/**
 * Collector for send events into the event bus.
 */
public interface EventBusCollector {
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
     * @throws EPException - when the processing of the event leads to an error
     */
    public void sendEvent(Map map, String eventTypeName) throws EPException;

    /**
     * Send an object array containing event property values as array elements to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     *
     * @param objectArray   - object array that contains event property values.
     *                      Your application must ensure that property values match the exact same order that the property names and
     *                      types have been declared, and that the array length matches the number of properties declared.
     * @param eventTypeName - the name for the Object-array event type that was previously configured
     * @throws EPException - when the processing of the event leads to an error
     */
    public void sendEvent(Object[] objectArray, String eventTypeName) throws EPException;

    /**
     * Send an event represented by a DOM node to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     *
     * @param node is the DOM node as an event
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    public void sendEvent(org.w3c.dom.Node node) throws EPException;
}
