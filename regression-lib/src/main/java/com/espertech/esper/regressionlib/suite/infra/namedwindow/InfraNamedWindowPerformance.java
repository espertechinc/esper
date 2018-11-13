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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.util.SupportInfraUtil;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowPerformance {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraOnSelectInKeywordPerformance());
        execs.add(new InfraOnSelectEqualsAndRangePerformance());
        execs.add(new InfraDeletePerformance());
        execs.add(new InfraDeletePerformanceCoercion());
        execs.add(new InfraDeletePerformanceTwoDeleters());
        execs.add(new InfraDeletePerformanceIndexReuse());
        return execs;
    }

    private static class InfraOnSelectInKeywordPerformance implements RegressionExecution {

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create window MyWindow#keepall as SupportBean_S0;\n" +
                "insert into MyWindow select * from SupportBean_S0;\n", path);

            int maxRows = 10000;   // for performance testing change to int maxRows = 100000;
            for (int i = 0; i < maxRows; i++) {
                env.sendEventBean(new SupportBean_S0(i, "p00_" + i));
            }

            String eplSingleIdx = "on SupportBean_S1 select sum(mw.id) as sumi from MyWindow mw where p00 in (p10, p11)";
            runOnDemandAssertion(env, path, eplSingleIdx, 1, new SupportBean_S1(0, "x", "p00_6523"), 6523);

            String eplMultiIndex = "on SupportBean_S1 select sum(mw.id) as sumi from MyWindow mw where p10 in (p00, p01)";
            runOnDemandAssertion(env, path, eplMultiIndex, 2, new SupportBean_S1(0, "p00_6524"), 6524);

            env.undeployAll();
        }
    }

    private static class InfraOnSelectEqualsAndRangePerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') create window MyWindow#keepall as SupportBean;\n" +
                "insert into MyWindow select * from SupportBean", path);

            // insert X rows
            int maxRows = 10000;   //for performance testing change to int maxRows = 100000;
            for (int i = 0; i < maxRows; i++) {
                SupportBean bean = new SupportBean((i < 5000) ? "A" : "B", i);
                bean.setLongPrimitive(i);
                bean.setLongBoxed((long) i + 1);
                env.sendEventBean(bean);
            }
            env.sendEventBean(new SupportBean("B", 100));

            String eplIdx1One = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow where intPrimitive = sbr.rangeStart";
            runOnDemandAssertion(env, path, eplIdx1One, 1, new SupportBeanRange("R", 5501, 0), 5501);

            String eplIdx1Two = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow where intPrimitive between sbr.rangeStart and sbr.rangeEnd";
            runOnDemandAssertion(env, path, eplIdx1Two, 1, new SupportBeanRange("R", 5501, 5503), 5501 + 5502 + 5503);

            String eplIdx1Three = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow where theString = key and intPrimitive between sbr.rangeStart and sbr.rangeEnd";
            runOnDemandAssertion(env, path, eplIdx1Three, 1, new SupportBeanRange("R", "A", 4998, 5503), 4998 + 4999);

            String eplIdx1Four = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow " +
                "where theString = key and longPrimitive = rangeStart and intPrimitive between rangeStart and rangeEnd " +
                "and longBoxed between rangeStart and rangeEnd";
            runOnDemandAssertion(env, path, eplIdx1Four, 1, new SupportBeanRange("R", "A", 4998, 5503), 4998);

            String eplIdx1Five = "on SupportBeanRange sbr select sum(intPrimitive) as sumi from MyWindow " +
                "where intPrimitive between rangeStart and rangeEnd " +
                "and longBoxed between rangeStart and rangeEnd";
            runOnDemandAssertion(env, path, eplIdx1Five, 1, new SupportBeanRange("R", "A", 4998, 5001), 4998 + 4999 + 5000);

            env.undeployAll();
        }
    }

    private static class InfraDeletePerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {

            String epl = "@name('create') create window MyWindow#keepall as select theString as a, intPrimitive as b from SupportBean;\n" +
                "on SupportBean_A delete from MyWindow where id = a;\n" +
                "insert into MyWindow select theString as a, intPrimitive as b from SupportBean;\n";
            env.compileDeploy(epl);

            // load window
            for (int i = 0; i < 50000; i++) {
                sendSupportBean(env, "S" + i, i);
            }

            // delete rows
            env.addListener("create");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                sendSupportBean_A(env, "S" + i);
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            assertTrue("Delta=" + delta, delta < 500);

            // assert they are deleted
            assertEquals(50000 - 10000, EPAssertionUtil.iteratorCount(env.iterator("create")));
            assertEquals(10000, env.listener("create").getOldDataList().size());

            env.undeployAll();
        }
    }

    private static class InfraDeletePerformanceCoercion implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindow#keepall as select theString as a, longPrimitive as b from SupportBean;\n" +
                "on SupportMarketDataBean delete from MyWindow where b = price;\n" +
                "insert into MyWindow select theString as a, longPrimitive as b from SupportBean;\n";
            env.compileDeploy(epl);

            // load window
            for (int i = 0; i < 50000; i++) {
                sendSupportBean(env, "S" + i, (long) i);
            }

            // delete rows
            env.addListener("create");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                sendMarketBean(env, "S" + i, i);
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            assertTrue("Delta=" + delta, delta < 500);

            // assert they are deleted
            assertEquals(50000 - 10000, EPAssertionUtil.iteratorCount(env.iterator("create")));
            assertEquals(10000, env.listener("create").getOldDataList().size());

            env.undeployAll();
        }
    }

    private static class InfraDeletePerformanceTwoDeleters implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('create') create window MyWindow#keepall as select theString as a, longPrimitive as b from SupportBean;\n" +
                "on SupportMarketDataBean delete from MyWindow where b = price;\n" +
                "on SupportBean_A delete from MyWindow where id = a;\n" +
                "insert into MyWindow select theString as a, longPrimitive as b from SupportBean;\n";
            env.compileDeploy(epl);

            // load window
            for (int i = 0; i < 20000; i++) {
                sendSupportBean(env, "S" + i, (long) i);
            }

            // delete all rows
            env.addListener("create");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                sendMarketBean(env, "S" + i, i);
                sendSupportBean_A(env, "S" + (i + 10000));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            assertTrue("Delta=" + delta, delta < 1500);

            // assert they are all deleted
            assertEquals(0, EPAssertionUtil.iteratorCount(env.iterator("create")));
            assertEquals(20000, env.listener("create").getOldDataList().size());

            env.undeployAll();
        }
    }

    private static class InfraDeletePerformanceIndexReuse implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // create window
            String stmtTextCreate = "@name('create') create window MyWindow#keepall as select theString as a, longPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextCreate, path);

            // create delete stmt
            String[] statements = new String[50];
            for (int i = 0; i < statements.length; i++) {
                String name = "s" + i;
                String stmtTextDelete = "@name('" + name + "') on SupportMarketDataBean delete from MyWindow where b = price";
                env.compileDeploy(stmtTextDelete, path);
                statements[i] = name;
            }

            // create insert into
            String stmtTextInsertOne = "insert into MyWindow select theString as a, longPrimitive as b from SupportBean";
            env.compileDeploy(stmtTextInsertOne, path);

            // load window
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                sendSupportBean(env, "S" + i, (long) i);
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            assertTrue("Delta=" + delta, delta < 1000);
            assertEquals(10000, EPAssertionUtil.iteratorCount(env.iterator("create")));

            // destroy all
            for (String statement : statements) {
                env.undeployModuleContaining(statement);
            }

            env.undeployAll();
        }
    }

    private static void runOnDemandAssertion(RegressionEnvironment env, RegressionPath path, String epl, int numIndexes, Object theEvent, Integer expected) {
        assertEquals(0, getIndexCount(env));

        env.compileDeploy("@name('s0')" + epl, path).addListener("s0");
        assertEquals(numIndexes, getIndexCount(env));

        long start = System.currentTimeMillis();
        int loops = 1000;

        for (int i = 0; i < loops; i++) {
            env.sendEventBean(theEvent);
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("sumi"));
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1000);

        env.undeployModuleContaining("s0");
        assertEquals(0, getIndexCount(env));
    }

    private static int getIndexCount(RegressionEnvironment env) {
        return SupportInfraUtil.getIndexCountNoContext(env, true, "create", "MyWindow");
    }

    private static void sendSupportBean_A(RegressionEnvironment env, String id) {
        SupportBean_A bean = new SupportBean_A(id);
        env.sendEventBean(bean);
    }

    private static void sendMarketBean(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, long longPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }
}
