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
package com.espertech.esper.regressionlib.suite.rowrecog;

import com.espertech.esper.common.client.hook.condition.ConditionHandlerContext;
import com.espertech.esper.common.client.hook.condition.ConditionMatchRecognizeStatesMax;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RowRecogMaxStatesEngineWide3Instance implements RegressionExecution {
    private SupportConditionHandlerFactory.SupportConditionHandler handler;

    public void run(RegressionEnvironment env) {
        handler = SupportConditionHandlerFactory.getLastHandler();

        runAssertionTwoStatementNoDelete(env);
        runAssertionContextPartitionAndOverflow(env);
        runAssertionNamedWindowInSequenceRemoveEvent(env);
        runAssertionNamedWindowOutOfSequenceRemoveEvent(env);
    }

    private void runAssertionTwoStatementNoDelete(RegressionEnvironment env) {
        String[] fields = "c0".split(",");
        String eplOne = "@name('S1') select * from SupportBean(theString='A') " +
            "match_recognize (" +
            "  measures P1.longPrimitive as c0" +
            "  pattern (P1 P2 P3) " +
            "  define " +
            "    P1 as P1.intPrimitive = 1," +
            "    P2 as P2.intPrimitive = 1," +
            "    P3 as P3.intPrimitive = 2 and P3.longPrimitive = P1.longPrimitive" +
            ")";
        env.compileDeploy(eplOne).addListener("S1");

        String eplTwo = "@name('S2') select * from SupportBean(theString='B') " +
            "match_recognize (" +
            "  measures P1.longPrimitive as c0" +
            "  pattern (P1 P2 P3) " +
            "  define " +
            "    P1 as P1.intPrimitive = 1," +
            "    P2 as P2.intPrimitive = 1," +
            "    P3 as P3.intPrimitive = 2 and P3.longPrimitive = P1.longPrimitive" +
            ")";
        env.compileDeploy(eplTwo).addListener("S2");

        env.sendEventBean(makeBean("A", 1, 10)); // A(10):P1->P2
        env.sendEventBean(makeBean("B", 1, 11)); // A(10):P1->P2, B(11):P1->P2
        env.sendEventBean(makeBean("A", 1, 12)); // A(10):P2->P3, A(12):P1->P2, B(11):P1->P2
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(makeBean("B", 1, 13)); // would be: A(10):P2->P3, A(12):P1->P2, B(11):P2->P3, B(13):P1->P2
        assertContextEnginePool(env, env.statement("S2"), handler.getAndResetContexts(), 3, getExpectedCountMap(env, "S1", 2, "S2", 1));

        // terminate B
        env.sendEventBean(makeBean("B", 2, 11)); // we have no more B-state
        EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{11L});

        // should not overflow
        env.sendEventBean(makeBean("B", 1, 15));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(makeBean("B", 1, 16));
        assertContextEnginePool(env, env.statement("S2"), handler.getAndResetContexts(), 3, getExpectedCountMap(env, "S1", 2, "S2", 1));

        // terminate A
        env.sendEventBean(makeBean("A", 2, 10)); // we have no more A-state
        EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{10L});

        // should not overflow
        env.sendEventBean(makeBean("B", 1, 17));
        env.sendEventBean(makeBean("B", 1, 18));
        env.sendEventBean(makeBean("A", 1, 19));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(makeBean("A", 1, 20));
        assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 3, getExpectedCountMap(env, "S1", 1, "S2", 2));

        // terminate B
        env.sendEventBean(makeBean("B", 2, 17));
        EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fields, new Object[]{17L});

        // terminate A
        env.sendEventBean(makeBean("A", 2, 19));
        EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{19L});

        env.undeployAll();
    }

    private void runAssertionContextPartitionAndOverflow(RegressionEnvironment env) {
        String[] fields = "c0".split(",");
        RegressionPath path = new RegressionPath();
        String eplCtx = "create context MyCtx initiated by SupportBean_S0 as s0 terminated by SupportBean_S1(p10 = s0.p00)";
        env.compileDeploy(eplCtx, path);

        String epl = "@name('S1') context MyCtx select * from SupportBean(theString = context.s0.p00) " +
            "match_recognize (" +
            "  measures P2.theString as c0" +
            "  pattern (P1 P2) " +
            "  define " +
            "    P1 as P1.intPrimitive = 1," +
            "    P2 as P2.intPrimitive = 2" +
            ")";
        env.compileDeploy(epl, path).addListener("S1");

        env.sendEventBean(new SupportBean_S0(0, "A"));
        env.sendEventBean(new SupportBean("A", 1));
        env.sendEventBean(new SupportBean_S0(0, "B"));
        env.sendEventBean(new SupportBean("B", 1));
        env.sendEventBean(new SupportBean_S0(0, "C"));
        env.sendEventBean(new SupportBean("C", 1));
        env.sendEventBean(new SupportBean_S0(0, "D"));
        assertTrue(handler.getContexts().isEmpty());

        env.sendEventBean(new SupportBean("D", 1));
        assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 3, getExpectedCountMap(env, "S1", 3));

        // terminate a context partition
        env.sendEventBean(new SupportBean_S1(0, "D"));
        env.sendEventBean(new SupportBean("D", 1));
        env.sendEventBean(new SupportBean_S0(0, "E"));
        assertTrue(handler.getContexts().isEmpty());

        env.sendEventBean(new SupportBean("E", 1));
        assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 3, getExpectedCountMap(env, "S1", 3));

        env.sendEventBean(new SupportBean("A", 2));
        EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{"A"});

        env.undeployAll();
    }

    private void runAssertionNamedWindowInSequenceRemoveEvent(RegressionEnvironment env) {
        String[] fields = "c0,c1".split(",");
        RegressionPath path = new RegressionPath();

        String namedWindow = "create window MyWindow#keepall as SupportBean";
        env.compileDeploy(namedWindow, path);
        String insert = "insert into MyWindow select * from SupportBean";
        env.compileDeploy(insert, path);
        String delete = "on SupportBean_S0 delete from MyWindow where theString = p00";
        env.compileDeploy(delete, path);

        String epl = "@name('S1') select * from MyWindow " +
            "match_recognize (" +
            "  partition by theString " +
            "  measures P1.longPrimitive as c0, P2.longPrimitive as c1" +
            "  pattern (P1 P2) " +
            "  define " +
            "    P1 as P1.intPrimitive = 0," +
            "    P2 as P2.intPrimitive = 1" +
            ")";

        env.compileDeploy(epl, path).addListener("S1");

        env.sendEventBean(makeBean("A", 0, 1));
        env.sendEventBean(makeBean("B", 0, 2));
        env.sendEventBean(makeBean("C", 0, 3));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(makeBean("D", 0, 4));
        assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 3, getExpectedCountMap(env, "S1", 3));

        // delete A (in-sequence remove)
        env.sendEventBean(new SupportBean_S0(1, "A"));
        env.sendEventBean(makeBean("D", 0, 5)); // now 3 states: B, C, D
        assertTrue(handler.getContexts().isEmpty());

        // test matching
        env.sendEventBean(makeBean("B", 1, 6)); // now 2 states: C, D
        EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{2L, 6L});

        // no overflows
        env.sendEventBean(makeBean("E", 0, 7));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(makeBean("F", 0, 9));
        assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 3, getExpectedCountMap(env, "S1", 3));

        // no match expected
        env.sendEventBean(makeBean("F", 1, 10));
        assertFalse(env.listener("S1").isInvoked());

        env.undeployAll();
    }

    private void runAssertionNamedWindowOutOfSequenceRemoveEvent(RegressionEnvironment env) {
        String[] fields = "c0,c1,c2".split(",");
        RegressionPath path = new RegressionPath();

        String namedWindow = "create window MyWindow#keepall as SupportBean";
        env.compileDeploy(namedWindow, path);
        String insert = "insert into MyWindow select * from SupportBean";
        env.compileDeploy(insert, path);
        String delete = "on SupportBean_S0 delete from MyWindow where theString = p00 and intPrimitive = id";
        env.compileDeploy(delete, path);

        String epl = "@name('S1') select * from MyWindow " +
            "match_recognize (" +
            "  partition by theString " +
            "  measures P1.longPrimitive as c0, P2.longPrimitive as c1, P3.longPrimitive as c2" +
            "  pattern (P1 P2 P3) " +
            "  define " +
            "    P1 as P1.intPrimitive = 0," +
            "    P2 as P2.intPrimitive = 1," +
            "    P3 as P3.intPrimitive = 2" +
            ")";
        env.compileDeploy(epl, path).addListener("S1");

        env.sendEventBean(makeBean("A", 0, 1));
        env.sendEventBean(makeBean("A", 1, 2));
        env.sendEventBean(makeBean("B", 0, 3));
        assertTrue(handler.getContexts().isEmpty());

        // delete A-1 (out-of-sequence remove)
        env.sendEventBean(new SupportBean_S0(1, "A"));
        env.sendEventBean(new SupportBean_S0(0, "A"));
        env.sendEventBean(makeBean("A", 2, 4));
        assertFalse(env.listener("S1").isInvoked());
        assertTrue(handler.getContexts().isEmpty()); // states: B

        // test overflow
        env.sendEventBean(makeBean("C", 0, 5));
        env.sendEventBean(makeBean("D", 0, 6));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(makeBean("E", 0, 7));
        assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 3, getExpectedCountMap(env, "S1", 3));

        // assert nothing matches for overflowed and deleted
        env.sendEventBean(makeBean("E", 1, 8));
        env.sendEventBean(makeBean("E", 2, 9));
        env.sendEventBean(new SupportBean_S0(0, "C")); // delete c
        env.sendEventBean(makeBean("C", 1, 10));
        env.sendEventBean(makeBean("C", 2, 11));
        assertFalse(env.listener("S1").isInvoked());

        // assert match found for B
        env.sendEventBean(makeBean("B", 1, 12));
        env.sendEventBean(makeBean("B", 2, 13));
        EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields, new Object[]{3L, 12L, 13L});

        // no overflow
        env.sendEventBean(makeBean("F", 0, 14));
        env.sendEventBean(makeBean("G", 0, 15));
        assertTrue(handler.getContexts().isEmpty());

        // overflow
        env.sendEventBean(makeBean("H", 0, 16));
        assertContextEnginePool(env, env.statement("S1"), handler.getAndResetContexts(), 3, getExpectedCountMap(env, "S1", 3));

        env.undeployAll();
    }

    protected static void assertContextEnginePool(RegressionEnvironment env, EPStatement stmt, List<ConditionHandlerContext> contexts, int max, Map<DeploymentIdNamePair, Long> counts) {
        assertEquals(1, contexts.size());
        ConditionHandlerContext context = contexts.get(0);
        assertEquals(env.runtimeURI(), context.getRuntimeURI());
        assertEquals(stmt.getDeploymentId(), context.getDeploymentId());
        assertEquals(stmt.getName(), context.getStatementName());
        ConditionMatchRecognizeStatesMax condition = (ConditionMatchRecognizeStatesMax) context.getCondition();
        assertEquals(max, condition.getMax());
        assertEquals(counts.size(), condition.getCounts().size());
        for (Map.Entry<DeploymentIdNamePair, Long> expected : counts.entrySet()) {
            assertEquals("failed for key " + expected.getKey(), expected.getValue(), condition.getCounts().get(expected.getKey()));
        }
        contexts.clear();
    }

    protected static Map<DeploymentIdNamePair, Long> getExpectedCountMap(RegressionEnvironment env, String stmtOne, long countOne, String stmtTwo, long countTwo) {
        Map<DeploymentIdNamePair, Long> result = new HashMap<>();
        result.put(new DeploymentIdNamePair(env.deploymentId(stmtOne), stmtOne), countOne);
        result.put(new DeploymentIdNamePair(env.deploymentId(stmtTwo), stmtTwo), countTwo);
        return result;
    }

    protected static Map<DeploymentIdNamePair, Long> getExpectedCountMap(RegressionEnvironment env, String stmtOne, long countOne) {
        Map<DeploymentIdNamePair, Long> result = new HashMap<>();
        result.put(new DeploymentIdNamePair(env.deploymentId(stmtOne), stmtOne), countOne);
        return result;
    }

    protected static SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean supportBean = new SupportBean(theString, intPrimitive);
        supportBean.setLongPrimitive(longPrimitive);
        return supportBean;
    }
}
