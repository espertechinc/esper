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
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;

/**
 * Test for multithread-safety for atomic module deployment.
 */
public class ExecMTDeployAtomic implements RegressionExecution {
    private final static int NUM_STMTS = 100;

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        MySendRunnable runnable = new MySendRunnable(epService);
        Thread thread = new Thread(runnable);
        thread.start();

        MyKeepFirstPerStmtListener listener = new MyKeepFirstPerStmtListener();
        epService.addStatementStateListener(new EPStatementStateListener() {
            public void onStatementCreate(EPServiceProvider serviceProvider, EPStatement statement) {
                statement.addListener(listener);
            }

            public void onStatementStateChange(EPServiceProvider serviceProvider, EPStatement statement) {
            }
        });

        // deploy
        StringWriter buf = new StringWriter();
        for (int i = 0; i < NUM_STMTS; i++) {
            buf.append("select * from SupportBean;");
        }
        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(buf.toString());

        // wait for some deliveries
        Thread.sleep(1000);

        // undeploy
        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentResult.getDeploymentId());

        // cooldown
        Thread.sleep(500);

        runnable.setShutdown(true);
        thread.join();

        assertNull(runnable.getThrowable());
        assertEquals(NUM_STMTS, listener.getFirstLastPerStmt().size());

        // all first events should be the same
        UniformPair<EventBean> reference = listener.firstLastPerStmt.values().iterator().next();
        assertNotNull(reference.getFirst());
        assertNotNull(reference.getSecond());
        assertNotSame(reference.getFirst(), reference.getSecond());
        for (UniformPair<EventBean> other : listener.firstLastPerStmt.values()) {
            assertSame(reference.getFirst(), other.getFirst());
            assertSame(reference.getSecond(), other.getSecond());
        }

        epService.removeAllStatementStateListeners();
    }

    private final static class MyKeepFirstPerStmtListener implements StatementAwareUpdateListener {
        private final Map<EPStatement, UniformPair<EventBean>> firstLastPerStmt = new HashMap<>();

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPServiceProvider epServiceProvider) {
            UniformPair<EventBean> pair = firstLastPerStmt.get(statement);
            if (pair == null) {
                firstLastPerStmt.put(statement, new UniformPair<>(newEvents[0], null));
            } else {
                pair.setSecond(newEvents[0]);
            }
        }

        public Map<EPStatement, UniformPair<EventBean>> getFirstLastPerStmt() {
            return firstLastPerStmt;
        }
    }

    private final static class MySendRunnable implements Runnable {
        private final EPServiceProvider engine;
        private int current;
        private boolean shutdown;
        private Throwable throwable;

        public MySendRunnable(EPServiceProvider engine) {
            this.engine = engine;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            try {
                while (!shutdown) {
                    engine.getEPRuntime().sendEvent(new SupportBean(null, current++));
                }
            } catch (Throwable t) {
                throwable = t;
            }
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }
}
