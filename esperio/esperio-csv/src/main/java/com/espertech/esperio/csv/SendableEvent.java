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
package com.espertech.esperio.csv;

/**
 * A wrapper that packages an event up so that it can be
 * sent into the caller-specified runtime. It also provides
 * the scheduling information for this event (send time and
 * schedule slot), so the user can send this event on schedule.
 */
public interface SendableEvent {
    /**
     * Send the event into the runtime.
     *
     * @param sender - the sender to send an event
     */
    public void send(AbstractSender sender);

    /**
     * Get the send time of this event, relative to all the other events sent or read by the same entity
     *
     * @return timestamp
     */
    public long getSendTime();

    /**
     * Get the schedule slot for the entity that created this event
     *
     * @return schedule slot
     */
    public long getScheduleSlot();
}
