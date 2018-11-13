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
package com.espertech.esper.regressionlib.suite.epl.database;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategyHashForge;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategyInKeywordMultiForge;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategyInKeywordMultiForge;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategyInKeywordSingleForge;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescHistorical;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EPLDatabaseJoinPerfWithCache implements IndexBackingTableInfo {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDatabaseConstants());
        execs.add(new EPLDatabaseRangeIndex());
        execs.add(new EPLDatabaseKeyAndRangeIndex());
        execs.add(new EPLDatabaseSelectLargeResultSet());
        execs.add(new EPLDatabaseSelectLargeResultSetCoercion());
        execs.add(new EPLDatabase2StreamOuterJoin());
        execs.add(new EPLDatabaseOuterJoinPlusWhere());
        execs.add(new EPLDatabaseInKeywordSingleIndex());
        execs.add(new EPLDatabaseInKeywordMultiIndex());
        return execs;
    }

    private static class EPLDatabaseConstants implements RegressionExecution {

        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl;

            epl = "@name('s0') select * from SupportBean sbr, sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 where mycol3 = 951";
            tryAssertion(env, epl, "s1.mycol1", "951");

            epl = "@name('s0') select * from SupportBean sbr, sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 where mycol3 = 950 and mycol1 = '950'";
            tryAssertion(env, epl, "s1.mycol1", "950");

            epl = "@name('s0') select sum(s1.mycol3) as val from SupportBean sbr unidirectional, sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 where mycol3 between 950 and 953";
            tryAssertion(env, epl, "val", 950 + 951 + 952 + 953);

            epl = "@name('s0') select sum(s1.mycol3) as val from SupportBean sbr unidirectional, sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 where mycol1 = '950' and mycol3 between 950 and 953";
            tryAssertion(env, epl, "val", 950);
        }
    }

    private static class EPLDatabaseRangeIndex implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * from SupportBeanRange sbr, " +
                " sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 where mycol3 between rangeStart and rangeEnd";
            env.compileDeploy(stmtText).addListener("s0");

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R", 10, 12));
                assertEquals(3, env.listener("s0").getAndResetLastNewData().length);
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            // log.info("delta=" + delta);
            assertTrue("Delta=" + delta, delta < 500);

            // test coercion
            env.undeployAll();
            stmtText = "@name('s0') select * from SupportBeanRange sbr, " +
                " sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 where mycol3 between rangeStartLong and rangeEndLong";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(SupportBeanRange.makeLong("R", "K", 10L, 12L));
            assertEquals(3, env.listener("s0").getAndResetLastNewData().length);

            env.undeployAll();
        }
    }

    private static class EPLDatabaseKeyAndRangeIndex implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * from SupportBeanRange sbr, " +
                " sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 where mycol1 = key and mycol3 between rangeStart and rangeEnd";
            env.compileDeploy(stmtText).addListener("s0");

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBeanRange("R", "11", 10, 12));
                assertEquals(1, env.listener("s0").getAndResetLastNewData().length);
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            // log.info("delta=" + delta);
            assertTrue("Delta=" + delta, delta < 500);

            // test coercion
            env.undeployAll();
            stmtText = "@name('s0') select * from SupportBeanRange sbr, " +
                " sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 where mycol1 = key and mycol3 between rangeStartLong and rangeEndLong";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(SupportBeanRange.makeLong("R", "11", 10L, 12L));
            assertEquals(1, env.listener("s0").getAndResetLastNewData().length);

            env.undeployAll();
        }
    }

    /**
     * Test for selecting from a table a large result set and then joining the result outside of the cache.
     * Verifies performance of indexes cached for resolving join criteria fast.
     */
    private static class EPLDatabaseSelectLargeResultSet implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select id, mycol3, mycol2 from " +
                "SupportBean_S0#keepall as s0," +
                " sql:MyDBWithLRU100000 ['select mycol3, mycol2 from mytesttable_large'] as s1 where s0.id = s1.mycol3";
            env.compileDeploy(stmtText).addListener("s0");

            // Send 100 events which all perform the join
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 200; i++) {
                int num = i + 1;
                String col2 = Integer.toString(Math.round((float) num / 10));
                SupportBean_S0 bean = new SupportBean_S0(num);
                env.sendEventBean(bean);
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"id", "mycol3", "mycol2"}, new Object[]{num, num, col2});
            }
            long endTime = System.currentTimeMillis();

            // log.info("delta=" + (endTime - startTime));
            assertTrue(endTime - startTime < 500);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLDatabaseSelectLargeResultSetCoercion implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select theString, mycol3, mycol4 from " +
                " sql:MyDBWithLRU100000 ['select mycol3, mycol4 from mytesttable_large'] as s0, " +
                "SupportBean#keepall as s1 where s1.doubleBoxed = s0.mycol3 and s1.byteBoxed = s0.mycol4";
            env.compileDeploy(stmtText).addListener("s0");

            // Send events which all perform the join
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 200; i++) {
                SupportBean bean = new SupportBean();
                bean.setDoubleBoxed(100d);
                bean.setByteBoxed((byte) 10);
                bean.setTheString("E" + i);
                env.sendEventBean(bean);
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"theString", "mycol3", "mycol4"}, new Object[]{"E" + i, 100, 10});
            }
            long endTime = System.currentTimeMillis();

            // log.info("delta=" + (endTime - startTime));
            assertTrue(endTime - startTime < 500);

            env.undeployAll();
        }
    }

    private static class EPLDatabase2StreamOuterJoin implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select theString, mycol3, mycol1 from " +
                " sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 right outer join " +
                "SupportBean as s0 on theString = mycol1";
            env.compileDeploy(stmtText).addListener("s0");

            // Send events which all perform the join
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 200; i++) {
                SupportBean bean = new SupportBean();
                bean.setTheString("50");
                env.sendEventBean(bean);
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"theString", "mycol3", "mycol1"}, new Object[]{"50", 50, "50"});
            }
            long endTime = System.currentTimeMillis();

            // no matching
            SupportBean bean = new SupportBean();
            bean.setTheString("-1");
            env.sendEventBean(bean);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"theString", "mycol3", "mycol1"}, new Object[]{"-1", null, null});

            // log.info("delta=" + (endTime - startTime));
            assertTrue(endTime - startTime < 500);

            env.undeployAll();
        }
    }

    private static class EPLDatabaseOuterJoinPlusWhere implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select theString, mycol3, mycol1 from " +
                " sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 right outer join " +
                "SupportBean as s0 on theString = mycol1 where s1.mycol3 = s0.intPrimitive";
            env.compileDeploy(stmtText).addListener("s0");

            // Send events which all perform the join
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 200; i++) {
                SupportBean bean = new SupportBean();
                bean.setTheString("50");
                bean.setIntPrimitive(50);
                env.sendEventBean(bean);
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"theString", "mycol3", "mycol1"}, new Object[]{"50", 50, "50"});
            }
            long endTime = System.currentTimeMillis();

            // no matching on-clause
            SupportBean bean = new SupportBean();
            assertFalse(env.listener("s0").isInvoked());

            // matching on-clause not matching where
            bean = new SupportBean();
            bean.setTheString("50");
            bean.setIntPrimitive(49);
            env.sendEventBean(bean);
            assertFalse(env.listener("s0").isInvoked());

            // log.info("delta=" + (endTime - startTime));
            assertTrue(endTime - startTime < 500);

            env.undeployAll();
        }
    }

    private static class EPLDatabaseInKeywordSingleIndex implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select * from SupportBean_S0 s0, " +
                " sql:MyDBWithLRU100000 ['select mycol1, mycol3 from mytesttable_large'] as s1 " +
                " where mycol1 in (p00, p01, p02)";
            env.compileDeploy(stmtText).addListener("s0");

            QueryPlanIndexDescHistorical historical = SupportQueryPlanIndexHook.assertHistoricalAndReset();
            assertEquals(PollResultIndexingStrategyHashForge.class.getSimpleName(), historical.getIndexName());
            assertEquals(HistoricalIndexLookupStrategyInKeywordSingleForge.class.getSimpleName(), historical.getStrategyName());

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 2000; i++) {
                env.sendEventBean(new SupportBean_S0(i, "x", "y", "815"));
                assertEquals(815, env.listener("s0").assertOneGetNewAndReset().get("s1.mycol3"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            // log.info("delta=" + delta);
            assertTrue("Delta=" + delta, delta < 500);

            env.undeployAll();
        }
    }

    private static class EPLDatabaseInKeywordMultiIndex implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') " + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select * from SupportBean_S0 s0, " +
                " sql:MyDBWithLRU100000 ['select mycol1, mycol2, mycol3 from mytesttable_large'] as s1 " +
                " where p00 in (mycol2, mycol1)";
            env.compileDeploy(stmtText).addListener("s0");

            QueryPlanIndexDescHistorical historical = SupportQueryPlanIndexHook.assertHistoricalAndReset();
            assertEquals(PollResultIndexingStrategyInKeywordMultiForge.class.getSimpleName(), historical.getIndexName());
            assertEquals(HistoricalIndexLookupStrategyInKeywordMultiForge.class.getSimpleName(), historical.getStrategyName());

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 2000; i++) {
                env.sendEventBean(new SupportBean_S0(i, "815"));
                assertEquals(815, env.listener("s0").assertOneGetNewAndReset().get("s1.mycol3"));
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            // log.info("delta=" + delta);
            assertTrue("Delta=" + delta, delta < 500);

            env.undeployAll();
        }
    }

    private static void tryAssertion(RegressionEnvironment env, String epl, String field, Object expected) {
        env.compileDeploy(epl).addListener("s0");

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            env.sendEventBean(new SupportBean("E", 0));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get(field));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        //log.info("delta=" + delta);
        assertTrue("Delta=" + delta, delta < 500);

        env.undeployAll();
    }
}
