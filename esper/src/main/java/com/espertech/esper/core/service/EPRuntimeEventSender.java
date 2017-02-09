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

import com.espertech.esper.client.EventBean;

/**
 * For use by {@link com.espertech.esper.client.EventSender} for direct feed of wrapped events for processing.
 */
public interface EPRuntimeEventSender {
    /**
     * Equivalent to the sendEvent method of EPRuntime, for use to process an known event.
     *
     * @param eventBean is the event object wrapped by an event bean providing the event metadata
     */
    public void processWrappedEvent(EventBean eventBean);

    /**
     * For processing a routed event.
     *
     * @param theEvent routed event
     */
    public void routeEventBean(EventBean theEvent);
}
