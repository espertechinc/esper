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
package com.espertech.esper.runtime.client;

import com.espertech.esper.common.client.EPException;
import org.w3c.dom.Node;

import java.util.Map;

/**
 * Service for processing events.
 * <p>
 * Use any of the route-event methods of {@link EPEventServiceRouteEvent} when listeners, subscribers or extension code
 * process events.
 * </p>
 */
public interface EPEventServiceSendEvent {
    /**
     * Send an object array containing event property values to the runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link EPEventServiceRouteEvent#routeEventObjectArray(Object[], String)}.
     *
     * @param event         - array that contains event property values. Your application must ensure that property values
     *                      match the exact same order that the property names and types have been declared, and that the array length matches the number of properties declared.
     * @param eventTypeName - event type name
     * @throws EPException - when the processing of the event leads to an error
     */
    void sendEventObjectArray(Object[] event, String eventTypeName);

    /**
     * Send an event represented by an object to the runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link EPEventServiceRouteEvent#routeEventBean(Object, String)}.
     *
     * @param event         is the event to sent to the runtime
     * @param eventTypeName event type name
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    void sendEventBean(Object event, String eventTypeName);

    /**
     * Send a map containing event property values to the runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link EPEventServiceRouteEvent#routeEventMap(java.util.Map, String)}).
     *
     * @param event         - map that contains event property values. Keys are expected to be of type String while values
     *                      can be of any type. Keys and values should match those declared via Configuration for the given eventTypeName.
     * @param eventTypeName - event type name
     * @throws EPException - when the processing of the event leads to an error
     */
    void sendEventMap(Map<String, Object> event, String eventTypeName);

    /**
     * Send an event represented by a DOM node to the runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link EPEventServiceRouteEvent#routeEventXMLDOM(Node, String)}.
     *
     * @param node          is the DOM node as an event
     * @param eventTypeName event type name
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    void sendEventXMLDOM(Node node, String eventTypeName);

    /**
     * Send an event represented by a Avro GenericData.Record to the runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link EPEventServiceRouteEvent#routeEventAvro(Object, String)}}).
     *
     * @param avroGenericDataDotRecord is the event to sent to the runtime
     * @param avroEventTypeName        event type name
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    void sendEventAvro(Object avroGenericDataDotRecord, String avroEventTypeName);
}
