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
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import junit.framework.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Callable;

public class StmtListenerCreateStmtCallable implements Callable {
    private final int numThread;
    private final EPServiceProvider engine;
    private final EPStatement statement;
    private final int numRoutes;
    private final Set<SupportMarketDataBean> routed;

    public StmtListenerCreateStmtCallable(int numThread, EPServiceProvider engine, EPStatement statement, int numRoutes,
                                          Set<SupportMarketDataBean> routed) {
        this.numThread = numThread;
        this.engine = engine;
        this.numRoutes = numRoutes;
        this.statement = statement;
        this.routed = routed;
    }

    public Object call() throws Exception {
        try {
            // add listener to triggering statement
            MyUpdateListener listener = new MyUpdateListener(engine, numRoutes, routed);
            statement.addListener(listener);
            Thread.sleep(100);      // wait to send trigger event, other threads receive all other's events

            engine.getEPRuntime().sendEvent(new SupportBean());

        } catch (AssertionFailedError ex) {
            log.error("Assertion error in thread " + Thread.currentThread().getId(), ex);
            return false;
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private class MyUpdateListener implements UpdateListener {
        private final EPServiceProvider engine;
        private final int numRepeats;
        private final Set<SupportMarketDataBean> routed;

        public MyUpdateListener(EPServiceProvider engine, int numRepeats, Set<SupportMarketDataBean> routed) {
            this.engine = engine;
            this.numRepeats = numRepeats;
            this.routed = routed;
        }

        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
            for (int i = 0; i < numRepeats; i++) {
                SupportMarketDataBean theEvent = new SupportMarketDataBean("", 0, (long) numThread, null);
                engine.getEPRuntime().route(theEvent);
                routed.add(theEvent);
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(StmtListenerCreateStmtCallable.class);
}
