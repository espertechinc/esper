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
package com.espertech.esper.example.transaction;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombinedEventListener implements UpdateListener {
    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        if (newEvents == null) {
            // we don't care about events leaving the window (old events)
            return;
        }

        EventBean theEvent = newEvents[0];
        log.debug("Combined event detected " +
                " transactionId=" + theEvent.get("transactionId") +
                " customerId=" + theEvent.get("customerId") +
                " supplierId=" + theEvent.get("supplierId") +
                " latencyAC=" + theEvent.get("latencyAC") +
                " latencyAB=" + theEvent.get("latencyAB") +
                " latencyBC=" + theEvent.get("latencyBC")
        );
    }

    private static final Logger log = LoggerFactory.getLogger(CombinedEventListener.class);
}
