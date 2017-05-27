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
package com.espertech.esper.regression.nwtable.infra;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescSubquery;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexAssertion;
import com.espertech.esper.supportregression.util.IndexAssertionEventSend;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ExecNWTableInfraSubqCorrelIndex implements RegressionExecution, IndexBackingTableInfo {
    private static final Logger log = LoggerFactory.getLogger(ExecNWTableInfraSubqCorrelIndex.class);

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ABean", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SB2", SupportBeanTwo.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SSB1", SupportSimpleBeanOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SSB2", SupportSimpleBeanTwo.class);

        // named window tests
        runAssertion(epService, true, false, false, false, false); // testNoShare
        runAssertion(epService, true, false, false, false, true); // testNoShareSetnoindex
        runAssertion(epService, true, false, false, true, false); // testNoShareCreate
        runAssertion(epService, true, true, false, false, false); // testShare
        runAssertion(epService, true, true, false, true, false); // testShareCreate
        runAssertion(epService, true, true, false, true, true); // testShareCreateSetnoindex
        runAssertion(epService, true, true, true, false, false); // testDisableShare
        runAssertion(epService, true, true, true, true, false); // testDisableShareCreate

        // table tests
        runAssertion(epService, false, false, false, false, false); // table no-index
        runAssertion(epService, false, false, false, true, false); // table yes-index

        runAssertionMultipleIndexHints(epService, true);
        runAssertionMultipleIndexHints(epService, false);

        runAssertionIndexShareIndexChoice(epService, true);
        runAssertionIndexShareIndexChoice(epService, false);

        runAssertionNoIndexShareIndexChoice(epService, true);
        runAssertionNoIndexShareIndexChoice(epService, false);
    }

    private void runAssertionNoIndexShareIndexChoice(EPServiceProvider epService, boolean namedWindow) {

        String backingUniqueS1 = "unique hash={s1(string)} btree={} advanced={}";

        Object[] preloadedEventsOne = new Object[]{new SupportSimpleBeanOne("E1", 10, 11, 12), new SupportSimpleBeanOne("E2", 20, 21, 22)};
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        IndexAssertionEventSend eventSendAssertion = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "s2,ssb1[0].s1,ssb1[0].i1".split(",");
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E2", 50, 21, 22));
                EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 20});
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E1", 60, 11, 12));
                EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10});
            }
        };
        IndexAssertionEventSend noAssertion = new IndexAssertionEventSend() {
            public void run() {
            }
        };

        // unique-s1
        assertIndexChoice(epService, listenerStmtOne, namedWindow, false, new String[0], preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", namedWindow ? null : "MyInfra", namedWindow ? BACKING_SINGLE_UNIQUE : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", namedWindow ? null : "MyInfra", namedWindow ? BACKING_SINGLE_UNIQUE : backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "i1 between 1 and 10", null, namedWindow ? BACKING_SORTED : null, noAssertion),
                    new IndexAssertion(null, "l1 = ssb2.l2", null, namedWindow ? BACKING_SINGLE_DUPS : null, eventSendAssertion),
                });

        // unique-s1+i1
        if (namedWindow) {
            assertIndexChoice(epService, listenerStmtOne, namedWindow, false, new String[0], preloadedEventsOne, "std:unique(s1, d1)",
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

    private void runAssertionIndexShareIndexChoice(EPServiceProvider epService, boolean namedWindow) {

        String backingUniqueS1 = "unique hash={s1(string)} btree={} advanced={}";
        String backingUniqueS1L1 = "unique hash={s1(string),l1(long)} btree={} advanced={}";
        String backingUniqueS1D1 = "unique hash={s1(string),d1(double)} btree={} advanced={}";
        String backingNonUniqueS1 = "non-unique hash={s1(string)} btree={} advanced={}";
        String backingNonUniqueD1 = "non-unique hash={d1(double)} btree={} advanced={}";
        String backingBtreeI1 = "non-unique hash={} btree={i1(int)} advanced={}";
        String backingBtreeD1 = "non-unique hash={} btree={d1(double)} advanced={}";
        String primaryIndexTable = namedWindow ? null : "MyInfra";

        Object[] preloadedEventsOne = new Object[]{new SupportSimpleBeanOne("E1", 10, 11, 12), new SupportSimpleBeanOne("E2", 20, 21, 22)};
        SupportUpdateListener listener = new SupportUpdateListener();
        IndexAssertionEventSend eventSendAssertion = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "s2,ssb1[0].s1,ssb1[0].i1".split(",");
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E2", 50, 21, 22));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", "E2", 20});
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E1", 60, 11, 12));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", "E1", 10});
            }
        };

        // no index one field (essentially duplicated since declared std:unique)
        String[] noindexes = new String[]{};
        assertIndexChoice(epService, listener, namedWindow, true, noindexes, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", primaryIndexTable, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", primaryIndexTable, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", primaryIndexTable, backingUniqueS1, eventSendAssertion),
                });

        // single index one field (essentially duplicated since declared std:unique)
        if (namedWindow) {
            String[] indexOneField = new String[]{"create unique index One on MyInfra (s1)"};
            assertIndexChoice(epService, listener, namedWindow, true, indexOneField, preloadedEventsOne, "std:unique(s1)",
                    new IndexAssertion[]{
                        new IndexAssertion(null, "s1 = ssb2.s2", "One", backingUniqueS1, eventSendAssertion),
                        new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1, eventSendAssertion),
                        new IndexAssertion("@Hint('index(One)')", "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1, eventSendAssertion),
                    });
        }

        // single index two field
        String[] indexTwoField = new String[]{"create unique index One on MyInfra (s1, l1)"};
        assertIndexChoice(epService, listener, namedWindow, true, indexTwoField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "s1 = ssb2.s2", primaryIndexTable, backingUniqueS1, eventSendAssertion),
                    new IndexAssertion(null, "s1 = ssb2.s2 and l1 = ssb2.l2", "One", backingUniqueS1L1, eventSendAssertion),
                });

        // two index one unique with std:unique(s1)
        String[] indexSetTwo = new String[]{
            "create index One on MyInfra (s1)",
            "create unique index Two on MyInfra (s1, d1)"};
        assertIndexChoice(epService, listener, namedWindow, true, indexSetTwo, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "d1 = ssb2.d2", null, namedWindow ? backingNonUniqueD1 : null, eventSendAssertion),
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
        assertIndexChoice(epService, listener, namedWindow, true, indexSetTwo, preloadedEventsOne, "win:keepall()",
                new IndexAssertion[]{
                    new IndexAssertion(null, "d1 = ssb2.d2", null, namedWindow ? backingNonUniqueD1 : null, eventSendAssertion),
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
        assertIndexChoice(epService, listener, namedWindow, true, indexSetThree, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[]{
                    new IndexAssertion(null, "i1 between 1 and 10", "One", backingBtreeI1, noAssertion),
                    new IndexAssertion(null, "d1 between 1 and 10", "Two", backingBtreeD1, noAssertion),
                    new IndexAssertion("@Hint('index(One, bust)')", "d1 between 1 and 10"), // busted
                });

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionMultipleIndexHints(EPServiceProvider epService, boolean namedWindow) {
        String eplCreate = namedWindow ?
                "@Hint('enable_window_subquery_indexshare') create window MyInfraMIH#keepall as select * from SSB1" :
                "create table MyInfraMIH(s1 String primary key, i1 int  primary key, d1 double primary key, l1 long primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("create unique index I1 on MyInfraMIH (s1)");
        epService.getEPAdministrator().createEPL("create unique index I2 on MyInfraMIH (i1)");

        epService.getEPAdministrator().createEPL(INDEX_CALLBACK_HOOK +
                "@Hint('index(subquery(1), I1, bust)')\n" +
                "@Hint('index(subquery(0), I2, bust)')\n" +
                "select " +
                "(select * from MyInfraMIH where s1 = ssb2.s2 and i1 = ssb2.i2) as sub1," +
                "(select * from MyInfraMIH where i1 = ssb2.i2 and s1 = ssb2.s2) as sub2 " +
                "from SSB2 ssb2");
        List<QueryPlanIndexDescSubquery> subqueries = SupportQueryPlanIndexHook.getAndResetSubqueries();
        Collections.sort(subqueries, new Comparator<QueryPlanIndexDescSubquery>() {
            public int compare(QueryPlanIndexDescSubquery o1, QueryPlanIndexDescSubquery o2) {
                return o1.getTables()[0].getIndexName().compareTo(o2.getTables()[0].getIndexName());
            }
        });
        SupportQueryPlanIndexHook.assertSubquery(subqueries.get(0), 1, "I1", "unique hash={s1(string)} btree={} advanced={}");
        SupportQueryPlanIndexHook.assertSubquery(subqueries.get(1), 0, "I2", "unique hash={i1(int)} btree={} advanced={}");

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraMIH", false);
    }

    private void assertIndexChoice(EPServiceProvider epService, SupportUpdateListener listenerStmtOne, boolean namedWindow, boolean indexShare, String[] indexes, Object[] preloadedEvents, String datawindow,
                                   IndexAssertion[] assertions) {
        String epl = namedWindow ?
                "create window MyInfra." + datawindow + " as select * from SSB1" :
                "create table MyInfra(s1 string primary key, i1 int, d1 double, l1 long)";
        if (indexShare) {
            epl = "@Hint('enable_window_subquery_indexshare') " + epl;
        }
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPAdministrator().createEPL("insert into MyInfra select * from SSB1");
        for (String index : indexes) {
            epService.getEPAdministrator().createEPL(index);
        }
        for (Object event : preloadedEvents) {
            epService.getEPRuntime().sendEvent(event);
        }

        int count = 0;
        for (IndexAssertion assertion : assertions) {
            log.info("======= Testing #" + count++);
            String consumeEpl = INDEX_CALLBACK_HOOK +
                    (assertion.getHint() == null ? "" : assertion.getHint()) + "select *, " +
                    "(select * from MyInfra where " + assertion.getWhereClause() + ") @eventbean as ssb1 from SSB2 as ssb2";

            EPStatement consumeStmt;
            try {
                consumeStmt = epService.getEPAdministrator().createEPL(consumeEpl);
            } catch (EPStatementException ex) {
                if (assertion.getEventSendAssertion() == null) {
                    // no assertion, expected
                    assertTrue(ex.getMessage().contains("index hint busted"));
                    continue;
                }
                throw new RuntimeException("Unexpected statement exception: " + ex.getMessage(), ex);
            }

            // assert index and access
            SupportQueryPlanIndexHook.assertSubqueryBackingAndReset(0, assertion.getExpectedIndexName(), assertion.getIndexBackingClass());
            consumeStmt.addListener(listenerStmtOne);
            assertion.getEventSendAssertion().run();
            consumeStmt.destroy();
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertion(EPServiceProvider epService, boolean namedWindow, boolean enableIndexShareCreate, boolean disableIndexShareConsumer, boolean createExplicitIndex, boolean setNoindex) {
        String createEpl = namedWindow ?
                "create window MyInfraNWT#unique(theString) as (theString string, intPrimitive int)" :
                "create table MyInfraNWT(theString string primary key, intPrimitive int)";
        if (enableIndexShareCreate) {
            createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
        }
        epService.getEPAdministrator().createEPL(createEpl);
        epService.getEPAdministrator().createEPL("insert into MyInfraNWT select theString, intPrimitive from SupportBean");

        EPStatement stmtIndex = null;
        if (createExplicitIndex) {
            stmtIndex = epService.getEPAdministrator().createEPL("create index MyIndex on MyInfraNWT (theString)");
        }

        String consumeEpl = "select status.*, (select * from MyInfraNWT where theString = ABean.p00) @eventbean as details from ABean as status";
        if (disableIndexShareConsumer) {
            consumeEpl = "@Hint('disable_window_subquery_indexshare') " + consumeEpl;
        }
        if (setNoindex) {
            consumeEpl = "@Hint('set_noindex') " + consumeEpl;
        }
        EPStatement consumeStmt = epService.getEPAdministrator().createEPL(consumeEpl);
        SupportUpdateListener listener = new SupportUpdateListener();
        consumeStmt.addListener(listener);

        String[] fields = "id,details[0].theString,details[0].intPrimitive".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 30));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "E1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "E2", 20});

        // test late start
        consumeStmt.destroy();
        consumeStmt = epService.getEPAdministrator().createEPL(consumeEpl);
        consumeStmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "E1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "E2", 20});

        if (stmtIndex != null) {
            stmtIndex.destroy();
        }
        consumeStmt.destroy();

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfraNWT", false);
    }
}
