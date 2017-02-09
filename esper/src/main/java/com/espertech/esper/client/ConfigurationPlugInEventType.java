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

import java.io.Serializable;
import java.net.URI;

/**
 * Configuration for a plug-in event type, which is an event type resolved via plug-in event representation.
 */
public class ConfigurationPlugInEventType implements Serializable {
    private URI[] eventRepresentationResolutionURIs;
    private Serializable initializer;
    private static final long serialVersionUID = -3595742117710810293L;

    /**
     * Sets the URIs to use to resolve the new event type against the plug-in event representations
     * registered.
     *
     * @param eventRepresentationResolutionURIs URIs to use for resolution
     */
    public void setEventRepresentationResolutionURIs(URI[] eventRepresentationResolutionURIs) {
        this.eventRepresentationResolutionURIs = eventRepresentationResolutionURIs;
    }

    /**
     * Sets the optional initialization information that the plug-in event representation
     * may use to set up the event type.
     *
     * @param initializer is an object carrying configuration info, or a String XML if coming
     *                    from an XML file
     */
    public void setInitializer(Serializable initializer) {
        this.initializer = initializer;
    }

    /**
     * Returns the URIs to use to resolve the new event type against the plug-in event representations
     * registered.
     *
     * @return URIs to use for resolution
     */
    public URI[] getEventRepresentationResolutionURIs() {
        return eventRepresentationResolutionURIs;
    }

    /**
     * Returns optional initialization information that the plug-in event representation
     * may use to set up the event type.
     *
     * @return is an object carrying configuration info, or a String XML if coming from an XML file
     */
    public Serializable getInitializer() {
        return initializer;
    }
}
