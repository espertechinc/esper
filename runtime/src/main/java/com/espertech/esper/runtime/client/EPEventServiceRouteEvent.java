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

import org.w3c.dom.Node;

import java.util.Map;

/**
 * Service for processing events that originate from listeners, subscribers or other extension code.
 */
public interface EPEventServiceRouteEvent {

    /**
     * Route the event object back to the runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param event         to route internally for processing by the runtime
     * @param eventTypeName event type name
     */
    void routeEventObjectArray(Object[] event, String eventTypeName);

    /**
     * Route the event object back to the runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param event         to route internally for processing by the runtime
     * @param eventTypeName event type name
     */
    void routeEventBean(Object event, String eventTypeName);

    /**
     * Route the event object back to the runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param event         to route internally for processing by the runtime
     * @param eventTypeName event type name
     */
    void routeEventMap(Map<String, Object> event, String eventTypeName);

    /**
     * Route the event object back to the runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param event         to route internally for processing by the runtime
     * @param eventTypeName event type name
     */
    void routeEventXMLDOM(Node event, String eventTypeName);

    /**
     * Route the event object back to the runtime for internal dispatching,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     * The route event is processed just like it was sent to the runtime, that is any
     * active expressions seeking that event receive it. The routed event has priority over other
     * events sent to the runtime. In a single-threaded application the routed event is
     * processed before the next event is sent to the runtime.
     * <p>
     * Note: when outbound-threading is enabled, the thread delivering to listeners
     * is not the thread processing the original event. Therefore with outbound-threading
     * enabled the sendEvent method should be used by listeners instead.
     * </p>
     *
     * @param avroGenericDataDotRecord to route internally for processing by the runtime
     * @param eventTypeName            event type name
     */
    void routeEventAvro(Object avroGenericDataDotRecord, String eventTypeName);
}
