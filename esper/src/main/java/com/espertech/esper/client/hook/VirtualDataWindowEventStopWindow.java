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
package com.espertech.esper.client.hook;

/**
 * This event is raised when a virtual data window is stopped.
 */
public class VirtualDataWindowEventStopWindow extends VirtualDataWindowEvent {

    private final String namedWindowName;

    /**
     * Ctor.
     *
     * @param namedWindowName named window name
     */
    public VirtualDataWindowEventStopWindow(String namedWindowName) {
        this.namedWindowName = namedWindowName;
    }

    /**
     * Returns the named window name.
     *
     * @return named window name
     */
    public String getNamedWindowName() {
        return namedWindowName;
    }
}
