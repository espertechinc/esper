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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.context.SupportContextMgmtHelper;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDW;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDWFactory;
import com.espertech.esper.regressionlib.support.util.SupportScheduleHelper;
import com.espertech.esper.runtime.client.EPUndeployException;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class ContextLifecycle {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextLifecycleSplitStream());
        execs.add(new ContextLifecycleVirtualDataWindow());
        execs.add(new ContextLifecycleNWOtherContextOnExpr());
        execs.add(new ContextLifecycleInvalid());
        execs.add(new ContextLifecycleSimple());
        return execs;
    }

    private static class ContextLifecycleSplitStream implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplOne = "create context CtxSegmentedByTarget partition by theString from SupportBean;" +
                "@Name('out') context CtxSegmentedByTarget on SupportBean insert into NewSupportBean select * where intPrimitive = 100;";
            env.compileDeploy(eplOne, path);
            env.compileDeploy("@name('s0') select * from NewSupportBean", path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("E1", 100));
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            env.undeployAll();
            path.clear();

            // test with subquery
            String[] fields = "mymax".split(",");
            String eplTwo = "create context CtxSegmentedByTarget partition by theString from SupportBean;" +
                "context CtxSegmentedByTarget create window NewEvent#unique(theString) as SupportBean;" +
                "@Name('out') context CtxSegmentedByTarget on SupportBean " +
                "insert into NewEvent select * where intPrimitive = 100 " +
                "insert into NewEventTwo select (select max(intPrimitive) from NewEvent) as mymax  " +
                "output all;";
            env.compileDeploy(eplTwo, path);
            env.compileDeploy("@name('s0') select * from NewEventTwo", path).addListener("s0");
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

            env.sendEventBean(new SupportBean("E1", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

            env.sendEventBean(new SupportBean("E1", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100});

            env.undeployAll();
        }
    }

    private static class ContextLifecycleVirtualDataWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportVirtualDWFactory.getWindows().clear();
            SupportVirtualDWFactory.setDestroyed(false);

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context CtxSegmented as partition by theString from SupportBean", path);
            env.compileDeploy("context CtxSegmented create window TestVDWWindow.test:vdw() as SupportBean", path);
            env.compileDeploy("select * from TestVDWWindow", path);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            assertEquals(2, SupportVirtualDWFactory.getWindows().size());   // Independent windows for independent contexts

            env.undeployAll();
            for (SupportVirtualDW vdw : SupportVirtualDWFactory.getWindows()) {
                TestCase.assertTrue(vdw.isDestroyed());
            }
            TestCase.assertTrue(SupportVirtualDWFactory.isDestroyed());
        }
    }

    private static class ContextLifecycleNWOtherContextOnExpr implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)", path);
            env.compileDeploy("create context TenToFive as start (0, 10, *, *, *) end (0, 17, *, *, *)", path);

            // Trigger not in context
            env.compileDeploy("@name('createwindow') context NineToFive create window MyWindow#keepall as SupportBean", path);
            tryInvalidCompile(env, path, "on SupportBean_S0 s0 merge MyWindow mw when matched then update set intPrimitive = 1",
                "Cannot create on-trigger expression: Named window 'MyWindow' was declared with context 'NineToFive', please declare the same context name");

            // Trigger in different context
            tryInvalidCompile(env, path, "context TenToFive on SupportBean_S0 s0 merge MyWindow mw when matched then update set intPrimitive = 1",
                "Cannot create on-trigger expression: Named window 'MyWindow' was declared with context 'NineToFive', please use the same context instead");

            // Named window not in context, trigger in different context
            env.undeployModuleContaining("createwindow");
            env.compileDeploy("create window MyWindowTwo#keepall as SupportBean", path);
            tryInvalidCompile(env, path, "context TenToFive on SupportBean_S0 s0 merge MyWindowTwo mw when matched then update set intPrimitive = 1",
                "Cannot create on-trigger expression: Named window 'MyWindowTwo' was declared without a context");

            env.undeployAll();
        }
    }

    private static class ContextLifecycleSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@Name('context') create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)";
            assertEquals(0, SupportContextMgmtHelper.getContextCount(env));
            assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            // create and destroy
            env.compileDeploy(epl);
            assertEquals(1, SupportContextMgmtHelper.getContextCount(env));
            assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            env.undeployModuleContaining("context");
            assertEquals(0, SupportContextMgmtHelper.getContextCount(env));

            // create context, create statement, destroy statement, destroy context
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            assertEquals(1, SupportContextMgmtHelper.getContextCount(env));

            env.compileDeploy("@Name('s0') context NineToFive select * from SupportBean", path);
            assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));

            env.undeployModuleContaining("s0");
            assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            env.undeployModuleContaining("context");
            assertEquals(0, SupportContextMgmtHelper.getContextCount(env));

            // create same context
            path.clear();
            env.compileDeploy(epl, path);
            env.compileDeploy("@Name('C') context NineToFive select * from SupportBean", path);
            env.compileDeploy("@Name('D') context NineToFive select * from SupportBean", path);

            assertEquals(1, SupportScheduleHelper.scheduleCountOverall(env));

            env.undeployAll();
            assertEquals(0, SupportContextMgmtHelper.getContextCount(env));
            assertEquals(0, SupportScheduleHelper.scheduleCountOverall(env));

            env.undeployAll();
        }
    }

    private static class ContextLifecycleInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // same context twice
            String eplCreateCtx = "@name('ctx') create context NineToFive as start (0, 9, *, *, *) end (0, 17, *, *, *)";
            env.compileDeploy(eplCreateCtx, path);
            tryInvalidCompile(env, path, eplCreateCtx, "Context by name 'NineToFive' already exists");

            // still in use
            env.compileDeploy("context NineToFive select * from SupportBean", path);
            try {
                env.deployment().undeploy(env.deploymentId("ctx"));
                fail();
            } catch (EPUndeployException ex) {
                SupportMessageAssertUtil.assertMessage(ex.getMessage(), "A precondition is not satisfied: Context 'NineToFive' cannot be un-deployed as it is referenced by deployment");
            }

            // not found
            tryInvalidCompile(env, path, "context EightToSix select * from SupportBean", "Context by name 'EightToSix' could not be found");

            // test update: update is not allowed as it is processed out-of-context by eventService
            env.compileDeploy("insert into ABCStream select * from SupportBean", path);
            env.compileDeploy("@Name('context') create context SegmentedByAString partition by theString from ABCStream", path);
            tryInvalidCompile(env, path, "context SegmentedByAString update istream ABCStream set intPrimitive = (select id from SupportBean_S0#lastevent) where intPrimitive < 0",
                "Update IStream is not supported in conjunction with a context");

            // context declaration for create-context
            env.compileDeploy("create context ABC start @now end after 5 seconds", path);
            tryInvalidCompile(env, path, "context ABC create context DEF start @now end after 5 seconds",
                "A create-context statement cannot itself be associated to a context, please declare a nested context instead [context ABC create context DEF start @now end after 5 seconds]");

            // statement references context but there is none
            tryInvalidCompile(env, "select context.sb.theString from SupportBean as sb",
                "Failed to validate select-clause expression 'context.sb.theString': Failed to resolve property 'context.sb.theString' to a stream or nested property in a stream");

            env.undeployAll();
        }
    }
}
