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

package com.espertech.esper.regression.expr;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportMarketDataBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;

public class TestPreviousFunction extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testExprNameAndTypeAndSODA() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String epl = "select " +
                "prev(1,intPrimitive), " +
                "prev(1,sb), " +
                "prevtail(1,intPrimitive), " +
                "prevtail(1,sb), " +
                "prevwindow(intPrimitive), " +
                "prevwindow(sb), " +
                "prevcount(intPrimitive), " +
                "prevcount(sb) " +
                "from SupportBean.win:time(1 minutes) as sb";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EventBean resultBean = listener.getNewDataListFlattened()[1];

        Object[][] rows = new Object[][] {
                {"prev(1,intPrimitive)", Integer.class},
                {"prev(1,sb)", SupportBean.class},
                {"prevtail(1,intPrimitive)", Integer.class},
                {"prevtail(1,sb)", SupportBean.class},
                {"prevwindow(intPrimitive)", Integer[].class},
                {"prevwindow(sb)", SupportBean[].class},
                {"prevcount(intPrimitive)", Long.class},
                {"prevcount(sb)", Long.class}
                };
        for (int i = 0; i < rows.length; i++) {
            String message = "For prop '" + rows[i][0] + "'";
            EventPropertyDescriptor prop = stmt.getEventType().getPropertyDescriptors()[i];
            assertEquals(message, rows[i][0], prop.getPropertyName());
            assertEquals(message, rows[i][1], prop.getPropertyType());
            Object result = resultBean.get(prop.getPropertyName());
            assertEquals(message, prop.getPropertyType(), result.getClass());
        }

        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(model.toEPL(), epl);
        stmt = epService.getEPAdministrator().createEPL(epl);
        assertEquals(stmt.getText(), epl);
    }

    public void testPrevStream()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        String text = "select prev(1, s0) as result, " +
                "prevtail(0, s0) as tailresult," +
                "prevwindow(s0) as windowresult," +
                "prevcount(s0) as countresult " +
                "from S0.win:length(2) as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        String[] fields = "result,tailresult,windowresult,countresult".split(",");

        SupportBean_S0 e1 = new SupportBean_S0(1);
        epService.getEPRuntime().sendEvent(e1);

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{null, e1, new Object[]{e1}, 1L});

        SupportBean_S0 e2 = new SupportBean_S0(2);
        epService.getEPRuntime().sendEvent(e2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{e1, e1, new Object[]{e2, e1}, 2L});
        assertEquals(SupportBean_S0.class, stmt.getEventType().getPropertyType("result"));

        SupportBean_S0 e3 = new SupportBean_S0(3);
        epService.getEPRuntime().sendEvent(e3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{e2, e2, new Object[]{e3, e2}, 2L});
    }

    public void testPrevCountStarWithStaticMethod()
    {
        String text = "select irstream count(*) as total, " +
                      "prev(" + TestPreviousFunction.class.getName() + ".intToLong(count(*)) - 1, price) as firstPrice from " + SupportMarketDataBean.class.getName() + ".win:time(60)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        assertPrevCount();
    }

    public void testPrevCountStar()
    {
        String text = "select irstream count(*) as total, " +
                      "prev(count(*) - 1, price) as firstPrice from " + SupportMarketDataBean.class.getName() + ".win:time(60)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(listener);

        assertPrevCount();
    }

    private void assertPrevCount()
    {
        sendTimer(0);
        sendMarketEvent("IBM", 75);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 1L, 75D);

        sendMarketEvent("IBM", 76);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 2L, 75D);

        sendTimer(10000);
        sendMarketEvent("IBM", 77);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 3L, 75D);

        sendTimer(20000);
        sendMarketEvent("IBM", 78);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 4L, 75D);

        sendTimer(50000);
        sendMarketEvent("IBM", 79);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 5L, 75D);

        sendTimer(60000);
        assertEquals(1, listener.getOldDataList().size());
        EventBean[] oldData = listener.getLastOldData();
        assertEquals(2, oldData.length);
        assertCountAndPrice(oldData[0], 3L, null);
        listener.reset();

        sendMarketEvent("IBM", 80);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 4L, 77D);

        sendTimer(65000);
        assertFalse(listener.isInvoked());

        sendTimer(70000);
        assertEquals(1, listener.getOldDataList().size());
        oldData = listener.getLastOldData();
        assertEquals(1, oldData.length);
        assertCountAndPrice(oldData[0], 3L, null);
        listener.reset();

        sendTimer(80000);
        listener.reset();

        sendMarketEvent("IBM", 81);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 3L, 79D);

        sendTimer(120000);
        listener.reset();

        sendMarketEvent("IBM", 82);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 2L, 81D);

        sendTimer(300000);
        listener.reset();

        sendMarketEvent("IBM", 83);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 1L, 83D);
    }

    public void testPerGroupTwoCriteria()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("MDBean", SupportMarketDataBean.class);
        String viewExpr = "select symbol, feed, " +
                "prev(1, price) as prevPrice, " +
                "prevtail(price) as tailPrice, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from MDBean.std:groupwin(symbol, feed).win:length(2)";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);
        String[] fields = "symbol,feed,prevPrice,tailPrice,countPrice,windowPrice".split(",");

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 10, 0L, "F1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"IBM", "F1", null, 10d, 1L, splitDoubles("10d")});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 11, 0L, "F1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"IBM", "F1", 10d, 10d, 2L, splitDoubles("11d,10d")});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("MSFT", 100, 0L, "F2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"MSFT", "F2", null, 100d, 1L, splitDoubles("100d")});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 12, 0L, "F2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"IBM", "F2", null, 12d, 1L, splitDoubles("12d")});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 13, 0L, "F1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"IBM", "F1", 11d, 11d, 2L, splitDoubles("13d,11d")});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("MSFT", 101, 0L, "F2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"MSFT", "F2", 100d, 100d, 2L, splitDoubles("101d,100d")});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("IBM", 17, 0L, "F2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"IBM", "F2", 12d, 12d, 2L, splitDoubles("17d,12d")});

        // test length window overflow
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("select prev(5,intPrimitive) as val0 from SupportBean.std:groupwin(theString).win:length(5)").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 11));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("A", 12));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("A", 13));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("A", 14));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("A", 15));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("C", 20));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("C", 21));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("C", 22));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("C", 23));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("C", 24));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("B", 31));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("C", 25));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean("A", 16));
        assertEquals(null, listener.assertOneGetNewAndReset().get("val0"));
    }

    public void testSortWindowPerGroup()
    {
        // descending sort
        String viewExpr = "select " +
                "symbol, " +
                "prev(1, price) as prevPrice, " +
                "prev(2, price) as prevPrevPrice, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + ".std:groupwin(symbol).ext:sort(10, price asc) ";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrice"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrevPrice"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevTail0Price"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevTail1Price"));
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("countPrice"));
        assertEquals(Double[].class, selectTestView.getEventType().getPropertyType("windowPrice"));

        sendMarketEvent("IBM", 75);
        assertReceived("IBM", null, null, 75d, null, 1L, splitDoubles("75d"));
        sendMarketEvent("IBM", 80);
        assertReceived("IBM", 80d, null, 80d, 75d, 2L, splitDoubles("75d,80d"));
        sendMarketEvent("IBM", 79);
        assertReceived("IBM", 79d, 80d, 80d, 79d, 3L, splitDoubles("75d,79d,80d"));
        sendMarketEvent("IBM", 81);
        assertReceived("IBM", 79d, 80d, 81d, 80d, 4L, splitDoubles("75d,79d,80d,81d"));
        sendMarketEvent("IBM", 79.5);
        assertReceived("IBM", 79d, 79.5d, 81d, 80d, 5L, splitDoubles("75d,79d,79.5,80d,81d"));    // 75, 79, 79.5, 80, 81

        sendMarketEvent("MSFT", 10);
        assertReceived("MSFT", null, null, 10d, null, 1L, splitDoubles("10d"));
        sendMarketEvent("MSFT", 20);
        assertReceived("MSFT", 20d, null, 20d, 10d, 2L, splitDoubles("10d,20d"));
        sendMarketEvent("MSFT", 21);
        assertReceived("MSFT", 20d, 21d, 21d, 20d, 3L, splitDoubles("10d,20d,21d")); // 10, 20, 21

        sendMarketEvent("IBM", 74d);
        assertReceived("IBM", 75d, 79d, 81d, 80d, 6L, splitDoubles("74d,75d,79d,79.5,80d,81d"));  // 74, 75, 79, 79.5, 80, 81

        sendMarketEvent("MSFT", 19);
        assertReceived("MSFT", 19d, 20d, 21d, 20d, 4L, splitDoubles("10d,19d,20d,21d")); // 10, 19, 20, 21
    }

    public void testTimeBatchPerGroup()
    {
        String viewExpr = "select " +
                "symbol, " +
                "prev(1, price) as prevPrice, " +
                "prev(2, price) as prevPrevPrice, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + ".std:groupwin(symbol).win:time_batch(1 sec) ";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrice"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrevPrice"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevTail0Price"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevTail1Price"));

        sendTimer(0);
        sendMarketEvent("IBM", 75);
        sendMarketEvent("MSFT", 40);
        sendMarketEvent("IBM", 76);
        sendMarketEvent("CIC", 1);
        sendTimer(1000);

        EventBean[] events = listener.getLastNewData();
        // order not guaranteed as timed batch, however for testing the order is reliable as schedule buckets are created
        // in a predictable order
        // Previous is looking at the same batch, doesn't consider outside of window
        assertReceived(events[0], "IBM", null, null, 75d, 76d, 2L, splitDoubles("76d,75d"));
        assertReceived(events[1], "IBM", 75d, null, 75d, 76d, 2L, splitDoubles("76d,75d"));
        assertReceived(events[2], "MSFT", null, null, 40d, null, 1L, splitDoubles("40d"));
        assertReceived(events[3], "CIC", null, null, 1d, null, 1L, splitDoubles("1d"));

        // Next batch, previous is looking only within the same batch
        sendMarketEvent("MSFT", 41);
        sendMarketEvent("IBM", 77);
        sendMarketEvent("IBM", 78);
        sendMarketEvent("CIC", 2);
        sendMarketEvent("MSFT", 42);
        sendMarketEvent("CIC", 3);
        sendMarketEvent("CIC", 4);
        sendTimer(2000);

        events = listener.getLastNewData();
        assertReceived(events[0], "IBM", null, null, 77d, 78d, 2L, splitDoubles("78d,77d"));
        assertReceived(events[1], "IBM", 77d, null, 77d, 78d, 2L, splitDoubles("78d,77d"));
        assertReceived(events[2], "MSFT", null, null, 41d, 42d, 2L, splitDoubles("42d,41d"));
        assertReceived(events[3], "MSFT", 41d, null, 41d, 42d, 2L, splitDoubles("42d,41d"));
        assertReceived(events[4], "CIC", null, null, 2d, 3d, 3L, splitDoubles("4d,3d,2d"));
        assertReceived(events[5], "CIC", 2d, null, 2d, 3d, 3L, splitDoubles("4d,3d,2d"));
        assertReceived(events[6], "CIC", 3d, 2d, 2d, 3d, 3L, splitDoubles("4d,3d,2d"));

        // test for memory leak - comment in and run with large number
        /*
        for (int i = 0; i < 10000; i++)
        {
            sendMarketEvent("MSFT", 41);
            sendTimer(1000 * i);
            listener.reset();
        }
        */
    }

    public void testLengthBatchPerGroup()
    {
        // Also testing the alternative syntax here of "prev(property)" and "prev(property, index)" versus "prev(index, property)"
        String viewExpr = "select irstream " +
                "symbol, " +
                "prev(price) as prevPrice, " +
                "prev(price, 2) as prevPrevPrice, " +
                "prevtail(price, 0) as prevTail0Price, " +
                "prevtail(price, 1) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + ".std:groupwin(symbol).win:length_batch(3) ";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrice"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrevPrice"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevTail0Price"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevTail1Price"));

        sendMarketEvent("IBM", 75);
        sendMarketEvent("MSFT", 50);
        sendMarketEvent("IBM", 76);
        sendMarketEvent("CIC", 1);
        assertFalse(listener.isInvoked());
        sendMarketEvent("IBM", 77);

        EventBean[] eventsNew = listener.getLastNewData();
        assertEquals(3, eventsNew.length);
        assertReceived(eventsNew[0], "IBM", null, null, 75d, 76d, 3L, splitDoubles("77d,76d,75d"));
        assertReceived(eventsNew[1], "IBM", 75d, null, 75d, 76d, 3L, splitDoubles("77d,76d,75d"));
        assertReceived(eventsNew[2], "IBM", 76d, 75d, 75d, 76d, 3L, splitDoubles("77d,76d,75d"));
        listener.reset();

        // Next batch, previous is looking only within the same batch
        sendMarketEvent("MSFT", 51);
        sendMarketEvent("IBM", 78);
        sendMarketEvent("IBM", 79);
        sendMarketEvent("CIC", 2);
        sendMarketEvent("CIC", 3);

        eventsNew = listener.getLastNewData();
        assertEquals(3, eventsNew.length);
        assertReceived(eventsNew[0], "CIC", null, null, 1d, 2d, 3L, splitDoubles("3d,2d,1d"));
        assertReceived(eventsNew[1], "CIC", 1d, null, 1d, 2d, 3L, splitDoubles("3d,2d,1d"));
        assertReceived(eventsNew[2], "CIC", 2d, 1d, 1d, 2d, 3L, splitDoubles("3d,2d,1d"));
        listener.reset();

        sendMarketEvent("MSFT", 52);

        eventsNew = listener.getLastNewData();
        assertEquals(3, eventsNew.length);
        assertReceived(eventsNew[0], "MSFT", null, null, 50d, 51d, 3L, splitDoubles("52d,51d,50d"));
        assertReceived(eventsNew[1], "MSFT", 50d, null, 50d, 51d, 3L, splitDoubles("52d,51d,50d"));
        assertReceived(eventsNew[2], "MSFT", 51d, 50d, 50d, 51d, 3L, splitDoubles("52d,51d,50d"));
        listener.reset();

        sendMarketEvent("IBM", 80);

        eventsNew = listener.getLastNewData();
        EventBean[] eventsOld = listener.getLastOldData();
        assertEquals(3, eventsNew.length);
        assertEquals(3, eventsOld.length);
        assertReceived(eventsNew[0], "IBM", null, null, 78d, 79d, 3L, splitDoubles("80d,79d,78d"));
        assertReceived(eventsNew[1], "IBM", 78d, null, 78d, 79d, 3L, splitDoubles("80d,79d,78d"));
        assertReceived(eventsNew[2], "IBM", 79d, 78d, 78d, 79d, 3L, splitDoubles("80d,79d,78d"));
        assertReceived(eventsOld[0], "IBM", null, null, null, null, null, null);
        assertReceived(eventsOld[1], "IBM", null, null, null, null, null, null);
        assertReceived(eventsOld[2], "IBM", null, null, null, null, null, null);
    }

    public void testTimeWindowPerGroup()
    {
        String viewExpr = "select " +
                "symbol, " +
                "prev(1, price) as prevPrice, " +
                "prev(2, price) as prevPrevPrice, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + ".std:groupwin(symbol).win:time(20 sec) ";
        assertPerGroup(viewExpr);
    }

    public void testExtTimeWindowPerGroup()
    {
        String viewExpr = "select " +
                "symbol, " +
                "prev(1, price) as prevPrice, " +
                "prev(2, price) as prevPrevPrice, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + ".std:groupwin(symbol).win:ext_timed(volume, 20 sec) ";
        assertPerGroup(viewExpr);
    }

    public void testLengthWindowPerGroup()
    {
        String viewExpr =
                "select symbol, " +
                "prev(1, price) as prevPrice, " +
                "prev(2, price) as prevPrevPrice, " +
                "prevtail(price, 0) as prevTail0Price, " +
                "prevtail(price, 1) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + ".std:groupwin(symbol).win:length(10) ";
        assertPerGroup(viewExpr);
    }

    public void testPreviousTimeWindow()
    {
        String viewExpr = "select irstream symbol as currSymbol, " +
                          " prev(2, symbol) as prevSymbol, " +
                          " prev(2, price) as prevPrice, " +
                          " prevtail(0, symbol) as prevTailSymbol, " +
                          " prevtail(0, price) as prevTailPrice, " +
                          " prevtail(1, symbol) as prevTail1Symbol, " +
                          " prevtail(1, price) as prevTail1Price, " +
                          " prevcount(price) as prevCountPrice, " +
                          " prevwindow(price) as prevWindowPrice " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:time(1 min) ";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("prevSymbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrice"));

        sendTimer(0);
        assertFalse(listener.isInvoked());

        sendMarketEvent("D1", 1);
        assertNewEventWTail("D1", null, null, "D1", 1d, null, null, 1L, splitDoubles("1d"));

        sendTimer(1000);
        assertFalse(listener.isInvoked());

        sendMarketEvent("D2", 2);
        assertNewEventWTail("D2", null, null, "D1", 1d, "D2", 2d, 2L, splitDoubles("2d,1d"));

        sendTimer(2000);
        assertFalse(listener.isInvoked());

        sendMarketEvent("D3", 3);
        assertNewEventWTail("D3", "D1", 1d, "D1", 1d, "D2", 2d, 3L, splitDoubles("3d,2d,1d"));

        sendTimer(3000);
        assertFalse(listener.isInvoked());

        sendMarketEvent("D4", 4);
        assertNewEventWTail("D4", "D2", 2d, "D1", 1d, "D2", 2d, 4L, splitDoubles("4d,3d,2d,1d"));

        sendTimer(4000);
        assertFalse(listener.isInvoked());

        sendMarketEvent("D5", 5);
        assertNewEventWTail("D5", "D3", 3d, "D1", 1d, "D2", 2d, 5L, splitDoubles("5d,4d,3d,2d,1d"));

        sendTimer(30000);
        assertFalse(listener.isInvoked());

        sendMarketEvent("D6", 6);
        assertNewEventWTail("D6", "D4", 4d, "D1", 1d, "D2", 2d, 6L, splitDoubles("6d,5d,4d,3d,2d,1d"));

        // Test remove stream, always returns null as previous function
        // returns null for remove stream for time windows
        sendTimer(60000);
        assertOldEventWTail("D1", null, null, null, null, null, null, null, null);
        sendTimer(61000);
        assertOldEventWTail("D2", null, null, null, null, null, null, null, null);
        sendTimer(62000);
        assertOldEventWTail("D3", null, null, null, null, null, null, null, null);
        sendTimer(63000);
        assertOldEventWTail("D4", null, null, null, null, null, null, null, null);
        sendTimer(64000);
        assertOldEventWTail("D5", null, null, null, null, null, null, null, null);
        sendTimer(90000);
        assertOldEventWTail("D6", null, null, null, null, null, null, null, null);
    }

    public void testPreviousExtTimedWindow()
    {
        String viewExpr = "select irstream symbol as currSymbol, " +
                          " prev(2, symbol) as prevSymbol, " +
                          " prev(2, price) as prevPrice, " +
                          " prevtail(0, symbol) as prevTailSymbol, " +
                          " prevtail(0, price) as prevTailPrice, " +
                          " prevtail(1, symbol) as prevTail1Symbol, " +
                          " prevtail(1, price) as prevTail1Price, " +
                          " prevcount(price) as prevCountPrice, " +
                          " prevwindow(price) as prevWindowPrice " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:ext_timed(volume, 1 min) ";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("prevSymbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrice"));
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("prevTailSymbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevTailPrice"));

        sendMarketEvent("D1", 1, 0);
        assertNewEventWTail("D1", null, null, "D1", 1d, null, null, 1L, splitDoubles("1d"));

        sendMarketEvent("D2", 2, 1000);
        assertNewEventWTail("D2", null, null, "D1", 1d, "D2", 2d, 2L, splitDoubles("2d,1d"));

        sendMarketEvent("D3", 3, 3000);
        assertNewEventWTail("D3", "D1", 1d, "D1", 1d, "D2", 2d, 3L, splitDoubles("3d,2d,1d"));

        sendMarketEvent("D4", 4, 4000);
        assertNewEventWTail("D4", "D2", 2d, "D1", 1d, "D2", 2d, 4L, splitDoubles("4d,3d,2d,1d"));

        sendMarketEvent("D5", 5, 5000);
        assertNewEventWTail("D5", "D3", 3d, "D1", 1d, "D2", 2d, 5L, splitDoubles("5d,4d,3d,2d,1d"));

        sendMarketEvent("D6", 6, 30000);
        assertNewEventWTail("D6", "D4", 4d, "D1", 1d, "D2", 2d, 6L, splitDoubles("6d,5d,4d,3d,2d,1d"));

        sendMarketEvent("D7", 7, 60000);
        assertEventWTail(listener.getLastNewData()[0], "D7", "D5", 5d, "D2", 2d, "D3", 3d, 6L, splitDoubles("7d,6d,5d,4d,3d,2d"));
        assertEventWTail(listener.getLastOldData()[0], "D1", null, null, null, null, null, null, null, null);
        listener.reset();

        sendMarketEvent("D8", 8, 61000);
        assertEventWTail(listener.getLastNewData()[0], "D8", "D6", 6d, "D3", 3d, "D4", 4d, 6L, splitDoubles("8d,7d,6d,5d,4d,3d"));
        assertEventWTail(listener.getLastOldData()[0], "D2", null, null, null, null, null, null, null, null);
        listener.reset();
    }

    public void testPreviousTimeBatchWindow()
    {
        String viewExpr = "select irstream symbol as currSymbol, " +
                          " prev(2, symbol) as prevSymbol, " +
                          " prev(2, price) as prevPrice, " +
                          " prevtail(0, symbol) as prevTailSymbol, " +
                          " prevtail(0, price) as prevTailPrice, " +
                          " prevtail(1, symbol) as prevTail1Symbol, " +
                          " prevtail(1, price) as prevTail1Price, " +
                          " prevcount(price) as prevCountPrice, " +
                          " prevwindow(price) as prevWindowPrice " +
                          "from " + SupportMarketDataBean.class.getName() + ".win:time_batch(1 min) ";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("prevSymbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrice"));

        sendTimer(0);
        assertFalse(listener.isInvoked());

        sendMarketEvent("A", 1);
        sendMarketEvent("B", 2);
        assertFalse(listener.isInvoked());

        sendTimer(60000);
        assertEquals(2, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "A", null, null, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        assertEventWTail(listener.getLastNewData()[1], "B", null, null, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(80000);
        sendMarketEvent("C", 3);
        assertFalse(listener.isInvoked());

        sendTimer(120000);
        assertEquals(1, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "C", null, null, "C", 3d, null, null, 1L, splitDoubles("3d"));
        assertEquals(2, listener.getLastOldData().length);
        assertEventWTail(listener.getLastOldData()[0], "A", null, null, null, null, null, null, null, null);
        listener.reset();

        sendTimer(300000);
        sendMarketEvent("D", 4);
        sendMarketEvent("E", 5);
        sendMarketEvent("F", 6);
        sendMarketEvent("G", 7);
        sendTimer(360000);
        assertEquals(4, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "D", null, null, "D", 4d, "E", 5d, 4L, splitDoubles("7d,6d,5d,4d"));
        assertEventWTail(listener.getLastNewData()[1], "E", null, null, "D", 4d, "E", 5d, 4L, splitDoubles("7d,6d,5d,4d"));
        assertEventWTail(listener.getLastNewData()[2], "F", "D", 4d, "D", 4d, "E", 5d, 4L, splitDoubles("7d,6d,5d,4d"));
        assertEventWTail(listener.getLastNewData()[3], "G", "E", 5d, "D", 4d, "E", 5d, 4L, splitDoubles("7d,6d,5d,4d"));
    }

    public void testPreviousTimeBatchWindowJoin()
    {
        String viewExpr = "select theString as currSymbol, " +
                          " prev(2, symbol) as prevSymbol, " +
                          " prev(1, price) as prevPrice, " +
                          " prevtail(0, symbol) as prevTailSymbol, " +
                          " prevtail(0, price) as prevTailPrice, " +
                          " prevtail(1, symbol) as prevTail1Symbol, " +
                          " prevtail(1, price) as prevTail1Price, " +
                          " prevcount(price) as prevCountPrice, " +
                          " prevwindow(price) as prevWindowPrice " +
                          "from " + SupportBean.class.getName() + ".win:keepall(), " +
                          SupportMarketDataBean.class.getName() + ".win:time_batch(1 min)";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("prevSymbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrice"));

        sendTimer(0);
        assertFalse(listener.isInvoked());

        sendMarketEvent("A", 1);
        sendMarketEvent("B", 2);
        sendBeanEvent("X1");
        assertFalse(listener.isInvoked());

        sendTimer(60000);
        assertEquals(2, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "X1", null, null, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        assertEventWTail(listener.getLastNewData()[1], "X1", null, 1d, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        assertNull(listener.getLastOldData());
        listener.reset();

        sendMarketEvent("C1", 11);
        sendMarketEvent("C2", 12);
        sendMarketEvent("C3", 13);
        assertFalse(listener.isInvoked());

        sendTimer(120000);
        assertEquals(3, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "X1", null, null, "C1", 11d, "C2", 12d, 3L, splitDoubles("13d,12d,11d"));
        assertEventWTail(listener.getLastNewData()[1], "X1", null, 11d, "C1", 11d, "C2", 12d, 3L, splitDoubles("13d,12d,11d"));
        assertEventWTail(listener.getLastNewData()[2], "X1", "C1", 12d, "C1", 11d, "C2", 12d, 3L, splitDoubles("13d,12d,11d"));
    }

    public void testPreviousLengthWindow()
    {
        String viewExpr =   "select irstream symbol as currSymbol, " +
                            "prev(0, symbol) as prev0Symbol, " +
                            "prev(1, symbol) as prev1Symbol, " +
                            "prev(2, symbol) as prev2Symbol, " +
                            "prev(0, price) as prev0Price, " +
                            "prev(1, price) as prev1Price, " +
                            "prev(2, price) as prev2Price," +
                            "prevtail(0, symbol) as prevTail0Symbol, " +
                            "prevtail(0, price) as prevTail0Price, " +
                            "prevtail(1, symbol) as prevTail1Symbol, " +
                            "prevtail(1, price) as prevTail1Price, " +
                            "prevcount(price) as prevCountPrice, " +
                            "prevwindow(price) as prevWindowPrice " +
                            "from " + SupportMarketDataBean.class.getName() + ".win:length(3) ";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("prev0Symbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prev0Price"));

        sendMarketEvent("A", 1);
        assertNewEvents("A", "A", 1d, null, null, null, null, "A", 1d, null, null, 1L, splitDoubles("1d"));
        sendMarketEvent("B", 2);
        assertNewEvents("B", "B", 2d, "A", 1d, null, null, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        sendMarketEvent("C", 3);
        assertNewEvents("C", "C", 3d, "B", 2d, "A", 1d, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d"));
        sendMarketEvent("D", 4);
        EventBean newEvent = listener.getLastNewData()[0];
        EventBean oldEvent = listener.getLastOldData()[0];
        assertEventProps(newEvent, "D", "D", 4d, "C", 3d, "B", 2d, "B", 2d, "C", 3d, 3L, splitDoubles("4d,3d,2d"));
        assertEventProps(oldEvent, "A", null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public void testPreviousLengthBatch()
    {
        String viewExpr =   "select irstream symbol as currSymbol, " +
                            "prev(0, symbol) as prev0Symbol, " +
                            "prev(1, symbol) as prev1Symbol, " +
                            "prev(2, symbol) as prev2Symbol, " +
                            "prev(0, price) as prev0Price, " +
                            "prev(1, price) as prev1Price, " +
                            "prev(2, price) as prev2Price, " +
                            "prevtail(0, symbol) as prevTail0Symbol, " +
                            "prevtail(0, price) as prevTail0Price, " +
                            "prevtail(1, symbol) as prevTail1Symbol, " +
                            "prevtail(1, price) as prevTail1Price, " +
                            "prevcount(price) as prevCountPrice, " +
                            "prevwindow(price) as prevWindowPrice " +
                            "from " + SupportMarketDataBean.class.getName() + ".win:length_batch(3) ";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("prev0Symbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prev0Price"));

        sendMarketEvent("A", 1);
        sendMarketEvent("B", 2);
        assertFalse(listener.isInvoked());

        sendMarketEvent("C", 3);
        EventBean[] newEvents = listener.getLastNewData();
        assertEquals(3, newEvents.length);
        assertEventProps(newEvents[0], "A", "A", 1d, null, null, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d"));
        assertEventProps(newEvents[1], "B", "B", 2d, "A", 1d, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d"));
        assertEventProps(newEvents[2], "C", "C", 3d, "B", 2d, "A", 1d, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d"));
        listener.reset();

        sendMarketEvent("D", 4);
        sendMarketEvent("E", 5);
        assertFalse(listener.isInvoked());

        sendMarketEvent("F", 6);
        newEvents = listener.getLastNewData();
        EventBean[] oldEvents = listener.getLastOldData();
        assertEquals(3, newEvents.length);
        assertEquals(3, oldEvents.length);
        assertEventProps(newEvents[0], "D", "D", 4d, null, null, null, null, "D", 4d, "E", 5d, 3L, splitDoubles("6d,5d,4d"));
        assertEventProps(newEvents[1], "E", "E", 5d, "D", 4d, null, null, "D", 4d, "E", 5d, 3L, splitDoubles("6d,5d,4d"));
        assertEventProps(newEvents[2], "F", "F", 6d, "E", 5d, "D", 4d, "D", 4d, "E", 5d, 3L, splitDoubles("6d,5d,4d"));
        assertEventProps(oldEvents[0], "A", null, null, null, null, null, null, null, null, null, null, null, null);
        assertEventProps(oldEvents[1], "B", null, null, null, null, null, null, null, null, null, null, null, null);
        assertEventProps(oldEvents[2], "C", null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public void testPreviousLengthWindowWhere()
    {
        String viewExpr =   "select prev(2, symbol) as currSymbol " +
                            "from " + SupportMarketDataBean.class.getName() + ".win:length(100) " +
                            "where prev(2, price) > 100";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        sendMarketEvent("A", 1);
        sendMarketEvent("B", 130);
        sendMarketEvent("C", 10);
        assertFalse(listener.isInvoked());
        sendMarketEvent("D", 5);
        assertEquals("B", listener.assertOneGetNewAndReset().get("currSymbol"));
    }

    public void testPreviousLengthWindowDynamic()
    {
        String viewExpr =   "select prev(intPrimitive, theString) as sPrev " +
                            "from " + SupportBean.class.getName() + ".win:length(100)";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        sendBeanEvent("A", 1);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("sPrev"));

        sendBeanEvent("B", 0);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("B", theEvent.get("sPrev"));

        sendBeanEvent("C", 2);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("A", theEvent.get("sPrev"));

        sendBeanEvent("D", 1);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("C", theEvent.get("sPrev"));

        sendBeanEvent("E", 4);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("A", theEvent.get("sPrev"));
    }

    public void testPreviousSortWindow()
    {
        String viewExpr = "select symbol as currSymbol, " +
                          " prev(0, symbol) as prev0Symbol, " +
                          " prev(1, symbol) as prev1Symbol, " +
                          " prev(2, symbol) as prev2Symbol, " +
                          " prev(0, price) as prev0Price, " +
                          " prev(1, price) as prev1Price, " +
                          " prev(2, price) as prev2Price, " +
                          " prevtail(0, symbol) as prevTail0Symbol, " +
                          " prevtail(0, price) as prevTail0Price, " +
                          " prevtail(1, symbol) as prevTail1Symbol, " +
                          " prevtail(1, price) as prevTail1Price, " +
                          " prevcount(price) as prevCountPrice, " +
                          " prevwindow(price) as prevWindowPrice " +
                          "from " + SupportMarketDataBean.class.getName() + ".ext:sort(100, symbol asc)";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        assertEquals(String.class, selectTestView.getEventType().getPropertyType("prev0Symbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prev0Price"));

        sendMarketEvent("COX", 30);
        assertNewEvents("COX", "COX", 30d, null, null, null, null, "COX", 30d, null, null, 1L, splitDoubles("30d"));

        sendMarketEvent("IBM", 45);
        assertNewEvents("IBM", "COX", 30d, "IBM", 45d, null, null, "IBM", 45d, "COX", 30d, 2L, splitDoubles("30d,45d"));

        sendMarketEvent("MSFT", 33);
        assertNewEvents("MSFT", "COX", 30d, "IBM", 45d, "MSFT", 33d, "MSFT", 33d, "IBM", 45d, 3L, splitDoubles("30d,45d,33d"));

        sendMarketEvent("XXX", 55);
        assertNewEvents("XXX", "COX", 30d, "IBM", 45d, "MSFT", 33d, "XXX", 55d, "MSFT", 33d, 4L, splitDoubles("30d,45d,33d,55d"));

        sendMarketEvent("CXX", 56);
        assertNewEvents("CXX", "COX", 30d, "CXX", 56d, "IBM", 45d, "XXX", 55d, "MSFT", 33d, 5L, splitDoubles("30d,56d,45d,33d,55d"));

        sendMarketEvent("GE", 1);
        assertNewEvents("GE", "COX", 30d, "CXX", 56d, "GE", 1d, "XXX", 55d, "MSFT", 33d, 6L, splitDoubles("30d,56d,1d,45d,33d,55d"));

        sendMarketEvent("AAA", 1);
        assertNewEvents("AAA", "AAA", 1d, "COX", 30d, "CXX", 56d, "XXX", 55d, "MSFT", 33d, 7L, splitDoubles("1d,30d,56d,1d,45d,33d,55d"));
    }

    public void testPreviousExtTimedBatch()
    {
        String[] fields = "currSymbol,prev0Symbol,prev0Price,prev1Symbol,prev1Price,prev2Symbol,prev2Price,prevTail0Symbol,prevTail0Price,prevTail1Symbol,prevTail1Price,prevCountPrice,prevWindowPrice".split(",");
        String viewExpr =   "select irstream symbol as currSymbol, " +
                "prev(0, symbol) as prev0Symbol, " +
                "prev(0, price) as prev0Price, " +
                "prev(1, symbol) as prev1Symbol, " +
                "prev(1, price) as prev1Price, " +
                "prev(2, symbol) as prev2Symbol, " +
                "prev(2, price) as prev2Price," +
                "prevtail(0, symbol) as prevTail0Symbol, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, symbol) as prevTail1Symbol, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as prevCountPrice, " +
                "prevwindow(price) as prevWindowPrice " +
                "from " + SupportMarketDataBean.class.getName() + ".win:ext_timed_batch(volume, 10, 0L) ";

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        sendMarketEvent("A", 1, 1000);
        sendMarketEvent("B", 2, 1001);
        sendMarketEvent("C", 3, 1002);
        sendMarketEvent("D", 4, 10000);

        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][] {
                        {"A", "A", 1d, null, null, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d")},
                        {"B", "B", 2d, "A", 1d, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d")},
                        {"C", "C", 3d, "B", 2d, "A", 1d, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d")}
                },
                null);

        sendMarketEvent("E", 5, 20000);

        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][] {
                        {"D", "D", 4d, null, null, null, null, "D", 4d, null, null, 1L, splitDoubles("4d")},
                },
                new Object[][] {
                        {"A", null, null, null, null, null, null, null, null, null, null, null, null},
                        {"B", null, null, null, null, null, null, null, null, null, null, null, null},
                        {"C", null, null, null, null, null, null, null, null, null, null, null, null},
                }
                );
    }

    public void testInvalid()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        tryInvalid("select prev(0, average) " +
                "from " + SupportMarketDataBean.class.getName() + ".win:length(100).stat:uni(price)",
                "Error starting statement: Previous function requires a single data window view onto the stream [select prev(0, average) from com.espertech.esper.support.bean.SupportMarketDataBean.win:length(100).stat:uni(price)]");

        tryInvalid("select count(*) from SupportBean.win:keepall() where prev(0, intPrimitive) = 5",
                "Error starting statement: The 'prev' function may not occur in the where-clause or having-clause of a statement with aggregations as 'previous' does not provide remove stream data; Use the 'first','last','window' or 'count' aggregation functions instead [select count(*) from SupportBean.win:keepall() where prev(0, intPrimitive) = 5]");

        tryInvalid("select count(*) from SupportBean.win:keepall() having prev(0, intPrimitive) = 5",
                "Error starting statement: The 'prev' function may not occur in the where-clause or having-clause of a statement with aggregations as 'previous' does not provide remove stream data; Use the 'first','last','window' or 'count' aggregation functions instead [select count(*) from SupportBean.win:keepall() having prev(0, intPrimitive) = 5]");
    }

    private void tryInvalid(String statement, String expectedError)
    {
        try
        {
            epService.getEPAdministrator().createEPL(statement);
            fail();
        }
        catch (EPException ex)
        {
            // expected
            assertEquals(expectedError, ex.getMessage());
        }
    }

    private void assertEventWTail(EventBean eventBean,
                             String currSymbol,
                             String prevSymbol,
                             Double prevPrice,
                             String prevTailSymbol,
                             Double prevTailPrice,
                             String prevTail1Symbol,
                             Double prevTail1Price,
                             Long prevcount,
                             Object[] prevwindow)
    {
        assertEquals(currSymbol, eventBean.get("currSymbol"));
        assertEquals(prevSymbol, eventBean.get("prevSymbol"));
        assertEquals(prevPrice, eventBean.get("prevPrice"));
        assertEquals(prevTailSymbol, eventBean.get("prevTailSymbol"));
        assertEquals(prevTailPrice, eventBean.get("prevTailPrice"));
        assertEquals(prevTail1Symbol, eventBean.get("prevTail1Symbol"));
        assertEquals(prevTail1Price, eventBean.get("prevTail1Price"));
        assertEquals(prevcount, eventBean.get("prevCountPrice"));
        EPAssertionUtil.assertEqualsExactOrder((Object[]) eventBean.get("prevWindowPrice"), prevwindow);
    }

    private void assertNewEvents(String currSymbol,
                                 String prev0Symbol,
                                 Double prev0Price,
                                 String prev1Symbol,
                                 Double prev1Price,
                                 String prev2Symbol,
                                 Double prev2Price,
                                 String prevTail0Symbol,
                                 Double prevTail0Price,
                                 String prevTail1Symbol,
                                 Double prevTail1Price,
                                 Long prevCount,
                                 Object[] prevWindow)
    {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);
        assertEventProps(newData[0], currSymbol, prev0Symbol, prev0Price, prev1Symbol, prev1Price, prev2Symbol, prev2Price,
                prevTail0Symbol, prevTail0Price, prevTail1Symbol, prevTail1Price, prevCount, prevWindow);

        listener.reset();
    }

    private void assertEventProps(EventBean eventBean,
                                  String currSymbol,
                                  String prev0Symbol,
                                  Double prev0Price,
                                  String prev1Symbol,
                                  Double prev1Price,
                                  String prev2Symbol,
                                  Double prev2Price,
                                  String prevTail0Symbol,
                                  Double prevTail0Price,
                                  String prevTail1Symbol,
                                  Double prevTail1Price,
                                  Long prevCount,
                                  Object[] prevWindow)
    {
        assertEquals(currSymbol, eventBean.get("currSymbol"));
        assertEquals(prev0Symbol, eventBean.get("prev0Symbol"));
        assertEquals(prev0Price, eventBean.get("prev0Price"));
        assertEquals(prev1Symbol, eventBean.get("prev1Symbol"));
        assertEquals(prev1Price, eventBean.get("prev1Price"));
        assertEquals(prev2Symbol, eventBean.get("prev2Symbol"));
        assertEquals(prev2Price, eventBean.get("prev2Price"));
        assertEquals(prevTail0Symbol, eventBean.get("prevTail0Symbol"));
        assertEquals(prevTail0Price, eventBean.get("prevTail0Price"));
        assertEquals(prevTail1Symbol, eventBean.get("prevTail1Symbol"));
        assertEquals(prevTail1Price, eventBean.get("prevTail1Price"));
        assertEquals(prevCount, eventBean.get("prevCountPrice"));
        EPAssertionUtil.assertEqualsExactOrder((Object[]) eventBean.get("prevWindowPrice"), prevWindow);

        listener.reset();
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendMarketEvent(String symbol, double price)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketEvent(String symbol, double price, long volume)
    {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendBeanEvent(String theString)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendBeanEvent(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void assertNewEventWTail(String currSymbol,
                                 String prevSymbol,
                                 Double prevPrice,
                                 String prevTailSymbol,
                                 Double prevTailPrice,
                                 String prevTail1Symbol,
                                 Double prevTail1Price,
                                 Long prevcount,
                                 Object[] prevwindow)
    {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEventWTail(newData[0], currSymbol,prevSymbol,prevPrice,prevTailSymbol,prevTailPrice,prevTail1Symbol,prevTail1Price,prevcount,prevwindow);

        listener.reset();
    }

    private void assertOldEventWTail(String currSymbol,
                                 String prevSymbol,
                                 Double prevPrice,
                                 String prevTailSymbol,
                                 Double prevTailPrice,
                                 String prevTail1Symbol,
                                 Double prevTail1Price,
                                 Long prevcount,
                                 Object[] prevwindow)
    {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(newData);
        assertEquals(1, oldData.length);

        assertEventWTail(oldData[0], currSymbol,prevSymbol,prevPrice,prevTailSymbol,prevTailPrice,prevTail1Symbol,prevTail1Price,prevcount,prevwindow);

        listener.reset();
    }

    private void assertPerGroup(String statement)
    {
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(statement);
        selectTestView.addListener(listener);

        // assert select result type
        assertEquals(String.class, selectTestView.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrice"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevPrevPrice"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevTail0Price"));
        assertEquals(Double.class, selectTestView.getEventType().getPropertyType("prevTail1Price"));
        assertEquals(Long.class, selectTestView.getEventType().getPropertyType("countPrice"));
        assertEquals(Double[].class, selectTestView.getEventType().getPropertyType("windowPrice"));

        sendMarketEvent("IBM", 75);
        assertReceived("IBM", null, null, 75d, null, 1L, splitDoubles("75d"));

        sendMarketEvent("MSFT", 40);
        assertReceived("MSFT", null, null, 40d, null, 1L, splitDoubles("40d"));

        sendMarketEvent("IBM", 76);
        assertReceived("IBM", 75d, null, 75d, 76d, 2L, splitDoubles("76d,75d"));

        sendMarketEvent("CIC", 1);
        assertReceived("CIC", null, null, 1d, null, 1L, splitDoubles("1d"));

        sendMarketEvent("MSFT", 41);
        assertReceived("MSFT", 40d, null, 40d, 41d, 2L, splitDoubles("41d,40d"));

        sendMarketEvent("IBM", 77);
        assertReceived("IBM", 76d, 75d, 75d, 76d, 3L, splitDoubles("77d,76d,75d"));

        sendMarketEvent("IBM", 78);
        assertReceived("IBM", 77d, 76d, 75d, 76d, 4L, splitDoubles("78d,77d,76d,75d"));

        sendMarketEvent("CIC", 2);
        assertReceived("CIC", 1d, null, 1d, 2d, 2L, splitDoubles("2d,1d"));

        sendMarketEvent("MSFT", 42);
        assertReceived("MSFT", 41d, 40d, 40d, 41d, 3L, splitDoubles("42d,41d,40d"));

        sendMarketEvent("CIC", 3);
        assertReceived("CIC", 2d, 1d, 1d, 2d, 3L, splitDoubles("3d,2d,1d"));
    }

    private void assertReceived(String symbol, Double prevPrice, Double prevPrevPrice,
                                Double prevTail1Price, Double prevTail2Price,
                                Long countPrice, Object[] windowPrice)
    {
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertReceived(theEvent, symbol, prevPrice, prevPrevPrice, prevTail1Price, prevTail2Price, countPrice, windowPrice);
    }

    private void assertReceived(EventBean theEvent, String symbol, Double prevPrice, Double prevPrevPrice,
                                Double prevTail0Price, Double prevTail1Price,
                                Long countPrice, Object[] windowPrice)
    {
        assertEquals(symbol, theEvent.get("symbol"));
        assertEquals(prevPrice, theEvent.get("prevPrice"));
        assertEquals(prevPrevPrice, theEvent.get("prevPrevPrice"));
        assertEquals(prevTail0Price, theEvent.get("prevTail0Price"));
        assertEquals(prevTail1Price, theEvent.get("prevTail1Price"));
        assertEquals(countPrice, theEvent.get("countPrice"));
        EPAssertionUtil.assertEqualsExactOrder(windowPrice, (Object[]) theEvent.get("windowPrice"));
    }

    private void assertCountAndPrice(EventBean theEvent, Long total, Double price)
    {
        assertEquals(total, theEvent.get("total"));
        assertEquals(price, theEvent.get("firstPrice"));
    }

    // Don't remove me, I'm dynamically referenced by EPL
    public static Integer intToLong(Long longValue)
    {
        if (longValue == null)
        {
            return null;
        }
        else
        {
            return longValue.intValue();
        }
    }

    private Object[] splitDoubles(String doubleList)
    {
        String[] doubles = doubleList.split(",");
        Object[] result = new Object[doubles.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Double.parseDouble(doubles[i]);
        }
        return result;
    }
}
