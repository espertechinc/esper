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

import com.espertech.esper.common.client.EventType;

/**
 * Service for managing event types.
 */
public interface EPEventTypeService {
    /**
     * Returns the event type for a preconfigured event type.
     *
     * @param eventTypeName event type name of a preconfigured event type
     * @return event type or null if not found
     */
    EventType getEventTypePreconfigured(String eventTypeName);

    /**
     * Returns the event type as defined by a given deployment.
     *
     * @param deploymentId  deployment id of the deployment
     * @param eventTypeName event type name of a preconfigured event type
     * @return event type or null if not found
     */
    EventType getEventType(String deploymentId, String eventTypeName);
}
