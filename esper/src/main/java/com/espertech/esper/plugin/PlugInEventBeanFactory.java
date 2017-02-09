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

import com.espertech.esper.client.EventBean;

import java.net.URI;

/**
 * Factory for creating {@link EventBean} event object wrapper for a plug-in event representation.
 * <p>
 * Implementations typically reflect on the event object to be processed and decides on the proper {@link com.espertech.esper.client.EventType}
 * to assign. If the implementation finds that it cannot handle the event object, it should return null.
 * Returning null gives another instance of this class as specified by the list of URI to handle the event object.
 */
public interface PlugInEventBeanFactory {
    /**
     * Returns an event wrapper for the event object specific to the plug-in event representation
     * or using one of the built-in types, or null if the event object is unknown and cannot be handled.
     *
     * @param theEvent      is the event object to reflect upon and wrap
     * @param resolutionURI is the URI used originally for obtaining the event sender
     * @return wrapped event object, or null if the event is of unknown type or content
     */
    public EventBean create(Object theEvent, URI resolutionURI);
}
