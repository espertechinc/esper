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
package com.espertech.esper.regression.epl.join;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.bean.SupportBean_S3;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.ArrayHandlingUtil;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.util.SerializableObjectCopier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecOuterJoinVarA3Stream implements RegressionExecution {
    private final static String EVENT_S0 = SupportBean_S0.class.getName();
    private final static String EVENT_S1 = SupportBean_S1.class.getName();
    private final static String EVENT_S2 = SupportBean_S2.class.getName();

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("P1", SupportBean_S1.class);
        configuration.addEventType("P2", SupportBean_S2.class);
        configuration.addEventType("P3", SupportBean_S3.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionMapLeftJoinUnsortedProps(epService);
        runAssertionLeftJoin_2sides_multicolumn(epService);
        runAssertionLeftOuterJoin_root_s0_OM(epService);
        runAssertionLeftOuterJoin_root_s0_Compiled(epService);
        runAssertionLeftOuterJoin_root_s0(epService);
        runAssertionRightOuterJoin_S2_root_s2(epService);
        runAssertionRightOuterJoin_S1_root_s1(epService);
        runAssertionInvalidMulticolumn(epService);
    }

    private void runAssertionMapLeftJoinUnsortedProps(EPServiceProvider epService) {
        String stmtText = "select t1.col1, t1.col2, t2.col1, t2.col2, t3.col1, t3.col2 from type1#keepall as t1" +
                " left outer join type2#keepall as t2" +
                " on t1.col2 = t2.col2 and t1.col1 = t2.col1" +
                " left outer join type3#keepall as t3" +
                " on t1.col1 = t3.col1";

        Map<String, Object> mapType = new HashMap<>();
        mapType.put("col1", String.class);
        mapType.put("col2", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("type1", mapType);
        epService.getEPAdministrator().getConfiguration().addEventType("type2", mapType);
        epService.getEPAdministrator().getConfiguration().addEventType("type3", mapType);

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = new String[]{"t1.col1", "t1.col2", "t2.col1", "t2.col2", "t3.col1", "t3.col2"};

        sendMapEvent(epService, "type2", "a1", "b1");
        assertFalse(listener.isInvoked());

        sendMapEvent(epService, "type1", "b1", "a1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"b1", "a1", null, null, null, null});

        sendMapEvent(epService, "type1", "a1", "a1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"a1", "a1", null, null, null, null});

        sendMapEvent(epService, "type1", "b1", "b1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"b1", "b1", null, null, null, null});

        sendMapEvent(epService, "type1", "a1", "b1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"a1", "b1", "a1", "b1", null, null});

        sendMapEvent(epService, "type3", "c1", "b1");
        assertFalse(listener.isInvoked());

        sendMapEvent(epService, "type1", "d1", "b1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"d1", "b1", null, null, null, null});

        sendMapEvent(epService, "type3", "d1", "bx");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"d1", "b1", null, null, "d1", "bx"});

        assertFalse(listener.isInvoked());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLeftJoin_2sides_multicolumn(EPServiceProvider epService) {
        String[] fields = "s0.id, s0.p00, s0.p01, s1.id, s1.p10, s1.p11, s2.id, s2.p20, s2.p21".split(",");

        String epl = "select * from " +
                EVENT_S0 + "#length(1000) as s0 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 and s0.p01 = s1.p11" +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 and s0.p01 = s2.p21";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "A_1", "B_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "A_2", "B_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(12, "A_1", "B_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(13, "A_2", "B_2"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(20, "A_1", "B_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(21, "A_2", "B_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(22, "A_1", "B_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(23, "A_2", "B_2"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A_3", "B_3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, "A_3", "B_3", null, null, null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "A_1", "B_3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{2, "A_1", "B_3", null, null, null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "A_3", "B_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3, "A_3", "B_1", null, null, null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "A_2", "B_2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{4, "A_2", "B_2", 13, "A_2", "B_2", 23, "A_2", "B_2"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "A_2", "B_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5, "A_2", "B_1", 11, "A_2", "B_1", 21, "A_2", "B_1"});

        stmt.destroy();
    }

    private void runAssertionLeftOuterJoin_root_s0_OM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        FromClause fromClause = FromClause.create(
                FilterStream.create(EVENT_S0, "s0").addView("keepall"),
                FilterStream.create(EVENT_S1, "s1").addView("keepall"),
                FilterStream.create(EVENT_S2, "s2").addView("keepall"));
        fromClause.add(OuterJoinQualifier.create("s0.p00", OuterJoinType.LEFT, "s1.p10"));
        fromClause.add(OuterJoinQualifier.create("s0.p00", OuterJoinType.LEFT, "s2.p20"));
        model.setFromClause(fromClause);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        assertEquals("select * from " + SupportBean_S0.class.getName() + "#keepall as s0 left outer join " + SupportBean_S1.class.getName() + "#keepall as s1 on s0.p00 = s1.p10 left outer join " + SupportBean_S2.class.getName() + "#keepall as s2 on s0.p00 = s2.p20", model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionLeftOuterJoin_root_s0_Compiled(EPServiceProvider epService) throws Exception {
        String epl = "select * from " +
                EVENT_S0 + "#length(1000) as s0 " +
                "left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
                "left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(epl, model.toEPL());

        tryAssertion(epService, listener);
    }

    private void runAssertionLeftOuterJoin_root_s0(EPServiceProvider epService) {
        /**
         * Query:
         *                  s0
         *           s1 <-      -> s2
         */
        String epl = "select * from " +
                EVENT_S0 + "#length(1000) as s0 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRightOuterJoin_S2_root_s2(EPServiceProvider epService) {
        /**
         * Query: right other join is eliminated/translated
         *                  s0
         *           s1 <-      -> s2
         */
        String epl = "select * from " +
                EVENT_S2 + "#length(1000) as s2 " +
                " right outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s2.p20 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void runAssertionRightOuterJoin_S1_root_s1(EPServiceProvider epService) {
        /**
         * Query: right other join is eliminated/translated
         *                  s0
         *           s1 <-      -> s2
         */
        String epl = "select * from " +
                EVENT_S1 + "#length(1000) as s1 " +
                " right outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertion(epService, listener);
    }

    private void tryAssertion(EPServiceProvider epService, SupportUpdateListener listener) {
        // Test s0 outer join to 2 streams, 2 results for each (cartesian product)
        //
        Object[] s1Events = SupportBean_S1.makeS1("A", new String[]{"A-s1-1", "A-s1-2"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());

        Object[] s2Events = SupportBean_S2.makeS2("A", new String[]{"A-s2-1", "A-s2-2"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        Object[] s0Events = SupportBean_S0.makeS0("A", new String[]{"A-s0-1"});
        sendEvent(epService, s0Events);
        Object[][] expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
                {s0Events[0], s1Events[1], s2Events[0]},
                {s0Events[0], s1Events[0], s2Events[1]},
                {s0Events[0], s1Events[1], s2Events[1]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s0 outer join to s1 and s2, no results for each s1 and s2
        //
        s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(listener));

        s0Events = SupportBean_S0.makeS0("B", new String[]{"B-s0-2"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(listener));

        // Test s0 outer join to s1 and s2, one row for s1 and no results for s2
        //
        s1Events = SupportBean_S1.makeS1("C", new String[]{"C-s1-1"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());

        s0Events = SupportBean_S0.makeS0("C", new String[]{"C-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], null}}, getAndResetNewEvents(listener));

        // Test s0 outer join to s1 and s2, two rows for s1 and no results for s2
        //
        s1Events = SupportBean_S1.makeS1("D", new String[]{"D-s1-1", "D-s1-2"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());

        s0Events = SupportBean_S0.makeS0("D", new String[]{"D-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], null},
                {s0Events[0], s1Events[1], null}}, getAndResetNewEvents(listener));

        // Test s0 outer join to s1 and s2, one row for s2 and no results for s1
        //
        s2Events = SupportBean_S2.makeS2("E", new String[]{"E-s2-1"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        s0Events = SupportBean_S0.makeS0("E", new String[]{"E-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[0]}}, getAndResetNewEvents(listener));

        // Test s0 outer join to s1 and s2, two rows for s2 and no results for s1
        //
        s2Events = SupportBean_S2.makeS2("F", new String[]{"F-s2-1", "F-s2-2"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        s0Events = SupportBean_S0.makeS0("F", new String[]{"F-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0]},
                {s0Events[0], null, s2Events[1]}}, getAndResetNewEvents(listener));

        // Test s0 outer join to s1 and s2, one row for s1 and two rows s2
        //
        s1Events = SupportBean_S1.makeS1("G", new String[]{"G-s1-1"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());

        s2Events = SupportBean_S2.makeS2("G", new String[]{"G-s2-1", "G-s2-2"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        s0Events = SupportBean_S0.makeS0("G", new String[]{"G-s0-2"});
        sendEvent(epService, s0Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
                {s0Events[0], s1Events[0], s2Events[1]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s0 outer join to s1 and s2, one row for s2 and two rows s1
        //
        s1Events = SupportBean_S1.makeS1("H", new String[]{"H-s1-1", "H-s1-2"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());

        s2Events = SupportBean_S2.makeS2("H", new String[]{"H-s2-1"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        s0Events = SupportBean_S0.makeS0("H", new String[]{"H-s0-2"});
        sendEvent(epService, s0Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
                {s0Events[0], s1Events[1], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s0 outer join to s1 and s2, one row for each s1 and s2
        //
        s1Events = SupportBean_S1.makeS1("I", new String[]{"I-s1-1"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());

        s2Events = SupportBean_S2.makeS2("I", new String[]{"I-s2-1"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        s0Events = SupportBean_S0.makeS0("I", new String[]{"I-s0-2"});
        sendEvent(epService, s0Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s1 inner join to s0 and outer to s2:  s0 with 1 rows, s2 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("Q", new String[]{"Q-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(listener));

        s2Events = SupportBean_S2.makeS2("Q", new String[]{"Q-s2-1", "Q-s2-2"});
        sendEvent(epService, s2Events[0]);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[0]}}, getAndResetNewEvents(listener));
        sendEvent(epService, s2Events[1]);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[1]}}, getAndResetNewEvents(listener));

        s1Events = SupportBean_S1.makeS1("Q", new String[]{"Q-s1-1"});
        sendEvent(epService, s1Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
                {s0Events[0], s1Events[0], s2Events[1]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s1 inner join to s0 and outer to s2:  s0 with 0 rows, s2 with 2 rows
        //
        s2Events = SupportBean_S2.makeS2("R", new String[]{"R-s2-1", "R-s2-2"});
        sendEventsAndReset(epService, listener, s2Events);

        s1Events = SupportBean_S1.makeS1("R", new String[]{"R-s1-1"});
        sendEvent(epService, s1Events);
        assertFalse(listener.isInvoked());

        // Test s1 inner join to s0 and outer to s2:  s0 with 1 rows, s2 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("S", new String[]{"S-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(listener));

        s1Events = SupportBean_S1.makeS1("S", new String[]{"S-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], null}}, getAndResetNewEvents(listener));

        // Test s1 inner join to s0 and outer to s2:  s0 with 1 rows, s2 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("T", new String[]{"T-s0-1"});
        sendEvent(epService, s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents(listener));

        s2Events = SupportBean_S2.makeS2("T", new String[]{"T-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s1Events = SupportBean_S1.makeS1("T", new String[]{"T-s1-1"});
        sendEvent(epService, s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], s2Events[0]}}, getAndResetNewEvents(listener));

        // Test s1 inner join to s0 and outer to s2:  s0 with 2 rows, s2 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("U", new String[]{"U-s0-1", "U-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("U", new String[]{"U-s1-1"});
        sendEvent(epService, s1Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], null},
                {s0Events[1], s1Events[0], null},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s1 inner join to s0 and outer to s2:  s0 with 2 rows, s2 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("V", new String[]{"V-s0-1", "V-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s2Events = SupportBean_S2.makeS2("V", new String[]{"V-s2-1"});
        sendEventsAndReset(epService, listener, s2Events);

        s1Events = SupportBean_S1.makeS1("V", new String[]{"V-s1-1"});
        sendEvent(epService, s1Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
                {s0Events[1], s1Events[0], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s1 inner join to s0 and outer to s2:  s0 with 2 rows, s2 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("W", new String[]{"W-s0-1", "W-s0-2"});
        sendEventsAndReset(epService, listener, s0Events);

        s2Events = SupportBean_S2.makeS2("W", new String[]{"W-s2-1", "W-s2-2"});
        sendEventsAndReset(epService, listener, s2Events);

        s1Events = SupportBean_S1.makeS1("W", new String[]{"W-s1-1"});
        sendEvent(epService, s1Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
                {s0Events[1], s1Events[0], s2Events[0]},
                {s0Events[0], s1Events[0], s2Events[1]},
                {s0Events[1], s1Events[0], s2Events[1]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s2 inner join to s0 and outer to s1:  s0 with 1 rows, s1 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("J", new String[]{"J-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("J", new String[]{"J-s1-1", "J-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("J", new String[]{"J-s2-1"});
        sendEvent(epService, s2Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
                {s0Events[0], s1Events[1], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s2 inner join to s0 and outer to s1:  s0 with 0 rows, s1 with 2 rows
        //
        s1Events = SupportBean_S1.makeS1("K", new String[]{"K-s1-1", "K-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("K", new String[]{"K-s2-1"});
        sendEvent(epService, s2Events);
        assertFalse(listener.isInvoked());

        // Test s2 inner join to s0 and outer to s1:  s0 with 1 rows, s1 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("L", new String[]{"L-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s2Events = SupportBean_S2.makeS2("L", new String[]{"L-s2-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[0]}}, getAndResetNewEvents(listener));

        // Test s2 inner join to s0 and outer to s1:  s0 with 1 rows, s1 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("M", new String[]{"M-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("M", new String[]{"M-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("M", new String[]{"M-s2-1"});
        sendEvent(epService, s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], s2Events[0]}}, getAndResetNewEvents(listener));

        // Test s2 inner join to s0 and outer to s1:  s0 with 2 rows, s1 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("N", new String[]{"N-s0-1", "N-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s2Events = SupportBean_S2.makeS2("N", new String[]{"N-s2-1"});
        sendEvent(epService, s2Events);
        expected = new Object[][]{
                {s0Events[0], null, s2Events[0]},
                {s0Events[1], null, s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s2 inner join to s0 and outer to s1:  s0 with 2 rows, s1 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("O", new String[]{"O-s0-1", "O-s0-1"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("O", new String[]{"O-s1-1"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("O", new String[]{"O-s2-1"});
        sendEvent(epService, s2Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
                {s0Events[1], s1Events[0], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        // Test s2 inner join to s0 and outer to s1:  s0 with 2 rows, s1 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("P", new String[]{"P-s0-1", "P-s0-2"});
        sendEventsAndReset(epService, listener, s0Events);

        s1Events = SupportBean_S1.makeS1("P", new String[]{"P-s1-1", "P-s1-2"});
        sendEventsAndReset(epService, listener, s1Events);

        s2Events = SupportBean_S2.makeS2("P", new String[]{"P-s2-1"});
        sendEvent(epService, s2Events);
        expected = new Object[][]{
                {s0Events[0], s1Events[0], s2Events[0]},
                {s0Events[1], s1Events[0], s2Events[0]},
                {s0Events[0], s1Events[1], s2Events[0]},
                {s0Events[1], s1Events[1], s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents(listener));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalidMulticolumn(EPServiceProvider epService) {
        try {
            String epl = "select * from " +
                    EVENT_S0 + "#length(1000) as s0 " +
                    " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 and s0.p01 = s1.p11" +
                    " left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 and s1.p11 = s2.p21";
            epService.getEPAdministrator().createEPL(epl);
            fail();
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error validating expression: Outer join ON-clause columns must refer to properties of the same joined streams when using multiple columns in the on-clause");
        }

        try {
            String epl = "select * from " +
                    EVENT_S0 + "#length(1000) as s0 " +
                    " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 and s0.p01 = s1.p11" +
                    " left outer join " + EVENT_S2 + "#length(1000) as s2 on s2.p20 = s0.p00 and s2.p20 = s1.p11";
            epService.getEPAdministrator().createEPL(epl);
            fail();
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error validating expression: Outer join ON-clause columns must refer to properties of the same joined streams when using multiple columns in the on-clause [");
        }
    }

    private void sendEvent(EPServiceProvider epService, Object theEvent) {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEventsAndReset(EPServiceProvider epService, SupportUpdateListener listener, Object[] events) {
        sendEvent(epService, events);
        listener.reset();
    }

    private void sendEvent(EPServiceProvider epService, Object[] events) {
        for (int i = 0; i < events.length; i++) {
            epService.getEPRuntime().sendEvent(events[i]);
        }
    }

    private void sendMapEvent(EPServiceProvider epService, String type, String col1, String col2) {
        Map<String, Object> mapEvent = new HashMap<String, Object>();
        mapEvent.put("col1", col1);
        mapEvent.put("col2", col2);
        epService.getEPRuntime().sendEvent(mapEvent, type);
    }

    private Object[][] getAndResetNewEvents(SupportUpdateListener listener) {
        EventBean[] newEvents = listener.getLastNewData();
        listener.reset();
        return ArrayHandlingUtil.getUnderlyingEvents(newEvents, new String[]{"s0", "s1", "s2"});
    }
}
