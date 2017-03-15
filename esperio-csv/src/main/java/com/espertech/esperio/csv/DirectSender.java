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

import java.util.Map;

/**
 * Sender that sends without a threadpool.
 */
public class DirectSender extends AbstractSender {

    public void sendEvent(AbstractSendableEvent theEvent, Object beanToSend) {
        runtime.sendEvent(beanToSend);
    }

    public void sendEvent(AbstractSendableEvent theEvent, Map mapToSend, String eventTypeName) {
        runtime.sendEvent(mapToSend, eventTypeName);
    }

    public void onFinish() {
        // do nothing
    }
}
