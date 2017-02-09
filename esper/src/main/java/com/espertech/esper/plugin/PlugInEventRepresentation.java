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
package com.espertech.esper.plugin;

/**
 * Plug-in event representation that can dynamically create event types and event instances based on
 * information available elsewhere.
 * <p>
 * A plug-in event representation can be useful when your application has existing Java classes that carry
 * event metadata and event property values and your application does not want to (or cannot) extract or transform
 * such event metadata and event data into one of the built-in event representations (POJO Java objects, Map or XML DOM).
 * <p>
 * Further use of a plug-in event representation is to provide a faster or short-cut access path to event data.
 * For example, the access to XML event data through a StAX Streaming API for XML (StAX) is known to be very efficient.
 * <p>
 * Further, a plug-in event representation can provide network lookup and general abstraction of event typing and
 * event sourcing.
 * <p>
 * Before use, an implementation of this interface must be registered via configuration. Upon engine initialization,
 * the engine invokes the {@link #init} method passing configuration information.
 * <p>
 * When a plug-in event type name is registered via configuration (runtime or configuration time), the
 * engine first asks the implementation whether the type is accepted via {@link #acceptsType}.
 * If accepted, the engine follows with a call to {@link #getTypeHandler} for creating and handling the type.
 * <p>
 * An implementation can participate in dynamic resolution of new (unseen)
 * event type names if the application configures the URI of the event representation, or a child URI (parameters possible) via
 * {@link com.espertech.esper.client.ConfigurationOperations#setPlugInEventTypeResolutionURIs(java.net.URI[])}.
 * <p>
 * Last, see {@link com.espertech.esper.client.EPRuntime#getEventSender(java.net.URI[])}. An event sender
 * allows dynamic reflection on an incoming event object. At the time such an event
 * sender is obtained and a matching URI specified, the {@link #acceptsEventBeanResolution} method
 * indicates that the event representation can or cannot inspect events, and the {@link PlugInEventBeanFactory}
 * returned is used by the event sender to wrap event objects for processing.
 */
public interface PlugInEventRepresentation {
    /**
     * Initializes the event representation.
     *
     * @param eventRepresentationContext URI and optional configuration information
     */
    public void init(PlugInEventRepresentationContext eventRepresentationContext);

    /**
     * Returns true to indicate that the event representation can handle the requested event type.
     * <p>
     * Called when a new plug-in event type and name is registered and the its resolution URI matches
     * or is a child URI of the event representation URI.
     * <p>
     * Also called when a new EPL statement is created with an unseen event type name
     * and the URIs for resolution have been configured.
     *
     * @param acceptTypeContext provides the URI specified for resolving the type, and configuration info.
     * @return true to accept the type, false such that another event representation may handle the type request
     */
    public boolean acceptsType(PlugInEventTypeHandlerContext acceptTypeContext);

    /**
     * Returns the event type handler that provides the event type and, upon request, event sender, for this type.
     *
     * @param eventTypeContext provides the URI specified for resolving the type, and configuration info.
     * @return provides event type and event sender
     */
    public PlugInEventTypeHandler getTypeHandler(PlugInEventTypeHandlerContext eventTypeContext);

    /**
     * For use with {@link com.espertech.esper.client.EPRuntime#getEventSender(java.net.URI[])},
     * returns true if the event representation intends to provide event wrappers for event objects passed in.
     *
     * @param acceptBeanContext provides the URI specified for resolving the event object reflection
     * @return true to accept the requested URI, false such that another event representation may handle the request
     */
    public boolean acceptsEventBeanResolution(PlugInEventBeanReflectorContext acceptBeanContext);

    /**
     * For use with {@link com.espertech.esper.client.EPRuntime#getEventSender(java.net.URI[])},
     * returns the factory that can inspect event objects and provide an event {@link com.espertech.esper.client.EventBean}
     * wrapper.
     *
     * @param eventBeanContext provides the URI specified for resolving the event object reflection
     * @return true to accept the requested URI, false such that another event representation may handle the request
     */
    public PlugInEventBeanFactory getEventBeanFactory(PlugInEventBeanReflectorContext eventBeanContext);
}
