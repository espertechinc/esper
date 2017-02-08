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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.event.SendableEvent;

public class SendableEventBean implements SendableEvent {
    private final Object event;

    public SendableEventBean(Object event) {
        this.event = event;
    }

    public void send(EPRuntime runtime) {
        runtime.sendEvent(event);
    }

    public Object getUnderlying() {
        return event;
    }
}
