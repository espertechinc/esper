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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EventBean;

import java.util.Map;

/**
 * SPI interface of the runtime exposes fire-and-forget, non-continuous query functionality.
 */
public interface EPRuntimeSPI extends EPRuntime {
    /**
     * Returns all declared variable names and their types.
     *
     * @return variable names and types
     */
    public Map<String, Class> getVariableTypeAll();

    /**
     * Returns a variable's type.
     *
     * @param variableName type or null if the variable is not declared
     * @return type of variable
     */
    public Class getVariableType(String variableName);

    /**
     * Number of events routed internally.
     *
     * @return event count routed internally
     */
    public long getRoutedInternal();

    /**
     * Number of events routed externally.
     *
     * @return event count routed externally
     */
    public long getRoutedExternal();

    public void destroy();

    public Map<String, Long> getStatementNearestSchedules();

    /**
     * Send an event represented by a plain Java object to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code,
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     *
     * @param object is the event to sent to the runtime
     * @return returns object wrapped as event bean
     * @throws com.espertech.esper.client.EPException is thrown when the processing of the event lead to an error
     */
    public EventBean wrapEvent(Object object);

    /**
     * Send a map containing event property values to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     *
     * @param map           - map that contains event property values. Keys are expected to be of type String while values
     *                      can be of any type. Keys and values should match those declared via Configuration for the given eventTypeName.
     * @param eventTypeName - the name for the Map event type that was previously configured
     * @return returns map wrapped as event bean
     * @throws EPException - when the processing of the event leads to an error
     */
    public EventBean wrapEvent(Map map, String eventTypeName);

    /**
     * Send an event represented by a DOM node to the event stream processing runtime.
     * <p>
     * Use the route method for sending events into the runtime from within UpdateListener code.
     * to avoid the possibility of a stack overflow due to nested calls to sendEvent.
     *
     * @param node is the DOM node as an event
     * @return returns node wrapped as event bean
     * @throws EPException is thrown when the processing of the event lead to an error
     */
    public EventBean wrapEvent(org.w3c.dom.Node node);

    public void processThreadWorkQueue();

    public void dispatch();

    public void initialize();

    public void processWrappedEvent(EventBean eventBean);

    public String getEngineURI();

    /**
     * Clear short-lived memory that may temporarily retain references to stopped or destroyed statements.
     * <p>
     * Use this method after stopping and destroying statements for the purpose
     * of clearing thread-local or other short lived storage to statement handles of deleted statements.
     * </p>
     * <p>
     * NOT safe to use without first acquiring the engine lock.
     * </p>
     */
    public void clearCaches();
}
