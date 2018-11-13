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
package com.espertech.esper.regressionlib.support.multithread;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.util.SupportMTUpdateListener;
import com.espertech.esper.runtime.client.*;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtListenerRouteCallable implements Callable {
    private final int numThread;
    private final RegressionEnvironment env;
    private final EPStatement statement;
    private final int numRepeats;

    public StmtListenerRouteCallable(int numThread, RegressionEnvironment env, EPStatement statement, int numRepeats) {
        this.numThread = numThread;
        this.env = env;
        this.numRepeats = numRepeats;
        this.statement = statement;
    }

    public Object call() throws Exception {
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                MyUpdateListener listener = new MyUpdateListener(env, numThread, loop);
                statement.addListener(listener);
                env.sendEventBean(new SupportBean(), "SupportBean");
                statement.removeListener(listener);
                listener.assertCalled();

                if (listener.lastException != null) {
                    throw new RuntimeException("Listener exception: " + listener.lastException.getMessage(), listener.lastException);
                }
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
        private final RegressionEnvironment env;
        private final int numThread;
        private boolean isCalled;
        private EPCompiled compiled;
        private Throwable lastException;

        public MyUpdateListener(RegressionEnvironment env, int numThread, int numRepeat) {
            this.env = env;
            this.numThread = numThread;
            compiled = env.compile("select * from SupportMarketDataBean where volume=" + numThread);
        }

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            isCalled = true;

            try {

                // create statement for thread - this can be called multiple times as other threads send SupportBean
                EPDeployment deployment;
                try {
                    deployment = env.runtime().getDeploymentService().deploy(compiled);
                } catch (EPDeployException e) {
                    throw new RuntimeException(e);
                }

                SupportMTUpdateListener listener = new SupportMTUpdateListener();
                deployment.getStatements()[0].addListener(listener);

                Object theEvent = new SupportMarketDataBean("", 0, (long) numThread, null);
                env.sendEventBean(theEvent, theEvent.getClass().getSimpleName());
                env.runtime().getDeploymentService().undeploy(deployment.getDeploymentId());

                EventBean[] eventsReceived = listener.getNewDataListFlattened();

                boolean found = false;
                for (int i = 0; i < eventsReceived.length; i++) {
                    if (eventsReceived[i].getUnderlying() == theEvent) {
                        found = true;
                    }
                }
                Assert.assertTrue(found);
            } catch (Throwable t) {
                lastException = t;
            }
        }

        public void assertCalled() {
            Assert.assertTrue(isCalled);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(StmtListenerRouteCallable.class);
}
