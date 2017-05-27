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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecExprPrevious implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        runAssertionExprNameAndTypeAndSODA(epService);
        runAssertionPrevStream(epService);
        runAssertionPrevCountStarWithStaticMethod(epService);
        runAssertionPrevCountStar(epService);
        runAssertionPerGroupTwoCriteria(epService);
        runAssertionSortWindowPerGroup(epService);
        runAssertionTimeBatchPerGroup(epService);
        runAssertionLengthBatchPerGroup(epService);
        runAssertionTimeWindowPerGroup(epService);
        runAssertionExtTimeWindowPerGroup(epService);
        runAssertionLengthWindowPerGroup(epService);
        runAssertionPreviousTimeWindow(epService);
        runAssertionPreviousExtTimedWindow(epService);
        runAssertionPreviousTimeBatchWindow(epService);
        runAssertionPreviousTimeBatchWindowJoin(epService);
        runAssertionPreviousLengthWindow(epService);
        runAssertionPreviousLengthBatch(epService);
        runAssertionPreviousLengthWindowWhere(epService);
        runAssertionPreviousLengthWindowDynamic(epService);
        runAssertionPreviousSortWindow(epService);
        runAssertionPreviousExtTimedBatch(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionExprNameAndTypeAndSODA(EPServiceProvider epService) {
        String epl = "select " +
                "prev(1,intPrimitive), " +
                "prev(1,sb), " +
                "prevtail(1,intPrimitive), " +
                "prevtail(1,sb), " +
                "prevwindow(intPrimitive), " +
                "prevwindow(sb), " +
                "prevcount(intPrimitive), " +
                "prevcount(sb) " +
                "from SupportBean#time(1 minutes) as sb";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EventBean resultBean = listener.getNewDataListFlattened()[1];

        Object[][] rows = new Object[][]{
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
        stmt.destroy();
    }

    private void runAssertionPrevStream(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        String text = "select prev(1, s0) as result, " +
                "prevtail(0, s0) as tailresult," +
                "prevwindow(s0) as windowresult," +
                "prevcount(s0) as countresult " +
                "from S0#length(2) as s0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void runAssertionPrevCountStarWithStaticMethod(EPServiceProvider epService) {
        String text = "select irstream count(*) as total, " +
                "prev(" + ExecExprPrevious.class.getName() + ".intToLong(count(*)) - 1, price) as firstPrice from " + SupportMarketDataBean.class.getName() + "#time(60)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertPrevCount(epService, listener);

        stmt.destroy();
    }

    private void runAssertionPrevCountStar(EPServiceProvider epService) {
        String text = "select irstream count(*) as total, " +
                "prev(count(*) - 1, price) as firstPrice from " + SupportMarketDataBean.class.getName() + "#time(60)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertPrevCount(epService, listener);

        stmt.destroy();
    }

    private void runAssertionPerGroupTwoCriteria(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("MDBean", SupportMarketDataBean.class);
        String epl = "select symbol, feed, " +
                "prev(1, price) as prevPrice, " +
                "prevtail(price) as tailPrice, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from MDBean#groupwin(symbol, feed)#length(2)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
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
        epService.getEPAdministrator().createEPL("select prev(5,intPrimitive) as val0 from SupportBean#groupwin(theString)#length(5)").addListener(listener);

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

        stmt.destroy();
    }

    private void runAssertionSortWindowPerGroup(EPServiceProvider epService) {
        // descending sort
        String epl = "select " +
                "symbol, " +
                "prev(1, price) as prevPrice, " +
                "prev(2, price) as prevPrevPrice, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#groupwin(symbol)#sort(10, price asc) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrice"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrevPrice"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevTail0Price"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevTail1Price"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("countPrice"));
        assertEquals(Double[].class, stmt.getEventType().getPropertyType("windowPrice"));

        sendMarketEvent(epService, "IBM", 75);
        assertReceived(listener, "IBM", null, null, 75d, null, 1L, splitDoubles("75d"));
        sendMarketEvent(epService, "IBM", 80);
        assertReceived(listener, "IBM", 80d, null, 80d, 75d, 2L, splitDoubles("75d,80d"));
        sendMarketEvent(epService, "IBM", 79);
        assertReceived(listener, "IBM", 79d, 80d, 80d, 79d, 3L, splitDoubles("75d,79d,80d"));
        sendMarketEvent(epService, "IBM", 81);
        assertReceived(listener, "IBM", 79d, 80d, 81d, 80d, 4L, splitDoubles("75d,79d,80d,81d"));
        sendMarketEvent(epService, "IBM", 79.5);
        assertReceived(listener, "IBM", 79d, 79.5d, 81d, 80d, 5L, splitDoubles("75d,79d,79.5,80d,81d"));    // 75, 79, 79.5, 80, 81

        sendMarketEvent(epService, "MSFT", 10);
        assertReceived(listener, "MSFT", null, null, 10d, null, 1L, splitDoubles("10d"));
        sendMarketEvent(epService, "MSFT", 20);
        assertReceived(listener, "MSFT", 20d, null, 20d, 10d, 2L, splitDoubles("10d,20d"));
        sendMarketEvent(epService, "MSFT", 21);
        assertReceived(listener, "MSFT", 20d, 21d, 21d, 20d, 3L, splitDoubles("10d,20d,21d")); // 10, 20, 21

        sendMarketEvent(epService, "IBM", 74d);
        assertReceived(listener, "IBM", 75d, 79d, 81d, 80d, 6L, splitDoubles("74d,75d,79d,79.5,80d,81d"));  // 74, 75, 79, 79.5, 80, 81

        sendMarketEvent(epService, "MSFT", 19);
        assertReceived(listener, "MSFT", 19d, 20d, 21d, 20d, 4L, splitDoubles("10d,19d,20d,21d")); // 10, 19, 20, 21

        stmt.destroy();
    }

    private void runAssertionTimeBatchPerGroup(EPServiceProvider epService) {
        String epl = "select " +
                "symbol, " +
                "prev(1, price) as prevPrice, " +
                "prev(2, price) as prevPrevPrice, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#groupwin(symbol)#time_batch(1 sec) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrice"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrevPrice"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevTail0Price"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevTail1Price"));

        sendTimer(epService, 0);
        sendMarketEvent(epService, "IBM", 75);
        sendMarketEvent(epService, "MSFT", 40);
        sendMarketEvent(epService, "IBM", 76);
        sendMarketEvent(epService, "CIC", 1);
        sendTimer(epService, 1000);

        EventBean[] events = listener.getLastNewData();
        // order not guaranteed as timed batch, however for testing the order is reliable as schedule buckets are created
        // in a predictable order
        // Previous is looking at the same batch, doesn't consider outside of window
        assertReceived(events[0], "IBM", null, null, 75d, 76d, 2L, splitDoubles("76d,75d"));
        assertReceived(events[1], "IBM", 75d, null, 75d, 76d, 2L, splitDoubles("76d,75d"));
        assertReceived(events[2], "MSFT", null, null, 40d, null, 1L, splitDoubles("40d"));
        assertReceived(events[3], "CIC", null, null, 1d, null, 1L, splitDoubles("1d"));

        // Next batch, previous is looking only within the same batch
        sendMarketEvent(epService, "MSFT", 41);
        sendMarketEvent(epService, "IBM", 77);
        sendMarketEvent(epService, "IBM", 78);
        sendMarketEvent(epService, "CIC", 2);
        sendMarketEvent(epService, "MSFT", 42);
        sendMarketEvent(epService, "CIC", 3);
        sendMarketEvent(epService, "CIC", 4);
        sendTimer(epService, 2000);

        events = listener.getLastNewData();
        assertReceived(events[0], "IBM", null, null, 77d, 78d, 2L, splitDoubles("78d,77d"));
        assertReceived(events[1], "IBM", 77d, null, 77d, 78d, 2L, splitDoubles("78d,77d"));
        assertReceived(events[2], "MSFT", null, null, 41d, 42d, 2L, splitDoubles("42d,41d"));
        assertReceived(events[3], "MSFT", 41d, null, 41d, 42d, 2L, splitDoubles("42d,41d"));
        assertReceived(events[4], "CIC", null, null, 2d, 3d, 3L, splitDoubles("4d,3d,2d"));
        assertReceived(events[5], "CIC", 2d, null, 2d, 3d, 3L, splitDoubles("4d,3d,2d"));
        assertReceived(events[6], "CIC", 3d, 2d, 2d, 3d, 3L, splitDoubles("4d,3d,2d"));

        stmt.destroy();
    }

    private void runAssertionLengthBatchPerGroup(EPServiceProvider epService) {
        // Also testing the alternative syntax here of "prev(property)" and "prev(property, index)" versus "prev(index, property)"
        String epl = "select irstream " +
                "symbol, " +
                "prev(price) as prevPrice, " +
                "prev(price, 2) as prevPrevPrice, " +
                "prevtail(price, 0) as prevTail0Price, " +
                "prevtail(price, 1) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#groupwin(symbol)#length_batch(3) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrice"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrevPrice"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevTail0Price"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevTail1Price"));

        sendMarketEvent(epService, "IBM", 75);
        sendMarketEvent(epService, "MSFT", 50);
        sendMarketEvent(epService, "IBM", 76);
        sendMarketEvent(epService, "CIC", 1);
        assertFalse(listener.isInvoked());
        sendMarketEvent(epService, "IBM", 77);

        EventBean[] eventsNew = listener.getLastNewData();
        assertEquals(3, eventsNew.length);
        assertReceived(eventsNew[0], "IBM", null, null, 75d, 76d, 3L, splitDoubles("77d,76d,75d"));
        assertReceived(eventsNew[1], "IBM", 75d, null, 75d, 76d, 3L, splitDoubles("77d,76d,75d"));
        assertReceived(eventsNew[2], "IBM", 76d, 75d, 75d, 76d, 3L, splitDoubles("77d,76d,75d"));
        listener.reset();

        // Next batch, previous is looking only within the same batch
        sendMarketEvent(epService, "MSFT", 51);
        sendMarketEvent(epService, "IBM", 78);
        sendMarketEvent(epService, "IBM", 79);
        sendMarketEvent(epService, "CIC", 2);
        sendMarketEvent(epService, "CIC", 3);

        eventsNew = listener.getLastNewData();
        assertEquals(3, eventsNew.length);
        assertReceived(eventsNew[0], "CIC", null, null, 1d, 2d, 3L, splitDoubles("3d,2d,1d"));
        assertReceived(eventsNew[1], "CIC", 1d, null, 1d, 2d, 3L, splitDoubles("3d,2d,1d"));
        assertReceived(eventsNew[2], "CIC", 2d, 1d, 1d, 2d, 3L, splitDoubles("3d,2d,1d"));
        listener.reset();

        sendMarketEvent(epService, "MSFT", 52);

        eventsNew = listener.getLastNewData();
        assertEquals(3, eventsNew.length);
        assertReceived(eventsNew[0], "MSFT", null, null, 50d, 51d, 3L, splitDoubles("52d,51d,50d"));
        assertReceived(eventsNew[1], "MSFT", 50d, null, 50d, 51d, 3L, splitDoubles("52d,51d,50d"));
        assertReceived(eventsNew[2], "MSFT", 51d, 50d, 50d, 51d, 3L, splitDoubles("52d,51d,50d"));
        listener.reset();

        sendMarketEvent(epService, "IBM", 80);

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

        stmt.destroy();
    }

    private void runAssertionTimeWindowPerGroup(EPServiceProvider epService) {
        String epl = "select " +
                "symbol, " +
                "prev(1, price) as prevPrice, " +
                "prev(2, price) as prevPrevPrice, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#groupwin(symbol)#time(20 sec) ";
        assertPerGroup(epl, epService);
    }

    private void runAssertionExtTimeWindowPerGroup(EPServiceProvider epService) {
        String epl = "select " +
                "symbol, " +
                "prev(1, price) as prevPrice, " +
                "prev(2, price) as prevPrevPrice, " +
                "prevtail(0, price) as prevTail0Price, " +
                "prevtail(1, price) as prevTail1Price, " +
                "prevcount(price) as countPrice, " +
                "prevwindow(price) as windowPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#groupwin(symbol)#ext_timed(volume, 20 sec) ";
        assertPerGroup(epl, epService);
    }

    private void runAssertionLengthWindowPerGroup(EPServiceProvider epService) {
        String epl =
                "select symbol, " +
                        "prev(1, price) as prevPrice, " +
                        "prev(2, price) as prevPrevPrice, " +
                        "prevtail(price, 0) as prevTail0Price, " +
                        "prevtail(price, 1) as prevTail1Price, " +
                        "prevcount(price) as countPrice, " +
                        "prevwindow(price) as windowPrice " +
                        "from " + SupportMarketDataBean.class.getName() + "#groupwin(symbol)#length(10) ";
        assertPerGroup(epl, epService);
    }

    private void runAssertionPreviousTimeWindow(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
                " prev(2, symbol) as prevSymbol, " +
                " prev(2, price) as prevPrice, " +
                " prevtail(0, symbol) as prevTailSymbol, " +
                " prevtail(0, price) as prevTailPrice, " +
                " prevtail(1, symbol) as prevTail1Symbol, " +
                " prevtail(1, price) as prevTail1Price, " +
                " prevcount(price) as prevCountPrice, " +
                " prevwindow(price) as prevWindowPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#time(1 min) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("prevSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrice"));

        sendTimer(epService, 0);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D1", 1);
        assertNewEventWTail(listener, "D1", null, null, "D1", 1d, null, null, 1L, splitDoubles("1d"));

        sendTimer(epService, 1000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D2", 2);
        assertNewEventWTail(listener, "D2", null, null, "D1", 1d, "D2", 2d, 2L, splitDoubles("2d,1d"));

        sendTimer(epService, 2000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D3", 3);
        assertNewEventWTail(listener, "D3", "D1", 1d, "D1", 1d, "D2", 2d, 3L, splitDoubles("3d,2d,1d"));

        sendTimer(epService, 3000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D4", 4);
        assertNewEventWTail(listener, "D4", "D2", 2d, "D1", 1d, "D2", 2d, 4L, splitDoubles("4d,3d,2d,1d"));

        sendTimer(epService, 4000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D5", 5);
        assertNewEventWTail(listener, "D5", "D3", 3d, "D1", 1d, "D2", 2d, 5L, splitDoubles("5d,4d,3d,2d,1d"));

        sendTimer(epService, 30000);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "D6", 6);
        assertNewEventWTail(listener, "D6", "D4", 4d, "D1", 1d, "D2", 2d, 6L, splitDoubles("6d,5d,4d,3d,2d,1d"));

        // Test remove stream, always returns null as previous function
        // returns null for remove stream for time windows
        sendTimer(epService, 60000);
        assertOldEventWTail(listener, "D1", null, null, null, null, null, null, null, null);
        sendTimer(epService, 61000);
        assertOldEventWTail(listener, "D2", null, null, null, null, null, null, null, null);
        sendTimer(epService, 62000);
        assertOldEventWTail(listener, "D3", null, null, null, null, null, null, null, null);
        sendTimer(epService, 63000);
        assertOldEventWTail(listener, "D4", null, null, null, null, null, null, null, null);
        sendTimer(epService, 64000);
        assertOldEventWTail(listener, "D5", null, null, null, null, null, null, null, null);
        sendTimer(epService, 90000);
        assertOldEventWTail(listener, "D6", null, null, null, null, null, null, null, null);

        stmt.destroy();
    }

    private void runAssertionPreviousExtTimedWindow(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
                " prev(2, symbol) as prevSymbol, " +
                " prev(2, price) as prevPrice, " +
                " prevtail(0, symbol) as prevTailSymbol, " +
                " prevtail(0, price) as prevTailPrice, " +
                " prevtail(1, symbol) as prevTail1Symbol, " +
                " prevtail(1, price) as prevTail1Price, " +
                " prevcount(price) as prevCountPrice, " +
                " prevwindow(price) as prevWindowPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#ext_timed(volume, 1 min) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("prevSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrice"));
        assertEquals(String.class, stmt.getEventType().getPropertyType("prevTailSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevTailPrice"));

        sendMarketEvent(epService, "D1", 1, 0);
        assertNewEventWTail(listener, "D1", null, null, "D1", 1d, null, null, 1L, splitDoubles("1d"));

        sendMarketEvent(epService, "D2", 2, 1000);
        assertNewEventWTail(listener, "D2", null, null, "D1", 1d, "D2", 2d, 2L, splitDoubles("2d,1d"));

        sendMarketEvent(epService, "D3", 3, 3000);
        assertNewEventWTail(listener, "D3", "D1", 1d, "D1", 1d, "D2", 2d, 3L, splitDoubles("3d,2d,1d"));

        sendMarketEvent(epService, "D4", 4, 4000);
        assertNewEventWTail(listener, "D4", "D2", 2d, "D1", 1d, "D2", 2d, 4L, splitDoubles("4d,3d,2d,1d"));

        sendMarketEvent(epService, "D5", 5, 5000);
        assertNewEventWTail(listener, "D5", "D3", 3d, "D1", 1d, "D2", 2d, 5L, splitDoubles("5d,4d,3d,2d,1d"));

        sendMarketEvent(epService, "D6", 6, 30000);
        assertNewEventWTail(listener, "D6", "D4", 4d, "D1", 1d, "D2", 2d, 6L, splitDoubles("6d,5d,4d,3d,2d,1d"));

        sendMarketEvent(epService, "D7", 7, 60000);
        assertEventWTail(listener.getLastNewData()[0], "D7", "D5", 5d, "D2", 2d, "D3", 3d, 6L, splitDoubles("7d,6d,5d,4d,3d,2d"));
        assertEventWTail(listener.getLastOldData()[0], "D1", null, null, null, null, null, null, null, null);
        listener.reset();

        sendMarketEvent(epService, "D8", 8, 61000);
        assertEventWTail(listener.getLastNewData()[0], "D8", "D6", 6d, "D3", 3d, "D4", 4d, 6L, splitDoubles("8d,7d,6d,5d,4d,3d"));
        assertEventWTail(listener.getLastOldData()[0], "D2", null, null, null, null, null, null, null, null);
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionPreviousTimeBatchWindow(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
                " prev(2, symbol) as prevSymbol, " +
                " prev(2, price) as prevPrice, " +
                " prevtail(0, symbol) as prevTailSymbol, " +
                " prevtail(0, price) as prevTailPrice, " +
                " prevtail(1, symbol) as prevTail1Symbol, " +
                " prevtail(1, price) as prevTail1Price, " +
                " prevcount(price) as prevCountPrice, " +
                " prevwindow(price) as prevWindowPrice " +
                "from " + SupportMarketDataBean.class.getName() + "#time_batch(1 min) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("prevSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrice"));

        sendTimer(epService, 0);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "A", 1);
        sendMarketEvent(epService, "B", 2);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 60000);
        assertEquals(2, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "A", null, null, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        assertEventWTail(listener.getLastNewData()[1], "B", null, null, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        assertNull(listener.getLastOldData());
        listener.reset();

        sendTimer(epService, 80000);
        sendMarketEvent(epService, "C", 3);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 120000);
        assertEquals(1, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "C", null, null, "C", 3d, null, null, 1L, splitDoubles("3d"));
        assertEquals(2, listener.getLastOldData().length);
        assertEventWTail(listener.getLastOldData()[0], "A", null, null, null, null, null, null, null, null);
        listener.reset();

        sendTimer(epService, 300000);
        sendMarketEvent(epService, "D", 4);
        sendMarketEvent(epService, "E", 5);
        sendMarketEvent(epService, "F", 6);
        sendMarketEvent(epService, "G", 7);
        sendTimer(epService, 360000);
        assertEquals(4, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "D", null, null, "D", 4d, "E", 5d, 4L, splitDoubles("7d,6d,5d,4d"));
        assertEventWTail(listener.getLastNewData()[1], "E", null, null, "D", 4d, "E", 5d, 4L, splitDoubles("7d,6d,5d,4d"));
        assertEventWTail(listener.getLastNewData()[2], "F", "D", 4d, "D", 4d, "E", 5d, 4L, splitDoubles("7d,6d,5d,4d"));
        assertEventWTail(listener.getLastNewData()[3], "G", "E", 5d, "D", 4d, "E", 5d, 4L, splitDoubles("7d,6d,5d,4d"));

        stmt.destroy();
    }

    private void runAssertionPreviousTimeBatchWindowJoin(EPServiceProvider epService) {
        String epl = "select theString as currSymbol, " +
                " prev(2, symbol) as prevSymbol, " +
                " prev(1, price) as prevPrice, " +
                " prevtail(0, symbol) as prevTailSymbol, " +
                " prevtail(0, price) as prevTailPrice, " +
                " prevtail(1, symbol) as prevTail1Symbol, " +
                " prevtail(1, price) as prevTail1Price, " +
                " prevcount(price) as prevCountPrice, " +
                " prevwindow(price) as prevWindowPrice " +
                "from " + SupportBean.class.getName() + "#keepall, " +
                SupportMarketDataBean.class.getName() + "#time_batch(1 min)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("prevSymbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrice"));

        sendTimer(epService, 0);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "A", 1);
        sendMarketEvent(epService, "B", 2);
        sendBeanEvent(epService, "X1");
        assertFalse(listener.isInvoked());

        sendTimer(epService, 60000);
        assertEquals(2, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "X1", null, null, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        assertEventWTail(listener.getLastNewData()[1], "X1", null, 1d, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        assertNull(listener.getLastOldData());
        listener.reset();

        sendMarketEvent(epService, "C1", 11);
        sendMarketEvent(epService, "C2", 12);
        sendMarketEvent(epService, "C3", 13);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 120000);
        assertEquals(3, listener.getLastNewData().length);
        assertEventWTail(listener.getLastNewData()[0], "X1", null, null, "C1", 11d, "C2", 12d, 3L, splitDoubles("13d,12d,11d"));
        assertEventWTail(listener.getLastNewData()[1], "X1", null, 11d, "C1", 11d, "C2", 12d, 3L, splitDoubles("13d,12d,11d"));
        assertEventWTail(listener.getLastNewData()[2], "X1", "C1", 12d, "C1", 11d, "C2", 12d, 3L, splitDoubles("13d,12d,11d"));

        stmt.destroy();
    }

    private void runAssertionPreviousLengthWindow(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
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
                "from " + SupportMarketDataBean.class.getName() + "#length(3) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("prev0Symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prev0Price"));

        sendMarketEvent(epService, "A", 1);
        assertNewEvents(listener, "A", "A", 1d, null, null, null, null, "A", 1d, null, null, 1L, splitDoubles("1d"));
        sendMarketEvent(epService, "B", 2);
        assertNewEvents(listener, "B", "B", 2d, "A", 1d, null, null, "A", 1d, "B", 2d, 2L, splitDoubles("2d,1d"));
        sendMarketEvent(epService, "C", 3);
        assertNewEvents(listener, "C", "C", 3d, "B", 2d, "A", 1d, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d"));
        sendMarketEvent(epService, "D", 4);
        EventBean newEvent = listener.getLastNewData()[0];
        EventBean oldEvent = listener.getLastOldData()[0];
        assertEventProps(listener, newEvent, "D", "D", 4d, "C", 3d, "B", 2d, "B", 2d, "C", 3d, 3L, splitDoubles("4d,3d,2d"));
        assertEventProps(listener, oldEvent, "A", null, null, null, null, null, null, null, null, null, null, null, null);

        stmt.destroy();
    }

    private void runAssertionPreviousLengthBatch(EPServiceProvider epService) {
        String epl = "select irstream symbol as currSymbol, " +
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
                "from " + SupportMarketDataBean.class.getName() + "#length_batch(3) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("prev0Symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prev0Price"));

        sendMarketEvent(epService, "A", 1);
        sendMarketEvent(epService, "B", 2);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "C", 3);
        EventBean[] newEvents = listener.getLastNewData();
        assertEquals(3, newEvents.length);
        assertEventProps(listener, newEvents[0], "A", "A", 1d, null, null, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d"));
        assertEventProps(listener, newEvents[1], "B", "B", 2d, "A", 1d, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d"));
        assertEventProps(listener, newEvents[2], "C", "C", 3d, "B", 2d, "A", 1d, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d"));
        listener.reset();

        sendMarketEvent(epService, "D", 4);
        sendMarketEvent(epService, "E", 5);
        assertFalse(listener.isInvoked());

        sendMarketEvent(epService, "F", 6);
        newEvents = listener.getLastNewData();
        EventBean[] oldEvents = listener.getLastOldData();
        assertEquals(3, newEvents.length);
        assertEquals(3, oldEvents.length);
        assertEventProps(listener, newEvents[0], "D", "D", 4d, null, null, null, null, "D", 4d, "E", 5d, 3L, splitDoubles("6d,5d,4d"));
        assertEventProps(listener, newEvents[1], "E", "E", 5d, "D", 4d, null, null, "D", 4d, "E", 5d, 3L, splitDoubles("6d,5d,4d"));
        assertEventProps(listener, newEvents[2], "F", "F", 6d, "E", 5d, "D", 4d, "D", 4d, "E", 5d, 3L, splitDoubles("6d,5d,4d"));
        assertEventProps(listener, oldEvents[0], "A", null, null, null, null, null, null, null, null, null, null, null, null);
        assertEventProps(listener, oldEvents[1], "B", null, null, null, null, null, null, null, null, null, null, null, null);
        assertEventProps(listener, oldEvents[2], "C", null, null, null, null, null, null, null, null, null, null, null, null);

        stmt.destroy();
    }

    private void runAssertionPreviousLengthWindowWhere(EPServiceProvider epService) {
        String epl = "select prev(2, symbol) as currSymbol " +
                "from " + SupportMarketDataBean.class.getName() + "#length(100) " +
                "where prev(2, price) > 100";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMarketEvent(epService, "A", 1);
        sendMarketEvent(epService, "B", 130);
        sendMarketEvent(epService, "C", 10);
        assertFalse(listener.isInvoked());
        sendMarketEvent(epService, "D", 5);
        assertEquals("B", listener.assertOneGetNewAndReset().get("currSymbol"));

        stmt.destroy();
    }

    private void runAssertionPreviousLengthWindowDynamic(EPServiceProvider epService) {
        String epl = "select prev(intPrimitive, theString) as sPrev " +
                "from " + SupportBean.class.getName() + "#length(100)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBeanEvent(epService, "A", 1);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("sPrev"));

        sendBeanEvent(epService, "B", 0);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("B", theEvent.get("sPrev"));

        sendBeanEvent(epService, "C", 2);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("A", theEvent.get("sPrev"));

        sendBeanEvent(epService, "D", 1);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("C", theEvent.get("sPrev"));

        sendBeanEvent(epService, "E", 4);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("A", theEvent.get("sPrev"));

        stmt.destroy();
    }

    private void runAssertionPreviousSortWindow(EPServiceProvider epService) {
        String epl = "select symbol as currSymbol, " +
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
                "from " + SupportMarketDataBean.class.getName() + "#sort(100, symbol asc)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(String.class, stmt.getEventType().getPropertyType("prev0Symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prev0Price"));

        sendMarketEvent(epService, "COX", 30);
        assertNewEvents(listener, "COX", "COX", 30d, null, null, null, null, "COX", 30d, null, null, 1L, splitDoubles("30d"));

        sendMarketEvent(epService, "IBM", 45);
        assertNewEvents(listener, "IBM", "COX", 30d, "IBM", 45d, null, null, "IBM", 45d, "COX", 30d, 2L, splitDoubles("30d,45d"));

        sendMarketEvent(epService, "MSFT", 33);
        assertNewEvents(listener, "MSFT", "COX", 30d, "IBM", 45d, "MSFT", 33d, "MSFT", 33d, "IBM", 45d, 3L, splitDoubles("30d,45d,33d"));

        sendMarketEvent(epService, "XXX", 55);
        assertNewEvents(listener, "XXX", "COX", 30d, "IBM", 45d, "MSFT", 33d, "XXX", 55d, "MSFT", 33d, 4L, splitDoubles("30d,45d,33d,55d"));

        sendMarketEvent(epService, "CXX", 56);
        assertNewEvents(listener, "CXX", "COX", 30d, "CXX", 56d, "IBM", 45d, "XXX", 55d, "MSFT", 33d, 5L, splitDoubles("30d,56d,45d,33d,55d"));

        sendMarketEvent(epService, "GE", 1);
        assertNewEvents(listener, "GE", "COX", 30d, "CXX", 56d, "GE", 1d, "XXX", 55d, "MSFT", 33d, 6L, splitDoubles("30d,56d,1d,45d,33d,55d"));

        sendMarketEvent(epService, "AAA", 1);
        assertNewEvents(listener, "AAA", "AAA", 1d, "COX", 30d, "CXX", 56d, "XXX", 55d, "MSFT", 33d, 7L, splitDoubles("1d,30d,56d,1d,45d,33d,55d"));

        stmt.destroy();
    }

    private void runAssertionPreviousExtTimedBatch(EPServiceProvider epService) {
        String[] fields = "currSymbol,prev0Symbol,prev0Price,prev1Symbol,prev1Price,prev2Symbol,prev2Price,prevTail0Symbol,prevTail0Price,prevTail1Symbol,prevTail1Price,prevCountPrice,prevWindowPrice".split(",");
        String epl = "select irstream symbol as currSymbol, " +
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
                "from " + SupportMarketDataBean.class.getName() + "#ext_timed_batch(volume, 10, 0L) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendMarketEvent(epService, "A", 1, 1000);
        sendMarketEvent(epService, "B", 2, 1001);
        sendMarketEvent(epService, "C", 3, 1002);
        sendMarketEvent(epService, "D", 4, 10000);

        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{
                        {"A", "A", 1d, null, null, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d")},
                        {"B", "B", 2d, "A", 1d, null, null, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d")},
                        {"C", "C", 3d, "B", 2d, "A", 1d, "A", 1d, "B", 2d, 3L, splitDoubles("3d,2d,1d")}
                },
                null);

        sendMarketEvent(epService, "E", 5, 20000);

        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{
                        {"D", "D", 4d, null, null, null, null, "D", 4d, null, null, 1L, splitDoubles("4d")},
                },
                new Object[][]{
                        {"A", null, null, null, null, null, null, null, null, null, null, null, null},
                        {"B", null, null, null, null, null, null, null, null, null, null, null, null},
                        {"C", null, null, null, null, null, null, null, null, null, null, null, null},
                }
        );

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        tryInvalid(epService, "select prev(0, average) " +
                        "from " + SupportMarketDataBean.class.getName() + "#length(100)#uni(price)",
                "Error starting statement: Previous function requires a single data window view onto the stream [");

        tryInvalid(epService, "select count(*) from SupportBean#keepall where prev(0, intPrimitive) = 5",
                "Error starting statement: The 'prev' function may not occur in the where-clause or having-clause of a statement with aggregations as 'previous' does not provide remove stream data; Use the 'first','last','window' or 'count' aggregation functions instead [select count(*) from SupportBean#keepall where prev(0, intPrimitive) = 5]");

        tryInvalid(epService, "select count(*) from SupportBean#keepall having prev(0, intPrimitive) = 5",
                "Error starting statement: The 'prev' function may not occur in the where-clause or having-clause of a statement with aggregations as 'previous' does not provide remove stream data; Use the 'first','last','window' or 'count' aggregation functions instead [select count(*) from SupportBean#keepall having prev(0, intPrimitive) = 5]");
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
                                  Object[] prevwindow) {
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

    private void assertNewEvents(SupportUpdateListener listener, String currSymbol,
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
                                 Object[] prevWindow) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);
        assertEventProps(listener, newData[0], currSymbol, prev0Symbol, prev0Price, prev1Symbol, prev1Price, prev2Symbol, prev2Price,
                prevTail0Symbol, prevTail0Price, prevTail1Symbol, prevTail1Price, prevCount, prevWindow);

        listener.reset();
    }

    private void assertEventProps(SupportUpdateListener listener,
                                  EventBean eventBean,
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
                                  Object[] prevWindow) {
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

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendMarketEvent(EPServiceProvider epService, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketEvent(EPServiceProvider epService, String symbol, double price, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendBeanEvent(EPServiceProvider epService, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendBeanEvent(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void assertNewEventWTail(SupportUpdateListener listener, String currSymbol,
                                     String prevSymbol,
                                     Double prevPrice,
                                     String prevTailSymbol,
                                     Double prevTailPrice,
                                     String prevTail1Symbol,
                                     Double prevTail1Price,
                                     Long prevcount,
                                     Object[] prevwindow) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        assertEventWTail(newData[0], currSymbol, prevSymbol, prevPrice, prevTailSymbol, prevTailPrice, prevTail1Symbol, prevTail1Price, prevcount, prevwindow);

        listener.reset();
    }

    private void assertOldEventWTail(SupportUpdateListener listener,
                                     String currSymbol,
                                     String prevSymbol,
                                     Double prevPrice,
                                     String prevTailSymbol,
                                     Double prevTailPrice,
                                     String prevTail1Symbol,
                                     Double prevTail1Price,
                                     Long prevcount,
                                     Object[] prevwindow) {
        EventBean[] oldData = listener.getLastOldData();
        EventBean[] newData = listener.getLastNewData();

        assertNull(newData);
        assertEquals(1, oldData.length);

        assertEventWTail(oldData[0], currSymbol, prevSymbol, prevPrice, prevTailSymbol, prevTailPrice, prevTail1Symbol, prevTail1Price, prevcount, prevwindow);

        listener.reset();
    }

    private void assertPerGroup(String statement, EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(statement);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert select result type
        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrice"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevPrevPrice"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevTail0Price"));
        assertEquals(Double.class, stmt.getEventType().getPropertyType("prevTail1Price"));
        assertEquals(Long.class, stmt.getEventType().getPropertyType("countPrice"));
        assertEquals(Double[].class, stmt.getEventType().getPropertyType("windowPrice"));

        sendMarketEvent(epService, "IBM", 75);
        assertReceived(listener, "IBM", null, null, 75d, null, 1L, splitDoubles("75d"));

        sendMarketEvent(epService, "MSFT", 40);
        assertReceived(listener, "MSFT", null, null, 40d, null, 1L, splitDoubles("40d"));

        sendMarketEvent(epService, "IBM", 76);
        assertReceived(listener, "IBM", 75d, null, 75d, 76d, 2L, splitDoubles("76d,75d"));

        sendMarketEvent(epService, "CIC", 1);
        assertReceived(listener, "CIC", null, null, 1d, null, 1L, splitDoubles("1d"));

        sendMarketEvent(epService, "MSFT", 41);
        assertReceived(listener, "MSFT", 40d, null, 40d, 41d, 2L, splitDoubles("41d,40d"));

        sendMarketEvent(epService, "IBM", 77);
        assertReceived(listener, "IBM", 76d, 75d, 75d, 76d, 3L, splitDoubles("77d,76d,75d"));

        sendMarketEvent(epService, "IBM", 78);
        assertReceived(listener, "IBM", 77d, 76d, 75d, 76d, 4L, splitDoubles("78d,77d,76d,75d"));

        sendMarketEvent(epService, "CIC", 2);
        assertReceived(listener, "CIC", 1d, null, 1d, 2d, 2L, splitDoubles("2d,1d"));

        sendMarketEvent(epService, "MSFT", 42);
        assertReceived(listener, "MSFT", 41d, 40d, 40d, 41d, 3L, splitDoubles("42d,41d,40d"));

        sendMarketEvent(epService, "CIC", 3);
        assertReceived(listener, "CIC", 2d, 1d, 1d, 2d, 3L, splitDoubles("3d,2d,1d"));

        stmt.destroy();
    }

    private void assertReceived(SupportUpdateListener listener, String symbol, Double prevPrice, Double prevPrevPrice,
                                Double prevTail1Price, Double prevTail2Price,
                                Long countPrice, Object[] windowPrice) {
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertReceived(theEvent, symbol, prevPrice, prevPrevPrice, prevTail1Price, prevTail2Price, countPrice, windowPrice);
    }

    private void assertReceived(EventBean theEvent, String symbol, Double prevPrice, Double prevPrevPrice,
                                Double prevTail0Price, Double prevTail1Price,
                                Long countPrice, Object[] windowPrice) {
        assertEquals(symbol, theEvent.get("symbol"));
        assertEquals(prevPrice, theEvent.get("prevPrice"));
        assertEquals(prevPrevPrice, theEvent.get("prevPrevPrice"));
        assertEquals(prevTail0Price, theEvent.get("prevTail0Price"));
        assertEquals(prevTail1Price, theEvent.get("prevTail1Price"));
        assertEquals(countPrice, theEvent.get("countPrice"));
        EPAssertionUtil.assertEqualsExactOrder(windowPrice, (Object[]) theEvent.get("windowPrice"));
    }

    private void assertCountAndPrice(EventBean theEvent, Long total, Double price) {
        assertEquals(total, theEvent.get("total"));
        assertEquals(price, theEvent.get("firstPrice"));
    }

    private void assertPrevCount(EPServiceProvider epService, SupportUpdateListener listener) {
        sendTimer(epService, 0);
        sendMarketEvent(epService, "IBM", 75);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 1L, 75D);

        sendMarketEvent(epService, "IBM", 76);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 2L, 75D);

        sendTimer(epService, 10000);
        sendMarketEvent(epService, "IBM", 77);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 3L, 75D);

        sendTimer(epService, 20000);
        sendMarketEvent(epService, "IBM", 78);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 4L, 75D);

        sendTimer(epService, 50000);
        sendMarketEvent(epService, "IBM", 79);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 5L, 75D);

        sendTimer(epService, 60000);
        assertEquals(1, listener.getOldDataList().size());
        EventBean[] oldData = listener.getLastOldData();
        assertEquals(2, oldData.length);
        assertCountAndPrice(oldData[0], 3L, null);
        listener.reset();

        sendMarketEvent(epService, "IBM", 80);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 4L, 77D);

        sendTimer(epService, 65000);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 70000);
        assertEquals(1, listener.getOldDataList().size());
        oldData = listener.getLastOldData();
        assertEquals(1, oldData.length);
        assertCountAndPrice(oldData[0], 3L, null);
        listener.reset();

        sendTimer(epService, 80000);
        listener.reset();

        sendMarketEvent(epService, "IBM", 81);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 3L, 79D);

        sendTimer(epService, 120000);
        listener.reset();

        sendMarketEvent(epService, "IBM", 82);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 2L, 81D);

        sendTimer(epService, 300000);
        listener.reset();

        sendMarketEvent(epService, "IBM", 83);
        assertCountAndPrice(listener.assertOneGetNewAndReset(), 1L, 83D);
    }

    // Don't remove me, I'm dynamically referenced by EPL
    public static Integer intToLong(Long longValue) {
        if (longValue == null) {
            return null;
        } else {
            return longValue.intValue();
        }
    }

    private Object[] splitDoubles(String doubleList) {
        String[] doubles = doubleList.split(",");
        Object[] result = new Object[doubles.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Double.parseDouble(doubles[i]);
        }
        return result;
    }
}
