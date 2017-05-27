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
package com.espertech.esper.supportregression.plugineventrep;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventSender;
import com.espertech.esper.core.service.EPRuntimeEventSender;

import java.util.Properties;

public class MyPlugInPropertiesEventSender implements EventSender {
    private final MyPlugInPropertiesEventType type;
    private final EPRuntimeEventSender runtimeSender;

    public MyPlugInPropertiesEventSender(MyPlugInPropertiesEventType type, EPRuntimeEventSender runtimeSender) {
        this.type = type;
        this.runtimeSender = runtimeSender;
    }

    public void sendEvent(Object theEvent) {
        if (!(theEvent instanceof Properties)) {
            throw new EPException("Sender expects a properties event");
        }
        EventBean eventBean = new MyPlugInPropertiesEventBean(type, (Properties) theEvent);
        runtimeSender.processWrappedEvent(eventBean);
    }

    public void route(Object theEvent) {
        if (!(theEvent instanceof Properties)) {
            throw new EPException("Sender expects a properties event");
        }
        EventBean eventBean = new MyPlugInPropertiesEventBean(type, (Properties) theEvent);
        runtimeSender.routeEventBean(eventBean);
    }
}
