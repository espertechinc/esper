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
package com.espertech.esper.example.terminal.mdb;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class CountPerTypeListener implements UpdateListener {
    private OutboundSender outboundSender;

    public CountPerTypeListener(OutboundSender outboundSender) {
        this.outboundSender = outboundSender;
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < newEvents.length; i++) {
            String type = (String) newEvents[i].get("type");
            long count = (Long) newEvents[i].get("countPerType");

            buffer.append("Type ");
            buffer.append(type);
            buffer.append(" counts ");
            buffer.append(count);
            buffer.append("\n");
        }

        outboundSender.send("Current count per type: " + buffer.toString());
    }
}
