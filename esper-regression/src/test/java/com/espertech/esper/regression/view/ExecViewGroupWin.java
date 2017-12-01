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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanTimestamp;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.support.EventRepresentationChoice;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecViewGroupWin implements RegressionExecution {
    private final static String SYMBOL_CISCO = "CSCO.O";
    private final static String SYMBOL_IBM = "IBM.N";
    private final static String SYMBOL_GE = "GE.N";

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionObjectArrayEvent(epService);
        runAssertionSelfJoin(epService);
        runAssertionReclaimTimeWindow(epService);
        if (!InstrumentationHelper.ENABLED) {
            runAssertionReclaimAgedHint(epService);
        }
        runAssertionStats(epService);
        runAssertionLengthWindowGrouped(epService);
        runAssertionExpressionGrouped(epService);
        runAssertionCorrel(epService);
        runAssertionLinest(epService);
        runAssertionExpressionBatch(epService);
        runAssertionMultiProperty(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epl = "select * from SupportBean#groupwin(theString)#length(1)#groupwin(theString)#uni(intPrimitive)";
        SupportMessageAssertUtil.tryInvalid(epService, epl,
                "Error starting statement: Multiple groupwin-declarations are not supported");

        epl = "select avg(price), symbol from " + SupportMarketDataBean.class.getName() + "#length(100)#groupwin(symbol)";
        SupportMessageAssertUtil.tryInvalid(epService, epl,
                "Error starting statement: Invalid use of the 'groupwin' view, the view requires one or more child views to group, or consider using the group-by clause");

        epl = "select * from SupportBean#keepall#groupwin(theString)#length(2)";
        SupportMessageAssertUtil.tryInvalid(epService, epl,
                "Error starting statement: The groupwin view must occur in the first position in conjunction with multiple data windows");

        epl = "select * from SupportBean#groupwin(theString)#length(2)#merge(theString)#keepall";
        SupportMessageAssertUtil.tryInvalid(epService, epl,
                "Error starting statement: The merge view cannot be used in conjunction with multiple data windows");
    }

    private void runAssertionMultiProperty(EPServiceProvider epService) {
        final String SYMBOL_MSFT = "MSFT";
        final String SYMBOL_GE = "GE";
        final String FEED_INFO = "INFO";
        final String FEED_REU = "REU";

        // Listen to all ticks
        EPStatement viewGrouped = epService.getEPAdministrator().createEPL(
                "select irstream datapoints as size, symbol, feed, volume from " + SupportMarketDataBean.class.getName() +
                        "#groupwin(symbol, feed, volume)#uni(price) order by symbol, feed, volume");
        SupportUpdateListener listener = new SupportUpdateListener();

        // Counts per symbol, feed and volume the events
        viewGrouped.addListener(listener);

        ArrayList<Map<String, Object>> mapList = new ArrayList<>();

        // Set up a map of expected values

        Map<String, Object> expectedValues[] = new HashMap[10];
        for (int i = 0; i < expectedValues.length; i++) {
            expectedValues[i] = new HashMap<>();
        }

        // Send one event, check results
        sendEvent(epService, SYMBOL_GE, FEED_INFO, 1);

        populateMap(expectedValues[0], SYMBOL_GE, FEED_INFO, 1L, 0);
        mapList.add(expectedValues[0]);
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), mapList);
        populateMap(expectedValues[0], SYMBOL_GE, FEED_INFO, 1L, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), mapList);
        EPAssertionUtil.assertPropsPerRow(viewGrouped.iterator(), mapList);

        // Send a couple of events
        sendEvent(epService, SYMBOL_GE, FEED_INFO, 1);
        sendEvent(epService, SYMBOL_GE, FEED_INFO, 2);
        sendEvent(epService, SYMBOL_GE, FEED_INFO, 1);
        sendEvent(epService, SYMBOL_GE, FEED_REU, 99);
        sendEvent(epService, SYMBOL_MSFT, FEED_INFO, 100);

        populateMap(expectedValues[1], SYMBOL_MSFT, FEED_INFO, 100, 0);
        mapList.clear();
        mapList.add(expectedValues[1]);
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), mapList);
        populateMap(expectedValues[1], SYMBOL_MSFT, FEED_INFO, 100, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), mapList);

        populateMap(expectedValues[0], SYMBOL_GE, FEED_INFO, 1, 3);
        populateMap(expectedValues[2], SYMBOL_GE, FEED_INFO, 2, 1);
        populateMap(expectedValues[3], SYMBOL_GE, FEED_REU, 99, 1);
        mapList.clear();
        mapList.add(expectedValues[0]);
        mapList.add(expectedValues[2]);
        mapList.add(expectedValues[3]);
        mapList.add(expectedValues[1]);
        EPAssertionUtil.assertPropsPerRow(viewGrouped.iterator(), mapList);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionExpressionBatch(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String epl = "@Name('create_var') create variable long ENGINE_TIME;\n" +
                "@Name('engine_time_update') on pattern[every timer:interval(10 seconds)] set ENGINE_TIME = current_timestamp();\n" +
                "@Name('out_null') select window(*) from SupportBean#groupwin(theString)#expr_batch(oldest_timestamp.plus(9 seconds) < ENGINE_TIME);";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out_null").addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(11000));

        assertFalse(listener.isInvoked());
    }

    private void runAssertionObjectArrayEvent(EPServiceProvider epService) {
        String[] fields = "p1,sp2".split(",");
        epService.getEPAdministrator().getConfiguration().addEventType("MyOAEvent", new String[]{"p1", "p2"}, new Object[]{String.class, int.class});
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select p1,sum(p2) as sp2 from MyOAEvent#groupwin(p1)#length(2)").addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[]{"A", 10}, "MyOAEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 10});

        epService.getEPRuntime().sendEvent(new Object[]{"B", 11}, "MyOAEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"B", 21});

        epService.getEPRuntime().sendEvent(new Object[]{"A", 12}, "MyOAEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 33});

        epService.getEPRuntime().sendEvent(new Object[]{"A", 13}, "MyOAEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", 36});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSelfJoin(EPServiceProvider epService) {
        // ESPER-528
        epService.getEPAdministrator().createEPL(EventRepresentationChoice.MAP.getAnnotationText() + " create schema Product (product string, productsize int)");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String query =
                " @Hint('reclaim_group_aged=1,reclaim_group_freq=1') select Product.product as product, Product.productsize as productsize from Product unidirectional" +
                        " left outer join Product#time(3 seconds)#groupwin(product,productsize)#size PrevProduct on Product.product=PrevProduct.product and Product.productsize=PrevProduct.productsize" +
                        " having PrevProduct.size<2";
        epService.getEPAdministrator().createEPL(query);

        // Set to larger number of executions and monitor memory
        for (int i = 0; i < 10; i++) {
            sendProductNew(epService, "The id of this product is deliberately very very long so that we can use up more memory per instance of this event sent into Esper " + i, i);
            epService.getEPRuntime().sendEvent(new CurrentTimeEvent(i * 100));
            //if (i % 2000 == 0) {
            //    System.out.println("i=" + i + "; Allocated: " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + "; Free: " + Runtime.getRuntime().freeMemory() / 1024 / 1024);
            //}
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionReclaimTimeWindow(EPServiceProvider epService) {
        sendTimer(epService, 0);

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("@Hint('reclaim_group_aged=30,reclaim_group_freq=5') " +
                "select longPrimitive, count(*) from SupportBean#groupwin(theString)#time(3000000)");

        for (int i = 0; i < 10; i++) {
            SupportBean theEvent = new SupportBean(Integer.toString(i), i);
            epService.getEPRuntime().sendEvent(theEvent);
        }

        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        int handleCountBefore = spi.getSchedulingService().getScheduleHandleCount();
        assertEquals(10, handleCountBefore);

        sendTimer(epService, 1000000);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        int handleCountAfter = spi.getSchedulingService().getScheduleHandleCount();
        assertEquals(1, handleCountAfter);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionReclaimAgedHint(EPServiceProvider epService) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String epl = "@Hint('reclaim_group_aged=5,reclaim_group_freq=1') " +
                "select * from SupportBean#groupwin(theString)#keepall";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);

        int maxSlots = 10;
        int maxEventsPerSlot = 1000;
        for (int timeSlot = 0; timeSlot < maxSlots; timeSlot++) {
            epService.getEPRuntime().sendEvent(new CurrentTimeEvent(timeSlot * 1000 + 1));

            for (int i = 0; i < maxEventsPerSlot; i++) {
                epService.getEPRuntime().sendEvent(new SupportBean("E" + timeSlot, 0));
            }
        }

        EventBean[] iterator = EPAssertionUtil.iteratorToArray(stmt.iterator());
        assertTrue(iterator.length <= 6 * maxEventsPerSlot);
        stmt.destroy();
    }

    private void runAssertionStats(EPServiceProvider epService) {
        EPAdministrator epAdmin = epService.getEPAdministrator();
        String filter = "select * from " + SupportMarketDataBean.class.getName();

        EPStatement priceLast3Stats = epAdmin.createEPL(filter + "#groupwin(symbol)#length(3)#uni(price) order by symbol asc");
        SupportUpdateListener priceLast3StatsListener = new SupportUpdateListener();
        priceLast3Stats.addListener(priceLast3StatsListener);

        EPStatement volumeLast3Stats = epAdmin.createEPL(filter + "#groupwin(symbol)#length(3)#uni(volume) order by symbol asc");
        SupportUpdateListener volumeLast3StatsListener = new SupportUpdateListener();
        volumeLast3Stats.addListener(volumeLast3StatsListener);

        EPStatement priceAllStats = epAdmin.createEPL(filter + "#groupwin(symbol)#uni(price) order by symbol asc");
        SupportUpdateListener priceAllStatsListener = new SupportUpdateListener();
        priceAllStats.addListener(priceAllStatsListener);

        EPStatement volumeAllStats = epAdmin.createEPL(filter + "#groupwin(symbol)#uni(volume) order by symbol asc");
        SupportUpdateListener volumeAllStatsListener = new SupportUpdateListener();
        volumeAllStats.addListener(volumeAllStatsListener);

        Vector<Map<String, Object>> expectedList = new Vector<>();
        for (int i = 0; i < 3; i++) {
            expectedList.add(new HashMap<>());
        }

        sendEvent(epService, SYMBOL_CISCO, 25, 50000);
        sendEvent(epService, SYMBOL_CISCO, 26, 60000);
        sendEvent(epService, SYMBOL_IBM, 10, 8000);
        sendEvent(epService, SYMBOL_IBM, 10.5, 8200);
        sendEvent(epService, SYMBOL_GE, 88, 1000);

        EPAssertionUtil.assertPropsPerRow(priceLast3StatsListener.getLastNewData(), makeMap(SYMBOL_GE, 88));
        EPAssertionUtil.assertPropsPerRow(priceAllStatsListener.getLastNewData(), makeMap(SYMBOL_GE, 88));
        EPAssertionUtil.assertPropsPerRow(volumeLast3StatsListener.getLastNewData(), makeMap(SYMBOL_GE, 1000));
        EPAssertionUtil.assertPropsPerRow(volumeAllStatsListener.getLastNewData(), makeMap(SYMBOL_GE, 1000));

        sendEvent(epService, SYMBOL_CISCO, 27, 70000);
        sendEvent(epService, SYMBOL_CISCO, 28, 80000);

        EPAssertionUtil.assertPropsPerRow(priceAllStatsListener.getLastNewData(), makeMap(SYMBOL_CISCO, 26.5d));
        EPAssertionUtil.assertPropsPerRow(volumeAllStatsListener.getLastNewData(), makeMap(SYMBOL_CISCO, 65000d));
        EPAssertionUtil.assertPropsPerRow(priceLast3StatsListener.getLastNewData(), makeMap(SYMBOL_CISCO, 27d));
        EPAssertionUtil.assertPropsPerRow(volumeLast3StatsListener.getLastNewData(), makeMap(SYMBOL_CISCO, 70000d));

        sendEvent(epService, SYMBOL_IBM, 11, 8700);
        sendEvent(epService, SYMBOL_IBM, 12, 8900);

        EPAssertionUtil.assertPropsPerRow(priceAllStatsListener.getLastNewData(), makeMap(SYMBOL_IBM, 10.875d));
        EPAssertionUtil.assertPropsPerRow(volumeAllStatsListener.getLastNewData(), makeMap(SYMBOL_IBM, 8450d));
        EPAssertionUtil.assertPropsPerRow(priceLast3StatsListener.getLastNewData(), makeMap(SYMBOL_IBM, 11d + 1 / 6d));
        EPAssertionUtil.assertPropsPerRow(volumeLast3StatsListener.getLastNewData(), makeMap(SYMBOL_IBM, 8600d));

        sendEvent(epService, SYMBOL_GE, 85.5, 950);
        sendEvent(epService, SYMBOL_GE, 85.75, 900);
        sendEvent(epService, SYMBOL_GE, 89, 1250);
        sendEvent(epService, SYMBOL_GE, 86, 1200);
        sendEvent(epService, SYMBOL_GE, 85, 1150);

        double averageGE = (88d + 85.5d + 85.75d + 89d + 86d + 85d) / 6d;
        EPAssertionUtil.assertPropsPerRow(priceAllStatsListener.getLastNewData(), makeMap(SYMBOL_GE, averageGE));
        EPAssertionUtil.assertPropsPerRow(volumeAllStatsListener.getLastNewData(), makeMap(SYMBOL_GE, 1075d));
        EPAssertionUtil.assertPropsPerRow(priceLast3StatsListener.getLastNewData(), makeMap(SYMBOL_GE, 86d + 2d / 3d));
        EPAssertionUtil.assertPropsPerRow(volumeLast3StatsListener.getLastNewData(), makeMap(SYMBOL_GE, 1200d));

        // Check iterator results
        expectedList.get(0).put("symbol", SYMBOL_CISCO);
        expectedList.get(0).put("average", 26.5d);
        expectedList.get(1).put("symbol", SYMBOL_GE);
        expectedList.get(1).put("average", averageGE);
        expectedList.get(2).put("symbol", SYMBOL_IBM);
        expectedList.get(2).put("average", 10.875d);
        EPAssertionUtil.assertPropsPerRow(priceAllStats.iterator(), expectedList);

        expectedList.get(0).put("symbol", SYMBOL_CISCO);
        expectedList.get(0).put("average", 27d);
        expectedList.get(1).put("symbol", SYMBOL_GE);
        expectedList.get(1).put("average", 86d + 2d / 3d);
        expectedList.get(2).put("symbol", SYMBOL_IBM);
        expectedList.get(2).put("average", 11d + 1 / 6d);
        EPAssertionUtil.assertPropsPerRow(priceLast3Stats.iterator(), expectedList);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionLengthWindowGrouped(EPServiceProvider epService) {
        String stmtText = "select symbol, price from " + SupportMarketDataBean.class.getName() + "#groupwin(symbol)#length(2)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendEvent(epService, "IBM", 100);

        stmt.destroy();
    }

    private void runAssertionExpressionGrouped(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanTimestamp.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from SupportBeanTimestamp#groupwin(timestamp.getDayOfWeek())#length(2)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanTimestamp("E1", DateTime.parseDefaultMSec("2002-01-01T09:0:00.000")));
        epService.getEPRuntime().sendEvent(new SupportBeanTimestamp("E2", DateTime.parseDefaultMSec("2002-01-08T09:0:00.000")));
        epService.getEPRuntime().sendEvent(new SupportBeanTimestamp("E3", DateTime.parseDefaultMSec("2002-01-015T09:0:00.000")));
        assertEquals(1, listener.getDataListsFlattened().getSecond().length);

        stmt.destroy();
    }

    private void runAssertionCorrel(EPServiceProvider epService) {
        // further math tests can be found in the view unit test
        EPAdministrator admin = epService.getEPAdministrator();
        admin.getConfiguration().addEventType("Market", SupportMarketDataBean.class);
        EPStatement statement = admin.createEPL("select * from Market#groupwin(symbol)#length(1000000)#correl(price, volume, feed)");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        assertEquals(Double.class, statement.getEventType().getPropertyType("correlation"));

        String[] fields = new String[]{"symbol", "correlation", "feed"};

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ABC", 10.0, 1000L, "f1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"ABC", Double.NaN, "f1"});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("DEF", 1.0, 2L, "f2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"DEF", Double.NaN, "f2"});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("DEF", 2.0, 4L, "f3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"DEF", 1.0, "f3"});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ABC", 20.0, 2000L, "f4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"ABC", 1.0, "f4"});

        statement.destroy();
    }

    private void runAssertionLinest(EPServiceProvider epService) {
        // further math tests can be found in the view unit test
        EPAdministrator admin = epService.getEPAdministrator();
        admin.getConfiguration().addEventType("Market", SupportMarketDataBean.class);
        EPStatement statement = admin.createEPL("select * from Market#groupwin(symbol)#length(1000000)#linest(price, volume, feed)");
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        assertEquals(Double.class, statement.getEventType().getPropertyType("slope"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("YIntercept"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("XAverage"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("XStandardDeviationPop"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("XStandardDeviationSample"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("XSum"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("XVariance"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("YAverage"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("YStandardDeviationPop"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("YStandardDeviationSample"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("YSum"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("YVariance"));
        assertEquals(Long.class, statement.getEventType().getPropertyType("dataPoints"));
        assertEquals(Long.class, statement.getEventType().getPropertyType("n"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("sumX"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("sumXSq"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("sumXY"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("sumY"));
        assertEquals(Double.class, statement.getEventType().getPropertyType("sumYSq"));

        String[] fields = new String[]{"symbol", "slope", "YIntercept", "feed"};

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ABC", 10.0, 50000L, "f1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"ABC", Double.NaN, Double.NaN, "f1"});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("DEF", 1.0, 1L, "f2"));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, fields, new Object[]{"DEF", Double.NaN, Double.NaN, "f2"});
        assertEquals(1d, theEvent.get("XAverage"));
        assertEquals(0d, theEvent.get("XStandardDeviationPop"));
        assertEquals(Double.NaN, theEvent.get("XStandardDeviationSample"));
        assertEquals(1d, theEvent.get("XSum"));
        assertEquals(Double.NaN, theEvent.get("XVariance"));
        assertEquals(1d, theEvent.get("YAverage"));
        assertEquals(0d, theEvent.get("YStandardDeviationPop"));
        assertEquals(Double.NaN, theEvent.get("YStandardDeviationSample"));
        assertEquals(1d, theEvent.get("YSum"));
        assertEquals(Double.NaN, theEvent.get("YVariance"));
        assertEquals(1L, theEvent.get("dataPoints"));
        assertEquals(1L, theEvent.get("n"));
        assertEquals(1d, theEvent.get("sumX"));
        assertEquals(1d, theEvent.get("sumXSq"));
        assertEquals(1d, theEvent.get("sumXY"));
        assertEquals(1d, theEvent.get("sumY"));
        assertEquals(1d, theEvent.get("sumYSq"));
        // above computed values tested in more detail in RegressionBean test

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("DEF", 2.0, 2L, "f3"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"DEF", 1.0, 0.0, "f3"});

        epService.getEPRuntime().sendEvent(new SupportMarketDataBean("ABC", 11.0, 50100L, "f4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"ABC", 100.0, 49000.0, "f4"});

        statement.destroy();
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price) {
        sendEvent(epService, symbol, price, -1);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, double price, long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, volume, "");
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private List<Map<String, Object>> makeMap(String symbol, double average) {
        Map<String, Object> result = new HashMap<>();

        result.put("symbol", symbol);
        result.put("average", average);

        ArrayList<Map<String, Object>> vec = new ArrayList<>();
        vec.add(result);

        return vec;
    }

    private void sendProductNew(EPServiceProvider epService, String product, int size) {
        Map<String, Object> theEvent = new HashMap<>();
        theEvent.put("product", product);
        theEvent.put("productsize", size);
        epService.getEPRuntime().sendEvent(theEvent, "Product");
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void populateMap(Map<String, Object> map, String symbol, String feed, long volume, long size) {
        map.put("symbol", symbol);
        map.put("feed", feed);
        map.put("volume", volume);
        map.put("size", size);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, String feed, long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, volume, feed);
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
