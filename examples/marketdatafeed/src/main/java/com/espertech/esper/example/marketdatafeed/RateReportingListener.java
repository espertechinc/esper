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
package com.espertech.esper.example.marketdatafeed;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RateReportingListener implements UpdateListener {
    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        if (newEvents.length > 0) {
            logRate(newEvents[0]);
        }
        if (newEvents.length > 1) {
            logRate(newEvents[1]);
        }
    }

    private void logRate(EventBean theEvent) {
        log.info("Current rate for feed " + theEvent.get("feed").toString() +
                " is " + theEvent.get("cnt"));
    }

    private static final Logger log = LoggerFactory.getLogger(RateReportingListener.class);
}
