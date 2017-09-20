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
package com.espertech.esper.regression.resultset.querytype;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanString;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecQuerytypeAggregateGrouped implements RegressionExecution {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCriteriaByDotMethod(epService);
        runAssertionIterateUnbound(epService);
        runAssertionUnaggregatedHaving(epService);
        runAssertionWildcard(epService);
        runAssertionAggregationOverGroupedProps(epService);
        runAssertionSumOneView(epService);
        runAssertionSumJoin(epService);
        runAssertionInsertInto(epService);
    }

    private void runAssertionCriteriaByDotMethod(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String epl = "select sb.getLongPrimitive() as c0, sum(intPrimitive) as c1 from SupportBean#length_batch(2) as sb group by sb.getTheString()";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        makeSendSupportBean(epService, "E1", 10, 100L);
        makeSendSupportBean(epService, "E1", 20, 200L);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "c0,c1".split(","),
                new Object[][]{{100L, 30}, {200L, 30}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionIterateUnbound(EPServiceProvider epService) {
        String[] fields = "c0,c1".split(",");
        String epl = "@IterableUnbound select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 10}, {"E2", 20}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});

        stmt.destroy();
    }

    private void runAssertionUnaggregatedHaving(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString from SupportBean group by theString having intPrimitive > 5");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 6));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 7));
        assertEquals("E3", listener.assertOneGetNewAndReset().get("theString"));

        stmt.destroy();
    }

    private void runAssertionWildcard(EPServiceProvider epService) {

        // test no output limit
        String[] fields = "theString, intPrimitive, minval".split(",");
        String epl = "select *, min(intPrimitive) as minval from SupportBean#length(2) group by theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, 10});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 9));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 9, 9});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 11, 9});

        stmt.destroy();
    }

    private void runAssertionAggregationOverGroupedProps(EPServiceProvider epService) {
        // test for ESPER-185
        String[] fields = "volume,symbol,price,mycount".split(",");
        String epl = "select irstream volume,symbol,price,count(price) as mycount " +
                "from " + SupportMarketDataBean.class.getName() + "#length(5) " +
                "group by symbol, price";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, SYMBOL_DELL, 1000, 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1000L, "DELL", 10.0, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 1L}});

        sendEvent(epService, SYMBOL_DELL, 900, 11);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{900L, "DELL", 11.0, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 1L}, {900L, "DELL", 11.0, 1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_DELL, 1500, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1500L, "DELL", 10.0, 2L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 2L}, {900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 2L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 500, 5);
        assertEquals(1, listener.getNewDataList().size());
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{500L, "IBM", 5.0, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 2L}, {900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 2L}, {500L, "IBM", 5.0, 1L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 600, 5);
        assertEquals(1, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{600L, "IBM", 5.0, 2L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 2L}, {900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 2L}, {500L, "IBM", 5.0, 2L}, {600L, "IBM", 5.0, 2L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 500, 5);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{500L, "IBM", 5.0, 3L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{1000L, "DELL", 10.0, 1L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 1L}, {500L, "IBM", 5.0, 3L}, {600L, "IBM", 5.0, 3L}, {500L, "IBM", 5.0, 3L}});
        listener.reset();

        sendEvent(epService, SYMBOL_IBM, 600, 5);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{600L, "IBM", 5.0, 4L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{900L, "DELL", 11.0, 0L});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{1500L, "DELL", 10.0, 1L}, {500L, "IBM", 5.0, 4L}, {600L, "IBM", 5.0, 4L}, {500L, "IBM", 5.0, 4L}, {600L, "IBM", 5.0, 4L}});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionSumOneView(EPServiceProvider epService) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = "select irstream symbol, volume, sum(price) as mySum " +
                "from " + SupportMarketDataBean.class.getName() + "#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        tryAssertionSum(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionSumJoin(EPServiceProvider epService) {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String epl = "select irstream symbol, volume, sum(price) as mySum " +
                "from " + SupportBeanString.class.getName() + "#length(100) as one, " +
                SupportMarketDataBean.class.getName() + "#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        tryAssertionSum(epService, listener, stmt);

        stmt.destroy();
    }

    private void runAssertionInsertInto(EPServiceProvider epService) {
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        String eventType = SupportMarketDataBean.class.getName();
        String stmt = " select symbol as symbol, avg(price) as average, sum(volume) as sumation from " + eventType + "#length(3000)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 10D, 20000L, null));
        EventBean eventBean = listenerOne.getLastNewData()[0];
        assertEquals("IBM", eventBean.get("symbol"));
        assertEquals(10d, eventBean.get("average"));
        assertEquals(20000L, eventBean.get("sumation"));

        // create insert into statements
        stmt = "insert into StockAverages select symbol as symbol, avg(price) as average, sum(volume) as sumation " +
                "from " + eventType + "#length(3000)";
        statement = epService.getEPAdministrator().createEPL(stmt);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        statement.addListener(listenerTwo);

        stmt = " select * from StockAverages";
        statement = epService.getEPAdministrator().createEPL(stmt);
        SupportUpdateListener listenerThree = new SupportUpdateListener();
        statement.addListener(listenerThree);

        // send event
        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 20D, 40000L, null));
        eventBean = listenerOne.getLastNewData()[0];
        assertEquals("IBM", eventBean.get("symbol"));
        assertEquals(15d, eventBean.get("average"));
        assertEquals(60000L, eventBean.get("sumation"));

        assertEquals(1, listenerThree.getNewDataList().size());
        assertEquals(1, listenerThree.getLastNewData().length);
        eventBean = listenerThree.getLastNewData()[0];
        assertEquals("IBM", eventBean.get("symbol"));
        assertEquals(20d, eventBean.get("average"));
        assertEquals(40000L, eventBean.get("sumation"));
    }

    private void tryAssertionSum(EPServiceProvider epService, SupportUpdateListener listener, EPStatement stmt) {
        String[] fields = new String[]{"symbol", "volume", "mySum"};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("volume"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("mySum"));

        sendEvent(epService, SYMBOL_DELL, 10000, 51);
        assertEvents(listener, SYMBOL_DELL, 10000, 51);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{
                {"DELL", 10000L, 51d}});

        sendEvent(epService, SYMBOL_DELL, 20000, 52);
        assertEvents(listener, SYMBOL_DELL, 20000, 103);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{
                {"DELL", 10000L, 103d}, {"DELL", 20000L, 103d}});

        sendEvent(epService, SYMBOL_IBM, 30000, 70);
        assertEvents(listener, SYMBOL_IBM, 30000, 70);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{
                {"DELL", 10000L, 103d}, {"DELL", 20000L, 103d}, {"IBM", 30000L, 70d}});

        sendEvent(epService, SYMBOL_IBM, 10000, 20);
        assertEvents(listener, SYMBOL_DELL, 10000, 52, SYMBOL_IBM, 10000, 90);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{
                {"DELL", 20000L, 52d}, {"IBM", 30000L, 90d}, {"IBM", 10000L, 90d}});

        sendEvent(epService, SYMBOL_DELL, 40000, 45);
        assertEvents(listener, SYMBOL_DELL, 20000, 45, SYMBOL_DELL, 40000, 45);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{
                {"IBM", 10000L, 90d}, {"IBM", 30000L, 90d}, {"DELL", 40000L, 45d}});
    }

    private void assertEvents(SupportUpdateListener listener, String symbol, long volume, double sum) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEquals(symbol, newData[0].get("symbol"));
        assertEquals(volume, newData[0].get("volume"));
        assertEquals(sum, newData[0].get("mySum"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void assertEvents(SupportUpdateListener listener, String symbolOld, long volumeOld, double sumOld,
                              String symbolNew, long volumeNew, double sumNew) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbolOld, oldData[0].get("symbol"));
        assertEquals(volumeOld, oldData[0].get("volume"));
        assertEquals(sumOld, oldData[0].get("mySum"));

        assertEquals(symbolNew, newData[0].get("symbol"));
        assertEquals(volumeNew, newData[0].get("volume"));
        assertEquals(sumNew, newData[0].get("mySum"));

        listener.reset();
        assertFalse(listener.isInvoked());
    }

    private void sendEvent(EPServiceProvider epService, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private SupportBean makeSendSupportBean(EPServiceProvider epService, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }
}
