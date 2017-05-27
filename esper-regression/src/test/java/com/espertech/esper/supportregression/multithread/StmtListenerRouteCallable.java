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
import com.espertech.esper.supportregression.util.SupportMTUpdateListener;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtListenerRouteCallable implements Callable {
    private final int numThread;
    private final EPServiceProvider engine;
    private final EPStatement statement;
    private final int numRepeats;

    public StmtListenerRouteCallable(int numThread, EPServiceProvider engine, EPStatement statement, int numRepeats) {
        this.numThread = numThread;
        this.engine = engine;
        this.numRepeats = numRepeats;
        this.statement = statement;
    }

    public Object call() throws Exception {
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                StmtListenerRouteCallable.MyUpdateListener listener = new StmtListenerRouteCallable.MyUpdateListener(engine, numThread);
                statement.addListener(listener);
                engine.getEPRuntime().sendEvent(new SupportBean());
                statement.removeListener(listener);
                listener.assertCalled();
            }
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
        private final int numThread;
        private boolean isCalled;

        public MyUpdateListener(EPServiceProvider engine, int numThread) {
            this.engine = engine;
            this.numThread = numThread;
        }

        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
            isCalled = true;

            // create statement for thread - this can be called multiple times as other threads send SupportBean
            EPStatement stmt = engine.getEPAdministrator().createEPL(
                    "select * from " + SupportMarketDataBean.class.getName() + " where volume=" + numThread);
            SupportMTUpdateListener listener = new SupportMTUpdateListener();
            stmt.addListener(listener);

            Object theEvent = new SupportMarketDataBean("", 0, (long) numThread, null);
            engine.getEPRuntime().sendEvent(theEvent);
            stmt.stop();

            EventBean[] eventsReceived = listener.getNewDataListFlattened();

            boolean found = false;
            for (int i = 0; i < eventsReceived.length; i++) {
                if (eventsReceived[i].getUnderlying() == theEvent) {
                    found = true;
                }
            }
            Assert.assertTrue(found);
        }

        public void assertCalled() {
            Assert.assertTrue(isCalled);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(StmtListenerRouteCallable.class);
}
