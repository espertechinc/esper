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
package com.espertech.esper.regressionlib.suite.multithread;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MultithreadContextStartedBySameEvent implements RegressionExecutionWithConfigure {

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void configure(Configuration configuration) {
        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
        configuration.getCommon().addEventType(PayloadEvent.class);
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        String eplStatement = "create context MyContext start PayloadEvent end after 0.5 seconds";
        env.compileDeploy(eplStatement, path);

        String aggStatement = "@name('select') context MyContext " +
            "select count(*) as theCount " +
            "from PayloadEvent " +
            "output snapshot when terminated";
        env.compileDeploy(aggStatement, path);
        MyListener listener = new MyListener();
        env.statement("select").addListener(listener);

        // start thread
        long numEvents = 10000000;
        MyRunnable myRunnable = new MyRunnable(env.runtime(), numEvents);
        Thread thread = new Thread(myRunnable, MultithreadContextStartedBySameEvent.class.getSimpleName());
        thread.start();
        SupportCompileDeployUtil.threadJoin(thread);

        SupportCompileDeployUtil.threadSleep(1000);

        // assert
        assertNull(myRunnable.exception);
        assertEquals(numEvents, listener.total);

        env.undeployAll();
    }

    public static class PayloadEvent {
    }

    public static class MyRunnable implements Runnable {
        private final EPRuntime runtime;
        private final long numEvents;

        private Exception exception;

        public MyRunnable(EPRuntime runtime, long numEvents) {
            this.runtime = runtime;
            this.numEvents = numEvents;
        }

        public void run() {
            try {
                for (int i = 0; i < numEvents; i++) {
                    PayloadEvent payloadEvent = new PayloadEvent();
                    runtime.getEventService().sendEventBean(payloadEvent, "PayloadEvent");
                    if (i > 0 && i % 1000000 == 0) {
                        System.out.println("sent " + i + " events");
                    }
                }
                System.out.println("sent " + numEvents + " events");
            } catch (Exception ex) {
                ex.printStackTrace();
                this.exception = ex;
            }
        }
    }

    public static class MyListener implements UpdateListener {
        private long total;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            long theCount = (Long) newEvents[0].get("theCount");
            total += theCount;
            System.out.println("count " + theCount + " total " + total);
        }
    }
}