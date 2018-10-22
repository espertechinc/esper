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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;

public class ClientRuntimeThreadedConfigInboundFastShutdown implements RegressionExecutionWithConfigure {
    public void configure(Configuration configuration) {
        configuration.getRuntime().getThreading().setThreadPoolInbound(true);
        configuration.getRuntime().getThreading().setThreadPoolInboundNumThreads(2);
        configuration.getCommon().addEventType(MyEvent.class);
        configuration.getCompiler().addPlugInSingleRowFunction("sleepaLittle", this.getClass().getName(), "sleepaLittle");
        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
    }

    public void run(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') select sleepaLittle(100) from MyEvent");
        env.statement("s0").setSubscriber(new MySubscriber());
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new MyEvent());
        }
        env.undeployAll();
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
