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

import java.io.Serializable;

/**
 * Abstract base class marker for events that control time keeping by an event stream processor instance.
 */
public abstract class TimerEvent implements Serializable {
    private static final long serialVersionUID = 7042146786401152079L;
}
