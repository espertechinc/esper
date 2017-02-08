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
package com.espertech.esper.event.arr;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.event.SendableEvent;

public class SendableEventObjectArray implements SendableEvent {
    private final Object[] event;
    private final String typeName;

    public SendableEventObjectArray(Object[] event, String typeName) {
        this.event = event;
        this.typeName = typeName;
    }

    public void send(EPRuntime runtime) {
        runtime.sendEvent(event, typeName);
    }

    public Object getUnderlying() {
        return event;
    }
}
