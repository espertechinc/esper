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
package com.espertech.esper.regression.epl.subselect;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExecSubselectAggregatedMultirowAndColumn implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
    }

    public void run(EPServiceProvider epService) throws Exception {

        runAssertionMultirowGroupedNoDataWindowUncorrelated(epService);
        runAssertionMultirowGroupedNamedWindowSubqueryIndexShared(epService);
        runAssertionMultirowGroupedUncorrelatedIteratorAndExpressionDef(epService);
        runAssertionMultirowGroupedCorrelatedWithEnumMethod(epService);
        runAssertionMultirowGroupedUncorrelatedWithEnumerationMethod(epService);
        runAssertionMultirowGroupedCorrelatedWHaving(epService);

        runAssertionMulticolumnGroupedUncorrelatedUnfiltered(epService);
        runAssertionMulticolumnGroupedContextPartitioned(epService);
        runAssertionMulticolumnGroupedWHaving(epService);

        // Invalid tests
        String epl;
        // not fully aggregated
        epl = "select (select theString, sum(longPrimitive) from SupportBean#keepall group by intPrimitive) from S0";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subselect with group-by requires non-aggregated properties in the select-clause to also appear in the group-by clause [select (select theString, sum(longPrimitive) from SupportBean#keepall group by intPrimitive) from S0]");

        // correlated group-by not allowed
        epl = "select (select theString, sum(longPrimitive) from SupportBean#keepall group by theString, s0.id) from S0 as s0";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subselect with group-by requires that group-by properties are provided by the subselect stream only (property 'id' is not) [select (select theString, sum(longPrimitive) from SupportBean#keepall group by theString, s0.id) from S0 as s0]");
        epl = "select (select theString, sum(longPrimitive) from SupportBean#keepall group by theString, s0.getP00()) from S0 as s0";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subselect with group-by requires that group-by properties are provided by the subselect stream only (expression 's0.getP00()' against stream 1 is not)");

        // aggregations not allowed in group-by
        epl = "select (select intPrimitive, sum(longPrimitive) from SupportBean#keepall group by sum(intPrimitive)) from S0 as s0";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Group-by expressions in a subselect may not have an aggregation function [select (select intPrimitive, sum(longPrimitive) from SupportBean#keepall group by sum(intPrimitive)) from S0 as s0]");

        // "prev" not allowed in group-by
        epl = "select (select intPrimitive, sum(longPrimitive) from SupportBean#keepall group by prev(1, intPrimitive)) from S0 as s0";
        SupportMessageAssertUtil.tryInvalid(epService, epl, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Group-by expressions in a subselect may not have a function that requires view resources (prior, prev) [select (select intPrimitive, sum(longPrimitive) from SupportBean#keepall group by prev(1, intPrimitive)) from S0 as s0]");
    }

    private void runAssertionMulticolumnGroupedWHaving(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl = "select (select theString as c0, sum(intPrimitive) as c1 from SupportBean#keepall group by theString having sum(intPrimitive) > 10) as subq from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSBEventAndTrigger(epService, "E1", 10);
        assertMapFieldAndReset("subq", listener, fields, null);

        sendSBEventAndTrigger(epService, "E2", 5);
        assertMapFieldAndReset("subq", listener, fields, null);

        sendSBEventAndTrigger(epService, "E2", 6);
        assertMapFieldAndReset("subq", listener, fields, new Object[]{"E2", 11});

        sendSBEventAndTrigger(epService, "E1", 1);
        assertMapFieldAndReset("subq", listener, fields, null);
    }

    private void runAssertionMulticolumnGroupedContextPartitioned(EPServiceProvider epService) {
        String fieldName = "subq";
        String[] fields = "c0,c1".split(",");

        epService.getEPAdministrator().createEPL(
                "create context MyCtx partition by theString from SupportBean, p00 from S0");

        String stmtText = "context MyCtx select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                " from SupportBean#keepall " +
                " group by theString) as subq " +
                "from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("P1", 100));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "P1"));
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"P1", 100});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "P2"));
        assertMapFieldAndReset(fieldName, listener, fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("P2", 200));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "P2"));
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"P2", 200});

        epService.getEPRuntime().sendEvent(new SupportBean("P2", 205));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "P2"));
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"P2", 405});

        stmt.destroy();
    }

    private void runAssertionMulticolumnGroupedUncorrelatedUnfiltered(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String fieldName = "subq";
        String[] fields = "c0,c1".split(",");
        String eplNoDelete = "select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#keepall " +
                "group by theString) as subq " +
                "from S0 as s0";
        EPStatement stmtNoDelete = epService.getEPAdministrator().createEPL(eplNoDelete);
        stmtNoDelete.addListener(listener);
        runAssertionNoDelete(epService, listener, fieldName, fields);
        stmtNoDelete.destroy();

        // try SODA
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(eplNoDelete);
        assertEquals(eplNoDelete, model.toEPL());
        stmtNoDelete = epService.getEPAdministrator().create(model);
        assertEquals(stmtNoDelete.getText(), eplNoDelete);
        stmtNoDelete.addListener(listener);
        runAssertionNoDelete(epService, listener, fieldName, fields);
        stmtNoDelete.destroy();

        // test named window with delete/remove
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("on S1 delete from MyWindow where id = intPrimitive");
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL("@Hint('disable_reclaim_group') select (select theString as c0, sum(intPrimitive) as c1 " +
                " from MyWindow group by theString) as subq from S0 as s0");
        stmtDelete.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertMapFieldAndReset(fieldName, listener, fields, null);

        sendSBEventAndTrigger(epService, "E1", 10);
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"E1", 10});

        sendS1EventAndTrigger(epService, 10);     // delete 10
        assertMapFieldAndReset(fieldName, listener, fields, null);

        sendSBEventAndTrigger(epService, "E2", 20);
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"E2", 20});

        sendSBEventAndTrigger(epService, "E2", 21);
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"E2", 41});

        sendSBEventAndTrigger(epService, "E1", 30);
        assertMapFieldAndReset(fieldName, listener, fields, null);

        sendS1EventAndTrigger(epService, 30);     // delete 30
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"E2", 41});

        sendS1EventAndTrigger(epService, 20);     // delete 20
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"E2", 21});

        sendSBEventAndTrigger(epService, "E1", 31);    // two groups
        assertMapFieldAndReset(fieldName, listener, fields, null);

        sendS1EventAndTrigger(epService, 21);     // delete 21
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"E1", 31});
        stmtDelete.destroy();

        // test multiple group-by criteria
        String[] fieldsMultiGroup = "c0,c1,c2,c3,c4".split(",");
        String eplMultiGroup = "select " +
                "(select theString as c0, intPrimitive as c1, theString||'x' as c2, " +
                "    intPrimitive * 1000 as c3, sum(longPrimitive) as c4 " +
                " from SupportBean#keepall " +
                " group by theString, intPrimitive) as subq " +
                "from S0 as s0";
        EPStatement stmtMultiGroup = epService.getEPAdministrator().createEPL(eplMultiGroup);
        stmtMultiGroup.addListener(listener);

        sendSBEventAndTrigger(epService, "G1", 1, 100L);
        assertMapFieldAndReset(fieldName, listener, fieldsMultiGroup, new Object[]{"G1", 1, "G1x", 1000, 100L});

        sendSBEventAndTrigger(epService, "G1", 1, 101L);
        assertMapFieldAndReset(fieldName, listener, fieldsMultiGroup, new Object[]{"G1", 1, "G1x", 1000, 201L});

        sendSBEventAndTrigger(epService, "G2", 1, 200L);
        assertMapFieldAndReset(fieldName, listener, fieldsMultiGroup, null);

        stmtMultiGroup.destroy();
    }

    private void runAssertionMultirowGroupedCorrelatedWHaving(EPServiceProvider epService) {
        String fieldName = "subq";
        String[] fields = "c0,c1".split(",");

        String eplEnumCorrelated = "select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                " from SupportBean#keepall " +
                " where intPrimitive = s0.id " +
                " group by theString" +
                " having sum(intPrimitive) > 10).take(100) as subq " +
                "from S0 as s0";
        EPStatement stmtEnumUnfiltered = epService.getEPAdministrator().createEPL(eplEnumCorrelated);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtEnumUnfiltered.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E2", 20}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 20}, {"E2", 20}});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 55));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 20}, {"E2", 20}});

        stmtEnumUnfiltered.destroy();
    }

    private void runAssertionMultirowGroupedCorrelatedWithEnumMethod(EPServiceProvider epService) {
        String fieldName = "subq";
        String[] fields = "c0,c1".split(",");

        String eplEnumCorrelated = "select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                " from SupportBean#keepall " +
                " where intPrimitive = s0.id " +
                " group by theString).take(100) as subq " +
                "from S0 as s0";
        EPStatement stmtEnumUnfiltered = epService.getEPAdministrator().createEPL(eplEnumCorrelated);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtEnumUnfiltered.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 10}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 20}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 100));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E2", 100}});

        stmtEnumUnfiltered.destroy();
    }

    private void runAssertionNoDelete(EPServiceProvider epService, SupportUpdateListener listener, String fieldName, String[] fields) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertMapFieldAndReset(fieldName, listener, fields, null);

        sendSBEventAndTrigger(epService, "E1", 10);
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"E1", 10});

        sendSBEventAndTrigger(epService, "E1", 20);
        assertMapFieldAndReset(fieldName, listener, fields, new Object[]{"E1", 30});

        // second group - this returns null as subquerys cannot return multiple rows (unless enumerated) (sql standard)
        sendSBEventAndTrigger(epService, "E2", 5);
        assertMapFieldAndReset(fieldName, listener, fields, null);
    }

    private void runAssertionMultirowGroupedNamedWindowSubqueryIndexShared(EPServiceProvider epService) {
        // test uncorrelated
        epService.getEPAdministrator().createEPL("@Hint('enable_window_subquery_indexshare')" +
                "create window SBWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into SBWindow select * from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 20));

        EPStatement stmtUncorrelated = epService.getEPAdministrator().createEPL("select " +
                "(select theString as c0, sum(intPrimitive) as c1 from SBWindow group by theString).take(10) as e1 from S0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtUncorrelated.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertMapMultiRow("e1", listener.assertOneGetNewAndReset(), "c0", "c0,c1".split(","), new Object[][]{{"E1", 30}});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 200));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertMapMultiRow("e1", listener.assertOneGetNewAndReset(), "c0", "c0,c1".split(","), new Object[][]{{"E1", 30}, {"E2", 200}});
        stmtUncorrelated.destroy();

        // test correlated
        EPStatement stmtCorrelated = epService.getEPAdministrator().createEPL("select " +
                "(select theString as c0, sum(intPrimitive) as c1 from SBWindow where theString = s0.p00 group by theString).take(10) as e1 from S0 as s0");
        stmtCorrelated.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        assertMapMultiRow("e1", listener.assertOneGetNewAndReset(), "c0", "c0,c1".split(","), new Object[][]{{"E1", 30}});

        stmtCorrelated.destroy();
    }

    private void runAssertionMultirowGroupedNoDataWindowUncorrelated(EPServiceProvider epService) {
        String stmtText = "select (select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString).take(10) as subq from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "c0,c1".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        ExecSubselectAggregatedMultirowAndColumn.assertMapMultiRow("subq", listener.assertOneGetNewAndReset(), "c0", fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E2"));
        ExecSubselectAggregatedMultirowAndColumn.assertMapMultiRow("subq", listener.assertOneGetNewAndReset(), "c0", fields, new Object[][]{{"G1", 10}});

        epService.getEPRuntime().sendEvent(new SupportBean("G2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "E3"));
        ExecSubselectAggregatedMultirowAndColumn.assertMapMultiRow("subq", listener.assertOneGetNewAndReset(), "c0", fields, new Object[][]{{"G1", 10}, {"G2", 20}});

        stmt.destroy();
    }

    private void runAssertionMultirowGroupedUncorrelatedIteratorAndExpressionDef(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl = "expression getGroups {" +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                "  from SupportBean#keepall group by theString)" +
                "}" +
                "select getGroups() as e1, getGroups().take(10) as e2 from S0#lastevent()";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSBEventAndTrigger(epService, "E1", 20);
        for (EventBean event : new EventBean[]{listener.assertOneGetNew(), stmt.iterator().next()}) {
            assertMapField("e1", event, fields, new Object[]{"E1", 20});
            assertMapMultiRow("e2", event, "c0", fields, new Object[][]{{"E1", 20}});
        }
        listener.reset();

        sendSBEventAndTrigger(epService, "E2", 30);
        for (EventBean event : new EventBean[]{listener.assertOneGetNew(), stmt.iterator().next()}) {
            assertMapField("e1", event, fields, null);
            assertMapMultiRow("e2", event, "c0", fields, new Object[][]{{"E1", 20}, {"E2", 30}});
        }
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionMultirowGroupedUncorrelatedWithEnumerationMethod(EPServiceProvider epService) {
        String fieldName = "subq";
        String[] fields = "c0,c1".split(",");

        // test unfiltered
        String eplEnumUnfiltered = "select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                " from SupportBean#keepall " +
                " group by theString).take(100) as subq " +
                "from S0 as s0";
        EPStatement stmtEnumUnfiltered = epService.getEPAdministrator().createEPL(eplEnumUnfiltered);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtEnumUnfiltered.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, null);

        sendSBEventAndTrigger(epService, "E1", 10);
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 10}});

        sendSBEventAndTrigger(epService, "E1", 20);
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 30}});

        sendSBEventAndTrigger(epService, "E2", 100);
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 30}, {"E2", 100}});

        sendSBEventAndTrigger(epService, "E3", 2000);
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 30}, {"E2", 100}, {"E3", 2000}});
        stmtEnumUnfiltered.destroy();

        // test filtered
        String eplEnumFiltered = "select " +
                "(select theString as c0, sum(intPrimitive) as c1 " +
                " from SupportBean#keepall " +
                " where intPrimitive > 100 " +
                " group by theString).take(100) as subq " +
                "from S0 as s0";
        EPStatement stmtEnumFiltered = epService.getEPAdministrator().createEPL(eplEnumFiltered);
        stmtEnumFiltered.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, null);

        sendSBEventAndTrigger(epService, "E1", 10);
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, null);

        sendSBEventAndTrigger(epService, "E1", 200);
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 200}});

        sendSBEventAndTrigger(epService, "E1", 11);
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 200}});

        sendSBEventAndTrigger(epService, "E1", 201);
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 401}});

        sendSBEventAndTrigger(epService, "E2", 300);
        assertMapMultiRowAndReset(fieldName, listener, "c0", fields, new Object[][]{{"E1", 401}, {"E2", 300}});

        stmtEnumFiltered.destroy();
    }

    private void sendSBEventAndTrigger(EPServiceProvider epService, String theString, int intPrimitive) {
        sendSBEventAndTrigger(epService, theString, intPrimitive, 0);
    }

    private void sendSBEventAndTrigger(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
    }

    private void sendS1EventAndTrigger(EPServiceProvider epService, int id) {
        epService.getEPRuntime().sendEvent(new SupportBean_S1(id, "x"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
    }

    private void assertMapFieldAndReset(String fieldName, SupportUpdateListener listener, String[] names, Object[] values) {
        assertMapField(fieldName, listener.assertOneGetNew(), names, values);
        listener.reset();
    }

    private void assertMapMultiRowAndReset(String fieldName, SupportUpdateListener listener, final String sortKey, String[] names, Object[][] values) {
        assertMapMultiRow(fieldName, listener.assertOneGetNew(), sortKey, names, values);
        listener.reset();
    }

    private void assertMapField(String fieldName, EventBean event, String[] names, Object[] values) {
        Map<String, Object> subq = (Map<String, Object>) event.get(fieldName);
        if (values == null && subq == null) {
            return;
        }
        EPAssertionUtil.assertPropsMap(subq, names, values);
    }

    protected static void assertMapMultiRow(String fieldName, EventBean event, final String sortKey, String[] names, Object[][] values) {
        Collection<Map> subq = (Collection<Map>) event.get(fieldName);
        if (values == null && subq == null) {
            return;
        }
        Map[] maps = subq.toArray(new Map[subq.size()]);
        Arrays.sort(maps, new Comparator<Map>() {
            public int compare(Map o1, Map o2) {
                return ((Comparable) o1.get(sortKey)).compareTo(o2.get(sortKey));
            }
        });
        EPAssertionUtil.assertPropsPerRow(maps, names, values);
    }
}
