/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.bean.SupportBean_S2;
import com.espertech.esper.support.bean.SupportBean_S3;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.ArrayHandlingUtil;
import com.espertech.esper.type.OuterJoinType;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestOuterJoinVarA3Stream extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    private final static String EVENT_S0 = SupportBean_S0.class.getName();
    private final static String EVENT_S1 = SupportBean_S1.class.getName();
    private final static String EVENT_S2 = SupportBean_S2.class.getName();

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("P1", SupportBean_S1.class);
        config.addEventType("P2", SupportBean_S2.class);
        config.addEventType("P3", SupportBean_S3.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        updateListener = null;
    }

    public void testMapLeftJoinUnsortedProps()
    {
        String stmtText = "select t1.col1, t1.col2, t2.col1, t2.col2, t3.col1, t3.col2 from type1#keepall() as t1" +
                " left outer join type2#keepall() as t2" +
                " on t1.col2 = t2.col2 and t1.col1 = t2.col1" +
                " left outer join type3#keepall() as t3" +
                " on t1.col1 = t3.col1";

        Map<String, Object> mapType = new HashMap<String, Object>();
        mapType.put("col1", String.class);
        mapType.put("col2", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("type1", mapType);
        epService.getEPAdministrator().getConfiguration().addEventType("type2", mapType);
        epService.getEPAdministrator().getConfiguration().addEventType("type3", mapType);

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(updateListener);

        String fields[] = new String[] {"t1.col1", "t1.col2", "t2.col1", "t2.col2", "t3.col1", "t3.col2"};

        sendMapEvent("type2", "a1", "b1");
        assertFalse(updateListener.isInvoked());

        sendMapEvent("type1", "b1", "a1");
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{"b1", "a1", null, null, null, null});

        sendMapEvent("type1", "a1", "a1");
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{"a1", "a1", null, null, null, null});

        sendMapEvent("type1", "b1", "b1");
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{"b1", "b1", null, null, null, null});

        sendMapEvent("type1", "a1", "b1");
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{"a1", "b1", "a1", "b1", null, null});

        sendMapEvent("type3", "c1", "b1");
        assertFalse(updateListener.isInvoked());

        sendMapEvent("type1", "d1", "b1");
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{"d1", "b1", null, null, null, null});

        sendMapEvent("type3", "d1", "bx");
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{"d1", "b1", null, null, "d1", "bx"});

        assertFalse(updateListener.isInvoked());
    }

    public void testLeftJoin_2sides_multicolumn()
    {
        String fields[] = "s0.id, s0.p00, s0.p01, s1.id, s1.p10, s1.p11, s2.id, s2.p20, s2.p21".split(",");

        String joinStatement = "select * from " +
                                  EVENT_S0 + "#length(1000) as s0 " +
            " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 and s0.p01 = s1.p11" +
            " left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 and s0.p01 = s2.p21";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "A_1", "B_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(11, "A_2", "B_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(12, "A_1", "B_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(13, "A_2", "B_2"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(20, "A_1", "B_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(21, "A_2", "B_1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(22, "A_1", "B_2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(23, "A_2", "B_2"));
        assertFalse(updateListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "A_3", "B_3"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{1, "A_3", "B_3", null, null, null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "A_1", "B_3"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{2, "A_1", "B_3", null, null, null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "A_3", "B_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{3, "A_3", "B_1", null, null, null, null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "A_2", "B_2"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{4, "A_2", "B_2", 13, "A_2", "B_2", 23, "A_2", "B_2"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "A_2", "B_1"));
        EPAssertionUtil.assertProps(updateListener.assertOneGetNewAndReset(), fields, new Object[]{5, "A_2", "B_1", 11, "A_2", "B_1", 21, "A_2", "B_1"});
    }

    public void testLeftOuterJoin_root_s0_OM() throws Exception
    {
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

        assertEquals("select * from com.espertech.esper.support.bean.SupportBean_S0#keepall() as s0 left outer join com.espertech.esper.support.bean.SupportBean_S1#keepall() as s1 on s0.p00 = s1.p10 left outer join com.espertech.esper.support.bean.SupportBean_S2#keepall() as s2 on s0.p00 = s2.p20", model.toEPL());
        EPStatement joinView = epService.getEPAdministrator().create(model);
        joinView.addListener(updateListener);

        runAsserts();
    }

    public void testLeftOuterJoin_root_s0_Compiled() throws Exception
    {
        String joinStatement = "select * from " +
                                  EVENT_S0 + "#length(1000) as s0 " +
            "left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
            "left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(joinStatement);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        EPStatement joinView = epService.getEPAdministrator().create(model);
        joinView.addListener(updateListener);

        assertEquals(joinStatement, model.toEPL());

        runAsserts();
    }

    public void testLeftOuterJoin_root_s0()
    {
        /**
         * Query:
         *                  s0
         *           s1 <-      -> s2
         */
        String joinStatement = "select * from " +
                                  EVENT_S0 + "#length(1000) as s0 " +
            " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 " +
            " left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 ";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        runAsserts();
    }

    public void testRightOuterJoin_S2_root_s2()
    {
        /**
         * Query: right other join is eliminated/translated
         *                  s0
         *           s1 <-      -> s2
         */
        String joinStatement = "select * from " +
                                  EVENT_S2 + "#length(1000) as s2 " +
            " right outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s2.p20 " +
            " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 ";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        runAsserts();
    }

    public void testRightOuterJoin_S1_root_s1()
    {
        /**
         * Query: right other join is eliminated/translated
         *                  s0
         *           s1 <-      -> s2
         */
        String joinStatement = "select * from " +
                                  EVENT_S1 + "#length(1000) as s1 " +
            " right outer join " + EVENT_S0 + "#length(1000) as s0 on s0.p00 = s1.p10 " +
            " left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 ";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        runAsserts();
    }

    private void runAsserts()
    {
        // Test s0 outer join to 2 streams, 2 results for each (cartesian product)
        //
        Object[] s1Events = SupportBean_S1.makeS1("A", new String[] {"A-s1-1", "A-s1-2"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        Object[] s2Events = SupportBean_S2.makeS2("A", new String[] {"A-s2-1", "A-s2-2"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        Object[] s0Events = SupportBean_S0.makeS0("A", new String[] {"A-s0-1"});
        sendEvent(s0Events);
        Object[][] expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
            { s0Events[0], s1Events[1], s2Events[0] },
            { s0Events[0], s1Events[0], s2Events[1] },
            { s0Events[0], s1Events[1], s2Events[1] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s0 outer join to s1 and s2, no results for each s1 and s2
        //
        s0Events = SupportBean_S0.makeS0("B", new String[] {"B-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents());

        s0Events = SupportBean_S0.makeS0("B", new String[] {"B-s0-2"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents());

        // Test s0 outer join to s1 and s2, one row for s1 and no results for s2
        //
        s1Events = SupportBean_S1.makeS1("C", new String[] {"C-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("C", new String[] {"C-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], null}}, getAndResetNewEvents());

        // Test s0 outer join to s1 and s2, two rows for s1 and no results for s2
        //
        s1Events = SupportBean_S1.makeS1("D", new String[] {"D-s1-1", "D-s1-2"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("D", new String[] {"D-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], s1Events[0], null},
                {s0Events[0], s1Events[1], null}}, getAndResetNewEvents());

        // Test s0 outer join to s1 and s2, one row for s2 and no results for s1
        //
        s2Events = SupportBean_S2.makeS2("E", new String[] {"E-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("E", new String[] {"E-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[0]}}, getAndResetNewEvents());

        // Test s0 outer join to s1 and s2, two rows for s2 and no results for s1
        //
        s2Events = SupportBean_S2.makeS2("F", new String[] {"F-s2-1", "F-s2-2"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("F", new String[] {"F-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{
                {s0Events[0], null, s2Events[0]},
                {s0Events[0], null, s2Events[1]}}, getAndResetNewEvents());

        // Test s0 outer join to s1 and s2, one row for s1 and two rows s2
        //
        s1Events = SupportBean_S1.makeS1("G", new String[] {"G-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("G", new String[] {"G-s2-1", "G-s2-2"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("G", new String[] {"G-s0-2"});
        sendEvent(s0Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
            { s0Events[0], s1Events[0], s2Events[1] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s0 outer join to s1 and s2, one row for s2 and two rows s1
        //
        s1Events = SupportBean_S1.makeS1("H", new String[] {"H-s1-1", "H-s1-2"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("H", new String[] {"H-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("H", new String[] {"H-s0-2"});
        sendEvent(s0Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
            { s0Events[0], s1Events[1], s2Events[0] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s0 outer join to s1 and s2, one row for each s1 and s2
        //
        s1Events = SupportBean_S1.makeS1("I", new String[] {"I-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        s2Events = SupportBean_S2.makeS2("I", new String[] {"I-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        s0Events = SupportBean_S0.makeS0("I", new String[] {"I-s0-2"});
        sendEvent(s0Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s1 inner join to s0 and outer to s2:  s0 with 1 rows, s2 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("Q", new String[] {"Q-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents());

        s2Events = SupportBean_S2.makeS2("Q", new String[] {"Q-s2-1", "Q-s2-2"});
        sendEvent(s2Events[0]);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[0]}}, getAndResetNewEvents());
        sendEvent(s2Events[1]);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[1]}}, getAndResetNewEvents());

        s1Events = SupportBean_S1.makeS1("Q", new String[] {"Q-s1-1"});
        sendEvent(s1Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
            { s0Events[0], s1Events[0], s2Events[1] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s1 inner join to s0 and outer to s2:  s0 with 0 rows, s2 with 2 rows
        //
        s2Events = SupportBean_S2.makeS2("R", new String[] {"R-s2-1", "R-s2-2"});
        sendEventsAndReset(s2Events);

        s1Events = SupportBean_S1.makeS1("R", new String[] {"R-s1-1"});
        sendEvent(s1Events);
        assertFalse(updateListener.isInvoked());

        // Test s1 inner join to s0 and outer to s2:  s0 with 1 rows, s2 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("S", new String[] {"S-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents());

        s1Events = SupportBean_S1.makeS1("S", new String[] {"S-s1-1"});
        sendEvent(s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], null}}, getAndResetNewEvents());

        // Test s1 inner join to s0 and outer to s2:  s0 with 1 rows, s2 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("T", new String[] {"T-s0-1"});
        sendEvent(s0Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, null}}, getAndResetNewEvents());

        s2Events = SupportBean_S2.makeS2("T", new String[] {"T-s2-1"});
        sendEventsAndReset(s2Events);

        s1Events = SupportBean_S1.makeS1("T", new String[] {"T-s1-1"});
        sendEvent(s1Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], s2Events[0]}}, getAndResetNewEvents());

        // Test s1 inner join to s0 and outer to s2:  s0 with 2 rows, s2 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("U", new String[] {"U-s0-1", "U-s0-1"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("U", new String[] {"U-s1-1"});
        sendEvent(s1Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], null },
            { s0Events[1], s1Events[0], null },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s1 inner join to s0 and outer to s2:  s0 with 2 rows, s2 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("V", new String[] {"V-s0-1", "V-s0-1"});
        sendEventsAndReset(s0Events);

        s2Events = SupportBean_S2.makeS2("V", new String[] {"V-s2-1"});
        sendEventsAndReset(s2Events);

        s1Events = SupportBean_S1.makeS1("V", new String[] {"V-s1-1"});
        sendEvent(s1Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
            { s0Events[1], s1Events[0], s2Events[0] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s1 inner join to s0 and outer to s2:  s0 with 2 rows, s2 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("W", new String[] {"W-s0-1", "W-s0-2"});
        sendEventsAndReset(s0Events);

        s2Events = SupportBean_S2.makeS2("W", new String[] {"W-s2-1", "W-s2-2"});
        sendEventsAndReset(s2Events);

        s1Events = SupportBean_S1.makeS1("W", new String[] {"W-s1-1"});
        sendEvent(s1Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
            { s0Events[1], s1Events[0], s2Events[0] },
            { s0Events[0], s1Events[0], s2Events[1] },
            { s0Events[1], s1Events[0], s2Events[1] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s2 inner join to s0 and outer to s1:  s0 with 1 rows, s1 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("J", new String[] {"J-s0-1"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("J", new String[] {"J-s1-1", "J-s1-2"});
        sendEventsAndReset(s1Events);

        s2Events = SupportBean_S2.makeS2("J", new String[] {"J-s2-1"});
        sendEvent(s2Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
            { s0Events[0], s1Events[1], s2Events[0] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s2 inner join to s0 and outer to s1:  s0 with 0 rows, s1 with 2 rows
        //
        s1Events = SupportBean_S1.makeS1("K", new String[] {"K-s1-1", "K-s1-2"});
        sendEventsAndReset(s2Events);

        s2Events = SupportBean_S2.makeS2("K", new String[] {"K-s2-1"});
        sendEvent(s2Events);
        assertFalse(updateListener.isInvoked());

        // Test s2 inner join to s0 and outer to s1:  s0 with 1 rows, s1 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("L", new String[] {"L-s0-1"});
        sendEventsAndReset(s0Events);

        s2Events = SupportBean_S2.makeS2("L", new String[] {"L-s2-1"});
        sendEvent(s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], null, s2Events[0]}}, getAndResetNewEvents());

        // Test s2 inner join to s0 and outer to s1:  s0 with 1 rows, s1 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("M", new String[] {"M-s0-1"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("M", new String[] {"M-s1-1"});
        sendEventsAndReset(s1Events);

        s2Events = SupportBean_S2.makeS2("M", new String[] {"M-s2-1"});
        sendEvent(s2Events);
        EPAssertionUtil.assertSameAnyOrder(new Object[][]{{s0Events[0], s1Events[0], s2Events[0]}}, getAndResetNewEvents());

        // Test s2 inner join to s0 and outer to s1:  s0 with 2 rows, s1 with 0 rows
        //
        s0Events = SupportBean_S0.makeS0("N", new String[] {"N-s0-1", "N-s0-1"});
        sendEventsAndReset(s0Events);

        s2Events = SupportBean_S2.makeS2("N", new String[] {"N-s2-1"});
        sendEvent(s2Events);
        expected = new Object[][] {
            { s0Events[0], null, s2Events[0]},
            { s0Events[1], null, s2Events[0]},
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s2 inner join to s0 and outer to s1:  s0 with 2 rows, s1 with 1 rows
        //
        s0Events = SupportBean_S0.makeS0("O", new String[] {"O-s0-1", "O-s0-1"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("O", new String[] {"O-s1-1"});
        sendEventsAndReset(s1Events);

        s2Events = SupportBean_S2.makeS2("O", new String[] {"O-s2-1"});
        sendEvent(s2Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
            { s0Events[1], s1Events[0], s2Events[0] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());

        // Test s2 inner join to s0 and outer to s1:  s0 with 2 rows, s1 with 2 rows
        //
        s0Events = SupportBean_S0.makeS0("P", new String[] {"P-s0-1", "P-s0-2"});
        sendEventsAndReset(s0Events);

        s1Events = SupportBean_S1.makeS1("P", new String[] {"P-s1-1", "P-s1-2"});
        sendEventsAndReset(s1Events);

        s2Events = SupportBean_S2.makeS2("P", new String[] {"P-s2-1"});
        sendEvent(s2Events);
        expected = new Object[][] {
            { s0Events[0], s1Events[0], s2Events[0] },
            { s0Events[1], s1Events[0], s2Events[0] },
            { s0Events[0], s1Events[1], s2Events[0] },
            { s0Events[1], s1Events[1], s2Events[0] },
        };
        EPAssertionUtil.assertSameAnyOrder(expected, getAndResetNewEvents());
    }

    public void testInvalidMulticolumn()
    {
        try
        {
            String joinStatement = "select * from " +
                                      EVENT_S0 + "#length(1000) as s0 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 and s0.p01 = s1.p11" +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s0.p00 = s2.p20 and s1.p11 = s2.p21";
            epService.getEPAdministrator().createEPL(joinStatement);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals("Error validating expression: Outer join ON-clause columns must refer to properties of the same joined streams when using multiple columns in the on-clause [select * from com.espertech.esper.support.bean.SupportBean_S0#length(1000) as s0  left outer join com.espertech.esper.support.bean.SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 and s0.p01 = s1.p11 left outer join com.espertech.esper.support.bean.SupportBean_S2#length(1000) as s2 on s0.p00 = s2.p20 and s1.p11 = s2.p21]", ex.getMessage());
        }

        try
        {
            String joinStatement = "select * from " +
                                      EVENT_S0 + "#length(1000) as s0 " +
                " left outer join " + EVENT_S1 + "#length(1000) as s1 on s0.p00 = s1.p10 and s0.p01 = s1.p11" +
                " left outer join " + EVENT_S2 + "#length(1000) as s2 on s2.p20 = s0.p00 and s2.p20 = s1.p11";
            epService.getEPAdministrator().createEPL(joinStatement);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals("Error validating expression: Outer join ON-clause columns must refer to properties of the same joined streams when using multiple columns in the on-clause [select * from com.espertech.esper.support.bean.SupportBean_S0#length(1000) as s0  left outer join com.espertech.esper.support.bean.SupportBean_S1#length(1000) as s1 on s0.p00 = s1.p10 and s0.p01 = s1.p11 left outer join com.espertech.esper.support.bean.SupportBean_S2#length(1000) as s2 on s2.p20 = s0.p00 and s2.p20 = s1.p11]", ex.getMessage());
        }
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEventsAndReset(Object[] events)
    {
        sendEvent(events);
        updateListener.reset();
    }

    private void sendEvent(Object[] events)
    {
        for (int i = 0; i < events.length; i++)
        {
            epService.getEPRuntime().sendEvent(events[i]);
        }
    }

    private void sendMapEvent(String type, String col1, String col2)
    {
        Map<String, Object> mapEvent = new HashMap<String, Object>();
        mapEvent.put("col1", col1);
        mapEvent.put("col2", col2);
        epService.getEPRuntime().sendEvent(mapEvent, type);
    }

    private Object[][] getAndResetNewEvents()
    {
        EventBean[] newEvents = updateListener.getLastNewData();
        updateListener.reset();
        return ArrayHandlingUtil.getUnderlyingEvents(newEvents, new String[] {"s0", "s1", "s2"});
    }
}
