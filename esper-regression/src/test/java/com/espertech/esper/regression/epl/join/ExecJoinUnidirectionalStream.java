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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.type.OuterJoinType;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecJoinUnidirectionalStream implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPatternUnidirectionalOuterJoinNoOn(epService);
        runAssertion2TableJoinGrouped(epService);
        runAssertion2TableJoinRowForAll(epService);
        runAssertion3TableOuterJoinVar1(epService);
        runAssertion3TableOuterJoinVar2(epService);
        runAssertionPatternJoin(epService);
        runAssertionPatternJoinOutputRate(epService);
        runAssertion3TableJoinVar1(epService);
        runAssertion3TableJoinVar2A(epService);
        runAssertion3TableJoinVar2B(epService);
        runAssertion3TableJoinVar3(epService);
        runAssertion2TableFullOuterJoin(epService);
        runAssertion2TableFullOuterJoinCompile(epService);
        runAssertion2TableFullOuterJoinOM(epService);
        runAssertion2TableFullOuterJoinBackwards(epService);
        runAssertion2TableJoin(epService);
        runAssertion2TableBackwards(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionPatternUnidirectionalOuterJoinNoOn(EPServiceProvider epService) {
        // test 2-stream left outer join and SODA
        //
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S0", SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_S1", SupportBean_S1.class);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        String stmtTextLO = "select sum(intPrimitive) as c0, count(*) as c1 " +
                "from pattern [every timer:interval(1 seconds)] unidirectional " +
                "left outer join " +
                "SupportBean#keepall";
        EPStatement stmtLO = epService.getEPAdministrator().createEPL(stmtTextLO);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtLO.addListener(listener);

        tryAssertionPatternUniOuterJoinNoOn(epService, listener, 0);

        stmtLO.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtTextLO);
        assertEquals(stmtTextLO, model.toEPL());
        stmtLO = epService.getEPAdministrator().create(model);
        assertEquals(stmtTextLO, stmtLO.getText());
        stmtLO.addListener(listener);

        tryAssertionPatternUniOuterJoinNoOn(epService, listener, 100000);

        stmtLO.destroy();

        // test 2-stream inner join
        //
        String[] fieldsIJ = "c0,c1".split(",");
        String stmtTextIJ = "select sum(intPrimitive) as c0, count(*) as c1 " +
                "from SupportBean_S0 unidirectional " +
                "inner join " +
                "SupportBean#keepall";
        EPStatement stmtIJ = epService.getEPAdministrator().createEPL(stmtTextIJ);
        stmtIJ.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "S0_2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsIJ, new Object[]{100, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 200));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "S0_3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsIJ, new Object[]{300, 2L});
        stmtIJ.destroy();

        // test 2-stream inner join with group-by
        tryAssertion2StreamInnerWGroupBy(epService, listener);

        // test 3-stream inner join
        //
        String[] fields3IJ = "c0,c1".split(",");
        String stmtText3IJ = "select sum(intPrimitive) as c0, count(*) as c1 " +
                "from " +
                "SupportBean_S0#keepall " +
                "inner join " +
                "SupportBean_S1#keepall " +
                "inner join " +
                "SupportBean#keepall";

        EPStatement stmt3IJ = epService.getEPAdministrator().createEPL(stmtText3IJ);
        stmt3IJ.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_1"));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 50));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "S1_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields3IJ, new Object[]{50, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 51));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields3IJ, new Object[]{101, 2L});

        stmt3IJ.destroy();

        // test 3-stream full outer join
        //
        String[] fields3FOJ = "p00,p10,theString".split(",");
        String stmtText3FOJ = "select p00, p10, theString " +
                "from " +
                "SupportBean_S0#keepall " +
                "full outer join " +
                "SupportBean_S1#keepall " +
                "full outer join " +
                "SupportBean#keepall";

        EPStatement stmt3FOJ = epService.getEPAdministrator().createEPL(stmtText3FOJ);
        stmt3FOJ.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "S0_1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields3FOJ, new Object[]{"S0_1", null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E10", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields3FOJ, new Object[]{null, null, "E10"});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "S0_2"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields3FOJ, new Object[][]{{"S0_2", null, null}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "S1_0"));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields3FOJ, new Object[][]{{"S0_1", "S1_0", "E10"}, {"S0_2", "S1_0", "E10"}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "S0_3"));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields3FOJ, new Object[][]{{"S0_3", "S1_0", "E10"}});

        epService.getEPRuntime().sendEvent(new SupportBean("E11", 0));
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields3FOJ, new Object[][]{{"S0_1", "S1_0", "E11"}, {"S0_2", "S1_0", "E11"}, {"S0_3", "S1_0", "E11"}});
        assertEquals(6, EPAssertionUtil.iteratorCount(stmt3FOJ.iterator()));

        stmt3FOJ.destroy();

        // test 3-stream full outer join with where-clause
        //
        String[] fields3FOJW = "p00,p10,theString".split(",");
        String stmtText3FOJW = "select p00, p10, theString " +
                "from " +
                "SupportBean_S0#keepall as s0 " +
                "full outer join " +
                "SupportBean_S1#keepall as s1 " +
                "full outer join " +
                "SupportBean#keepall as sb " +
                "where s0.p00 = s1.p10";

        EPStatement stmt3FOJW = epService.getEPAdministrator().createEPL(stmtText3FOJW);
        stmt3FOJW.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "X1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "Y1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "Y1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields3FOJW, new Object[]{"Y1", "Y1", null});

        stmt3FOJW.destroy();
    }

    private void tryAssertion2StreamInnerWGroupBy(EPServiceProvider epService, SupportUpdateListener listener) {
        epService.getEPAdministrator().createEPL("create objectarray schema E1 (id string, grp string, value int)");
        epService.getEPAdministrator().createEPL("create objectarray schema E2 (id string, value2 int)");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select count(*) as c0, sum(E1.value) as c1, E1.id as c2 " +
                "from E1 unidirectional inner join E2#keepall on E1.id = E2.id group by E1.grp");
        stmt.addListener(listener);
        String[] fields = "c0,c1,c2".split(",");

        epService.getEPRuntime().sendEvent(new Object[]{"A", 100}, "E2");
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new Object[]{"A", "X", 10}, "E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, 10, "A"});

        epService.getEPRuntime().sendEvent(new Object[]{"A", "Y", 20}, "E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L, 20, "A"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionPatternUniOuterJoinNoOn(EPServiceProvider epService, SupportUpdateListener listener, long startTime) {
        String[] fields = "c0,c1".split(",");
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime + 2000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime + 3000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 11));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime + 4000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{21, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 12));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime + 5000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{33, 3L});
    }

    private void runAssertion2TableJoinGrouped(EPServiceProvider epService) {
        String stmtText = "select irstream symbol, count(*) as cnt " +
                "from " + SupportMarketDataBean.class.getName() + " unidirectional, " +
                SupportBean.class.getName() + "#keepall " +
                "where theString = symbol group by theString, symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryUnsupportedIterator(stmt);

        // send event, expect result
        sendEventMD(epService, "E1", 1L);
        String[] fields = "symbol,cnt".split(",");
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E1", 10);
        assertFalse(listener.isInvoked());

        sendEventMD(epService, "E1", 2L);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E1", 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E1", 0L});
        listener.reset();

        sendEvent(epService, "E1", 20);
        assertFalse(listener.isInvoked());

        sendEventMD(epService, "E1", 3L);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E1", 2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E1", 0L});
        listener.reset();

        try {
            stmt.safeIterator();
            fail();
        } catch (UnsupportedOperationException ex) {
            assertEquals("Iteration over a unidirectional join is not supported", ex.getMessage());
        }
        // assure lock given up by sending more events

        sendEvent(epService, "E2", 40);
        sendEventMD(epService, "E2", 4L);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{"E2", 1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{"E2", 0L});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertion2TableJoinRowForAll(EPServiceProvider epService) {
        String stmtText = "select irstream count(*) as cnt " +
                "from " + SupportMarketDataBean.class.getName() + " unidirectional, " +
                SupportBean.class.getName() + "#keepall " +
                "where theString = symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryUnsupportedIterator(stmt);

        // send event, expect result
        sendEventMD(epService, "E1", 1L);
        String[] fields = "cnt".split(",");
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E1", 10);
        assertFalse(listener.isInvoked());

        sendEventMD(epService, "E1", 2L);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        listener.reset();

        sendEvent(epService, "E1", 20);
        assertFalse(listener.isInvoked());

        sendEventMD(epService, "E1", 3L);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{2L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});
        listener.reset();

        sendEvent(epService, "E2", 40);
        sendEventMD(epService, "E2", 4L);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{0L});

        stmt.destroy();
    }

    private void runAssertion3TableOuterJoinVar1(EPServiceProvider epService) {
        String stmtText = "select s0.id, s1.id, s2.id " +
                "from " +
                SupportBean_S0.class.getName() + " as s0 unidirectional " +
                " full outer join " + SupportBean_S1.class.getName() + "#keepall as s1" +
                " on p00 = p10 " +
                " full outer join " + SupportBean_S2.class.getName() + "#keepall as s2" +
                " on p10 = p20";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        try3TableOuterJoin(epService, stmt);
        stmt.destroy();
    }

    private void runAssertion3TableOuterJoinVar2(EPServiceProvider epService) {
        String stmtText = "select s0.id, s1.id, s2.id " +
                "from " +
                SupportBean_S0.class.getName() + " as s0 unidirectional " +
                " left outer join " + SupportBean_S1.class.getName() + "#keepall as s1 " +
                " on p00 = p10 " +
                " left outer join " + SupportBean_S2.class.getName() + "#keepall as s2 " +
                " on p10 = p20";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        try3TableOuterJoin(epService, stmt);
        stmt.destroy();
    }

    private void runAssertionPatternJoin(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        // no iterator allowed
        String stmtText = "select count(*) as num " +
                "from pattern [every timer:at(*/1,*,*,*,*)] unidirectional,\n" +
                "SupportBean(intPrimitive=1)#unique(theString) a,\n" +
                "SupportBean(intPrimitive=2)#unique(theString) b\n" +
                "where a.theString = b.theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "A", 1);
        sendEvent(epService, "A", 2);
        sendEvent(epService, "B", 1);
        sendEvent(epService, "B", 2);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(70000));
        assertEquals(2L, listener.assertOneGetNewAndReset().get("num"));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(140000));
        assertEquals(2L, listener.assertOneGetNewAndReset().get("num"));
    }

    private void runAssertionPatternJoinOutputRate(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));

        // no iterator allowed
        String stmtText = "select count(*) as num " +
                "from pattern [every timer:at(*/1,*,*,*,*)] unidirectional,\n" +
                "SupportBean(intPrimitive=1)#unique(theString) a,\n" +
                "SupportBean(intPrimitive=2)#unique(theString) b\n" +
                "where a.theString = b.theString output every 2 minutes";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "A", 1);
        sendEvent(epService, "A", 2);
        sendEvent(epService, "B", 1);
        sendEvent(epService, "B", 2);
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(70000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(140000));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(210000));
        assertEquals(2L, listener.getLastNewData()[0].get("num"));
        assertEquals(2L, listener.getLastNewData()[1].get("num"));
    }

    private void try3TableOuterJoin(EPServiceProvider epService, EPStatement statement) {
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        String[] fields = "s0.id,s1.id,s2.id".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1, null, null});
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(3, "E1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "E2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, 20, null});
        epService.getEPRuntime().sendEvent(new SupportBean_S2(30, "E2"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(300, "E3"));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{100, null, null});
        epService.getEPRuntime().sendEvent(new SupportBean_S1(200, "E3"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(31, "E4"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(21, "E4"));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "E4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 21, 31});

        epService.getEPRuntime().sendEvent(new SupportBean_S2(32, "E4"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(22, "E4"));
        assertFalse(listener.isInvoked());
    }

    private void runAssertion3TableJoinVar1(EPServiceProvider epService) {
        String stmtText = "select s0.id, s1.id, s2.id " +
                "from " +
                SupportBean_S0.class.getName() + " as s0 unidirectional, " +
                SupportBean_S1.class.getName() + "#keepall as s1, " +
                SupportBean_S2.class.getName() + "#keepall as s2 " +
                "where p00 = p10 and p10 = p20";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        try3TableJoin(epService, stmt);
        stmt.destroy();
    }

    private void runAssertion3TableJoinVar2A(EPServiceProvider epService) {
        String stmtText = "select s0.id, s1.id, s2.id " +
                "from " +
                SupportBean_S1.class.getName() + "#keepall as s1, " +
                SupportBean_S0.class.getName() + " as s0 unidirectional, " +
                SupportBean_S2.class.getName() + "#keepall as s2 " +
                "where p00 = p10 and p10 = p20";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        try3TableJoin(epService, stmt);
        stmt.destroy();
    }

    private void runAssertion3TableJoinVar2B(EPServiceProvider epService) {
        String stmtText = "select s0.id, s1.id, s2.id " +
                "from " +
                SupportBean_S2.class.getName() + "#keepall as s2, " +
                SupportBean_S0.class.getName() + " as s0 unidirectional, " +
                SupportBean_S1.class.getName() + "#keepall as s1 " +
                "where p00 = p10 and p10 = p20";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        try3TableJoin(epService, stmt);
        stmt.destroy();
    }

    private void runAssertion3TableJoinVar3(EPServiceProvider epService) {
        String stmtText = "select s0.id, s1.id, s2.id " +
                "from " +
                SupportBean_S1.class.getName() + "#keepall as s1, " +
                SupportBean_S2.class.getName() + "#keepall as s2, " +
                SupportBean_S0.class.getName() + " as s0 unidirectional " +
                "where p00 = p10 and p10 = p20";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        try3TableJoin(epService, stmt);
        stmt.destroy();
    }

    private void try3TableJoin(EPServiceProvider epService, EPStatement statement) {
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2, "E1"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(3, "E1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(20, "E2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E2"));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(30, "E2"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(300, "E3"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(100, "E3"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(200, "E3"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(31, "E4"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(21, "E4"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "E4"));
        String[] fields = "s0.id,s1.id,s2.id".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 21, 31});

        epService.getEPRuntime().sendEvent(new SupportBean_S2(32, "E4"));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(22, "E4"));
        assertFalse(listener.isInvoked());
    }

    private void runAssertion2TableFullOuterJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, theString, intPrimitive " +
                "from " + SupportMarketDataBean.class.getName() + " unidirectional " +
                "full outer join " +
                SupportBean.class.getName() +
                "#keepall on theString = symbol";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        tryFullOuterPassive2Stream(epService, stmt);
        stmt.destroy();
    }

    private void runAssertion2TableFullOuterJoinCompile(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, theString, intPrimitive " +
                "from " + SupportMarketDataBean.class.getName() + " unidirectional " +
                "full outer join " +
                SupportBean.class.getName() +
                "#keepall on theString = symbol";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);

        tryFullOuterPassive2Stream(epService, stmt);

        stmt.destroy();
    }

    private void runAssertion2TableFullOuterJoinOM(EPServiceProvider epService) {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("symbol", "volume", "theString", "intPrimitive"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName()).unidirectional(true)));
        model.getFromClause().add(FilterStream.create(SupportBean.class.getName()).addView("keepall"));
        model.getFromClause().add(OuterJoinQualifier.create("theString", OuterJoinType.FULL, "symbol"));

        String stmtText = "select symbol, volume, theString, intPrimitive " +
                "from " + SupportMarketDataBean.class.getName() + " unidirectional " +
                "full outer join " +
                SupportBean.class.getName() +
                "#keepall on theString = symbol";
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);

        tryFullOuterPassive2Stream(epService, stmt);

        stmt.destroy();
    }

    private void runAssertion2TableFullOuterJoinBackwards(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, theString, intPrimitive " +
                "from " + SupportBean.class.getName() +
                "#keepall full outer join " +
                SupportMarketDataBean.class.getName() + " unidirectional " +
                "on theString = symbol";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);

        tryFullOuterPassive2Stream(epService, stmt);

        stmt.destroy();
    }

    private void runAssertion2TableJoin(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, theString, intPrimitive " +
                "from " + SupportMarketDataBean.class.getName() + " unidirectional, " +
                SupportBean.class.getName() +
                "#keepall where theString = symbol";

        tryJoinPassive2Stream(epService, stmtText);
    }

    private void runAssertion2TableBackwards(EPServiceProvider epService) {
        String stmtText = "select symbol, volume, theString, intPrimitive " +
                "from " + SupportBean.class.getName() + "#keepall, " +
                SupportMarketDataBean.class.getName() + " unidirectional " +
                "where theString = symbol";

        tryJoinPassive2Stream(epService, stmtText);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String text = "select * from " + SupportBean.class.getName() + " unidirectional " +
                "full outer join " +
                SupportMarketDataBean.class.getName() + "#keepall unidirectional " +
                "on theString = symbol";
        tryInvalid(epService, text, "Error starting statement: The unidirectional keyword requires that no views are declared onto the stream (applies to stream 1)");

        text = "select * from " + SupportBean.class.getName() + "#length(2) unidirectional " +
                "full outer join " +
                SupportMarketDataBean.class.getName() + "#keepall " +
                "on theString = symbol";
        tryInvalid(epService, text, "Error starting statement: The unidirectional keyword requires that no views are declared onto the stream");
    }

    private void tryFullOuterPassive2Stream(EPServiceProvider epService, EPStatement stmt) {
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryUnsupportedIterator(stmt);

        // send event, expect result
        sendEventMD(epService, "E1", 1L);
        String[] fields = "symbol,volume,theString,intPrimitive".split(",");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, null, null});

        sendEvent(epService, "E1", 10);
        assertFalse(listener.isInvoked());

        sendEventMD(epService, "E1", 2L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 2L, "E1", 10});

        sendEvent(epService, "E1", 20);
        assertFalse(listener.isInvoked());
    }

    private void tryJoinPassive2Stream(EPServiceProvider epService, String stmtText) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryUnsupportedIterator(stmt);

        // send event, expect result
        sendEventMD(epService, "E1", 1L);
        String[] fields = "symbol,volume,theString,intPrimitive".split(",");
        assertFalse(listener.isInvoked());

        sendEvent(epService, "E1", 10);
        assertFalse(listener.isInvoked());

        sendEventMD(epService, "E1", 2L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 2L, "E1", 10});

        sendEvent(epService, "E1", 20);
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String s, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendEventMD(EPServiceProvider epService, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        epService.getEPRuntime().sendEvent(bean);
    }

    private void tryUnsupportedIterator(EPStatement stmt) {
        try {
            stmt.iterator();
            fail();
        } catch (UnsupportedOperationException ex) {
            assertEquals("Iteration over a unidirectional join is not supported", ex.getMessage());
        }
    }
}
