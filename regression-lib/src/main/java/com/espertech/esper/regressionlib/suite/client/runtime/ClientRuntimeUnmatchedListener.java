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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.UnmatchedListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ClientRuntimeUnmatchedListener {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeUnmatchedSendEvent());
        execs.add(new ClientRuntimeUnmatchedCreateStatement());
        execs.add(new ClientRuntimeUnmatchedInsertInto());
        return execs;
    }

    private static class ClientRuntimeUnmatchedSendEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("@name('s0') select * from SupportBean");
            MyUnmatchedListener listener = new MyUnmatchedListener();
            env.eventService().setUnmatchedListener(listener);

            // no statement, should be unmatched
            SupportBean theEvent = sendEvent(env, "E1");
            assertEquals(1, listener.getReceived().size());
            assertSame(theEvent, listener.getReceived().get(0).getUnderlying());
            listener.reset();

            // no unmatched listener
            env.eventService().setUnmatchedListener(null);
            sendEvent(env, "E1");
            assertEquals(0, listener.getReceived().size());

            // create statement and re-register unmatched listener
            env.deploy(compiled);
            env.eventService().setUnmatchedListener(listener);
            sendEvent(env, "E1");
            assertEquals(0, listener.getReceived().size());

            // stop statement
            env.undeployModuleContaining("s0");
            theEvent = sendEvent(env, "E1");
            assertEquals(1, listener.getReceived().size());
            assertSame(theEvent, listener.getReceived().get(0).getUnderlying());
            listener.reset();

            // start statement
            env.deploy(compiled);
            sendEvent(env, "E1");
            assertEquals(0, listener.getReceived().size());

            // destroy statement
            env.undeployModuleContaining("s0");
            theEvent = sendEvent(env, "E1");
            assertEquals(1, listener.getReceived().size());
            assertSame(theEvent, listener.getReceived().get(0).getUnderlying());
        }
    }

    private static class ClientRuntimeUnmatchedCreateStatement implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            UnmatchListenerCreateStmt listener = new UnmatchListenerCreateStmt(env);
            env.eventService().setUnmatchedListener(listener);

            // no statement, should be unmatched
            sendEvent(env, "E1");
            assertEquals(1, listener.getReceived().size());
            listener.reset();

            sendEvent(env, "E1");
            assertEquals(0, listener.getReceived().size());

            env.undeployAll();
        }
    }

    private static class ClientRuntimeUnmatchedInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            MyUnmatchedListener listener = new MyUnmatchedListener();
            env.eventService().setUnmatchedListener(listener);

            // create insert into
            env.compileDeploy("@name('s0') insert into MyEvent select theString from SupportBean");

            // no statement, should be unmatched
            sendEvent(env, "E1");
            assertEquals(1, listener.getReceived().size());
            Assert.assertEquals("E1", listener.getReceived().get(0).get("theString"));
            listener.reset();

            // stop insert into, now SupportBean itself is unmatched
            env.undeployModuleContaining("s0");
            SupportBean theEvent = sendEvent(env, "E2");
            assertEquals(1, listener.getReceived().size());
            assertSame(theEvent, listener.getReceived().get(0).getUnderlying());
            listener.reset();

            // start insert-into
            sendEvent(env, "E3");
            assertEquals(1, listener.getReceived().size());
            Assert.assertEquals("E3", listener.getReceived().get(0).get("theString"));
            listener.reset();
        }
    }

    private static SupportBean sendEvent(RegressionEnvironment env, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        env.sendEventBean(bean);
        return bean;
    }

    public static class MyUnmatchedListener implements UnmatchedListener {
        private List<EventBean> received;

        MyUnmatchedListener() {
            this.received = new ArrayList<>();
        }

        public void update(EventBean theEvent) {
            received.add(theEvent);
        }

        public List<EventBean> getReceived() {
            return received;
        }

        public void reset() {
            received.clear();
        }
    }

    public static class UnmatchListenerCreateStmt implements UnmatchedListener {

        private List<EventBean> received;
        private final RegressionEnvironment env;

        private UnmatchListenerCreateStmt(RegressionEnvironment env) {
            this.env = env;
            this.received = new ArrayList<>();
        }

        public void update(EventBean theEvent) {
            received.add(theEvent);
            env.compileDeploy("select * from SupportBean");
        }

        public List<EventBean> getReceived() {
            return received;
        }

        public void reset() {
            received.clear();
        }
    }
}
