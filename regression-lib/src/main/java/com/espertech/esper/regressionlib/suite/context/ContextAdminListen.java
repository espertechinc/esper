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
package com.espertech.esper.regressionlib.suite.context;

import com.espertech.esper.common.client.context.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.context.SupportContextListenUtil;
import com.espertech.esper.regressionlib.support.context.SupportContextListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.espertech.esper.regressionlib.support.context.SupportContextListenUtil.*;
import static org.junit.Assert.*;

public class ContextAdminListen {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextAdminListenInitTerm());
        execs.add(new ContextAdminListenHash());
        execs.add(new ContextAdminListenCategory());
        execs.add(new ContextAdminListenNested());
        execs.add(new ContextAddRemoveListener());
        execs.add(new ContextAdminPartitionAddRemoveListener());
        execs.add(new ContextAdminListenMultipleStatements());
        return execs;
    }

    private static class ContextAdminListenMultipleStatements implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String name = "MyContextStartS0EndS1";
            RegressionPath path = new RegressionPath();
            String contextEPL = "@name('ctx') create context MyContextStartS0EndS1 start SupportBean_S0 as s0 end SupportBean_S1";
            env.compileDeploy(contextEPL, path);
            String depIdCtx = env.deploymentId("ctx");

            SupportContextListener listener = new SupportContextListener(env);
            env.runtime().getContextPartitionService().addContextPartitionStateListener(depIdCtx, "MyContextStartS0EndS1", listener);
            env.compileDeploy("@name('a') context MyContextStartS0EndS1 select count(*) from SupportBean", path);
            String depIdA = env.deploymentId("a");
            env.compileDeploy("@name('b') context MyContextStartS0EndS1 select count(*) from SupportBean_S0", path);
            String depIdB = env.deploymentId("b");

            listener.assertAndReset(SupportContextListenUtil.eventContextWStmt(depIdCtx, name, ContextStateEventContextStatementAdded.class, depIdA, "a"), SupportContextListenUtil.eventContext(depIdCtx, name, ContextStateEventContextActivated.class), SupportContextListenUtil.eventContextWStmt(depIdCtx, name, ContextStateEventContextStatementAdded.class, depIdB, "b"));

            env.sendEventBean(new SupportBean_S0(1));
            listener.assertAndReset(SupportContextListenUtil.eventPartitionInitTerm(depIdCtx, name, ContextStateEventContextPartitionAllocated.class));

            env.undeployAll();
        }
    }

    private static class ContextAdminPartitionAddRemoveListener implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create context MyContextStartEnd start SupportBean_S0 as s0 end SupportBean_S1";
            runAssertionPartitionAddRemoveListener(env, epl, "MyContextStartEnd");

            epl = "create context MyContextStartEndWithNeverEnding " +
                "context NeverEndingStory start @now, " +
                "context ABSession start SupportBean_S0 as s0 end SupportBean_S1";
            runAssertionPartitionAddRemoveListener(env, epl, "MyContextStartEndWithNeverEnding");
        }
    }

    private static void runAssertionPartitionAddRemoveListener(RegressionEnvironment env, String eplContext, String contextName) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('ctx') " + eplContext, path);
        env.compileDeploy("@name('s0') context " + contextName + " select count(*) from SupportBean", path);
        EPContextPartitionService api = env.runtime().getContextPartitionService();
        String depIdCtx = env.deploymentId("ctx");

        SupportContextListener[] listeners = new SupportContextListener[3];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new SupportContextListener(env);
            env.runtime().getContextPartitionService().addContextPartitionStateListener(depIdCtx, contextName, listeners[i]);
        }

        env.sendEventBean(new SupportBean_S0(1));
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].assertAndReset(SupportContextListenUtil.eventPartitionInitTerm(depIdCtx, contextName, ContextStateEventContextPartitionAllocated.class));
        }

        api.removeContextPartitionStateListener(depIdCtx, contextName, listeners[0]);
        env.sendEventBean(new SupportBean_S1(1));
        listeners[0].assertNotInvoked();
        listeners[1].assertAndReset(SupportContextListenUtil.eventPartitionInitTerm(depIdCtx, contextName, ContextStateEventContextPartitionDeallocated.class));
        listeners[2].assertAndReset(SupportContextListenUtil.eventPartitionInitTerm(depIdCtx, contextName, ContextStateEventContextPartitionDeallocated.class));

        Iterator<ContextPartitionStateListener> it = api.getContextPartitionStateListeners(depIdCtx, contextName);
        assertSame(listeners[1], it.next());
        assertSame(listeners[2], it.next());
        assertFalse(it.hasNext());

        api.removeContextPartitionStateListeners(depIdCtx, contextName);
        assertFalse(api.getContextPartitionStateListeners(depIdCtx, contextName).hasNext());

        env.sendEventBean(new SupportBean_S0(2));
        env.sendEventBean(new SupportBean_S1(2));
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].assertNotInvoked();
        }

        env.undeployAll();
    }

    private static class ContextAddRemoveListener implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPContextPartitionService api = env.runtime().getContextPartitionService();

            String epl = "@name('ctx') create context MyContext start SupportBean_S0 as s0 end SupportBean_S1";
            SupportContextListener[] listeners = new SupportContextListener[3];
            for (int i = 0; i < listeners.length; i++) {
                listeners[i] = new SupportContextListener(env);
                env.runtime().getContextPartitionService().addContextStateListener(listeners[i]);
            }

            env.compileDeploy(epl);
            String depIdCtx = env.deploymentId("ctx");
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].assertAndReset(SupportContextListenUtil.eventContext(depIdCtx, "MyContext", ContextStateEventContextCreated.class));
            }

            api.removeContextStateListener(listeners[0]);
            env.undeployModuleContaining("ctx");
            listeners[0].assertNotInvoked();
            listeners[1].assertAndReset(SupportContextListenUtil.eventContext(depIdCtx, "MyContext", ContextStateEventContextDestroyed.class));
            listeners[2].assertAndReset(SupportContextListenUtil.eventContext(depIdCtx, "MyContext", ContextStateEventContextDestroyed.class));

            Iterator<ContextStateListener> it = api.getContextStateListeners();
            assertSame(listeners[1], it.next());
            assertSame(listeners[2], it.next());
            assertFalse(it.hasNext());

            api.removeContextStateListeners();
            assertFalse(api.getContextStateListeners().hasNext());

            env.compileDeploy(epl);
            env.undeployAll();
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].assertNotInvoked();
            }
        }
    }

    private static class ContextAdminListenNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportContextListener listener = new SupportContextListener(env);
            env.runtime().getContextPartitionService().addContextStateListener(listener);

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context MyContext " +
                "context ContextPosNeg group by intPrimitive > 0 as pos, group by intPrimitive < 0 as neg from SupportBean, " +
                "context ByString partition by theString from SupportBean", path);
            String depIdCtx = env.deploymentId("ctx");
            listener.assertAndReset(SupportContextListenUtil.eventContext(depIdCtx, "MyContext", ContextStateEventContextCreated.class));

            env.compileDeploy("@name('s0') context MyContext select count(*) from SupportBean", path);
            String depIdStmt = env.deploymentId("s0");
            listener.assertAndReset(SupportContextListenUtil.eventContextWStmt(depIdCtx, "MyContext", ContextStateEventContextStatementAdded.class, depIdStmt, "s0"), SupportContextListenUtil.eventContext(depIdCtx, "MyContext", ContextStateEventContextActivated.class));

            env.sendEventBean(new SupportBean("E1", 1));
            List<ContextStateEventContextPartitionAllocated> allocated = listener.getAllocatedEvents();
            assertEquals(1, allocated.size());
            ContextPartitionIdentifierNested nested = (ContextPartitionIdentifierNested) allocated.get(0).getIdentifier();
            EPAssertionUtil.assertEqualsExactOrder("E1".split(","), ((ContextPartitionIdentifierPartitioned) nested.getIdentifiers()[1]).getKeys());
            assertEquals(1, listener.getAndReset().size());

            env.undeployModuleContaining("s0");
            listener.assertAndReset(
                SupportContextListenUtil.eventContextWStmt(depIdCtx, "MyContext", ContextStateEventContextStatementRemoved.class, depIdStmt, "s0"),
                SupportContextListenUtil.eventPartitionInitTerm(depIdCtx, "MyContext", ContextStateEventContextPartitionDeallocated.class),
                SupportContextListenUtil.eventContext(depIdCtx, "MyContext", ContextStateEventContextDeactivated.class));

            env.undeployModuleContaining("ctx");
            listener.assertAndReset(SupportContextListenUtil.eventContext(depIdCtx, "MyContext", ContextStateEventContextDestroyed.class));

            env.runtime().getContextPartitionService().removeContextStateListeners();
        }
    }

    private static class ContextAdminListenCategory implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportContextListener listener = new SupportContextListener(env);
            env.runtime().getContextPartitionService().addContextStateListener(listener);

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context MyContext group by intPrimitive > 0 as pos, group by intPrimitive < 0 as neg from SupportBean", path);
            env.compileDeploy("@name('s0') context MyContext select count(*) from SupportBean", path);

            List<ContextStateEventContextPartitionAllocated> allocated = listener.getAllocatedEvents();
            assertEquals(2, allocated.size());
            assertEquals("neg", ((ContextPartitionIdentifierCategory) allocated.get(1).getIdentifier()).getLabel());
            listener.getAndReset();

            env.undeployModuleContaining("s0");
            env.undeployModuleContaining("ctx");
            env.runtime().getContextPartitionService().removeContextStateListeners();
        }
    }

    private static class ContextAdminListenHash implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportContextListener listener = new SupportContextListener(env);
            env.runtime().getContextPartitionService().addContextStateListener(listener);

            String epl = "@name('ctx') create context MyContext coalesce by consistent_hash_crc32(theString) from SupportBean granularity 2 preallocate;\n" +
                "@name('s0') context MyContext select count(*) from SupportBean;\n";
            env.compileDeploy(epl);
            String deploymentId = env.deploymentId("s0");

            List<ContextStateEventContextPartitionAllocated> allocated = listener.getAllocatedEvents();
            assertEquals(2, allocated.size());
            assertEquals(1, ((ContextPartitionIdentifierHash) allocated.get(1).getIdentifier()).getHash());
            listener.getAndReset();

            env.undeployAll();

            listener.assertAndReset(
                SupportContextListenUtil.eventContextWStmt(deploymentId, "MyContext", ContextStateEventContextStatementRemoved.class, deploymentId, "s0"),
                SupportContextListenUtil.eventPartitionInitTerm(deploymentId, "MyContext", ContextStateEventContextPartitionDeallocated.class),
                SupportContextListenUtil.eventPartitionInitTerm(deploymentId, "MyContext", ContextStateEventContextPartitionDeallocated.class),
                SupportContextListenUtil.eventContext(deploymentId, "MyContext", ContextStateEventContextDeactivated.class),
                SupportContextListenUtil.eventContext(deploymentId, "MyContext", ContextStateEventContextDestroyed.class));

            env.runtime().getContextPartitionService().removeContextStateListeners();
        }
    }

    private static class ContextAdminListenInitTerm implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportContextListener listener = new SupportContextListener(env);
            env.runtime().getContextPartitionService().addContextStateListener(listener);

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('ctx') create context MyContext start SupportBean_S0 as s0 end SupportBean_S1", path);
            String depIdCtx = env.deploymentId("ctx");
            listener.assertAndReset(eventContext(depIdCtx, "MyContext", ContextStateEventContextCreated.class));

            env.compileDeploy("@name('s0') context MyContext select count(*) from SupportBean", path);
            String depIdStmt = env.deploymentId("s0");
            listener.assertAndReset(eventContextWStmt(depIdCtx, "MyContext", ContextStateEventContextStatementAdded.class, depIdStmt, "s0"), eventContext(depIdCtx, "MyContext", ContextStateEventContextActivated.class));

            env.sendEventBean(new SupportBean_S0(1));
            listener.assertAndReset(eventPartitionInitTerm(depIdCtx, "MyContext", ContextStateEventContextPartitionAllocated.class));

            env.sendEventBean(new SupportBean_S1(1));
            listener.assertAndReset(eventPartitionInitTerm(depIdCtx, "MyContext", ContextStateEventContextPartitionDeallocated.class));

            env.undeployModuleContaining("s0");
            listener.assertAndReset(eventContextWStmt(depIdCtx, "MyContext", ContextStateEventContextStatementRemoved.class, depIdStmt, "s0"), eventContext(depIdCtx, "MyContext", ContextStateEventContextDeactivated.class));

            env.undeployModuleContaining("ctx");
            listener.assertAndReset(eventContext(depIdCtx, "MyContext", ContextStateEventContextDestroyed.class));

            env.runtime().getContextPartitionService().removeContextStateListeners();
        }
    }
}
