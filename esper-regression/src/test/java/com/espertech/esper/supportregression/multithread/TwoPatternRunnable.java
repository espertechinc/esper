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
package com.espertech.esper.supportregression.multithread;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportTradeEvent;
import com.espertech.esper.supportregression.util.SupportStmtAwareUpdateListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class TwoPatternRunnable implements Runnable {
    private final EPServiceProvider engine;
    private final SupportStmtAwareUpdateListener listener;
    private boolean isShutdown;

    public TwoPatternRunnable(EPServiceProvider engine) {
        this.engine = engine;
        listener = new SupportStmtAwareUpdateListener();
    }

    public void setShutdown(boolean shutdown) {
        isShutdown = shutdown;
    }

    public void run() {
        String stmtText = "every event1=SupportEvent(userId in ('100','101'),amount>=1000)";
        EPStatement statement = engine.getEPAdministrator().createPattern(stmtText);
        statement.addListener(listener);

        while (!isShutdown) {
            List<SupportTradeEvent> matches = new ArrayList<SupportTradeEvent>();

            for (int i = 0; i < 10000; i++) {
                SupportTradeEvent bean;
                if (i % 1000 == 1) {
                    bean = new SupportTradeEvent(i, "100", 1001);
                    matches.add(bean);
                } else {
                    bean = new SupportTradeEvent(i, "101", 10);
                }
                engine.getEPRuntime().sendEvent(bean);

                if (isShutdown) {
                    break;
                }
            }

            // check results
            EventBean[] received = listener.getNewDataListFlattened();
            Assert.assertEquals(matches.size(), received.length);
            for (int i = 0; i < received.length; i++) {
                Assert.assertSame(matches.get(i), received[i].get("event1"));
            }

            // System.out.println("Found " + received.length + " matches in loop #" + countLoops);
            listener.reset();
        }
    }
}
