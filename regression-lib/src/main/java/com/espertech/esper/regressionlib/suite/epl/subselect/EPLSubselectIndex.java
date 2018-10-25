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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanOne;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanTwo;
import com.espertech.esper.regressionlib.support.util.IndexAssertionEventSend;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EPLSubselectIndex implements IndexBackingTableInfo {

    private final static int SUBQUERY_NUM_FIRST = 0;

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLSubselectIndexChoicesOverdefinedWhere());
        execs.add(new EPLSubselectUniqueIndexCorrelated());
        return execs;
    }

    private static class EPLSubselectIndexChoicesOverdefinedWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            AtomicInteger milestone = new AtomicInteger();

            // test no where clause with unique
            IndexAssertionEventSend assertNoWhere = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "c0,c1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 1, 2, 3));

                    env.milestoneInc(milestone);

                    env.sendEventBean(new SupportSimpleBeanOne("EX", 10, 11, 12));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"EX", "E1"});
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 1, 2, 3));

                    env.milestoneInc(milestone);

                    env.sendEventBean(new SupportSimpleBeanOne("EY", 10, 11, 12));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"EY", null});
                }
            };
            tryAssertion(env, false, "s2,i2", "", IndexBackingTableInfo.BACKING_UNINDEXED, assertNoWhere);

            // test no where clause with unique on multiple props, exact specification of where-clause
            IndexAssertionEventSend assertSendEvents = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "c0,c1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 1, 3, 10));
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 1, 2, 0));
                    env.sendEventBean(new SupportSimpleBeanTwo("E3", 1, 3, 9));

                    env.milestoneInc(milestone);

                    env.sendEventBean(new SupportSimpleBeanOne("EX", 1, 3, 9));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"EX", "E3"});
                }
            };
            tryAssertion(env, false, "d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", IndexBackingTableInfo.BACKING_MULTI_UNIQUE, assertSendEvents);
            tryAssertion(env, false, "d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", IndexBackingTableInfo.BACKING_MULTI_UNIQUE, assertSendEvents);
            tryAssertion(env, false, "d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", IndexBackingTableInfo.BACKING_MULTI_UNIQUE, assertSendEvents);
            tryAssertion(env, false, "d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1", IndexBackingTableInfo.BACKING_MULTI_DUPS, assertSendEvents);
            tryAssertion(env, false, "d2,i2", "where ssb2.d2 = ssb1.d1", IndexBackingTableInfo.BACKING_SINGLE_DUPS, assertSendEvents);
            tryAssertion(env, false, "d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", IndexBackingTableInfo.BACKING_MULTI_UNIQUE, assertSendEvents);
            tryAssertion(env, false, "d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", IndexBackingTableInfo.BACKING_COMPOSITE, assertSendEvents);
            tryAssertion(env, false, "i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1", IndexBackingTableInfo.BACKING_MULTI_DUPS, assertSendEvents);
            tryAssertion(env, false, "i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", IndexBackingTableInfo.BACKING_MULTI_UNIQUE, assertSendEvents);
            tryAssertion(env, false, "d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", IndexBackingTableInfo.BACKING_MULTI_UNIQUE, assertSendEvents);
            tryAssertion(env, false, "d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.s2 between 'E3' and 'E4'", IndexBackingTableInfo.BACKING_MULTI_UNIQUE, assertSendEvents);
            tryAssertion(env, false, "l2", "where ssb2.l2 = ssb1.l1", IndexBackingTableInfo.BACKING_SINGLE_UNIQUE, assertSendEvents);
            tryAssertion(env, true, "l2", "where ssb2.l2 = ssb1.l1", IndexBackingTableInfo.BACKING_SINGLE_DUPS, assertSendEvents);
            tryAssertion(env, false, "l2", "where ssb2.l2 = ssb1.l1 and ssb1.i1 between 1 and 20", IndexBackingTableInfo.BACKING_SINGLE_UNIQUE, assertSendEvents);

            // greater
            IndexAssertionEventSend assertGreater = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "c0,c1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 1));
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 2));

                    env.milestoneInc(milestone);

                    sendAssert(env, "A", 1, fields, new Object[]{"A", null});
                    sendAssert(env, "B", 2, fields, new Object[]{"B", "E1"});
                    sendAssert(env, "C", 3, fields, new Object[]{"C", null});
                    sendAssert(env, "D", 4, fields, new Object[]{"D", null});
                    sendAssert(env, "E", 5, fields, new Object[]{"E", null});
                }
            };
            tryAssertion(env, false, "s2", "where ssb1.i1 > ssb2.i2", BACKING_SORTED, assertGreater);

            // greater-equals
            IndexAssertionEventSend assertGreaterEquals = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "c0,c1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 2));
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 4));

                    env.milestoneInc(milestone);

                    sendAssert(env, "A", 1, fields, new Object[]{"A", null});
                    sendAssert(env, "B", 2, fields, new Object[]{"B", "E1"});
                    sendAssert(env, "C", 3, fields, new Object[]{"C", "E1"});
                    sendAssert(env, "D", 4, fields, new Object[]{"D", null});
                    sendAssert(env, "E", 5, fields, new Object[]{"E", null});
                }
            };
            tryAssertion(env, false, "s2", "where ssb1.i1 >= ssb2.i2", BACKING_SORTED, assertGreaterEquals);

            // less
            IndexAssertionEventSend assertLess = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "c0,c1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 2));
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 3));

                    env.milestoneInc(milestone);

                    sendAssert(env, "A", 1, fields, new Object[]{"A", null});
                    sendAssert(env, "B", 2, fields, new Object[]{"B", "E2"});
                    sendAssert(env, "C", 3, fields, new Object[]{"C", null});
                    sendAssert(env, "D", 4, fields, new Object[]{"D", null});
                    sendAssert(env, "E", 5, fields, new Object[]{"E", null});
                }
            };
            tryAssertion(env, false, "s2", "where ssb1.i1 < ssb2.i2", BACKING_SORTED, assertLess);

            // less-equals
            IndexAssertionEventSend assertLessEquals = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "c0,c1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 1));
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 3));

                    env.milestoneInc(milestone);

                    sendAssert(env, "A", 1, fields, new Object[]{"A", null});
                    sendAssert(env, "B", 2, fields, new Object[]{"B", "E2"});
                    sendAssert(env, "C", 3, fields, new Object[]{"C", "E2"});
                    sendAssert(env, "D", 4, fields, new Object[]{"D", null});
                    sendAssert(env, "E", 5, fields, new Object[]{"E", null});
                }
            };
            tryAssertion(env, false, "s2", "where ssb1.i1 <= ssb2.i2", BACKING_SORTED, assertLessEquals);
        }
    }

    private static class EPLSubselectUniqueIndexCorrelated implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            AtomicInteger milestone = new AtomicInteger();

            // test std:unique
            SupportQueryPlanIndexHook.reset();
            String eplUnique = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('s0') select id as c0, " +
                "(select intPrimitive from SupportBean#unique(theString) where theString = s0.p00) as c1 " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMile(eplUnique, "s0", milestone.getAndIncrement());

            SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(SUBQUERY_NUM_FIRST, null, IndexBackingTableInfo.BACKING_SINGLE_UNIQUE);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E1", 3));
            env.sendEventBean(new SupportBean("E2", 4));

            env.sendEventBean(new SupportBean_S0(10, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 4});

            env.sendEventBean(new SupportBean_S0(11, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11, 3});

            env.undeployAll();

            // test std:firstunique
            SupportQueryPlanIndexHook.reset();
            String eplFirstUnique = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('s0') select id as c0, " +
                "(select intPrimitive from SupportBean#firstunique(theString) where theString = s0.p00) as c1 " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMile(eplFirstUnique, "s0", milestone.getAndIncrement());

            SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(SUBQUERY_NUM_FIRST, null, IndexBackingTableInfo.BACKING_SINGLE_UNIQUE);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 2));
            env.sendEventBean(new SupportBean("E1", 3));
            env.sendEventBean(new SupportBean("E2", 4));

            env.sendEventBean(new SupportBean_S0(10, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 2});

            env.sendEventBean(new SupportBean_S0(11, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11, 1});

            env.undeployAll();

            // test intersection std:firstunique
            SupportQueryPlanIndexHook.reset();
            String eplIntersection = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('s0') select id as c0, " +
                "(select intPrimitive from SupportBean#time(1)#unique(theString) where theString = s0.p00) as c1 " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMile(eplIntersection, "s0", milestone.getAndIncrement());

            SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(SUBQUERY_NUM_FIRST, null, IndexBackingTableInfo.BACKING_SINGLE_UNIQUE);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E1", 2));
            env.sendEventBean(new SupportBean("E1", 3));
            env.sendEventBean(new SupportBean("E2", 4));

            env.sendEventBean(new SupportBean_S0(10, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 4});

            env.sendEventBean(new SupportBean_S0(11, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11, 3});

            env.undeployAll();

            // test grouped unique
            SupportQueryPlanIndexHook.reset();
            String eplGrouped = IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "@name('s0') select id as c0, " +
                "(select longPrimitive from SupportBean#groupwin(theString)#unique(intPrimitive) where theString = s0.p00 and intPrimitive = s0.id) as c1 " +
                "from SupportBean_S0 as s0";
            env.compileDeployAddListenerMile(eplGrouped, "s0", milestone.getAndIncrement());

            SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(SUBQUERY_NUM_FIRST, null, IndexBackingTableInfo.BACKING_MULTI_UNIQUE);

            env.sendEventBean(makeBean("E1", 1, 100));
            env.sendEventBean(makeBean("E1", 2, 101));
            env.sendEventBean(makeBean("E1", 1, 102));

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, 102L});

            env.undeployAll();
        }
    }

    private static void tryAssertion(RegressionEnvironment env, boolean disableImplicitUniqueIdx, String uniqueFields, String whereClause, String backingTable, IndexAssertionEventSend assertion) {
        SupportQueryPlanIndexHook.reset();
        String eplUnique = "@name('s0')" + IndexBackingTableInfo.INDEX_CALLBACK_HOOK + "select s1 as c0, " +
            "(select s2 from SupportSimpleBeanTwo#unique(" + uniqueFields + ") as ssb2 " + whereClause + ") as c1 " +
            "from SupportSimpleBeanOne as ssb1";
        if (disableImplicitUniqueIdx) {
            eplUnique = "@Hint('DISABLE_UNIQUE_IMPLICIT_IDX')" + eplUnique;
        }
        env.compileDeploy(eplUnique).addListener("s0");

        SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(SUBQUERY_NUM_FIRST, null, backingTable);

        assertion.run();

        env.undeployAll();
    }

    private static SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    private static void sendAssert(RegressionEnvironment env, String sbOneS1, int sbOneI1, String[] fields, Object[] expected) {
        env.sendEventBean(new SupportSimpleBeanOne(sbOneS1, sbOneI1));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, expected);
    }
}
