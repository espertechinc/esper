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

import java.io.Serializable;
import java.net.URI;

/**
 * Context for use in {@link PlugInEventRepresentation} to provide information to help decide
 * whether an event representation can handle the requested event type.
 */
public class PlugInEventTypeHandlerContext {
    private final URI eventTypeResolutionURI;
    private final Serializable typeInitializer;
    private final String eventTypeName;
    private final int eventTypeId;

    /**
     * Ctor.
     *
     * @param eventTypeResolutionURI the URI specified for resolving the event type, may be a child URI
     *                               of the event representation URI and may carry additional parameters
     * @param typeInitializer        optional configuration for the type, or null if none supplied
     * @param eventTypeName          the name of the event
     * @param eventTypeId            event type id
     */
    public PlugInEventTypeHandlerContext(URI eventTypeResolutionURI, Serializable typeInitializer, String eventTypeName, int eventTypeId) {
        this.eventTypeResolutionURI = eventTypeResolutionURI;
        this.typeInitializer = typeInitializer;
        this.eventTypeName = eventTypeName;
        this.eventTypeId = eventTypeId;
    }

    /**
     * Returns the event type id.
     *
     * @return event type id
     */
    public int getEventTypeId() {
        return eventTypeId;
    }

    /**
     * Returns the URI specified for resolving the event type, may be a child URI
     * of the event representation URI and may carry additional parameters
     *
     * @return URI
     */
    public URI getEventTypeResolutionURI() {
        return eventTypeResolutionURI;
    }

    /**
     * Returns optional configuration for the type, or null if none supplied. An String XML document if
     * the configuration was read from an XML file.
     *
     * @return configuration, or null if none supplied
     */
    public Serializable getTypeInitializer() {
        return typeInitializer;
    }

    /**
     * Returns the name assigned to the event type.
     *
     * @return name
     */
    public String getEventTypeName() {
        return eventTypeName;
    }
}
