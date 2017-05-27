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
package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecMTContextStartedBySameEvent implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(true);
        configuration.addEventType(PayloadEvent.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String eplStatement = "create context MyContext start PayloadEvent end after 0.5 seconds";
        epService.getEPAdministrator().createEPL(eplStatement);

        String aggStatement = "@name('select') context MyContext " +
                "select count(*) as theCount " +
                "from PayloadEvent " +
                "output snapshot when terminated";
        EPStatement epAggStatement = epService.getEPAdministrator().createEPL(aggStatement);
        MyListener listener = new MyListener();
        epAggStatement.addListener(listener);

        // start thread
        long numEvents = 10000000;
        MyRunnable myRunnable = new MyRunnable(epService, numEvents);
        Thread thread = new Thread(myRunnable);
        thread.start();
        thread.join();

        Thread.sleep(1000);

        // assert
        assertNull(myRunnable.exception);
        assertEquals(numEvents, listener.total);
    }

    public static class PayloadEvent {
    }

    public static class MyRunnable implements Runnable {
        private final EPServiceProvider engine;
        private final long numEvents;

        private Exception exception;

        public MyRunnable(EPServiceProvider engine, long numEvents) {
            this.engine = engine;
            this.numEvents = numEvents;
        }

        public void run() {
            try {
                for (int i = 0; i < numEvents; i++) {
                    PayloadEvent payloadEvent = new PayloadEvent();
                    engine.getEPRuntime().sendEvent(payloadEvent);
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

        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
            long theCount = (Long) newEvents[0].get("theCount");
            total += theCount;
            System.out.println("count " + theCount + " total " + total);
        }
    }
}