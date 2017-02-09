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

public class FindMissingEventListener implements UpdateListener {
    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        if (oldEvents == null) {
            // we don't care about events entering the window (new events)
            // this is because we must wait for C to arri
            return;
        }

        // Missing C events can be reported either through A or through B
        // We assume that duplicates are ok, if not, then streams A and B could be joined and then fed,
        // or duplicates could be removed via another statement as well.
        TxnEventA eventA = (TxnEventA) oldEvents[0].get("A");
        TxnEventB eventB = (TxnEventB) oldEvents[0].get("B");

        if (eventA != null) {
            log.debug("Missing TxnEventC event detected for TxnEventA " + eventA.toString());
        } else {
            log.debug("Missing TxnEventC event detected for TxnEventB " + eventB.toString());
        }
    }

    private static final Logger log = LoggerFactory.getLogger(FindMissingEventListener.class);
}
