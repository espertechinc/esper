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
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class ContextInitTermWithDistinct {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ContextInitTermWithDistinctInvalid());
        execs.add(new ContextInitTermWithDistinctNullSingleKey());
        execs.add(new ContextInitTermWithDistinctNullKeyMultiKey());
        execs.add(new ContextInitTermWithDistinctOverlappingSingleKey());
        execs.add(new ContextInitTermWithDistinctOverlappingMultiKey());
        execs.add(new ContextInitTermWithDistinctNullSingleKey());
        execs.add(new ContextInitTermWithDistinctNullKeyMultiKey());
        execs.add(new ContextInitTermWithDistinctMultikeyWArray());
        return execs;
    }

    private static class ContextInitTermWithDistinctMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyContext initiated by distinct(array) SupportEventWithIntArray as se", path);
            env.compileDeploy("@name('s0') context MyContext select context.se.id as id, sum(intPrimitive) as thesum from SupportBean", path);
            env.addListener("s0");
            String[] fields = "id,thesum".split(",");

            env.sendEventBean(new SupportEventWithIntArray("SE1", new int[] {1, 2}, 0));
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][] {{"SE1", 1}});

            env.sendEventBean(new SupportEventWithIntArray("SE2", new int[] {1}, 0));
            env.sendEventBean(new SupportEventWithIntArray("SE2", new int[] {1}, 0));
            env.sendEventBean(new SupportEventWithIntArray("SE1", new int[] {1, 2}, 0));
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][] {{"SE1", 3}, {"SE2", 2}});

            env.milestone(0);

            env.sendEventBean(new SupportEventWithIntArray("SE1", new int[] {1, 2}, 0));
            env.sendEventBean(new SupportEventWithIntArray("SE2", new int[] {1}, 0));
            env.sendEventBean(new SupportEventWithIntArray("SE3", new int[] {}, 0));
            env.sendEventBean(new SupportBean("E3", 4));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][] {{"SE1", 7}, {"SE2", 6}, {"SE3", 4}});

            env.undeployAll();
        }
    }

    private static class ContextInitTermWithDistinctInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // require stream name assignment using 'as'
            SupportMessageAssertUtil.tryInvalidCompile(env, "create context MyContext initiated by distinct(theString) SupportBean terminated after 15 seconds",
                "Distinct-expressions require that a stream name is assigned to the stream using 'as' [create context MyContext initiated by distinct(theString) SupportBean terminated after 15 seconds]");

            // require stream
            SupportMessageAssertUtil.tryInvalidCompile(env, "create context MyContext initiated by distinct(a.theString) pattern [a=SupportBean] terminated after 15 seconds",
                "Distinct-expressions require a stream as the initiated-by condition [create context MyContext initiated by distinct(a.theString) pattern [a=SupportBean] terminated after 15 seconds]");

            // invalid distinct-clause expression
            SupportMessageAssertUtil.tryInvalidCompile(env, "create context MyContext initiated by distinct((select * from MyWindow)) SupportBean as sb terminated after 15 seconds",
                "Invalid context distinct-clause expression 'subselect_0': Aggregation, sub-select, previous or prior functions are not supported in this context [create context MyContext initiated by distinct((select * from MyWindow)) SupportBean as sb terminated after 15 seconds]");

            // empty list of expressions
            SupportMessageAssertUtil.tryInvalidCompile(env, "create context MyContext initiated by distinct() SupportBean terminated after 15 seconds",
                "Distinct-expressions have not been provided [create context MyContext initiated by distinct() SupportBean terminated after 15 seconds]");

            // non-overlapping context not allowed with distinct
            SupportMessageAssertUtil.tryInvalidCompile(env, "create context MyContext start distinct(theString) SupportBean end after 15 seconds",
                "Incorrect syntax near 'distinct' (a reserved keyword) at line 1 column 31 [create context MyContext start distinct(theString) SupportBean end after 15 seconds]");
        }
    }

    private static class ContextInitTermWithDistinctOverlappingSingleKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy(
                "create context MyContext " +
                    "  initiated by distinct(s0.theString) SupportBean(intPrimitive = 0) s0" +
                    "  terminated by SupportBean(theString = s0.theString and intPrimitive = 1)", path);

            String[] fields = "theString,longPrimitive,cnt".split(",");
            env.compileDeploy(
                "@name('s0') context MyContext " +
                    "select theString, longPrimitive, count(*) as cnt from SupportBean(theString = context.s0.theString)", path);
            env.addListener("s0");

            sendEvent(env, "A", -1, 10);

            env.milestone(0);

            sendEvent(env, "A", 1, 11);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendEvent(env, "A", 0, 12);   // allocate context
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 12L, 1L});

            sendEvent(env, "A", 0, 13);   // counts towards the existing context, not having a new one
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 13L, 2L});

            env.milestone(2);

            sendEvent(env, "A", -1, 14);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 14L, 3L});

            sendEvent(env, "A", 1, 15);   // context termination
            sendEvent(env, "A", -1, 16);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            sendEvent(env, "A", 0, 17);   // allocate context
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 17L, 1L});

            env.milestone(4);

            sendEvent(env, "A", -1, 18);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 18L, 2L});

            env.milestone(5);

            sendEvent(env, "B", 0, 19);   // allocate context
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 19L, 1L});

            env.milestone(6);

            sendEvent(env, "B", -1, 20);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 20L, 2L});

            sendEvent(env, "A", 1, 21);   // context termination

            env.milestone(7);

            sendEvent(env, "B", 1, 22);   // context termination
            sendEvent(env, "A", -1, 23);

            env.milestone(8);

            sendEvent(env, "B", -1, 24);
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "A", 0, 25);   // allocate context
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 25L, 1L});

            env.milestone(9);

            sendEvent(env, "B", 0, 26);   // allocate context
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 26L, 1L});

            env.undeployAll();
        }
    }

    private static class ContextInitTermWithDistinctOverlappingMultiKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "create context MyContext as " +
                "initiated by distinct(theString, intPrimitive) SupportBean as sb " +
                "terminated SupportBean_S1";         // any S1 ends the contexts
            env.eplToModelCompileDeploy(epl, path);

            String[] fields = "id,p00,p01,cnt".split(",");
            env.compileDeploy(
                "@name('s0') context MyContext " +
                    "select id, p00, p01, count(*) as cnt " +
                    "from SupportBean_S0(id = context.sb.intPrimitive and p00 = context.sb.theString)", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "A"));

            env.milestone(0);

            env.sendEventBean(new SupportBean("A", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(1, "A", "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "A", "E1", 1L});

            env.milestone(1);

            env.sendEventBean(new SupportBean_S0(1, "A", "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "A", "E2", 2L});

            env.sendEventBean(new SupportBean_S1(-1)); // terminate all
            env.sendEventBean(new SupportBean_S0(1, "A", "E3"));

            env.milestone(2);

            env.sendEventBean(new SupportBean("A", 1));
            env.sendEventBean(new SupportBean("B", 2));
            env.sendEventBean(new SupportBean("B", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            env.sendEventBean(new SupportBean_S0(1, "A", "E4"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "A", "E4", 1L});

            env.sendEventBean(new SupportBean_S0(2, "B", "E5"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "B", "E5", 1L});

            env.milestone(4);

            env.sendEventBean(new SupportBean_S0(1, "B", "E6"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "B", "E6", 1L});

            env.sendEventBean(new SupportBean_S0(2, "B", "E7"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "B", "E7", 2L});

            env.milestone(5);

            env.sendEventBean(new SupportBean_S1(-1)); // terminate all

            env.milestone(6);

            env.sendEventBean(new SupportBean_S0(2, "B", "E8"));
            env.sendEventBean(new SupportBean("B", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(2, "B", "E9"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "B", "E9", 1L});

            env.milestone(7);

            env.sendEventBean(new SupportBean_S0(2, "B", "E10"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "B", "E10", 2L});

            env.undeployAll();
        }
    }

    private static class ContextInitTermWithDistinctNullSingleKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyContext initiated by distinct(theString) SupportBean as sb terminated after 24 hours", path);
            env.compileDeploy("@name('s0') context MyContext select count(*) as cnt from SupportBean", path);
            env.addListener("s0");

            env.sendEventBean(new SupportBean(null, 10));
            Assert.assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            env.milestone(0);

            env.sendEventBean(new SupportBean(null, 20));
            Assert.assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            env.milestone(1);

            env.sendEventBean(new SupportBean("A", 30));
            Assert.assertEquals(2, env.listener("s0").getAndResetLastNewData().length);

            env.undeployAll();
        }
    }

    private static class ContextInitTermWithDistinctNullKeyMultiKey implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create context MyContext initiated by distinct(theString, intBoxed, intPrimitive) SupportBean as sb terminated after 100 hours", path);
            env.compileDeploy("@name('s0') context MyContext select count(*) as cnt from SupportBean", path);
            env.addListener("s0");

            sendSBEvent(env, "A", null, 1);
            Assert.assertEquals(1L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            sendSBEvent(env, "A", null, 1);
            Assert.assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("cnt"));

            env.milestone(0);

            sendSBEvent(env, "A", 10, 1);
            Assert.assertEquals(2, env.listener("s0").getAndResetLastNewData().length);

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean event = new SupportBean(theString, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        env.sendEventBean(event);
    }

    private static void sendSBEvent(RegressionEnvironment env, String string, Integer intBoxed, int intPrimitive) {
        SupportBean bean = new SupportBean(string, intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
    }
}
