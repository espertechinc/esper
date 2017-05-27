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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.epl.join.plan.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanRange;
import com.espertech.esper.supportregression.bean.SupportBeanSimple;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.IndexAssertion;
import com.espertech.esper.supportregression.util.IndexAssertionEventSend;
import com.espertech.esper.supportregression.util.IndexBackingTableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class ExecTableJoin implements RegressionExecution, IndexBackingTableInfo {
    private final static Logger log = LoggerFactory.getLogger(ExecTableJoin.class);

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBeanSimple.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionFromClause(epService);
        runAssertionJoinIndexChoice(epService);
        runAssertionCoercion(epService);
        runAssertionUnkeyedTable(epService);
    }

    private void runAssertionFromClause(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create table varaggFC as (" +
                "key string primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("into table varaggFC " +
                "select sum(intPrimitive) as total from SupportBean group by theString");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select total as value from SupportBean_S0 as s0, varaggFC as va " +
                "where va.key = s0.p00").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 100));
        assertValues(epService, listener, "G1,G2", new Integer[]{100, null});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 200));
        assertValues(epService, listener, "G1,G2", new Integer[]{100, 200});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionJoinIndexChoice(EPServiceProvider epService) {

        String eplDeclare = "create table varagg as (k0 string primary key, k1 int primary key, v1 string, total sum(long))";
        String eplPopulate = "into table varagg select sum(longPrimitive) as total from SupportBean group by theString, intPrimitive";
        String eplQuery = "select total as value from SupportBean_S0 as s0 unidirectional";

        String[] createIndexEmpty = new String[]{};
        Object[] preloadedEventsTwo = new Object[]{makeEvent("G1", 10, 1000L), makeEvent("G2", 20, 2000L),
                makeEvent("G3", 30, 3000L), makeEvent("G4", 40, 4000L)};
        SupportUpdateListener listener = new SupportUpdateListener();

        IndexAssertionEventSend eventSendAssertionRangeTwoExpected = new IndexAssertionEventSend() {
            public void run() {
                epService.getEPRuntime().sendEvent(new SupportBean_S0(-1, null));
                EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getNewDataListFlattened(), "value".split(","),
                        new Object[][]{{2000L}, {3000L}});
                listener.reset();
            }
        };

        Object[] preloadedEventsHash = new Object[]{makeEvent("G1", 10, 1000L)};
        IndexAssertionEventSend eventSendAssertionHash = new IndexAssertionEventSend() {
            public void run() {
                epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "G1"));
                EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), "value".split(","),
                        new Object[][]{{1000L}});
                listener.reset();
            }
        };

        // no secondary indexes
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexEmpty, preloadedEventsHash,
                new IndexAssertion[]{
                    // primary index found
                    new IndexAssertion("k1 = id and k0 = p00", "varagg", IndexedTableLookupPlanMulti.class, eventSendAssertionHash),
                    new IndexAssertion("k0 = p00 and k1 = id", "varagg", IndexedTableLookupPlanMulti.class, eventSendAssertionHash),
                    new IndexAssertion("k0 = p00 and k1 = id and v1 is null", "varagg", IndexedTableLookupPlanMulti.class, eventSendAssertionHash),
                    // no index found
                    new IndexAssertion("k1 = id", "varagg", FullTableScanUniquePerKeyLookupPlan.class, eventSendAssertionHash)
                }
        );

        // one secondary hash index on single field
        String[] createIndexHashSingleK1 = new String[]{"create index idx_k1 on varagg (k1)"};
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexHashSingleK1, preloadedEventsHash,
                new IndexAssertion[]{
                    // primary index found
                    new IndexAssertion("k1 = id and k0 = p00", "varagg", IndexedTableLookupPlanMulti.class, eventSendAssertionHash),
                    // secondary index found
                    new IndexAssertion("k1 = id", "idx_k1", IndexedTableLookupPlanSingle.class, eventSendAssertionHash),
                    new IndexAssertion("id = k1", "idx_k1", IndexedTableLookupPlanSingle.class, eventSendAssertionHash),
                    // no index found
                    new IndexAssertion("k0 = p00", "varagg", FullTableScanUniquePerKeyLookupPlan.class, eventSendAssertionHash)
                }
        );

        // two secondary hash indexes on one field each
        String[] createIndexHashTwoDiscrete = new String[]{"create index idx_k1 on varagg (k1)", "create index idx_k0 on varagg (k0)"};
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexHashTwoDiscrete, preloadedEventsHash,
                new IndexAssertion[]{
                    // primary index found
                    new IndexAssertion("k1 = id and k0 = p00", "varagg", IndexedTableLookupPlanMulti.class, eventSendAssertionHash),
                    // secondary index found
                    new IndexAssertion("k0 = p00", "idx_k0", IndexedTableLookupPlanSingle.class, eventSendAssertionHash),
                    new IndexAssertion("k1 = id", "idx_k1", IndexedTableLookupPlanSingle.class, eventSendAssertionHash),
                    new IndexAssertion("v1 is null and k1 = id", "idx_k1", IndexedTableLookupPlanSingle.class, eventSendAssertionHash),
                    // no index found
                    new IndexAssertion("1=1", "varagg", FullTableScanUniquePerKeyLookupPlan.class, eventSendAssertionHash)
                }
        );

        // one range secondary index
        // no secondary indexes
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexEmpty, preloadedEventsTwo,
                new IndexAssertion[]{
                    // no index found
                    new IndexAssertion("k1 between 20 and 30", "varagg", FullTableScanUniquePerKeyLookupPlan.class, eventSendAssertionRangeTwoExpected)
                }
        );

        // single range secondary index, expecting two events
        String[] createIndexRangeOne = new String[]{"create index b_k1 on varagg (k1 btree)"};
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexRangeOne, preloadedEventsTwo,
                new IndexAssertion[]{
                    new IndexAssertion("k1 between 20 and 30", "b_k1", SortedTableLookupPlan.class, eventSendAssertionRangeTwoExpected),
                    new IndexAssertion("(k0 = 'G3' or k0 = 'G2') and k1 between 20 and 30", "b_k1", SortedTableLookupPlan.class, eventSendAssertionRangeTwoExpected),
                }
        );

        // single range secondary index, expecting single event
        IndexAssertionEventSend eventSendAssertionRangeOneExpected = new IndexAssertionEventSend() {
            public void run() {
                epService.getEPRuntime().sendEvent(new SupportBean_S0(-1, null));
                EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getNewDataListFlattened(), "value".split(","),
                    new Object[][]{{2000L}});
                listener.reset();
            }
        };
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexRangeOne, preloadedEventsTwo,
                new IndexAssertion[]{
                    new IndexAssertion("k0 = 'G2' and k1 between 20 and 30", "b_k1", SortedTableLookupPlan.class, eventSendAssertionRangeOneExpected),
                    new IndexAssertion("k1 between 20 and 30 and k0 = 'G2'", "b_k1", SortedTableLookupPlan.class, eventSendAssertionRangeOneExpected),
                }
        );

        // combined hash+range index
        String[] createIndexRangeCombined = new String[]{"create index h_k0_b_k1 on varagg (k0 hash, k1 btree)"};
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexRangeCombined, preloadedEventsTwo,
                new IndexAssertion[]{
                    new IndexAssertion("k0 = 'G2' and k1 between 20 and 30", "h_k0_b_k1", CompositeTableLookupPlan.class, eventSendAssertionRangeOneExpected),
                    new IndexAssertion("k1 between 20 and 30 and k0 = 'G2'", "h_k0_b_k1", CompositeTableLookupPlan.class, eventSendAssertionRangeOneExpected),
                }
        );

        String[] createIndexHashSingleK0 = new String[]{"create index idx_k0 on varagg (k0)"};
        // in-keyword single-directional use
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexHashSingleK0, preloadedEventsTwo,
                new IndexAssertion[]{
                    new IndexAssertion("k0 in ('G2', 'G3')", "idx_k0", InKeywordTableLookupPlanSingleIdx.class, eventSendAssertionRangeTwoExpected),
                }
        );
        // in-keyword multi-directional use
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexHashSingleK0, preloadedEventsHash,
                new IndexAssertion[]{
                    new IndexAssertion("'G1' in (k0)", "varagg", FullTableScanUniquePerKeyLookupPlan.class, eventSendAssertionHash),
                }
        );

        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varagg__public", false);
    }

    private void runAssertionCoercion(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanRange.class);
        String eplDeclare = "create table varagg as (k0 int primary key, total sum(long))";
        String eplPopulate = "into table varagg select sum(longPrimitive) as total from SupportBean group by intPrimitive";
        String eplQuery = "select total as value from SupportBeanRange unidirectional";

        String[] createIndexEmpty = new String[]{};
        Object[] preloadedEvents = new Object[]{makeEvent("G1", 10, 1000L), makeEvent("G2", 20, 2000L),
                makeEvent("G3", 30, 3000L), makeEvent("G4", 40, 4000L)};
        SupportUpdateListener listener = new SupportUpdateListener();

        IndexAssertionEventSend eventSendAssertion = new IndexAssertionEventSend() {
            public void run() {
                epService.getEPRuntime().sendEvent(new SupportBeanRange(20L));
                EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getNewDataListFlattened(), "value".split(","),
                        new Object[][]{{2000L}});
                listener.reset();
            }
        };
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, createIndexEmpty, preloadedEvents,
                new IndexAssertion[]{
                    new IndexAssertion("k0 = keyLong", "varagg", FullTableScanUniquePerKeyLookupPlan.class, eventSendAssertion),
                    new IndexAssertion("k0 = keyLong", "varagg", FullTableScanUniquePerKeyLookupPlan.class, eventSendAssertion),
                }
        );
    }

    private void runAssertionUnkeyedTable(EPServiceProvider epService) {
        // Prepare
        epService.getEPAdministrator().createEPL("create table MyTable (sumint sum(int))");
        epService.getEPAdministrator().createEPL("@name('into') into table MyTable select sum(intPrimitive) as sumint from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 101));
        epService.getEPAdministrator().getStatement("into").destroy();

        // join simple
        EPStatement stmtJoinOne = epService.getEPAdministrator().createEPL("select sumint from MyTable, SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtJoinOne.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(201, listener.assertOneGetNewAndReset().get("sumint"));
        stmtJoinOne.destroy();

        // test regular columns inserted-into
        epService.getEPAdministrator().createEPL("create table SecondTable (a string, b int)");
        epService.getEPRuntime().executeQuery("insert into SecondTable values ('a1', 10)");
        EPStatement stmtJoinTwo = epService.getEPAdministrator().createEPL("select a, b from SecondTable, SupportBean");
        stmtJoinTwo.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a,b".split(","), new Object[]{"a1", 10});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertIndexChoice(EPServiceProvider epService, SupportUpdateListener listener, String eplDeclare, String eplPopulate, String eplQuery,
                                   String[] indexes, Object[] preloadedEvents,
                                   IndexAssertion[] assertions) {
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, indexes, preloadedEvents, assertions, false);
        assertIndexChoice(epService, listener, eplDeclare, eplPopulate, eplQuery, indexes, preloadedEvents, assertions, true);
    }

    private void assertIndexChoice(EPServiceProvider epService, SupportUpdateListener listener, String eplDeclare, String eplPopulate, String eplQuery,
                                   String[] indexes, Object[] preloadedEvents,
                                   IndexAssertion[] assertions, boolean multistream) {

        epService.getEPAdministrator().createEPL(eplDeclare);
        epService.getEPAdministrator().createEPL(eplPopulate);

        for (String index : indexes) {
            epService.getEPAdministrator().createEPL(index);
        }
        for (Object event : preloadedEvents) {
            epService.getEPRuntime().sendEvent(event);
        }

        int count = 0;
        for (IndexAssertion assertion : assertions) {
            log.info("======= Testing #" + count++);
            String epl = INDEX_CALLBACK_HOOK + (assertion.getHint() == null ? "" : assertion.getHint()) + eplQuery;
            epl += ", varagg as va";
            if (multistream) {
                epl += ", SupportBeanSimple#lastevent";
            }
            epl += " where " + assertion.getWhereClause();

            EPStatement stmt;
            try {
                stmt = epService.getEPAdministrator().createEPL(epl);
                stmt.addListener(listener);
            } catch (EPStatementException ex) {
                if (assertion.getEventSendAssertion() == null) {
                    // no assertion, expected
                    assertTrue(ex.getMessage().contains("index hint busted"));
                    continue;
                }
                throw new RuntimeException("Unexpected statement exception: " + ex.getMessage(), ex);
            }

            // send multistream seed event
            epService.getEPRuntime().sendEvent(new SupportBeanSimple("", -1));

            // assert index and access
            assertion.getEventSendAssertion().run();
            QueryPlan plan = SupportQueryPlanIndexHook.assertJoinAndReset();

            TableLookupPlan tableLookupPlan;
            if (plan.getExecNodeSpecs()[0] instanceof TableLookupNode) {
                tableLookupPlan = ((TableLookupNode) plan.getExecNodeSpecs()[0]).getTableLookupPlan();
            } else {
                LookupInstructionQueryPlanNode lqp = (LookupInstructionQueryPlanNode) plan.getExecNodeSpecs()[0];
                tableLookupPlan = lqp.getLookupInstructions().get(0).getLookupPlans()[0];
            }
            assertEquals(assertion.getExpectedIndexName(), tableLookupPlan.getIndexNum()[0].getName());
            assertEquals(assertion.getExpectedStrategy(), tableLookupPlan.getClass());
            stmt.destroy();
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private static void assertValues(EPServiceProvider engine, SupportUpdateListener listener, String keys, Integer[] values) {
        String[] keyarr = keys.split(",");
        for (int i = 0; i < keyarr.length; i++) {
            engine.getEPRuntime().sendEvent(new SupportBean_S0(0, keyarr[i]));
            if (values[i] == null) {
                assertFalse(listener.isInvoked());
            } else {
                EventBean event = listener.assertOneGetNewAndReset();
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
