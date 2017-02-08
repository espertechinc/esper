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
package com.espertech.esper.example.terminal.jse.event;

/**
 * The parent class of all events
 * <p/>
 * We have added a convenient getType() method to get the short class name of the actual event type
 * and a timestamp property
 */
public abstract class BaseTerminalEvent {

    private final Terminal terminal;

    private long timestamp;

    public BaseTerminalEvent(Terminal terminal) {
        this.terminal = terminal;
        timestamp = System.currentTimeMillis();
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return this.getClass().getSimpleName();
    }

}
