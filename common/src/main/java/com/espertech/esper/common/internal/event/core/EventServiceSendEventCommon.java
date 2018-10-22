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
package com.espertech.esper.common.internal.event.core;

import com.espertech.esper.common.client.EPException;
import org.w3c.dom.Node;

import java.util.Map;

public interface EventServiceSendEventCommon {
    void sendEventObjectArray(Object[] event, String eventTypeName);

    void sendEventBean(Object event, String eventTypeName);

    void sendEventMap(Map<String, Object> event, String eventTypeName);

    void sendEventXMLDOM(Node node, String eventTypeName);

    /**
     * Send an event represented by a Avro GenericData.Record to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent
     * (except with the outbound-threading configuration), see {@link EventServiceRouteEventCommon#routeEventAvro(Object, String)}}).
     *
     * @param avroGenericDataDotRecord is the event to sent to the runtime
     * @param avroEventTypeName        event type name
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    void sendEventAvro(Object avroGenericDataDotRecord, String avroEventTypeName);
}
