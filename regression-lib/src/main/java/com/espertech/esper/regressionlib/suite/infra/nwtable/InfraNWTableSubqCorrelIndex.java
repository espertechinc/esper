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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.join.support.QueryPlanIndexDescSubquery;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanOne;
import com.espertech.esper.regressionlib.support.bean.SupportSimpleBeanTwo;
import com.espertech.esper.regressionlib.support.util.IndexAssertion;
import com.espertech.esper.regressionlib.support.util.IndexAssertionEventSend;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;
import com.espertech.esper.regressionlib.support.util.SupportQueryPlanIndexHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.espertech.esper.common.internal.util.CollectionUtil.appendArrayConditional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InfraNWTableSubqCorrelIndex implements IndexBackingTableInfo {
    private static final Logger log = LoggerFactory.getLogger(InfraNWTableSubqCorrelIndex.class);

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();

        // named window tests
        execs.add(new InfraNWTableSubqCorrelIndexAssertion(true, false, false, false, false)); // testNoShare
        execs.add(new InfraNWTableSubqCorrelIndexAssertion(true, false, false, false, true)); // testNoShareSetnoindex
        execs.add(new InfraNWTableSubqCorrelIndexAssertion(true, false, false, true, false)); // testNoShareCreate
        execs.add(new InfraNWTableSubqCorrelIndexAssertion(true, true, false, false, false)); // testShare

        execs.add(new InfraNWTableSubqCorrelIndexAssertion(true, true, false, true, false)); // testShareCreate
        execs.add(new InfraNWTableSubqCorrelIndexAssertion(true, true, false, true, true)); // testShareCreateSetnoindex
        execs.add(new InfraNWTableSubqCorrelIndexAssertion(true, true, true, false, false)); // testDisableShare
        execs.add(new InfraNWTableSubqCorrelIndexAssertion(true, true, true, true, false)); // testDisableShareCreate

        // table tests
        execs.add(new InfraNWTableSubqCorrelIndexAssertion(false, false, false, false, false)); // table no-index
        execs.add(new InfraNWTableSubqCorrelIndexAssertion(false, false, false, true, false)); // table yes-index

        execs.add(new InfraNWTableSubqCorrelIndexMultipleIndexHints(true));
        execs.add(new InfraNWTableSubqCorrelIndexMultipleIndexHints(false));

        execs.add(new InfraNWTableSubqCorrelIndexShareIndexChoice(true));
        execs.add(new InfraNWTableSubqCorrelIndexShareIndexChoice(false));

        execs.add(new InfraNWTableSubqCorrelIndexNoIndexShareIndexChoice(true));
        execs.add(new InfraNWTableSubqCorrelIndexNoIndexShareIndexChoice(false));

        execs.add(new InfraNWTableSubqIndexShareMultikeyWArraySingleArray(true));
        execs.add(new InfraNWTableSubqIndexShareMultikeyWArraySingleArray(false));

        execs.add(new InfraNWTableSubqIndexShareMultikeyWArrayTwoArray(true));
        execs.add(new InfraNWTableSubqIndexShareMultikeyWArrayTwoArray(false));

        return execs;
    }

    private static class InfraNWTableSubqIndexShareMultikeyWArraySingleArray implements RegressionExecution {
        private final boolean namedWindow;

        public InfraNWTableSubqIndexShareMultikeyWArraySingleArray(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String infra;
            RegressionPath path = new RegressionPath();
            if (namedWindow) {
                infra = "@Hint('enable_window_subquery_indexshare') create window MyInfra#keepall as (k string[], v int);\n" +
                    "create index MyInfraIndex on MyInfra(k);\n";
            } else {
                infra = "create table MyInfra(k string[] primary key, v int);\n";
            }
            env.compileDeploy(infra, path);

            insert(env, path, "{'a', 'b'}", 10);
            insert(env, path, "{'a', 'c'}", 20);
            insert(env, path, "{'a'}", 30);

            String epl = "@name('s0') select (select v from MyInfra as mi where mi.k = ma.stringOne) as v from SupportEventWithManyArray as ma";
            epl = namedWindow ? "@Hint('index(MyInfraIndex, bust)')" + epl : epl;
            env.compileDeploy(epl, path).addListener("s0");

            sendAssertManyArray(env, "a,c", 20);
            sendAssertManyArray(env, "a,b", 10);
            sendAssertManyArray(env, "a", 30);
            sendAssertManyArray(env, "a,d", null);

            env.undeployAll();
        }

        private void sendAssertManyArray(RegressionEnvironment env, String stringOne, Integer expected) {
            env.sendEventBean(new SupportEventWithManyArray("id").withStringOne(stringOne.split(",")));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("v"));
        }

        private void insert(RegressionEnvironment env, RegressionPath path, String k, int v) {
            env.compileExecuteFAF("insert into MyInfra(k,v) values (" + k + "," + v + ")", path);
        }
    }

    private static class InfraNWTableSubqIndexShareMultikeyWArrayTwoArray implements RegressionExecution {
        private final boolean namedWindow;

        public InfraNWTableSubqIndexShareMultikeyWArrayTwoArray(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String infra;
            RegressionPath path = new RegressionPath();
            if (namedWindow) {
                infra = "@Hint('enable_window_subquery_indexshare') create window MyInfra#keepall as (k1 string[], k2 string[], v int);\n" +
                    "create index MyInfraIndex on MyInfra(k1, k2);\n";
            } else {
                infra = "create table MyInfra(k1 string[] primary key, k2 string[] primary key, v int);\n";
            }
            env.compileDeploy(infra, path);

            insert(env, path, "{'a', 'b'}", "{'c', 'd'}", 10);
            insert(env, path, "{'a'}", "{'b'}", 20);
            insert(env, path, "{'a'}", "{'c', 'd'}", 30);

            String epl = "@name('s0') select (select v from MyInfra as mi where mi.k1 = ma.stringOne and mi.k2 = ma.stringTwo) as v from SupportEventWithManyArray as ma";
            epl = namedWindow ? "@Hint('index(MyInfraIndex, bust)')" + epl : epl;
            env.compileDeploy(epl, path).addListener("s0");

            sendAssertManyArray(env, "a", "b", 20);
            sendAssertManyArray(env, "a,b", "c,d", 10);
            sendAssertManyArray(env, "a", "c,d", 30);
            sendAssertManyArray(env, "a", "c", null);
            sendAssertManyArray(env, "a,b", "d,c", null);

            env.undeployAll();
        }

        private void sendAssertManyArray(RegressionEnvironment env, String stringOne, String stringTwo, Integer expected) {
            env.sendEventBean(new SupportEventWithManyArray("id").withStringOne(stringOne.split(",")).withStringTwo(stringTwo.split(",")));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("v"));
        }

        private void insert(RegressionEnvironment env, RegressionPath path, String k1, String k2, int v) {
            env.compileExecuteFAF("insert into MyInfra(k1,k2,v) values (" + k1 + "," + k2 + "," + v + ")", path);
        }
    }

    private static class InfraNWTableSubqCorrelIndexNoIndexShareIndexChoice implements RegressionExecution {
        private final boolean namedWindow;

        public InfraNWTableSubqCorrelIndexNoIndexShareIndexChoice(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String backingUniqueS1 = "unique hash={s1(string)} btree={} advanced={}";

            Object[] preloadedEventsOne = new Object[]{new SupportSimpleBeanOne("E1", 10, 11, 12), new SupportSimpleBeanOne("E2", 20, 21, 22)};
            IndexAssertionEventSend eventSendAssertion = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "s2,ssb1[0].s1,ssb1[0].i1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 50, 21, 22));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 20});
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 60, 11, 12));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10});
                }
            };
            IndexAssertionEventSend noAssertion = new IndexAssertionEventSend() {
                public void run() {
                }
            };

            // unique-s1
            assertIndexChoice(env, namedWindow, false, new String[0], preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", namedWindow ? null : "MyInfra", namedWindow ? BACKING_SINGLE_UNIQUE : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? null : "MyInfra", namedWindow ? BACKING_SINGLE_UNIQUE : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "i1 between 1 and 10", null, namedWindow ? BACKING_SORTED : null, noAssertion),
                    new IndexAssertion(null, "l1 = ssb2.l2", null, namedWindow ? BACKING_SINGLE_DUPS : null, eventSendAssertion),
                });

            // unique-s1+i1
            if (namedWindow) {
                assertIndexChoice(env, namedWindow, false, new String[0], preloadedEventsOne, "std:unique(s1, d1)",
                    new IndexAssertion[]{
                        new IndexAssertion(null, "s1 = ssb2.s2", null, BACKING_SINGLE_DUPS, eventSendAssertion),
                        new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", null, BACKING_MULTI_DUPS, eventSendAssertion),
                        new IndexAssertion(null, "s1 = ssb2.s2 and d1 = ssb2.d2", null, BACKING_MULTI_UNIQUE, eventSendAssertion),
                        new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2 and d1 = ssb2.d2", null, BACKING_MULTI_UNIQUE, eventSendAssertion),
                        new IndexAssertion(null, "d1 = ssb2.d2 and s1 = ssb2.s2 and l1 = ssb2.l2", null, BACKING_MULTI_UNIQUE, eventSendAssertion),
                        new IndexAssertion(null, "l1 = ssb2.l2 and s1 = ssb2.s2 and d1 = ssb2.d2", null, BACKING_MULTI_UNIQUE, eventSendAssertion),
                    });
            }
        }
    }

    private static class InfraNWTableSubqCorrelIndexShareIndexChoice implements RegressionExecution {

        private final boolean namedWindow;

        public InfraNWTableSubqCorrelIndexShareIndexChoice(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            String[] noindexes = new String[]{};
            String backingUniqueS1 = "unique hash={s1(string)} btree={} advanced={}";
            String backingUniqueS1L1 = "unique hash={s1(string),l1(long)} btree={} advanced={}";
            String backingUniqueS1D1 = "unique hash={s1(string),d1(double)} btree={} advanced={}";
            String backingNonUniqueS1 = "non-unique hash={s1(string)} btree={} advanced={}";
            String backingNonUniqueD1 = "non-unique hash={d1(double)} btree={} advanced={}";
            String backingBtreeI1 = "non-unique hash={} btree={i1(int)} advanced={}";
            String backingBtreeD1 = "non-unique hash={} btree={d1(double)} advanced={}";
            String primaryIndexTable = namedWindow ? "MyNWIndex" : "MyInfra";
            String primaryIndexEPL = "create unique index MyNWIndex on MyInfra(s1)";

            Object[] preloadedEventsOne = new Object[]{new SupportSimpleBeanOne("E1", 10, 11, 12), new SupportSimpleBeanOne("E2", 20, 21, 22)};
            IndexAssertionEventSend eventSendAssertion = new IndexAssertionEventSend() {
                public void run() {
                    String[] fields = "s2,ssb1[0].s1,ssb1[0].i1".split(",");
                    env.sendEventBean(new SupportSimpleBeanTwo("E2", 50, 21, 22));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 20});
                    env.sendEventBean(new SupportSimpleBeanTwo("E1", 60, 11, 12));
                    EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10});
                }
            };

            // no index one field (essentially duplicated since declared std:unique)
            String[] primaryIndex = appendArrayConditional(noindexes, namedWindow, primaryIndexEPL);
            assertIndexChoice(env, namedWindow, true, primaryIndex, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", primaryIndexTable, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", primaryIndexTable, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", primaryIndexTable, backingUniqueS1, eventSendAssertion),
                });

            // single index one field (essentially duplicated since declared std:unique)
            if (namedWindow) {
                String[] indexOneField = new String[]{"create unique index One on MyInfra (s1)"};
                assertIndexChoice(env, namedWindow, true, indexOneField, preloadedEventsOne, "std:unique(s1)",
                    new IndexAssertion[]{
                        new IndexAssertion(null, "s1 = ssb2.s2", "One", backingUniqueS1, eventSendAssertion),
                        new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1, eventSendAssertion),
                        new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1, eventSendAssertion),
                    });
            }

            // single index two field
            String secondaryEPL = "create unique index One on MyInfra (s1, l1)";
            String[] indexTwoField = appendArrayConditional(secondaryEPL, namedWindow, primaryIndexEPL);
            assertIndexChoice(env, namedWindow, true, indexTwoField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", primaryIndexTable, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1L1, eventSendAssertion),
                });

            // two index one unique with std:unique(s1)
            String[] indexSetTwo = new String[]{
                "create index One on MyInfra (s1)",
                "create unique index Two on MyInfra (s1, d1)"};
            assertIndexChoice(env, namedWindow, true, indexSetTwo, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "d1 = ssb2.d2", null, null, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingNonUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(Two,One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingNonUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(Two,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2"), // busted
                    new IndexAssertion("@Hint('index(explicit,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and d1 = ssb2.d2 and l1 = ssb2.l2", namedWindow ? "Two" : "MyInfra", namedWindow ? backingUniqueS1D1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(explicit,bust)')", "d1 = ssb2.d2 and l1 = ssb2.l2") // busted
                });

            // two index one unique with keep-all
            assertIndexChoice(env, namedWindow, true, indexSetTwo, preloadedEventsOne, "win:keepall()",
                new IndexAssertion[]{
                    new IndexAssertion(null, "d1 = ssb2.d2", null, null, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingNonUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(Two,One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingNonUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(Two,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2"), // busted
                    new IndexAssertion("@Hint('index(explicit,bust)')", "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? "One" : "MyInfra", namedWindow ? backingNonUniqueS1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and d1 = ssb2.d2 and l1 = ssb2.l2", namedWindow ? "Two" : "MyInfra", namedWindow ? backingUniqueS1D1 : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(explicit,bust)')", "d1 = ssb2.d2 and l1 = ssb2.l2") // busted
                });

            // range
            IndexAssertionEventSend noAssertion = new IndexAssertionEventSend() {
                public void run() {
                }
            };
            String[] indexSetThree = new String[]{
                "create index One on MyInfra (i1 btree)",
                "create index Two on MyInfra (d1 btree)"};
            assertIndexChoice(env, namedWindow, true, indexSetThree, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "i1 between 1 and 10", "One", backingBtreeI1, noAssertion),
                    new IndexAssertion(null, "d1 between 1 and 10", "Two", backingBtreeD1, noAssertion),
                    new IndexAssertion("@Hint('index(One, bust)')", "d1 between 1 and 10"), // busted
                });

            env.undeployAll();
        }
    }

    private static class InfraNWTableSubqCorrelIndexMultipleIndexHints implements RegressionExecution {

        private final boolean namedWindow;

        public InfraNWTableSubqCorrelIndexMultipleIndexHints(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplCreate = namedWindow ?
                "@Hint('enable_window_subquery_indexshare') create window MyInfraMIH#keepall as select * from SupportSimpleBeanOne" :
                "create table MyInfraMIH(s1 String primary key, i1 int  primary key, d1 double primary key, l1 long primary key)";
            env.compileDeploy(eplCreate, path);
            env.compileDeploy("create unique index I1 on MyInfraMIH (s1)", path);
            env.compileDeploy("create unique index I2 on MyInfraMIH (i1)", path);

            env.compileDeploy(INDEX_CALLBACK_HOOK +
                "@Hint('index(subquery(1), I1, bust)')\n" +
                "@Hint('index(subquery(0), I2, bust)')\n" +
                "select " +
                "(select * from MyInfraMIH where s1 = ssb2.s2 and i1 = ssb2.i2) as sub1," +
                "(select * from MyInfraMIH where i1 = ssb2.i2 and s1 = ssb2.s2) as sub2 " +
                "from SupportSimpleBeanTwo ssb2", path);
            List<QueryPlanIndexDescSubquery> subqueries = SupportQueryPlanIndexHook.getAndResetSubqueries();
            Collections.sort(subqueries, new Comparator<QueryPlanIndexDescSubquery>() {
                public int compare(QueryPlanIndexDescSubquery o1, QueryPlanIndexDescSubquery o2) {
                    return o1.getTables()[0].getIndexName().compareTo(o2.getTables()[0].getIndexName());
                }
            });
            SupportQueryPlanIndexHook.assertSubquery(subqueries.get(0), 1, "I1", "unique hash={s1(string)} btree={} advanced={}");
            SupportQueryPlanIndexHook.assertSubquery(subqueries.get(1), 0, "I2", "unique hash={i1(int)} btree={} advanced={}");

            env.undeployAll();
        }
    }

    private static void assertIndexChoice(RegressionEnvironment env, boolean namedWindow, boolean indexShare, String[] indexes, Object[] preloadedEvents, String datawindow,
                                          IndexAssertion[] assertions) {
        RegressionPath path = new RegressionPath();
        String epl = namedWindow ?
            "create window MyInfra." + datawindow + " as select * from SupportSimpleBeanOne" :
            "create table MyInfra(s1 string primary key, i1 int, d1 double, l1 long)";
        if (indexShare) {
            epl = "@Hint('enable_window_subquery_indexshare') " + epl;
        }
        env.compileDeploy(epl, path);
        env.compileDeploy("insert into MyInfra select * from SupportSimpleBeanOne", path);
        for (String index : indexes) {
            env.compileDeploy(index, path);
        }
        for (Object event : preloadedEvents) {
            env.sendEventBean(event);
        }

        int count = 0;
        for (IndexAssertion assertion : assertions) {
            log.info("======= Testing #" + count++);
            String consumeEpl = INDEX_CALLBACK_HOOK + "@name('s0') " +
                (assertion.getHint() == null ? "" : assertion.getHint()) + "select *, " +
                "(select * from MyInfra where " + assertion.getWhereClause() + ") @eventbean as ssb1 from SupportSimpleBeanTwo as ssb2";

            EPCompiled compiled;
            try {
                compiled = env.compileWCheckedEx(consumeEpl, path);
            } catch (EPCompileException ex) {
                if (assertion.getEventSendAssertion() == null) {
                    // no assertion, expected
                    assertTrue(ex.getMessage().contains("index hint busted"));
                    continue;
                }
                throw new RuntimeException("Unexpected statement exception: " + ex.getMessage(), ex);
            }
            env.deploy(compiled);

            // assert index and access
            SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(0, assertion.getExpectedIndexName(), assertion.getIndexBackingClass());
            env.addListener("s0");
            assertion.getEventSendAssertion().run();
            env.undeployModuleContaining("s0");
        }

        env.undeployAll();
    }

    private static class InfraNWTableSubqCorrelIndexAssertion implements RegressionExecution {
        private final boolean namedWindow;
        private final boolean enableIndexShareCreate;
        private final boolean disableIndexShareConsumer;
        private final boolean createExplicitIndex;
        private final boolean setNoindex;

        public InfraNWTableSubqCorrelIndexAssertion(boolean namedWindow, boolean enableIndexShareCreate, boolean disableIndexShareConsumer, boolean createExplicitIndex, boolean setNoindex) {
            this.namedWindow = namedWindow;
            this.enableIndexShareCreate = enableIndexShareCreate;
            this.disableIndexShareConsumer = disableIndexShareConsumer;
            this.createExplicitIndex = createExplicitIndex;
            this.setNoindex = setNoindex;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String createEpl = namedWindow ?
                "create window MyInfraNWT#unique(theString) as (theString string, intPrimitive int)" :
                "create table MyInfraNWT(theString string primary key, intPrimitive int)";
            if (enableIndexShareCreate) {
                createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
            }
            env.compileDeploy(createEpl, path);
            env.compileDeploy("insert into MyInfraNWT select theString, intPrimitive from SupportBean", path);

            if (createExplicitIndex) {
                env.compileDeploy("@name('index') create index MyIndex on MyInfraNWT (theString)", path);
            }

            String consumeEpl = "@name('s0') select status.*, (select * from MyInfraNWT where theString = SupportBean_S0.p00) @eventbean as details from SupportBean_S0 as status";
            if (disableIndexShareConsumer) {
                consumeEpl = "@Hint('disable_window_subquery_indexshare') " + consumeEpl;
            }
            if (setNoindex) {
                consumeEpl = "@Hint('set_noindex') " + consumeEpl;
            }
            env.compileDeploy(consumeEpl, path).addListener("s0");

            String[] fields = "id,details[0].theString,details[0].intPrimitive".split(",");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean("E3", 30));

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "E1", 10});

            env.sendEventBean(new SupportBean_S0(2, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "E2", 20});

            // test late start
            env.undeployModuleContaining("s0");
            env.compileDeploy(consumeEpl, path).addListener("s0");

            env.sendEventBean(new SupportBean_S0(1, "E1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, "E1", 10});

            env.sendEventBean(new SupportBean_S0(2, "E2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2, "E2", 20});

            env.undeployModuleContaining("s0");
            if (env.statement("index") != null) {
                env.undeployModuleContaining("index");
            }
            env.undeployAll();
        }
    }
}
