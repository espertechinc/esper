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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecClientThreadedConfigInboundFastShutdown implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setThreadPoolInbound(true);
        configuration.getEngineDefaults().getThreading().setThreadPoolInboundNumThreads(2);
        configuration.addEventType(MyEvent.class);
        configuration.addPlugInSingleRowFunction("sleepaLittle", this.getClass().getName(), "sleepaLittle");
    }

    public void run(EPServiceProvider epService) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select sleepaLittle(100) from MyEvent");
        stmt.setSubscriber(new MySubscriber());
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new MyEvent());
        }
    }

    public static void sleepaLittle(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    public static class MySubscriber {
        public void update(Object[] args) {
        }
    }

    public static class MyEvent {
    }
}
