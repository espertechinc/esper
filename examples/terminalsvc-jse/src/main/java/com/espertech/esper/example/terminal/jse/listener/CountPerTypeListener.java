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
package com.espertech.esper.example.terminal.jse.listener;

import com.espertech.esper.client.EventBean;

import java.util.Formatter;

public class CountPerTypeListener extends BaseTerminalListener {

    public CountPerTypeListener(ComplexEventListener complexEventListener) {
        super(complexEventListener);
    }

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        Formatter outFormatter = new Formatter(new StringBuffer());
        outFormatter.format("%10s | %10s\n", "type", "count");
        outFormatter.format("---------- | ----------\n");

        int total = 0;
        for (int i = 0; i < newEvents.length; i++) {
            String type = (String) newEvents[i].get("type");
            long count = (Long) newEvents[i].get("countPerType");
            outFormatter.format("%10s | %10s\n", type, count);
            total += count;
        }
        outFormatter.format("%10s | %10s\n", "total =", total);

        complexEventListener.onComplexEvent("Current count per type:\n" + outFormatter.out().toString());
    }
}
