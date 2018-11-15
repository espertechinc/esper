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
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.runtime.client.*;
import junit.framework.TestCase;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.threadJoin;
import static org.junit.Assert.*;

/**
 * Test for multithread-safety for atomic module deployment.
 */
public class MultithreadDeployAtomic implements RegressionExecution {
    private final static int NUM_STMTS = 100;

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        MySendRunnable runnable = new MySendRunnable(env.runtime());
        Thread thread = new Thread(runnable, MultithreadDeployAtomic.class.getSimpleName());
        thread.start();

        MyKeepFirstPerStmtListener listener = new MyKeepFirstPerStmtListener();
        env.deployment().addDeploymentStateListener(new DeploymentStateListener() {
            public void onDeployment(DeploymentStateEventDeployed event) {
                for (EPStatement stmt : event.getStatements()) {
                    stmt.addListener(listener);
                }
            }

            public void onUndeployment(DeploymentStateEventUndeployed event) {
            }
        });

        // deploy
        StringWriter buf = new StringWriter();
        for (int i = 0; i < NUM_STMTS; i++) {
            buf.append("select * from SupportBean;");
        }
        EPDeployment deploymentResult = SupportCompileDeployUtil.compileDeploy(buf.toString(), env.runtime(), env.getConfiguration());

        // wait for some deliveries
        SupportCompileDeployUtil.threadSleep(1000);

        // undeploy
        env.undeploy(deploymentResult.getDeploymentId());

        // cooldown
        SupportCompileDeployUtil.threadSleep(500);

        runnable.setShutdown(true);
        threadJoin(thread);

        assertNull(runnable.getThrowable());
        assertEquals(NUM_STMTS, listener.getFirstLastPerStmt().size());

        // all first events should be the same
        UniformPair<EventBean> reference = listener.firstLastPerStmt.values().iterator().next();
        TestCase.assertNotNull(reference.getFirst());
        TestCase.assertNotNull(reference.getSecond());
        assertNotSame(reference.getFirst(), reference.getSecond());
        for (UniformPair<EventBean> other : listener.firstLastPerStmt.values()) {
            assertSame("last event not the same", reference.getSecond(), other.getSecond());
        }
        for (UniformPair<EventBean> other : listener.firstLastPerStmt.values()) {
            assertSame("first event not the same", reference.getFirst(), other.getFirst());
        }

        env.deployment().removeAllDeploymentStateListeners();
    }

    private final static class MyKeepFirstPerStmtListener implements UpdateListener {
        private final Map<EPStatement, UniformPair<EventBean>> firstLastPerStmt = new HashMap<>();

        public synchronized void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
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
        private final EPRuntime runtime;
        private int current;
        private boolean shutdown;
        private Throwable throwable;

        public MySendRunnable(EPRuntime runtime) {
            this.runtime = runtime;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            try {
                while (!shutdown) {
                    runtime.getEventService().sendEventBean(new SupportBean(null, current++), "SupportBean");
                    Thread.sleep(50);
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
