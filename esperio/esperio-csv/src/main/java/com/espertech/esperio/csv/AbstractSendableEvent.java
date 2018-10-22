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
 * Base class for sendable event, provides timestamp and schedule slot.
 */
public abstract class AbstractSendableEvent implements SendableEvent {

    private final long timestamp;
    private final long scheduleSlot;

    /**
     * Ctor.
     *
     * @param timestamp    to send
     * @param scheduleSlot the schedule slot assigned by scheduling service
     */
    public AbstractSendableEvent(long timestamp, long scheduleSlot) {
        if (scheduleSlot == -1) {
            throw new NullPointerException("ScheduleSlot cannot be undefined");
        }

        this.timestamp = timestamp;
        this.scheduleSlot = scheduleSlot;
    }

    public abstract void send(AbstractSender sender);

    /* (non-Javadoc)
     * @see com.espertech.esperio.csv.SendableEvent#getScheduleSlot()
     */
    public long getScheduleSlot() {
        return scheduleSlot;
    }

    /* (non-Javadoc)
     * @see com.espertech.esperio.csv.SendableEvent#getSendTime()
     */
    public long getSendTime() {
        return timestamp;
    }
}
