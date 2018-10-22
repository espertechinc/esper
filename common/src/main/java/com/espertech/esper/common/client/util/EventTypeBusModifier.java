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
package com.espertech.esper.common.client.util;

/**
 * Modifier that dictates whether an event type allows or does not allow sending events in using one of the send-event
 * methods.
 */
public enum EventTypeBusModifier {
    /**
     * Allow sending in events of this type using the send-event API on event service.
     */
    BUS,

    /**
     * Disallow sending in events of this type using the send-event API on event service.
     */
    NONBUS;
}
