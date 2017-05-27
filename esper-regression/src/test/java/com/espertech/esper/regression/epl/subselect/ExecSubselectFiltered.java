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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static junit.framework.TestCase.*;

public class ExecSubselectFiltered implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Sensor", SupportSensorEvent.class);
        configuration.addEventType("MyEvent", SupportBean.class);
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
        configuration.addEventType("S2", SupportBean_S2.class);
        configuration.addEventType("S3", SupportBean_S3.class);
        configuration.addEventType("S4", SupportBean_S4.class);
        configuration.addEventType("S5", SupportBean_S5.class);
        configuration.addEventType("SupportBeanRange", SupportBeanRange.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("ST0", SupportBean_ST0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ST1", SupportBean_ST1.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ST2", SupportBean_ST2.class);

        runAssertionHavingClauseNoAggregation(epService);
        runAssertion3StreamKeyRangeCoercion(epService);
        runAssertion2StreamRangeCoercion(epService);
        runAssertionSameEventCompile(epService);
        runAssertionSameEventOM(epService);
        runAssertionSameEvent(epService);
        runAssertionSelectWildcard(epService);
        runAssertionSelectWildcardNoName(epService);
        runAssertionWhereConstant(epService);
        runAssertionWherePrevious(epService);
        runAssertionWherePreviousOM(epService);
        runAssertionWherePreviousCompile(epService);
        runAssertionSelectWithWhereJoined(epService);
        runAssertionSelectWhereJoined2Streams(epService);
        runAssertionSelectWhereJoined3Streams(epService);
        runAssertionSelectWhereJoined3SceneTwo(epService);
        runAssertionSelectWhereJoined4Coercion(epService);
        runAssertionSelectWhereJoined4BackCoercion(epService);
        runAssertionSelectWithWhere2Subqery(epService);
        runAssertionJoinFilteredOne(epService);
        runAssertionJoinFilteredTwo(epService);
        runAssertionSubselectPrior(epService);
        runAssertionSubselectMixMax(epService);
    }

    private void runAssertionHavingClauseNoAggregation(EPServiceProvider epService) {
        tryAssertionHavingNoAggNoFilterNoWhere(epService);
        tryAssertionHavingNoAggWWhere(epService);
        tryAssertionHavingNoAggWFilterWWhere(epService);
    }

    private void runAssertion3StreamKeyRangeCoercion(EPServiceProvider epService) {
        String epl = "select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where theString = st2.key2 and intPrimitive between s0.p01Long and s1.p11Long) " +
                "from ST2#lastevent st2, ST0#lastevent s0, ST1#lastevent s1";
        tryAssertion3StreamKeyRangeCoercion(epService, epl, true);

        epl = "select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where theString = st2.key2 and s1.p11Long >= intPrimitive and s0.p01Long <= intPrimitive) " +
                "from ST2#lastevent st2, ST0#lastevent s0, ST1#lastevent s1";
        tryAssertion3StreamKeyRangeCoercion(epService, epl, false);

        epl = "select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where theString = st2.key2 and s1.p11Long > intPrimitive) " +
                "from ST2#lastevent st2, ST0#lastevent s0, ST1#lastevent s1";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G", 21));
        epService.getEPRuntime().sendEvent(new SupportBean("G", 13));
        epService.getEPRuntime().sendEvent(new SupportBean_ST2("ST2", "G", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", -1L));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", 20L));
        assertEquals(13, listener.assertOneGetNewAndReset().get("sumi"));

        stmt.destroy();
        epl = "select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where theString = st2.key2 and s1.p11Long < intPrimitive) " +
                "from ST2#lastevent st2, ST0#lastevent s0, ST1#lastevent s1";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G", 21));
        epService.getEPRuntime().sendEvent(new SupportBean("G", 13));
        epService.getEPRuntime().sendEvent(new SupportBean_ST2("ST2", "G", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", -1L));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1", 20L));
        assertEquals(21, listener.assertOneGetNewAndReset().get("sumi"));

        stmt.destroy();
    }

    private void tryAssertion3StreamKeyRangeCoercion(EPServiceProvider epService, String epl, boolean isHasRangeReversal) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("G", 9));
        epService.getEPRuntime().sendEvent(new SupportBean("G", 21));
        epService.getEPRuntime().sendEvent(new SupportBean("G", 13));
        epService.getEPRuntime().sendEvent(new SupportBean("G", 17));
        epService.getEPRuntime().sendEvent(new SupportBean_ST2("ST21", "X", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST01", 10L));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST11", 20L));
        assertEquals(null, listener.assertOneGetNewAndReset().get("sumi")); // range 10 to 20

        epService.getEPRuntime().sendEvent(new SupportBean_ST2("ST22", "G", 0));
        assertEquals(30, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST01", 0L));    // range 0 to 20
        assertEquals(39, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST2("ST21", null, 0));
        assertEquals(null, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST2("ST21", "G", 0));
        assertEquals(39, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST11", 100L));   // range 0 to 100
        assertEquals(60, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST11", null));   // range 0 to null
        assertEquals(null, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST01", null));    // range null to null
        assertEquals(null, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST11", -1L));   // range null to -1
        assertEquals(null, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST01", 10L));    // range 10 to -1
        if (isHasRangeReversal) {
            assertEquals(8, listener.assertOneGetNewAndReset().get("sumi"));
        } else {
            assertEquals(null, listener.assertOneGetNewAndReset().get("sumi"));
        }

        stmt.destroy();
    }

    private void runAssertion2StreamRangeCoercion(EPServiceProvider epService) {

        // between and 'in' automatically revert the range (20 to 10 is the same as 10 to 20)
        String epl = "select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where intPrimitive between s0.p01Long and s1.p11Long) " +
                "from ST0#lastevent s0, ST1#lastevent s1";
        tryAssertion2StreamRangeCoercion(epService, epl, true);

        epl = "select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where intPrimitive between s1.p11Long and s0.p01Long) " +
                "from ST1#lastevent s1, ST0#lastevent s0";
        tryAssertion2StreamRangeCoercion(epService, epl, true);

        // >= and <= should not automatically revert the range
        epl = "select (" +
                "select sum(intPrimitive) as sumi from SupportBean#keepall where intPrimitive >= s0.p01Long and intPrimitive <= s1.p11Long) " +
                "from ST0#lastevent s0, ST1#lastevent s1";
        tryAssertion2StreamRangeCoercion(epService, epl, false);
    }

    private void tryAssertion2StreamRangeCoercion(EPServiceProvider epService, String epl, boolean isHasRangeReversal) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST01", 10L));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST11", 20L));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 9));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 21));
        assertEquals(null, listener.assertOneGetNewAndReset().get("sumi")); // range 10 to 20

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 13));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0_1", 10L));  // range 10 to 20
        assertEquals(13, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1_1", 13L));  // range 10 to 13
        assertEquals(13, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0_2", 13L));  // range 13 to 13
        assertEquals(13, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 14));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 12));
        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1_3", 13L));  // range 13 to 13
        assertEquals(13, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1_4", 20L));  // range 13 to 20
        assertEquals(27, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0_3", 11L));  // range 11 to 20
        assertEquals(39, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0_4", null));  // range null to 16
        assertEquals(null, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1_5", null));  // range null to null
        assertEquals(null, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0_5", 20L));  // range 20 to null
        assertEquals(null, listener.assertOneGetNewAndReset().get("sumi"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST1("ST1_6", 13L));  // range 20 to 13
        if (isHasRangeReversal) {
            assertEquals(27, listener.assertOneGetNewAndReset().get("sumi"));
        } else {
            assertEquals(null, listener.assertOneGetNewAndReset().get("sumi"));
        }

        stmt.destroy();
    }

    private void runAssertionSameEventCompile(EPServiceProvider epService) throws Exception {
        String stmtText = "select (select * from S1#length(1000)) as events1 from S1";
        EPStatementObjectModel subquery = epService.getEPAdministrator().compileEPL(stmtText);
        subquery = (EPStatementObjectModel) SerializableObjectCopier.copy(subquery);

        EPStatement stmt = epService.getEPAdministrator().create(subquery);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EventType type = stmt.getEventType();
        assertEquals(SupportBean_S1.class, type.getPropertyType("events1"));

        Object theEvent = new SupportBean_S1(-1, "Y");
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean result = listener.assertOneGetNewAndReset();
        assertSame(theEvent, result.get("events1"));

        stmt.destroy();
    }

    private void runAssertionSameEventOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel subquery = new EPStatementObjectModel();
        subquery.setSelectClause(SelectClause.createWildcard());
        subquery.setFromClause(FromClause.create(FilterStream.create("S1").addView(View.create("length", Expressions.constant(1000)))));

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setFromClause(FromClause.create(FilterStream.create("S1")));
        model.setSelectClause(SelectClause.create().add(Expressions.subquery(subquery), "events1"));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String stmtText = "select (select * from S1#length(1000)) as events1 from S1";
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EventType type = stmt.getEventType();
        assertEquals(SupportBean_S1.class, type.getPropertyType("events1"));

        Object theEvent = new SupportBean_S1(-1, "Y");
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean result = listener.assertOneGetNewAndReset();
        assertSame(theEvent, result.get("events1"));

        stmt.destroy();
    }

    private void runAssertionSameEvent(EPServiceProvider epService) {
        String stmtText = "select (select * from S1#length(1000)) as events1 from S1";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EventType type = stmt.getEventType();
        assertEquals(SupportBean_S1.class, type.getPropertyType("events1"));

        Object theEvent = new SupportBean_S1(-1, "Y");
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean result = listener.assertOneGetNewAndReset();
        assertSame(theEvent, result.get("events1"));

        stmt.destroy();
    }

    private void runAssertionSelectWildcard(EPServiceProvider epService) {
        String stmtText = "select (select * from S1#length(1000)) as events1 from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EventType type = stmt.getEventType();
        assertEquals(SupportBean_S1.class, type.getPropertyType("events1"));

        Object theEvent = new SupportBean_S1(-1, "Y");
        epService.getEPRuntime().sendEvent(theEvent);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EventBean result = listener.assertOneGetNewAndReset();
        assertSame(theEvent, result.get("events1"));

        stmt.destroy();
    }

    private void runAssertionSelectWildcardNoName(EPServiceProvider epService) {
        String stmtText = "select (select * from S1#length(1000)) from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EventType type = stmt.getEventType();
        assertEquals(SupportBean_S1.class, type.getPropertyType("subselect_1"));

        Object theEvent = new SupportBean_S1(-1, "Y");
        epService.getEPRuntime().sendEvent(theEvent);
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EventBean result = listener.assertOneGetNewAndReset();
        assertSame(theEvent, result.get("subselect_1"));

        stmt.destroy();
    }

    private void runAssertionWhereConstant(EPServiceProvider epService) {
        // single-column constant
        String stmtText = "select (select id from S1#length(1000) where p10='X') as ids1 from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1, "Y"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertNull(listener.assertOneGetNewAndReset().get("ids1"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "X"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "Y"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(3, "Z"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(1, listener.assertOneGetNewAndReset().get("ids1"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(1, listener.assertOneGetNewAndReset().get("ids1"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "X"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(null, listener.assertOneGetNewAndReset().get("ids1"));
        stmt.destroy();

        // two-column constant
        stmtText = "select (select id from S1#length(1000) where p10='X' and p11='Y') as ids1 from S0";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "X", "Y"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(1, listener.assertOneGetNewAndReset().get("ids1"));
        stmt.destroy();

        // single range
        stmtText = "select (select theString from SupportBean#lastevent where intPrimitive between 10 and 20) as ids1 from S0";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 15));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("ids1"));

        stmt.destroy();
    }

    private void runAssertionWherePrevious(EPServiceProvider epService) {
        String stmtText = "select (select prev(1, id) from S1#length(1000) where id=s0.id) as value from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        runWherePrevious(epService, listener);
        stmt.destroy();
    }

    private void runAssertionWherePreviousOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel subquery = new EPStatementObjectModel();
        subquery.setSelectClause(SelectClause.create().add(Expressions.previous(1, "id")));
        subquery.setFromClause(FromClause.create(FilterStream.create("S1").addView(View.create("length", Expressions.constant(1000)))));
        subquery.setWhereClause(Expressions.eqProperty("id", "s0.id"));

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setFromClause(FromClause.create(FilterStream.create("S0", "s0")));
        model.setSelectClause(SelectClause.create().add(Expressions.subquery(subquery), "value"));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String stmtText = "select (select prev(1,id) from S1#length(1000) where id=s0.id) as value from S0 as s0";
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        runWherePrevious(epService, listener);

        stmt.destroy();
    }

    private void runAssertionWherePreviousCompile(EPServiceProvider epService) {
        String stmtText = "select (select prev(1,id) from S1#length(1000) where id=s0.id) as value from S0 as s0";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        runWherePrevious(epService, listener);

        stmt.destroy();
    }

    private void runWherePrevious(EPServiceProvider epService, SupportUpdateListener listener) {
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertNull(listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(1, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(3));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(2, listener.assertOneGetNewAndReset().get("value"));
    }

    private void runAssertionSelectWithWhereJoined(EPServiceProvider epService) {
        String stmtText = "select (select id from S1#length(1000) where p10=s0.p00) as ids1 from S0 as s0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertNull(listener.assertOneGetNewAndReset().get("ids1"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "X"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "Y"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(3, "Z"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertNull(listener.assertOneGetNewAndReset().get("ids1"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "X"));
        assertEquals(1, listener.assertOneGetNewAndReset().get("ids1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "Y"));
        assertEquals(2, listener.assertOneGetNewAndReset().get("ids1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "Z"));
        assertEquals(3, listener.assertOneGetNewAndReset().get("ids1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "A"));
        assertEquals(null, listener.assertOneGetNewAndReset().get("ids1"));

        stmt.destroy();
    }

    private void runAssertionSelectWhereJoined2Streams(EPServiceProvider epService) {
        String stmtText = "select (select id from S0#length(1000) where p00=s1.p10 and p00=s2.p20) as ids0 from S1#keepall as s1, S2#keepall as s2 where s1.id = s2.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(10, "s0_1"));
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(99, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(11, "s0_1"));
        assertEquals(99, listener.assertOneGetNewAndReset().get("ids0"));

        stmt.destroy();
    }

    private void runAssertionSelectWhereJoined3Streams(EPServiceProvider epService) {
        String stmtText = "select (select id from S0#length(1000) where p00=s1.p10 and p00=s3.p30) as ids0 " +
                "from S1#keepall as s1, S2#keepall as s2, S3#keepall as s3 where s1.id = s2.id and s2.id = s3.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(10, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(10, "s0_1"));
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(99, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(11, "xxx"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(11, "s0_1"));
        assertEquals(99, listener.assertOneGetNewAndReset().get("ids0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(98, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(12, "s0_x"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(12, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(12, "s0_1"));
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(13, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(13, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(13, "s0_x"));
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(14, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(14, "xx"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(14, "s0_2"));
        assertEquals(98, listener.assertOneGetNewAndReset().get("ids0"));

        stmt.destroy();
    }

    private void runAssertionSelectWhereJoined3SceneTwo(EPServiceProvider epService) {
        String stmtText = "select (select id from S0#length(1000) where p00=s1.p10 and p00=s3.p30 and p00=s2.p20) as ids0 " +
                "from S1#keepall as s1, S2#keepall as s2, S3#keepall as s3 where s1.id = s2.id and s2.id = s3.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(10, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(10, "s0_1"));
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(99, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "s0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(11, "xxx"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(11, "s0_1"));
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(98, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(12, "s0_x"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(12, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(12, "s0_1"));
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(13, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(13, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(13, "s0_x"));
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(14, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(14, "s0_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S3(14, "s0_2"));
        assertEquals(98, listener.assertOneGetNewAndReset().get("ids0"));

        stmt.destroy();
    }

    private void runAssertionSelectWhereJoined4Coercion(EPServiceProvider epService) {
        String stmtText = "select " +
                "(select intPrimitive from MyEvent(theString='S')#length(1000) " +
                "  where intBoxed=s1.longBoxed and " +
                "intBoxed=s2.doubleBoxed and " +
                "doubleBoxed=s3.intBoxed" +
                ") as ids0 from " +
                "MyEvent(theString='A')#keepall as s1, " +
                "MyEvent(theString='B')#keepall as s2, " +
                "MyEvent(theString='C')#keepall as s3 " +
                "where s1.intPrimitive = s2.intPrimitive and s2.intPrimitive = s3.intPrimitive";
        trySelectWhereJoined4Coercion(epService, stmtText);

        stmtText = "select " +
                "(select intPrimitive from MyEvent(theString='S')#length(1000) " +
                "  where doubleBoxed=s3.intBoxed and " +
                "intBoxed=s2.doubleBoxed and " +
                "intBoxed=s1.longBoxed" +
                ") as ids0 from " +
                "MyEvent(theString='A')#keepall as s1, " +
                "MyEvent(theString='B')#keepall as s2, " +
                "MyEvent(theString='C')#keepall as s3 " +
                "where s1.intPrimitive = s2.intPrimitive and s2.intPrimitive = s3.intPrimitive";
        trySelectWhereJoined4Coercion(epService, stmtText);

        stmtText = "select " +
                "(select intPrimitive from MyEvent(theString='S')#length(1000) " +
                "  where doubleBoxed=s3.intBoxed and " +
                "intBoxed=s1.longBoxed and " +
                "intBoxed=s2.doubleBoxed" +
                ") as ids0 from " +
                "MyEvent(theString='A')#keepall as s1, " +
                "MyEvent(theString='B')#keepall as s2, " +
                "MyEvent(theString='C')#keepall as s3 " +
                "where s1.intPrimitive = s2.intPrimitive and s2.intPrimitive = s3.intPrimitive";
        trySelectWhereJoined4Coercion(epService, stmtText);
    }

    private void runAssertionSelectWhereJoined4BackCoercion(EPServiceProvider epService) {
        String stmtText = "select " +
                "(select intPrimitive from MyEvent(theString='S')#length(1000) " +
                "  where longBoxed=s1.intBoxed and " +
                "longBoxed=s2.doubleBoxed and " +
                "intBoxed=s3.longBoxed" +
                ") as ids0 from " +
                "MyEvent(theString='A')#keepall as s1, " +
                "MyEvent(theString='B')#keepall as s2, " +
                "MyEvent(theString='C')#keepall as s3 " +
                "where s1.intPrimitive = s2.intPrimitive and s2.intPrimitive = s3.intPrimitive";
        trySelectWhereJoined4CoercionBack(epService, stmtText);

        stmtText = "select " +
                "(select intPrimitive from MyEvent(theString='S')#length(1000) " +
                "  where longBoxed=s2.doubleBoxed and " +
                "intBoxed=s3.longBoxed and " +
                "longBoxed=s1.intBoxed " +
                ") as ids0 from " +
                "MyEvent(theString='A')#keepall as s1, " +
                "MyEvent(theString='B')#keepall as s2, " +
                "MyEvent(theString='C')#keepall as s3 " +
                "where s1.intPrimitive = s2.intPrimitive and s2.intPrimitive = s3.intPrimitive";
        trySelectWhereJoined4CoercionBack(epService, stmtText);
    }

    private void trySelectWhereJoined4CoercionBack(EPServiceProvider epService, String stmtText) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBean(epService, "A", 1, 10, 200, 3000);        // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "B", 1, 10, 200, 3000);
        sendBean(epService, "C", 1, 10, 200, 3000);
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        sendBean(epService, "S", -1, 11, 201, 0);     // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "A", 2, 201, 0, 0);
        sendBean(epService, "B", 2, 0, 0, 201);
        sendBean(epService, "C", 2, 0, 11, 0);
        assertEquals(-1, listener.assertOneGetNewAndReset().get("ids0"));

        sendBean(epService, "S", -2, 12, 202, 0);     // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "A", 3, 202, 0, 0);
        sendBean(epService, "B", 3, 0, 0, 202);
        sendBean(epService, "C", 3, 0, -1, 0);
        assertEquals(null, listener.assertOneGetNewAndReset().get("ids0"));

        sendBean(epService, "S", -3, 13, 203, 0);     // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "A", 4, 203, 0, 0);
        sendBean(epService, "B", 4, 0, 0, 203.0001);
        sendBean(epService, "C", 4, 0, 13, 0);
        assertEquals(null, listener.assertOneGetNewAndReset().get("ids0"));

        sendBean(epService, "S", -4, 14, 204, 0);     // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "A", 5, 205, 0, 0);
        sendBean(epService, "B", 5, 0, 0, 204);
        sendBean(epService, "C", 5, 0, 14, 0);
        assertEquals(null, listener.assertOneGetNewAndReset().get("ids0"));

        stmt.stop();
    }

    private void trySelectWhereJoined4Coercion(EPServiceProvider epService, String stmtText) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBean(epService, "A", 1, 10, 200, 3000);        // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "B", 1, 10, 200, 3000);
        sendBean(epService, "C", 1, 10, 200, 3000);
        assertNull(listener.assertOneGetNewAndReset().get("ids0"));

        sendBean(epService, "S", -2, 11, 0, 3001);
        sendBean(epService, "A", 2, 0, 11, 0);        // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "B", 2, 0, 0, 11);
        sendBean(epService, "C", 2, 3001, 0, 0);
        assertEquals(-2, listener.assertOneGetNewAndReset().get("ids0"));

        sendBean(epService, "S", -3, 12, 0, 3002);
        sendBean(epService, "A", 3, 0, 12, 0);        // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "B", 3, 0, 0, 12);
        sendBean(epService, "C", 3, 3003, 0, 0);
        assertEquals(null, listener.assertOneGetNewAndReset().get("ids0"));

        sendBean(epService, "S", -4, 11, 0, 3003);
        sendBean(epService, "A", 4, 0, 0, 0);        // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "B", 4, 0, 0, 11);
        sendBean(epService, "C", 4, 3003, 0, 0);
        assertEquals(null, listener.assertOneGetNewAndReset().get("ids0"));

        sendBean(epService, "S", -5, 14, 0, 3004);
        sendBean(epService, "A", 5, 0, 14, 0);        // intPrimitive, intBoxed, longBoxed, doubleBoxed
        sendBean(epService, "B", 5, 0, 0, 11);
        sendBean(epService, "C", 5, 3004, 0, 0);
        assertEquals(null, listener.assertOneGetNewAndReset().get("ids0"));

        stmt.stop();
    }

    private void runAssertionSelectWithWhere2Subqery(EPServiceProvider epService) {
        String stmtText = "select id from S0 as s0 where " +
                " id = (select id from S1#length(1000) where s0.id = id) or id = (select id from S2#length(1000) where s0.id = id)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(1, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S2(2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(3));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(3, listener.assertOneGetNewAndReset().get("id"));

        stmt.destroy();
    }

    private void runAssertionJoinFilteredOne(EPServiceProvider epService) {
        String stmtText = "select s0.id as s0id, s1.id as s1id, " +
                "(select p20 from S2#length(1000) where id=s0.id) as s2p20, " +
                "(select prior(1, p20) from S2#length(1000) where id=s0.id) as s2p20Prior, " +
                "(select prev(1, p20) from S2#length(10) where id=s0.id) as s2p20Prev " +
                "from S0#keepall as s0, S1#keepall as s1 " +
                "where s0.id = s1.id and p00||p10 = (select p20 from S2#length(1000) where id=s0.id)";
        tryJoinFiltered(epService, stmtText);
    }

    private void runAssertionJoinFilteredTwo(EPServiceProvider epService) {
        String stmtText = "select s0.id as s0id, s1.id as s1id, " +
                "(select p20 from S2#length(1000) where id=s0.id) as s2p20, " +
                "(select prior(1, p20) from S2#length(1000) where id=s0.id) as s2p20Prior, " +
                "(select prev(1, p20) from S2#length(10) where id=s0.id) as s2p20Prev " +
                "from S0#keepall as s0, S1#keepall as s1 " +
                "where s0.id = s1.id and (select s0.p00||s1.p10 = p20 from S2#length(1000) where id=s0.id)";
        tryJoinFiltered(epService, stmtText);
    }

    private void runAssertionSubselectPrior(EPServiceProvider epService) {
        String stmtTextOne = "insert into Pair " +
                "select * from Sensor(device='A')#lastevent as a, Sensor(device='B')#lastevent as b " +
                "where a.type = b.type";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        epService.getEPAdministrator().createEPL("insert into PairDuplicatesRemoved select * from Pair(1=2)");

        String stmtTextTwo = "insert into PairDuplicatesRemoved " +
                "select * from Pair " +
                "where a.id != coalesce((select a.id from PairDuplicatesRemoved#lastevent), -1)" +
                "  and b.id != coalesce((select b.id from PairDuplicatesRemoved#lastevent), -1)";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(1, "Temperature", "A", 51, 94.5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(2, "Temperature", "A", 57, 95.5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(3, "Humidity", "B", 29, 67.5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(4, "Temperature", "B", 55, 88.0));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(2, theEvent.get("a.id"));
        assertEquals(4, theEvent.get("b.id"));

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(5, "Temperature", "B", 65, 85.0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(6, "Temperature", "B", 49, 87.0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(7, "Temperature", "A", 51, 99.5));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(7, theEvent.get("a.id"));
        assertEquals(6, theEvent.get("b.id"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubselectMixMax(EPServiceProvider epService) {
        String stmtTextOne =
                "select " +
                        " (select * from Sensor#sort(1, measurement desc)) as high, " +
                        " (select * from Sensor#sort(1, measurement asc)) as low " +
                        " from Sensor";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextOne);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(1, "Temp", "Dev1", 68.0, 96.5));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(68.0, ((SupportSensorEvent) theEvent.get("high")).getMeasurement());
        assertEquals(68.0, ((SupportSensorEvent) theEvent.get("low")).getMeasurement());

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(2, "Temp", "Dev2", 70.0, 98.5));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(70.0, ((SupportSensorEvent) theEvent.get("high")).getMeasurement());
        assertEquals(68.0, ((SupportSensorEvent) theEvent.get("low")).getMeasurement());

        epService.getEPRuntime().sendEvent(new SupportSensorEvent(3, "Temp", "Dev2", 65.0, 99.5));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(70.0, ((SupportSensorEvent) theEvent.get("high")).getMeasurement());
        assertEquals(65.0, ((SupportSensorEvent) theEvent.get("low")).getMeasurement());

        stmt.destroy();
    }

    private void tryJoinFiltered(EPServiceProvider epService, String stmtText) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "X"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "Y"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(1, "ab"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "b"));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(1, theEvent.get("s0id"));
        assertEquals(1, theEvent.get("s1id"));
        assertEquals("ab", theEvent.get("s2p20"));
        assertEquals(null, theEvent.get("s2p20Prior"));
        assertEquals(null, theEvent.get("s2p20Prev"));

        epService.getEPRuntime().sendEvent(new SupportBean_S2(2, "qx"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "q"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "x"));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(2, theEvent.get("s0id"));
        assertEquals(2, theEvent.get("s1id"));
        assertEquals("qx", theEvent.get("s2p20"));
        assertEquals("ab", theEvent.get("s2p20Prior"));
        assertEquals("ab", theEvent.get("s2p20Prev"));

        stmt.destroy();
    }

    private void tryAssertionHavingNoAggWFilterWWhere(EPServiceProvider epService) {
        String epl = "select (select intPrimitive from SupportBean(intPrimitive < 20) #keepall where intPrimitive > 15 having theString = 'ID1') as c0 from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendS0AndAssert(epService, listener, null);
        sendSBAndS0Assert(epService, listener, "ID2", 10, null);
        sendSBAndS0Assert(epService, listener, "ID1", 11, null);
        sendSBAndS0Assert(epService, listener, "ID1", 20, null);
        sendSBAndS0Assert(epService, listener, "ID1", 19, 19);

        stmt.destroy();
    }

    private void tryAssertionHavingNoAggWWhere(EPServiceProvider epService) {
        String epl = "select (select intPrimitive from SupportBean#keepall where intPrimitive > 15 having theString = 'ID1') as c0 from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendS0AndAssert(epService, listener, null);
        sendSBAndS0Assert(epService, listener, "ID2", 10, null);
        sendSBAndS0Assert(epService, listener, "ID1", 11, null);
        sendSBAndS0Assert(epService, listener, "ID1", 20, 20);

        stmt.destroy();
    }

    private void tryAssertionHavingNoAggNoFilterNoWhere(EPServiceProvider epService) {
        String epl = "select (select intPrimitive from SupportBean#keepall having theString = 'ID1') as c0 from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendS0AndAssert(epService, listener, null);
        sendSBAndS0Assert(epService, listener, "ID2", 10, null);
        sendSBAndS0Assert(epService, listener, "ID1", 11, 11);

        stmt.destroy();
    }

    private void sendBean(EPServiceProvider epService, String theString, int intPrimitive, int intBoxed, long longBoxed, double doubleBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSBAndS0Assert(EPServiceProvider epService, SupportUpdateListener listener, String theString, int intPrimitive, Integer expected) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
        sendS0AndAssert(epService, listener, expected);
    }

    private void sendS0AndAssert(EPServiceProvider epService, SupportUpdateListener listener, Integer expected) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }
}
