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
package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

public class TestSubselectAggregatedSingleValue extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();        
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("S0", SupportBean_S0.class);
        config.addEventType("S1", SupportBean_S1.class);
        config.addEventType("MarketData", SupportMarketDataBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testAggregatedSingleValue() {
        runAssertionUngroupedUncorrelatedInSelect();
        runAssertionUngroupedUncorrelatedTwoAggStopStart();
        runAssertionUngroupedUncorrelatedNoDataWindow();
        runAssertionUngroupedUncorrelatedWHaving();
        runAssertionUngroupedUncorrelatedInWhereClause();
        runAssertionUngroupedUncorrelatedInSelectClause();
        runAssertionUngroupedUncorrelatedFiltered();
        runAssertionUngroupedUncorrelatedWWhereClause();
        runAssertionUngroupedCorrelated();
        runAssertionUngroupedCorrelatedInWhereClause();
        runAssertionUngroupedCorrelatedWHaving();
        runAssertionUngroupedCorrelationInsideHaving();
        runAssertionUngroupedTableWHaving();
        runAssertionGroupedUncorrelatedWHaving();
        runAssertionGroupedCorrelatedWHaving();
        runAssertionGroupedTableWHaving();
        runAssertionGroupedCorrelationInsideHaving();
    }

    public void testInvalid()
    {
        String stmtText;

        SupportMessageAssertUtil.tryInvalid(epService, "", "Unexpected end-of-input []");

        stmtText = "select (select sum(s0.id) from S1#length(3) as s1) as value from S0 as s0";
        SupportMessageAssertUtil.tryInvalid(epService, stmtText, "Error starting statement: Failed to plan subquery number 1 querying S1: Subselect aggregation functions cannot aggregate across correlated properties");

        stmtText = "select (select s1.id + sum(s1.id) from S1#length(3) as s1) as value from S0 as s0";
        SupportMessageAssertUtil.tryInvalid(epService, stmtText, "Error starting statement: Failed to plan subquery number 1 querying S1: Subselect properties must all be within aggregation functions");

        stmtText = "select (select sum(s0.id + s1.id) from S1#length(3) as s1) as value from S0 as s0";
        SupportMessageAssertUtil.tryInvalid(epService, stmtText, "Error starting statement: Failed to plan subquery number 1 querying S1: Subselect aggregation functions cannot aggregate across correlated properties");

        // having-clause cannot aggregate over properties from other streams
        stmtText = "select (select last(theString) from SupportBean#keepall having sum(s0.p00) = 1) as c0 from S0 as s0";
        SupportMessageAssertUtil.tryInvalid(epService, stmtText, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Failed to validate having-clause expression '(sum(s0.p00))=1': Implicit conversion from datatype 'String' to numeric is not allowed for aggregation function 'sum' [");

        // having-clause properties must be aggregated
        stmtText = "select (select last(theString) from SupportBean#keepall having sum(intPrimitive) = intPrimitive) as c0 from S0 as s0";
        SupportMessageAssertUtil.tryInvalid(epService, stmtText, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subselect having-clause requires that all properties are under aggregation, consider using the 'first' aggregation function instead");

        // having-clause not returning boolean
        stmtText = "select (select last(theString) from SupportBean#keepall having sum(intPrimitive)) as c0 from S0";
        SupportMessageAssertUtil.tryInvalid(epService, stmtText, "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subselect having-clause expression must return a boolean value ");
    }

    private void runAssertionGroupedCorrelationInsideHaving() {
        String epl = "select (select theString from SupportBean#keepall group by theString having sum(intPrimitive) = s0.id) as c0 from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendSB("E1", 100);
        sendSB("E2", 5);
        sendSB("E3", 20);
        sendEventS0Assert(1, null);
        sendEventS0Assert(5, "E2");

        sendSB("E2", 3);
        sendEventS0Assert(5, null);
        sendEventS0Assert(8, "E2");
        sendEventS0Assert(20, "E3");

        stmt.destroy();
    }

    private void runAssertionUngroupedCorrelationInsideHaving() {
        String epl = "select (select last(theString) from SupportBean#keepall having sum(intPrimitive) = s0.id) as c0 from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendSB("E1", 100);
        sendEventS0Assert(1, null);
        sendEventS0Assert(100, "E1");

        sendSB("E2", 5);
        sendEventS0Assert(100, null);
        sendEventS0Assert(105, "E2");

        stmt.destroy();
    }

    private void runAssertionGroupedTableWHaving() {
        epService.getEPAdministrator().createEPL("create table MyTableWith2Keys(k1 string primary key, k2 string primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTableWith2Keys select p10 as k1, p11 as k2, sum(id) as total from S1 group by p10, p11");

        String epl = "select (select sum(total) from MyTableWith2Keys group by k1 having sum(total) > 100) as c0 from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventS1(50, "G1", "S1");
        sendEventS1(50, "G1", "S2");
        sendEventS1(50, "G2", "S1");
        sendEventS1(50, "G2", "S2");
        sendEventS0Assert(null);

        sendEventS1(1, "G2", "S3");
        sendEventS0Assert(101);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionUngroupedTableWHaving() {
        epService.getEPAdministrator().createEPL("create table MyTable(total sum(int))");
        epService.getEPAdministrator().createEPL("into table MyTable select sum(intPrimitive) as total from SupportBean");

        String epl = "select (select sum(total) from MyTable having sum(total) > 100) as c0 from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventS0Assert(null);

        sendSB("E1", 50);
        sendEventS0Assert(null);

        sendSB("E2", 55);
        sendEventS0Assert(105);

        sendSB("E3", -5);
        sendEventS0Assert(null);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionGroupedCorrelatedWHaving() {
        String epl = "select (select sum(intPrimitive) from SupportBean#keepall where s0.id = intPrimitive group by theString having sum(intPrimitive) > 10) as c0 from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventS0Assert(10, null);

        sendSB("G1", 10);
        sendSB("G2", 10);
        sendSB("G2", 2);
        sendSB("G1", 9);
        sendEventS0Assert(null);

        sendSB("G2", 10);
        sendEventS0Assert(10, 20);

        sendSB("G1", 10);
        sendEventS0Assert(10, null);

        stmt.destroy();
    }

    private void runAssertionGroupedUncorrelatedWHaving() {
        String epl = "select (select sum(intPrimitive) from SupportBean#keepall group by theString having sum(intPrimitive) > 10) as c0 from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventS0Assert(null);

        sendSB("G1", 10);
        sendSB("G2", 9);
        sendEventS0Assert(null);

        sendSB("G2", 2);
        sendEventS0Assert(11);

        sendSB("G1", 3);
        sendEventS0Assert(null);

        stmt.destroy();
    }

    private void runAssertionUngroupedCorrelatedWHaving() {
        String epl = "select (select sum(intPrimitive) from SupportBean#keepall where theString = s0.p00 having sum(intPrimitive) > 10) as c0 from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventS0Assert("G1", null);

        sendSB("G1", 10);
        sendEventS0Assert("G1", null);

        sendSB("G2", 11);
        sendEventS0Assert("G1", null);
        sendEventS0Assert("G2", 11);

        sendSB("G1", 12);
        sendEventS0Assert("G1", 22);

        stmt.destroy();
    }

    private void runAssertionUngroupedUncorrelatedFiltered()
    {
        String stmtText = "select (select sum(id) from S1(id < 0)#length(3)) as value from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        runAssertionSumFilter();

        stmt.destroy();
    }

    private void runAssertionUngroupedUncorrelatedWWhereClause()
    {
        String stmtText = "select (select sum(id) from S1#length(3) where id < 0) as value from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        runAssertionSumFilter();

        stmt.destroy();
    }

    private void runAssertionCorrAggWhereGreater() {
        String[] fields = "p00".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "T1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("T1", 10));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "T1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "T1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"T1"});

        epService.getEPRuntime().sendEvent(new SupportBean("T1", 11));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(21, "T1"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(22, "T1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"T1"});
    }

    private void runAssertionSumFilter()
    {
        sendEventS0(1);
        assertEquals(null, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(1);
        sendEventS0(2);
        assertEquals(null, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(0);
        sendEventS0(3);
        assertEquals(null, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(-1);
        sendEventS0(4);
        assertEquals(-1, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(-3);
        sendEventS0(5);
        assertEquals(-4, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(-5);
        sendEventS0(6);
        assertEquals(-9, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(-2);   // note event leaving window
        sendEventS0(6);
        assertEquals(-10, listener.assertOneGetNewAndReset().get("value"));
    }

    private void runAssertionUngroupedUncorrelatedNoDataWindow() {
        String stmtText = "select p00 as c0, (select sum(intPrimitive) from SupportBean) as c1 from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        String[] fields = "c0,c1".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

        epService.getEPRuntime().sendEvent(new SupportBean("", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("", 20));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "E3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 30});

        stmt.destroy();
    }

    private void runAssertionUngroupedUncorrelatedWHaving() {
        String[] fields = "c0,c1".split(",");
        String epl = "select *, " +
                "(select sum(intPrimitive) from SupportBean#keepall having sum(intPrimitive) > 100) as c0," +
                "exists (select sum(intPrimitive) from SupportBean#keepall having sum(intPrimitive) > 100) as c1 " +
                "from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventS0Assert(fields, new Object[] {null, false});
        sendSB("E1", 10);
        sendEventS0Assert(fields, new Object[] {null, false});
        sendSB("E1", 91);
        sendEventS0Assert(fields, new Object[] {101, true});
        sendSB("E1", 2);
        sendEventS0Assert(fields, new Object[] {103, true});

        stmt.destroy();
    }

    private void runAssertionUngroupedCorrelated()
    {
        String stmtText = "select p00, " +
                "(select sum(intPrimitive) from SupportBean#keepall where theString = s0.p00) as sump00 " +
                "from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        String[] fields = "p00,sump00".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "T1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"T1", null});

        epService.getEPRuntime().sendEvent(new SupportBean("T1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, "T1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"T1", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("T1", 11));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, "T1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"T1", 21});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "T2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"T2", null});

        epService.getEPRuntime().sendEvent(new SupportBean("T2", -2));
        epService.getEPRuntime().sendEvent(new SupportBean("T2", -7));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "T2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"T2", -9});
        stmt.destroy();

        // test distinct
        fields = "theString,c0,c1,c2,c3".split(",");
        String viewExpr = "select theString, " +
                "(select count(sb.intPrimitive) from SupportBean()#keepall as sb where bean.theString = sb.theString) as c0, " +
                "(select count(distinct sb.intPrimitive) from SupportBean()#keepall as sb where bean.theString = sb.theString) as c1, " +
                "(select count(sb.intPrimitive, true) from SupportBean()#keepall as sb where bean.theString = sb.theString) as c2, " +
                "(select count(distinct sb.intPrimitive, true) from SupportBean()#keepall as sb where bean.theString = sb.theString) as c3 " +
                "from SupportBean as bean";
        stmt = epService.getEPAdministrator().createEPL(viewExpr);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E1", 1L, 1L, 1L, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 1L, 1L, 1L, 1L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 2L, 2L, 2L, 2L});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {"E2", 3L, 2L, 3L, 2L});

        stmt.destroy();
    }

    private void runAssertionUngroupedCorrelatedInWhereClause()
    {
        String stmtText = "select p00 from S0 as s0 where id > " +
                "(select sum(intPrimitive) from SupportBean#keepall where theString = s0.p00)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        runAssertionCorrAggWhereGreater();
        stmt.destroy();

        stmtText = "select p00 from S0 as s0 where id > " +
                "(select sum(intPrimitive) from SupportBean#keepall where theString||'X' = s0.p00||'X')";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        runAssertionCorrAggWhereGreater();
        stmt.destroy();
    }

    private void runAssertionUngroupedUncorrelatedInWhereClause()
    {
        String stmtText = "select * from MarketData " +
                "where price > (select max(price) from MarketData(symbol='GOOG')#lastevent) ";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEventMD("GOOG", 1);
        assertFalse(listener.isInvoked());

        sendEventMD("GOOG", 2);
        assertFalse(listener.isInvoked());

        Object theEvent = sendEventMD("IBM", 3);
        assertEquals(theEvent, listener.assertOneGetNewAndReset().getUnderlying());

        stmt.destroy();
    }

    private void runAssertionUngroupedUncorrelatedInSelectClause()
    {
        String stmtText = "select (select s0.id + max(s1.id) from S1#length(3) as s1) as value from S0 as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEventS0(1);
        assertEquals(null, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(100);
        sendEventS0(2);
        assertEquals(102, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(30);
        sendEventS0(3);
        assertEquals(103, listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void runAssertionUngroupedUncorrelatedInSelect()
    {
        String stmtText = "select (select max(id) from S1#length(3)) as value from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEventS0(1);
        assertEquals(null, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(100);
        sendEventS0(2);
        assertEquals(100, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(200);
        sendEventS0(3);
        assertEquals(200, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(190);
        sendEventS0(4);
        assertEquals(200, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(180);
        sendEventS0(5);
        assertEquals(200, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(170);   // note event leaving window
        sendEventS0(6);
        assertEquals(190, listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void runAssertionUngroupedUncorrelatedTwoAggStopStart()
    {
        String stmtText = "select (select avg(id) + max(id) from S1#length(3)) as value from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEventS0(1);
        assertEquals(null, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(100);
        sendEventS0(2);
        assertEquals(200.0, listener.assertOneGetNewAndReset().get("value"));

        sendEventS1(200);
        sendEventS0(3);
        assertEquals(350.0, listener.assertOneGetNewAndReset().get("value"));

        stmt.stop();
        sendEventS1(10000);
        sendEventS0(4);
        assertFalse(listener.isInvoked());
        stmt.start();

        sendEventS1(10);
        sendEventS0(5);
        assertEquals(20.0, listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void sendEventS0(int id)
    {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(id));
    }

    private void sendEventS0(int id, String p00)
    {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(id, p00));
    }

    private void sendEventS1(int id, String p10, String p11)
    {
        epService.getEPRuntime().sendEvent(new SupportBean_S1(id, p10, p11));
    }

    private void sendEventS1(int id)
    {
        epService.getEPRuntime().sendEvent(new SupportBean_S1(id));
    }

    private Object sendEventMD(String symbol, double price)
    {
        Object theEvent = new SupportMarketDataBean(symbol, price, 0L, "");
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }

    private void sendSB(String theString, int intPrimitive) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
    }

    private void sendEventS0Assert(Object expected) {
        sendEventS0Assert(0, expected);
    }

    private void sendEventS0Assert(int id, Object expected) {
        sendEventS0(id, null);
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }

    private void sendEventS0Assert(String p00, Object expected) {
        sendEventS0(0, p00);
        assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
    }

    private void sendEventS0Assert(String[] fields, Object[] expected) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
    }
}
