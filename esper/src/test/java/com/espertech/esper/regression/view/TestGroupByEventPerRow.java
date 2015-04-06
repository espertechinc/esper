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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanString;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestGroupByEventPerRow extends TestCase
{
    private static String SYMBOL_DELL = "DELL";
    private static String SYMBOL_IBM = "IBM";

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testCriteriaByDotMethod() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String epl = "select sb.getLongPrimitive() as c0, sum(intPrimitive) as c1 from SupportBean.win:length_batch(2) as sb group by sb.getTheString()";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        makeSendSupportBean("E1", 10, 100L);
        makeSendSupportBean("E1", 20, 200L);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "c0,c1".split(","),
                new Object[][]{{100L, 30}, {200L, 30}});
    }

    public void testIterateUnbound() {
        String[] fields = "c0,c1".split(",");
        String epl = "@IterableUnbound select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 10}, {"E2", 20}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 11));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][] {{"E1", 21}, {"E2", 20}});
    }

    public void testUnaggregatedHaving() {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select theString from SupportBean group by theString having intPrimitive > 5");
        stmt.addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 5));
        assertFalse(listener.isInvoked());
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 6));
        assertEquals("E1", listener.assertOneGetNewAndReset().get("theString"));

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 7));
        assertEquals("E3", listener.assertOneGetNewAndReset().get("theString"));
    }

    public void testWildcard() {

        // test no output limit
        String fields[] = "theString, intPrimitive, minval".split(",");
        String epl = "select *, min(intPrimitive) as minval from SupportBean.win:length(2) group by theString";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(epl);
        selectTestView.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, 10});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 9));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 9, 9});

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 11));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"G1", 11, 9});
    }

    public void testAggregationOverGroupedProps()
    {
        // test for ESPER-185
        String fields[] = "volume,symbol,price,mycount".split(",");
        String viewExpr = "select irstream volume,symbol,price,count(price) as mycount " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:length(5) " +
                          "group by symbol, price";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        sendEvent(SYMBOL_DELL, 1000, 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1000L, "DELL", 10.0, 1L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 1L}});

        sendEvent(SYMBOL_DELL, 900, 11);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{900L, "DELL", 11.0, 1L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 1L}, {900L, "DELL", 11.0, 1L}});
        listener.reset();

        sendEvent(SYMBOL_DELL, 1500, 10);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{1500L, "DELL", 10.0, 2L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 2L}, {900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 2L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 500, 5);
        assertEquals(1, listener.getNewDataList().size());
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{500L, "IBM", 5.0, 1L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 2L}, {900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 2L}, {500L, "IBM", 5.0, 1L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 600, 5);
        assertEquals(1, listener.getLastNewData().length);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{600L, "IBM", 5.0, 2L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 2L}, {900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 2L}, {500L, "IBM", 5.0, 2L}, {600L, "IBM", 5.0, 2L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 500, 5);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{500L, "IBM", 5.0, 3L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{1000L, "DELL", 10.0, 1L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 1L}, {500L, "IBM", 5.0, 3L}, {600L, "IBM", 5.0, 3L}, {500L, "IBM", 5.0, 3L}});
        listener.reset();

        sendEvent(SYMBOL_IBM, 600, 5);
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fields, new Object[]{600L, "IBM", 5.0, 4L});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fields, new Object[]{900L, "DELL", 11.0, 0L});
        EPAssertionUtil.assertPropsPerRow(selectTestView.iterator(), fields, new Object[][]{{1500L, "DELL", 10.0, 1L}, {500L, "IBM", 5.0, 4L}, {600L, "IBM", 5.0, 4L}, {500L, "IBM", 5.0, 4L}, {600L, "IBM", 5.0, 4L}});
        listener.reset();
    }

    public void testSumOneView()
    {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String viewExpr = "select irstream symbol, volume, sum(price) as mySum " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:length(3) " +
                          "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                          "group by symbol";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runAssertion(selectTestView);
    }

    public void testSumJoin()
    {
        // Every event generates a new row, this time we sum the price by symbol and output volume
        String viewExpr = "select irstream symbol, volume, sum(price) as mySum " +
                          "from " + SupportBeanString.class.getName() + ".win:length(100) as one, " +
                                    SupportMarketDataBean.class.getName() + ".win:length(3) as two " +
                          "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                          "  and one.theString = two.symbol " +
                          "group by symbol";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_DELL));
        epService.getEPRuntime().sendEvent(new SupportBeanString(SYMBOL_IBM));

        runAssertion(selectTestView);
    }

    public void testInsertInto()
    {
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        String eventType = SupportMarketDataBean.class.getName();
        String stmt = " select symbol as symbol, avg(price) as average, sum(volume) as sumation from " + eventType + ".win:length(3000)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmt);
        statement.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 10D, 20000L, null));
        EventBean eventBean = listenerOne.getLastNewData()[0];
        assertEquals("IBM", eventBean.get("symbol"));
        assertEquals(10d, eventBean.get("average"));
        assertEquals(20000L, eventBean.get("sumation"));

        // create insert into statements
        stmt =  "insert into StockAverages select symbol as symbol, avg(price) as average, sum(volume) as sumation " +
                    "from " + eventType + ".win:length(3000)";
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

    private void runAssertion(EPStatement selectTestView)
    {
        String[] fields = new String[] {"symbol", "volume", "mySum"};
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, null);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("volume"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("mySum"));

        sendEvent(SYMBOL_DELL, 10000, 51);
        assertEvents(SYMBOL_DELL, 10000, 51);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{
                {"DELL", 10000L, 51d}});

        sendEvent(SYMBOL_DELL, 20000, 52);
        assertEvents(SYMBOL_DELL, 20000, 103);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{
                {"DELL", 10000L, 103d}, {"DELL", 20000L, 103d}});

        sendEvent(SYMBOL_IBM, 30000, 70);
        assertEvents(SYMBOL_IBM, 30000, 70);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{
                {"DELL", 10000L, 103d}, {"DELL", 20000L, 103d}, {"IBM", 30000L, 70d}});

        sendEvent(SYMBOL_IBM, 10000, 20);
        assertEvents(SYMBOL_DELL, 10000, 52, SYMBOL_IBM, 10000, 90);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{
                {"DELL", 20000L, 52d}, {"IBM", 30000L, 90d}, {"IBM", 10000L, 90d}});

        sendEvent(SYMBOL_DELL, 40000, 45);
        assertEvents(SYMBOL_DELL, 20000, 45, SYMBOL_DELL, 40000, 45);
        EPAssertionUtil.assertPropsPerRowAnyOrder(selectTestView.iterator(), fields, new Object[][]{
                {"IBM", 10000L, 90d}, {"IBM", 30000L, 90d}, {"DELL", 40000L, 45d}});
    }

    private void assertEvents(String symbol, long volume, double sum)
    {
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

    private void assertEvents(String symbolOld, long volumeOld, double sumOld,
                              String symbolNew, long volumeNew, double sumNew)
    {
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

    private void sendEvent(String symbol, long volume, double price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private SupportBean makeSendSupportBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private static final Log log = LogFactory.getLog(TestGroupByEventPerRow.class);
}
