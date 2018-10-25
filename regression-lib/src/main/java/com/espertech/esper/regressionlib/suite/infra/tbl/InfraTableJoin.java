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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.*;
import com.espertech.esper.common.internal.epl.join.queryplan.LookupInstructionQueryPlanNodeForge;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupNodeForge;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlanForge;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRange;
import com.espertech.esper.regressionlib.support.bean.SupportBeanSimple;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.util.IndexAssertion;
import com.espertech.esper.regressionlib.support.util.IndexAssertionEventSend;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableJoin implements IndexBackingTableInfo {
    private final static Logger log = LoggerFactory.getLogger(InfraTableJoin.class);

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraFromClause());
        execs.add(new InfraJoinIndexChoice());
        execs.add(new InfraCoercion());
        execs.add(new InfraUnkeyedTable());
        execs.add(new InfraOuterJoin());
        return execs;
    }

    private static class InfraOuterJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString, p1".split(",");
            RegressionPath path = new RegressionPath();
            String epl = "create table MyTable as (p0 string primary key, p1 int);\n" +
                "@name('s0') select theString, p1 from SupportBean unidirectional left outer join MyTable on theString = p0;\n";
            env.compileDeploy(epl, path).addListener("s0");
            env.compileExecuteFAF("insert into MyTable select 'a' as p0, 10 as p1", path);

            env.sendEventBean(new SupportBean("a", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"a", 10});

            env.sendEventBean(new SupportBean("b", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"b", null});

            env.undeployAll();
        }
    }

    private static class InfraFromClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table varaggFC as (" +
                "key string primary key, total sum(int))", path);
            env.compileDeploy("into table varaggFC " +
                "select sum(intPrimitive) as total from SupportBean group by theString", path);
            env.compileDeploy("@name('s0') select total as value from SupportBean_S0 as s0, varaggFC as va " +
                "where va.key = s0.p00", path).addListener("s0");

            env.sendEventBean(new SupportBean("G1", 100));
            assertValues(env, "G1,G2", new Integer[]{100, null});

            env.sendEventBean(new SupportBean("G2", 200));
            assertValues(env, "G1,G2", new Integer[]{100, 200});

            env.undeployAll();
        }
    }

    private static class InfraJoinIndexChoice implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplDeclare = "create table varagg as (k0 string primary key, k1 int primary key, v1 string, total sum(long))";
            String eplPopulate = "into table varagg select sum(longPrimitive) as total from SupportBean group by theString, intPrimitive";
            String eplQuery = "select total as value from SupportBean_S0 as s0 unidirectional";

            String[] createIndexEmpty = new String[]{};
            Object[] preloadedEventsTwo = new Object[]{makeEvent("G1", 10, 1000L), makeEvent("G2", 20, 2000L),
                makeEvent("G3", 30, 3000L), makeEvent("G4", 40, 4000L)};
            AtomicInteger milestone = new AtomicInteger();

            IndexAssertionEventSend eventSendAssertionRangeTwoExpected = new IndexAssertionEventSend() {
                public void run() {
                    env.sendEventBean(new SupportBean_S0(-1, null));
                    EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getNewDataListFlattened(), "value".split(","),
                        new Object[][]{{2000L}, {3000L}});
                    env.listener("s0").reset();
                }
            };

            Object[] preloadedEventsHash = new Object[]{makeEvent("G1", 10, 1000L)};
            IndexAssertionEventSend eventSendAssertionHash = new IndexAssertionEventSend() {
                public void run() {
                    env.sendEventBean(new SupportBean_S0(10, "G1"));
                    EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), "value".split(","),
                        new Object[][]{{1000L}});
                    env.listener("s0").reset();
                }
            };

            // no secondary indexes
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexEmpty, preloadedEventsHash, milestone,
                new IndexAssertion[]{
                    // primary index found
                    new IndexAssertion("k1 = id and k0 = p00", "varagg", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    new IndexAssertion("k0 = p00 and k1 = id", "varagg", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    new IndexAssertion("k0 = p00 and k1 = id and v1 is null", "varagg", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    // no index found
                    new IndexAssertion("k1 = id", "varagg", FullTableScanUniquePerKeyLookupPlanForge.class, eventSendAssertionHash)
                }
            );

            // one secondary hash index on single field
            String[] createIndexHashSingleK1 = new String[]{"create index idx_k1 on varagg (k1)"};
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexHashSingleK1, preloadedEventsHash,
                milestone, new IndexAssertion[]{
                    // primary index found
                    new IndexAssertion("k1 = id and k0 = p00", "varagg", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    // secondary index found
                    new IndexAssertion("k1 = id", "idx_k1", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    new IndexAssertion("id = k1", "idx_k1", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    // no index found
                    new IndexAssertion("k0 = p00", "varagg", FullTableScanUniquePerKeyLookupPlanForge.class, eventSendAssertionHash)
                }
            );

            // two secondary hash indexes on one field each
            String[] createIndexHashTwoDiscrete = new String[]{"create index idx_k1 on varagg (k1)", "create index idx_k0 on varagg (k0)"};
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexHashTwoDiscrete, preloadedEventsHash,
                milestone, new IndexAssertion[]{
                    // primary index found
                    new IndexAssertion("k1 = id and k0 = p00", "varagg", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    // secondary index found
                    new IndexAssertion("k0 = p00", "idx_k0", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    new IndexAssertion("k1 = id", "idx_k1", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    new IndexAssertion("v1 is null and k1 = id", "idx_k1", IndexedTableLookupPlanHashedOnlyForge.class, eventSendAssertionHash),
                    // no index found
                    new IndexAssertion("1=1", "varagg", FullTableScanUniquePerKeyLookupPlanForge.class, eventSendAssertionHash)
                }
            );

            // one range secondary index
            // no secondary indexes
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexEmpty, preloadedEventsTwo,
                milestone, new IndexAssertion[]{
                    // no index found
                    new IndexAssertion("k1 between 20 and 30", "varagg", FullTableScanUniquePerKeyLookupPlanForge.class, eventSendAssertionRangeTwoExpected)
                }
            );

            // single range secondary index, expecting two events
            String[] createIndexRangeOne = new String[]{"create index b_k1 on varagg (k1 btree)"};
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexRangeOne, preloadedEventsTwo,
                milestone, new IndexAssertion[]{
                    new IndexAssertion("k1 between 20 and 30", "b_k1", SortedTableLookupPlanForge.class, eventSendAssertionRangeTwoExpected),
                    new IndexAssertion("(k0 = 'G3' or k0 = 'G2') and k1 between 20 and 30", "b_k1", SortedTableLookupPlanForge.class, eventSendAssertionRangeTwoExpected),
                }
            );

            // single range secondary index, expecting single event
            IndexAssertionEventSend eventSendAssertionRangeOneExpected = new IndexAssertionEventSend() {
                public void run() {
                    env.sendEventBean(new SupportBean_S0(-1, null));
                    EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getNewDataListFlattened(), "value".split(","),
                        new Object[][]{{2000L}});
                    env.listener("s0").reset();
                }
            };
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexRangeOne, preloadedEventsTwo,
                milestone, new IndexAssertion[]{
                    new IndexAssertion("k0 = 'G2' and k1 between 20 and 30", "b_k1", SortedTableLookupPlanForge.class, eventSendAssertionRangeOneExpected),
                    new IndexAssertion("k1 between 20 and 30 and k0 = 'G2'", "b_k1", SortedTableLookupPlanForge.class, eventSendAssertionRangeOneExpected),
                }
            );

            // combined hash+range index
            String[] createIndexRangeCombined = new String[]{"create index h_k0_b_k1 on varagg (k0 hash, k1 btree)"};
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexRangeCombined, preloadedEventsTwo,
                milestone, new IndexAssertion[]{
                    new IndexAssertion("k0 = 'G2' and k1 between 20 and 30", "h_k0_b_k1", CompositeTableLookupPlanForge.class, eventSendAssertionRangeOneExpected),
                    new IndexAssertion("k1 between 20 and 30 and k0 = 'G2'", "h_k0_b_k1", CompositeTableLookupPlanForge.class, eventSendAssertionRangeOneExpected),
                }
            );

            String[] createIndexHashSingleK0 = new String[]{"create index idx_k0 on varagg (k0)"};
            // in-keyword single-directional use
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexHashSingleK0, preloadedEventsTwo,
                milestone, new IndexAssertion[]{
                    new IndexAssertion("k0 in ('G2', 'G3')", "idx_k0", InKeywordTableLookupPlanSingleIdxForge.class, eventSendAssertionRangeTwoExpected),
                }
            );
            // in-keyword multi-directional use
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexHashSingleK0, preloadedEventsHash,
                milestone, new IndexAssertion[]{
                    new IndexAssertion("'G1' in (k0)", "varagg", FullTableScanUniquePerKeyLookupPlanForge.class, eventSendAssertionHash),
                }
            );
        }
    }

    private static class InfraCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String eplDeclare = "create table varagg as (k0 int primary key, total sum(long))";
            String eplPopulate = "into table varagg select sum(longPrimitive) as total from SupportBean group by intPrimitive";
            String eplQuery = "select total as value from SupportBeanRange unidirectional";

            String[] createIndexEmpty = new String[]{};
            Object[] preloadedEvents = new Object[]{makeEvent("G1", 10, 1000L), makeEvent("G2", 20, 2000L),
                makeEvent("G3", 30, 3000L), makeEvent("G4", 40, 4000L)};
            AtomicInteger milestone = new AtomicInteger();

            IndexAssertionEventSend eventSendAssertion = new IndexAssertionEventSend() {
                public void run() {
                    env.sendEventBean(new SupportBeanRange(20L));
                    EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getNewDataListFlattened(), "value".split(","),
                        new Object[][]{{2000L}});
                    env.listener("s0").reset();
                }
            };
            assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, createIndexEmpty, preloadedEvents,
                milestone, new IndexAssertion[]{
                    new IndexAssertion("k0 = keyLong", "varagg", FullTableScanUniquePerKeyLookupPlanForge.class, eventSendAssertion),
                    new IndexAssertion("k0 = keyLong", "varagg", FullTableScanUniquePerKeyLookupPlanForge.class, eventSendAssertion),
                }
            );
        }
    }

    private static class InfraUnkeyedTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            // Prepare
            env.compileDeploy("create table MyTable (sumint sum(int))", path);
            env.compileDeploy("@name('into') into table MyTable select sum(intPrimitive) as sumint from SupportBean", path);
            env.sendEventBean(new SupportBean("E1", 100));
            env.sendEventBean(new SupportBean("E2", 101));
            env.undeployModuleContaining("into");

            // join simple
            env.compileDeploy("@name('join') select sumint from MyTable, SupportBean", path).addListener("join");
            env.sendEventBean(new SupportBean());
            assertEquals(201, env.listener("join").assertOneGetNewAndReset().get("sumint"));
            env.undeployModuleContaining("join");

            // test regular columns inserted-into
            env.compileDeploy("create table SecondTable (a string, b int)", path);
            env.compileExecuteFAF("insert into SecondTable values ('a1', 10)", path);
            env.compileDeploy("@name('s0')select a, b from SecondTable, SupportBean", path).addListener("s0");
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "a,b".split(","), new Object[]{"a1", 10});

            env.undeployAll();
        }
    }

    private static void assertIndexChoice(RegressionEnvironment env, String eplDeclare, String eplPopulate, String eplQuery,
                                          String[] indexes, Object[] preloadedEvents,
                                          AtomicInteger milestone, IndexAssertion[] assertions) {
        assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, indexes, preloadedEvents, assertions, milestone, false);
        assertIndexChoice(env, eplDeclare, eplPopulate, eplQuery, indexes, preloadedEvents, assertions, milestone, true);
    }

    private static void assertIndexChoice(RegressionEnvironment env, String eplDeclare, String eplPopulate, String eplQuery,
                                          String[] indexes, Object[] preloadedEvents,
                                          IndexAssertion[] assertions, AtomicInteger milestone, boolean multistream) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(eplDeclare, path);
        env.compileDeploy(eplPopulate, path);

        for (String index : indexes) {
            env.compileDeploy(index, path);
        }
        for (Object event : preloadedEvents) {
            env.sendEventBean(event);
        }

        env.milestoneInc(milestone);

        int count = -1;
        for (IndexAssertion assertion : assertions) {
            count++;
            log.info("======= Testing #" + count++);
            String epl = INDEX_CALLBACK_HOOK + (assertion.getHint() == null ? "" : assertion.getHint()) + eplQuery;
            epl += ", varagg as va";
            if (multistream) {
                epl += ", SupportBeanSimple#lastevent";
            }
            epl += " where " + assertion.getWhereClause();

            try {
                EPCompiled compiled = env.compileWCheckedEx("@name('s0')" + epl, path);
                env.deploy(compiled).addListener("s0");
            } catch (EPCompileException ex) {
                if (assertion.getEventSendAssertion() == null) {
                    // no assertion, expected
                    assertTrue(ex.getMessage().contains("index hint busted"));
                    continue;
                }
                throw new RuntimeException("Unexpected statement exception: " + ex.getMessage(), ex);
            }

            // send multistream seed event
            env.sendEventBean(new SupportBeanSimple("", -1));

            // assert index and access
            assertion.getEventSendAssertion().run();
            QueryPlanForge plan = SupportQueryPlanIndexHook.assertJoinAndReset();

            TableLookupPlanForge tableLookupPlan;
            if (plan.getExecNodeSpecs()[0] instanceof TableLookupNodeForge) {
                tableLookupPlan = ((TableLookupNodeForge) plan.getExecNodeSpecs()[0]).getTableLookupPlan();
            } else {
                LookupInstructionQueryPlanNodeForge lqp = (LookupInstructionQueryPlanNodeForge) plan.getExecNodeSpecs()[0];
                tableLookupPlan = lqp.getLookupInstructions().get(0).getLookupPlans()[0];
            }
            assertEquals(assertion.getExpectedIndexName(), tableLookupPlan.getIndexNum()[0].getIndexName());
            assertEquals(assertion.getExpectedStrategy(), tableLookupPlan.getClass());
            env.undeployModuleContaining("s0");
        }

        env.undeployAll();
    }

    private static void assertValues(RegressionEnvironment env, String keys, Integer[] values) {
        String[] keyarr = keys.split(",");
        for (int i = 0; i < keyarr.length; i++) {
            env.sendEventBean(new SupportBean_S0(0, keyarr[i]));
            if (values[i] == null) {
                assertFalse(env.listener("s0").isInvoked());
            } else {
                EventBean event = env.listener("s0").assertOneGetNewAndReset();
                assertEquals("Failed for key '" + keyarr[i] + "'", values[i], event.get("value"));
            }
        }
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }
}
