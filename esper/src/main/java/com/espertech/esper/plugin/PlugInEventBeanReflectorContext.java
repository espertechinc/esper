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

import java.net.URI;

/**
 * Context for use in {@link PlugInEventRepresentation} to provide information to help decide
 * whether an event representation can handle the requested resolution URI for creating event object wrappers.
 */
public class PlugInEventBeanReflectorContext {
    private final URI resolutionURI;

    /**
     * Ctor.
     *
     * @param uri is the resolution URI provided as part of {@link com.espertech.esper.client.EPRuntime#getEventSender(java.net.URI[])}
     */
    public PlugInEventBeanReflectorContext(URI uri) {
        this.resolutionURI = uri;
    }

    /**
     * Returns the resolution URI.
     *
     * @return resolution URI
     */
    public URI getResolutionURI() {
        return resolutionURI;
    }
}
