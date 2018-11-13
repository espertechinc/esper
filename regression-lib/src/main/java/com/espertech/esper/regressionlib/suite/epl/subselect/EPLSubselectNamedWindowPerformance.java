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
package com.espertech.esper.regressionlib.suite.epl.subselect;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class EPLSubselectNamedWindowPerformance {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectConstantValue(false, false));
        execs.add(new EPLSubselectConstantValue(true, true));

        execs.add(new EPLSubselectKeyAndRange(false, false));
        execs.add(new EPLSubselectKeyAndRange(true, true));

        execs.add(new EPLSubselectRange(false, false));
        execs.add(new EPLSubselectRange(true, true));

        execs.add(new EPLSubselectKeyedRange());
        execs.add(new EPLSubselectNoShare());

        execs.add(new EPLSubselectShareCreate());
        execs.add(new EPLSubselectDisableShare());
        execs.add(new EPLSubselectDisableShareCreate());
        return execs;
    }

    private void runAssertionConstantValue(RegressionEnvironment env) {
    }

    private static class EPLSubselectConstantValue implements RegressionExecution {
        private final boolean indexShare;
        private final boolean buildIndex;

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public EPLSubselectConstantValue(boolean indexShare, boolean buildIndex) {
            this.indexShare = indexShare;
            this.buildIndex = buildIndex;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String createEpl = "create window MyWindow#keepall as select * from SupportBean";
            if (indexShare) {
                createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
            }
            env.compileDeploy(createEpl, path);

            if (buildIndex) {
                env.compileDeploy("create index idx1 on MyWindow(theString hash)", path);
            }
            env.compileDeploy("insert into MyWindow select * from SupportBean", path);

            // preload
            for (int i = 0; i < 10000; i++) {
                SupportBean bean = new SupportBean("E" + i, i);
                bean.setDoublePrimitive(i);
                env.sendEventBean(bean);
            }

            // single-field compare
            String[] fields = "val".split(",");
            String eplSingle = "@name('s0') select (select intPrimitive from MyWindow where theString = 'E9734') as val from SupportBeanRange sbr";
            env.compileDeploy(eplSingle, path).addListener("s0");

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R", "", -1, -1));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{9734});
            }
            long delta = System.currentTimeMillis() - startTime;
            assertTrue("delta=" + delta, delta < 500);
            env.undeployModuleContaining("s0");

            // two-field compare
            String eplTwoHash = "@name('s1') select (select intPrimitive from MyWindow where theString = 'E9736' and intPrimitive = 9736) as val from SupportBeanRange sbr";
            env.compileDeploy(eplTwoHash, path).addListener("s1");

            startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R", "", -1, -1));
                EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), fields, new Object[]{9736});
            }
            delta = System.currentTimeMillis() - startTime;
            assertTrue("delta=" + delta, delta < 500);
            env.undeployModuleContaining("s1");

            // range compare single
            if (buildIndex) {
                env.compileDeploy("create index idx2 on MyWindow(intPrimitive btree)", path);
            }
            String eplSingleBTree = "@name('s2') select (select intPrimitive from MyWindow where intPrimitive between 9735 and 9735) as val from SupportBeanRange sbr";
            env.compileDeploy(eplSingleBTree, path).addListener("s2");

            startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R", "", -1, -1));
                EPAssertionUtil.assertProps(env.listener("s2").assertOneGetNewAndReset(), fields, new Object[]{9735});
            }
            delta = System.currentTimeMillis() - startTime;
            assertTrue("delta=" + delta, delta < 500);
            env.undeployModuleContaining("s2");

            // range compare composite
            String eplComposite = "@name('s3') select (select intPrimitive from MyWindow where theString = 'E9738' and intPrimitive between 9738 and 9738) as val from SupportBeanRange sbr";
            env.compileDeploy(eplComposite, path).addListener("s3");

            startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R", "", -1, -1));
                EPAssertionUtil.assertProps(env.listener("s3").assertOneGetNewAndReset(), fields, new Object[]{9738});
            }
            delta = System.currentTimeMillis() - startTime;
            assertTrue("delta=" + delta, delta < 500);
            env.undeployModuleContaining("s3");

            // destroy all
            env.undeployAll();
        }
    }

    private static class EPLSubselectKeyAndRange implements RegressionExecution {
        private final boolean indexShare;
        private final boolean buildIndex;

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public EPLSubselectKeyAndRange(boolean indexShare, boolean buildIndex) {
            this.indexShare = indexShare;
            this.buildIndex = buildIndex;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String createEpl = "create window MyWindow#keepall as select * from SupportBean";
            if (indexShare) {
                createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
            }
            env.compileDeploy(createEpl, path);

            if (buildIndex) {
                env.compileDeploy("create index idx1 on MyWindow(theString hash, intPrimitive btree)", path);
            }
            env.compileDeploy("insert into MyWindow select * from SupportBean", path);

            // preload
            for (int i = 0; i < 10000; i++) {
                String theString = i < 5000 ? "A" : "B";
                env.sendEventBean(new SupportBean(theString, i));
            }

            String[] fields = "cols.mini,cols.maxi".split(",");
            String queryEpl = "@name('s0') select (select min(intPrimitive) as mini, max(intPrimitive) as maxi from MyWindow where theString = sbr.key and intPrimitive between sbr.rangeStart and sbr.rangeEnd) as cols from SupportBeanRange sbr";
            env.compileDeploy(queryEpl, path).addListener("s0");

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R1", "A", 300, 312));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{300, 312});
            }
            long delta = System.currentTimeMillis() - startTime;
            assertTrue("delta=" + delta, delta < 500);

            env.undeployAll();
        }
    }

    private static class EPLSubselectRange implements RegressionExecution {

        private final boolean indexShare;
        private final boolean buildIndex;

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public EPLSubselectRange(boolean indexShare, boolean buildIndex) {
            this.indexShare = indexShare;
            this.buildIndex = buildIndex;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String createEpl = "create window MyWindow#keepall as select * from SupportBean";
            if (indexShare) {
                createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
            }
            env.compileDeploy(createEpl, path);

            if (buildIndex) {
                env.compileDeploy("create index idx1 on MyWindow(intPrimitive btree)", path);
            }
            env.compileDeploy("insert into MyWindow select * from SupportBean", path);

            // preload
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean("E1", i));
            }

            String[] fields = "cols.mini,cols.maxi".split(",");
            String queryEpl = "@name('s0') select (select min(intPrimitive) as mini, max(intPrimitive) as maxi from MyWindow where intPrimitive between sbr.rangeStart and sbr.rangeEnd) as cols from SupportBeanRange sbr";
            env.compileDeploy(queryEpl, path).addListener("s0");

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R1", "K", 300, 312));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{300, 312});
            }
            long delta = System.currentTimeMillis() - startTime;
            assertTrue("delta=" + delta, delta < 500);

            env.undeployAll();
        }
    }

    private static class EPLSubselectKeyedRange implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String createEpl = "create window MyWindow#keepall as select * from SupportBean";
            env.compileDeploy(createEpl, path);
            env.compileDeploy("insert into MyWindow select * from SupportBean", path);

            // preload
            for (int i = 0; i < 10000; i++) {
                String key = i < 5000 ? "A" : "B";
                env.sendEventBean(new SupportBean(key, i));
            }

            String[] fields = "cols.mini,cols.maxi".split(",");
            String queryEpl = "@name('s0') select (select min(intPrimitive) as mini, max(intPrimitive) as maxi from MyWindow " +
                "where theString = sbr.key and intPrimitive between sbr.rangeStart and sbr.rangeEnd) as cols from SupportBeanRange sbr";
            env.compileDeploy(queryEpl, path).addListener("s0").milestone(0);

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 500; i++) {
                env.sendEventBean(new SupportBeanRange("R1", "A", 299, 313));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{299, 313});

                env.sendEventBean(new SupportBeanRange("R2", "B", 7500, 7510));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{7500, 7510});
            }
            long delta = System.currentTimeMillis() - startTime;
            assertTrue("delta=" + delta, delta < 500);

            env.undeployAll();
        }
    }

    private static class EPLSubselectNoShare implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            tryAssertion(env, false, false, false);
        }
    }

    private static class EPLSubselectShareCreate implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            tryAssertion(env, true, false, true);
        }
    }

    private static class EPLSubselectDisableShare implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            tryAssertion(env, true, true, false);
        }
    }

    private static class EPLSubselectDisableShareCreate implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            tryAssertion(env, true, true, true);
        }
    }

    private static void tryAssertion(RegressionEnvironment env, boolean enableIndexShareCreate, boolean disableIndexShareConsumer, boolean createExplicitIndex) {
        RegressionPath path = new RegressionPath();
        env.compileDeployWBusPublicType("create schema EventSchema(e0 string, e1 int, e2 string)", path);

        String createEpl = "create window MyWindow#keepall as select * from SupportBean";
        if (enableIndexShareCreate) {
            createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
        }
        env.compileDeploy(createEpl, path);
        env.compileDeploy("insert into MyWindow select * from SupportBean", path);

        if (createExplicitIndex) {
            env.compileDeploy("create index MyIndex on MyWindow (theString)", path);
        }

        String consumeEpl = "@name('s0') select e0, (select theString from MyWindow where intPrimitive = es.e1 and theString = es.e2) as val from EventSchema as es";
        if (disableIndexShareConsumer) {
            consumeEpl = "@Hint('disable_window_subquery_indexshare') " + consumeEpl;
        }
        env.compileDeploy(consumeEpl, path).addListener("s0");

        String[] fields = "e0,val".split(",");

        // test once
        env.sendEventBean(new SupportBean("WX", 10));
        sendEvent(env, "E1", 10, "WX");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "WX"});

        // preload
        for (int i = 0; i < 10000; i++) {
            env.sendEventBean(new SupportBean("W" + i, i));
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            sendEvent(env, "E" + i, i, "W" + i);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E" + i, "W" + i});
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("delta=" + delta, delta < 500);

        env.undeployAll();
    }

    private static void sendEvent(RegressionEnvironment env, String e0, int e1, String e2) {
        HashMap<String, Object> theEvent = new LinkedHashMap<>();
        theEvent.put("e0", e0);
        theEvent.put("e1", e1);
        theEvent.put("e2", e2);
        if (EventRepresentationChoice.getEngineDefault(env.getConfiguration()).isObjectArrayEvent()) {
            env.sendEventObjectArray(theEvent.values().toArray(), "EventSchema");
        } else {
            env.sendEventMap(theEvent, "EventSchema");
        }
    }
}
