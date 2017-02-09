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
package com.espertech.esper.client.time;

/**
 * Event for controlling clocking, i.e. to enable and disable external clocking.
 */
public final class TimerControlEvent extends TimerEvent {
    private static final long serialVersionUID = -5204351618041414666L;

    /**
     * Constants controlling the clocking.
     */
    public enum ClockType {
        /**
         * For external clocking.
         */
        CLOCK_EXTERNAL,
        /**
         * For internal clocking.
         */
        CLOCK_INTERNAL
    }

    private final ClockType clockType;

    /**
     * Constructor takes a clocking type as parameter.
     *
     * @param clockType for internal or external clocking
     */
    public TimerControlEvent(final ClockType clockType) {
        this.clockType = clockType;
    }

    /**
     * Returns clocking type.
     *
     * @return clocking type
     */
    public ClockType getClockType() {
        return clockType;
    }
}
