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
package com.espertech.esper.regressionlib.suite.view;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.bean.SupportBeanTimestamp;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.bean.SupportSensorEvent;
import com.espertech.esper.regressionlib.support.util.SupportScheduleHelper;
import com.espertech.esper.runtime.client.EPStatement;

import java.io.Serializable;
import java.util.*;

import static com.espertech.esper.regressionlib.framework.RegressionFlag.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ViewGroup {
    private final static String SYMBOL_CISCO = "CSCO.O";
    private final static String SYMBOL_IBM = "IBM.N";
    private final static String SYMBOL_GE = "GE.N";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewGroupObjectArrayEvent());
        execs.add(new ViewGroupStats());
        execs.add(new ViewGroupReclaimTimeWindow());
        execs.add(new ViewGroupReclaimAgedHint());
        execs.add(new ViewGroupCorrel());
        execs.add(new ViewGroupLinest());
        execs.add(new ViewGroupMultiProperty());
        execs.add(new ViewGroupInvalid());
        execs.add(new ViewGroupLengthWinWeightAvg());
        execs.add(new ViewGroupReclaimWithFlipTime(5000L));
        execs.add(new ViewGroupTimeBatch());
        execs.add(new ViewGroupTimeAccum());
        execs.add(new ViewGroupTimeOrder());
        execs.add(new ViewGroupTimeLengthBatch());
        execs.add(new ViewGroupLengthWin());
        execs.add(new ViewGroupLengthBatch());
        execs.add(new ViewGroupTimeWin());
        execs.add(new ViewGroupExpressionGrouped());
        execs.add(new ViewGroupExpressionBatch());
        execs.add(new ViewGroupEscapedPropertyText());
        return execs;
    }

    private static class ViewGroupEscapedPropertyText implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema event as " + EventWithTags.class.getName() + ";\n" +
                "\n" +
                "insert into stream1\n" +
                "select name, tags from event;\n" +
                "\n" +
                "select name, tags('a\\.b') from stream1.std:groupwin(name, tags('a\\.b')).win:length(10)\n" +
                "having count(1) >= 5;\n";
            env.compileDeploy(epl).undeployAll();
        }

        @Override
        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(SERDEREQUIRED);
        }
    }

    private static class ViewGroupInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select * from SupportBean#groupwin(theString)#length(1)#groupwin(theString)#uni(intPrimitive)";
            env.tryInvalidCompile(epl,
                "Failed to validate data window declaration: Multiple groupwin-declarations are not supported");

            epl = "select avg(price), symbol from SupportMarketDataBean#length(100)#groupwin(symbol)";
            env.tryInvalidCompile(epl,
                "Failed to validate data window declaration: Invalid use of the 'groupwin' view, the view requires one or more child views to group, or consider using the group-by clause");

            epl = "select * from SupportBean#keepall#groupwin(theString)#length(2)";
            env.tryInvalidCompile(epl,
                "Failed to validate data window declaration: The 'groupwin' declaration must occur in the first position");

            epl = "select * from SupportBean#groupwin(theString)#length(2)#merge(theString)#keepall";
            env.tryInvalidCompile(epl,
                "Failed to validate data window declaration: The 'merge' declaration cannot be used in conjunction with multiple data windows");

            epl = "create schema MyEvent(somefield null);\n" +
                "select * from MyEvent#groupwin(somefield)#length(2)";
            env.tryInvalidCompile(epl,
                "Failed to validate data window declaration: Group-window received a null-typed criteria expression");
        }
    }

    private static class ViewGroupMultiProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            final String[] fields = "symbol,feed,volume,size".split(",");
            final String symbolMsft = "MSFT";
            final String symbolGe = "GE";
            final String feedInfo = "INFO";
            final String feedReu = "REU";

            // Listen to all ticks
            String epl = "@name('s0') select irstream datapoints as size, symbol, feed, volume " +
                "from SupportMarketDataBean#groupwin(symbol, feed, volume)#uni(price) order by symbol, feed, volume";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, symbolGe, feedInfo, 1);
            Object[] row0GEOld = new Object[] {symbolGe, feedInfo, 1L, 0L};
            Object[] row0GENew = new Object[] {symbolGe, feedInfo, 1L, 1L};
            env.assertPropsPerRowIRPair("s0", fields, new Object[][] {row0GENew}, new Object[][] {row0GEOld});

            env.milestone(0);

            // Send a couple of events
            sendEvent(env, symbolGe, feedInfo, 1);
            sendEvent(env, symbolGe, feedInfo, 2);
            sendEvent(env, symbolGe, feedInfo, 1);
            env.listenerReset("s0");

            env.milestone(1);

            sendEvent(env, symbolGe, feedReu, 99);
            sendEvent(env, symbolMsft, feedInfo, 100);

            Object[] row1GENew = new Object[] {symbolGe, feedReu, 99L, 1L};
            Object[] row1GEOld = new Object[] {symbolGe, feedReu, 99L, 0L};
            Object[] row1MSOld = new Object[] {symbolMsft, feedInfo, 100L, 0L};
            Object[] row1MSNew = new Object[] {symbolMsft, feedInfo, 100L, 1L};
            env.assertPropsPerRowIRPairFlattened("s0", fields, new Object[][] {row1GENew, row1MSNew}, new Object[][] {row1GEOld, row1MSOld});

            row0GENew = new Object[] {symbolGe, feedInfo, 1L, 3L};
            Object[] row2GENew = new Object[] {symbolGe, feedInfo, 2L, 1L};
            Object[] row3GENew = new Object[] {symbolGe, feedReu, 99L, 1L};
            env.assertPropsPerRowIterator("s0", fields, new Object[][] {row0GENew, row2GENew, row3GENew, row1MSNew});

            env.undeployAll();
        }
    }

    private static class ViewGroupExpressionBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@Name('create_var') create variable long ENGINE_TIME;\n" +
                "@Name('runtime_time_update') on pattern[every timer:interval(10 seconds)] set ENGINE_TIME = current_timestamp();\n" +
                "@Name('out_null') select window(*) from SupportBean#groupwin(theString)#expr_batch(oldest_timestamp.plus(9 seconds) < ENGINE_TIME);";
            env.compileDeploy(epl).addListener("out_null");

            env.advanceTime(5000);
            env.advanceTime(10000);
            env.advanceTime(11000);

            env.assertListenerNotInvoked("out_null");

            env.undeployAll();
        }
    }

    private static class ViewGroupObjectArrayEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "p1,sp2".split(",");
            String epl = "@name('s0') select p1,sum(p2) as sp2 from OAEventStringInt#groupwin(p1)#length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventObjectArray(new Object[]{"A", 10}, "OAEventStringInt");
            env.assertPropsNew("s0", fields, new Object[]{"A", 10});

            env.sendEventObjectArray(new Object[]{"B", 11}, "OAEventStringInt");
            env.assertPropsNew("s0", fields, new Object[]{"B", 21});

            env.milestone(0);

            env.sendEventObjectArray(new Object[]{"A", 12}, "OAEventStringInt");
            env.assertPropsNew("s0", fields, new Object[]{"A", 33});

            env.sendEventObjectArray(new Object[]{"A", 13}, "OAEventStringInt");
            env.assertPropsNew("s0", fields, new Object[]{"A", 36});

            env.undeployAll();
        }
    }

    private static class ViewGroupReclaimTimeWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String epl = "@name('s0') @Hint('reclaim_group_aged=30,reclaim_group_freq=5') " +
                "select longPrimitive, count(*) from SupportBean#groupwin(theString)#time(3000000)";
            env.compileDeploy(epl).addListener("s0");

            for (int i = 0; i < 10; i++) {
                SupportBean theEvent = new SupportBean(Integer.toString(i), i);
                env.sendEventBean(theEvent);
            }

            env.assertStatement("s0", statement -> assertEquals(10, SupportScheduleHelper.scheduleCount(statement)));

            env.milestone(0);

            env.advanceTime(1000000);
            env.sendEventBean(new SupportBean("E1", 1));

            env.assertStatement("s0", statement -> assertEquals(1, SupportScheduleHelper.scheduleCount(statement)));

            env.undeployAll();

            env.assertRuntime(rt -> assertEquals(0, SupportScheduleHelper.scheduleCountOverall(rt)));
        }
    }

    private static class ViewGroupReclaimAgedHint implements RegressionExecution {
        @Override
        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(EXCLUDEWHENINSTRUMENTED);
        }

        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@name('s0') @Hint('reclaim_group_aged=5,reclaim_group_freq=1') " +
                "select * from SupportBean#groupwin(theString)#keepall";
            env.compileDeploy(epl).addListener("s0");

            int maxEventsPerSlot = 1000;
            for (int timeSlot = 0; timeSlot < 10; timeSlot++) {
                env.advanceTime(timeSlot * 1000 + 1);

                for (int i = 0; i < maxEventsPerSlot; i++) {
                    env.sendEventBean(new SupportBean("E" + timeSlot, 0));
                }
            }

            env.milestone(0);

            env.assertIterator("s0", iterator -> {
                EventBean[] events = EPAssertionUtil.iteratorToArray(iterator);
                assertTrue(events.length <= 6 * maxEventsPerSlot);
            });

            env.sendEventBean(new SupportBean("E0", 1));

            env.milestone(1);

            env.assertIterator("s0", iterator -> {
                EventBean[] events = EPAssertionUtil.iteratorToArray(iterator);
                assertEquals(6 * maxEventsPerSlot + 1, events.length);
            });

            env.undeployAll();
        }
    }

    private static class ViewGroupStats implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;
            String filter = "select * from SupportMarketDataBean";

            epl = "@name('priceLast3Stats')" + filter + "#groupwin(symbol)#length(3)#uni(price) order by symbol asc";
            env.compileDeploy(epl).addListener("priceLast3Stats");

            epl = "@name('volumeLast3Stats')" + filter + "#groupwin(symbol)#length(3)#uni(volume) order by symbol asc";
            env.compileDeploy(epl).addListener("volumeLast3Stats");

            epl = "@name('priceAllStats')" + filter + "#groupwin(symbol)#uni(price) order by symbol asc";
            env.compileDeploy(epl).addListener("priceAllStats");

            epl = "@name('volumeAllStats')" + filter + "#groupwin(symbol)#uni(volume) order by symbol asc";
            env.compileDeploy(epl).addListener("volumeAllStats");

            Vector<Map<String, Object>> expectedList = new Vector<>();
            for (int i = 0; i < 3; i++) {
                expectedList.add(new HashMap<>());
            }

            sendEvent(env, SYMBOL_CISCO, 25, 50000);
            sendEvent(env, SYMBOL_CISCO, 26, 60000);
            sendEvent(env, SYMBOL_IBM, 10, 8000);
            sendEvent(env, SYMBOL_IBM, 10.5, 8200);
            sendEvent(env, SYMBOL_GE, 88, 1000);

            assertLastNewRow(env, "priceLast3Stats", SYMBOL_GE, 88);
            assertLastNewRow(env, "priceAllStats", SYMBOL_GE, 88);
            assertLastNewRow(env, "volumeLast3Stats", SYMBOL_GE, 1000);
            assertLastNewRow(env, "volumeAllStats", SYMBOL_GE, 1000);

            sendEvent(env, SYMBOL_CISCO, 27, 70000);
            sendEvent(env, SYMBOL_CISCO, 28, 80000);

            assertLastNewRow(env, "priceAllStats", SYMBOL_CISCO, 26.5d);
            assertLastNewRow(env, "volumeAllStats", SYMBOL_CISCO, 65000d);
            assertLastNewRow(env, "priceLast3Stats", SYMBOL_CISCO, 27d);
            assertLastNewRow(env, "volumeLast3Stats", SYMBOL_CISCO, 70000d);

            sendEvent(env, SYMBOL_IBM, 11, 8700);
            sendEvent(env, SYMBOL_IBM, 12, 8900);

            assertLastNewRow(env, "priceAllStats", SYMBOL_IBM, 10.875d);
            assertLastNewRow(env, "volumeAllStats", SYMBOL_IBM, 8450d);
            assertLastNewRow(env, "priceLast3Stats", SYMBOL_IBM, 11d + 1 / 6d);
            assertLastNewRow(env, "volumeLast3Stats", SYMBOL_IBM, 8600d);

            sendEvent(env, SYMBOL_GE, 85.5, 950);
            sendEvent(env, SYMBOL_GE, 85.75, 900);
            sendEvent(env, SYMBOL_GE, 89, 1250);
            sendEvent(env, SYMBOL_GE, 86, 1200);
            sendEvent(env, SYMBOL_GE, 85, 1150);

            double averageGE = (88d + 85.5d + 85.75d + 89d + 86d + 85d) / 6d;
            assertLastNewRow(env, "priceAllStats", SYMBOL_GE, averageGE);
            assertLastNewRow(env, "volumeAllStats", SYMBOL_GE, 1075d);
            assertLastNewRow(env, "priceLast3Stats", SYMBOL_GE, 86d + 2d / 3d);
            assertLastNewRow(env, "volumeLast3Stats", SYMBOL_GE, 1200d);

            // Check iterator results
            String[] fields = new String[]{"symbol", "average"};
            env.assertPropsPerRowIterator("priceAllStats", fields, new Object[][] {{SYMBOL_CISCO, 26.5d}, {SYMBOL_GE, averageGE}, {SYMBOL_IBM, 10.875d}});
            env.assertPropsPerRowIterator("priceLast3Stats", fields, new Object[][] {{SYMBOL_CISCO, 27d}, {SYMBOL_GE, 86d + 2d / 3d}, {SYMBOL_IBM, 11d + 1 / 6d}});

            env.undeployAll();
        }
    }

    private static class ViewGroupExpressionGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportBeanTimestamp#groupwin(timestamp.getDayOfWeek())#length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanTimestamp("E1", DateTime.parseDefaultMSec("2002-01-01T09:0:00.000")));
            env.sendEventBean(new SupportBeanTimestamp("E2", DateTime.parseDefaultMSec("2002-01-08T09:0:00.000")));
            env.sendEventBean(new SupportBeanTimestamp("E3", DateTime.parseDefaultMSec("2002-01-015T09:0:00.000")));
            env.assertListener("s0", listener -> {
                assertEquals(1, listener.getDataListsFlattened().getSecond().length);
            });

            env.undeployAll();
        }
    }

    private static class ViewGroupCorrel implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // further math tests can be found in the view unit test
            String epl = "@name('s0') select * from SupportMarketDataBean#groupwin(symbol)#length(1000000)#correl(price, volume, feed)";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                assertEquals(Double.class, statement.getEventType().getPropertyType("correlation"));
            });

            String[] fields = new String[]{"symbol", "correlation", "feed"};

            env.sendEventBean(new SupportMarketDataBean("ABC", 10.0, 1000L, "f1"));
            env.assertPropsNew("s0", fields, new Object[]{"ABC", Double.NaN, "f1"});

            env.sendEventBean(new SupportMarketDataBean("DEF", 1.0, 2L, "f2"));
            env.assertPropsNew("s0", fields, new Object[]{"DEF", Double.NaN, "f2"});

            env.milestone(0);

            env.sendEventBean(new SupportMarketDataBean("DEF", 2.0, 4L, "f3"));
            env.assertPropsNew("s0", fields, new Object[]{"DEF", 1.0, "f3"});

            env.sendEventBean(new SupportMarketDataBean("ABC", 20.0, 2000L, "f4"));
            env.assertPropsNew("s0", fields, new Object[]{"ABC", 1.0, "f4"});

            env.undeployAll();
        }
    }

    private static class ViewGroupLinest implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // further math tests can be found in the view unit test
            String epl = "@name('s0') select * from SupportMarketDataBean#groupwin(symbol)#length(1000000)#linest(price, volume, feed)";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
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
            });

            String[] fields = new String[]{"symbol", "slope", "YIntercept", "feed"};

            env.sendEventBean(new SupportMarketDataBean("ABC", 10.0, 50000L, "f1"));
            env.assertPropsNew("s0", fields, new Object[]{"ABC", Double.NaN, Double.NaN, "f1"});

            env.milestone(0);

            env.sendEventBean(new SupportMarketDataBean("DEF", 1.0, 1L, "f2"));
            env.assertEventNew("s0", theEvent -> {
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
            });


            env.sendEventBean(new SupportMarketDataBean("DEF", 2.0, 2L, "f3"));
            env.assertPropsNew("s0", fields, new Object[]{"DEF", 1.0, 0.0, "f3"});

            env.sendEventBean(new SupportMarketDataBean("ABC", 11.0, 50100L, "f4"));
            env.assertPropsNew("s0", fields, new Object[]{"ABC", 100.0, 49000.0, "f4"});

            env.undeployAll();
        }
    }

    public static class ViewGroupLengthWinWeightAvg implements RegressionExecution {
        @Override
        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(EXCLUDEWHENINSTRUMENTED, PERFORMANCE);
        }

        public void run(RegressionEnvironment env) {
            boolean useGroup = true;
            if (useGroup) {
                // 0.69 sec for 100k
                String stmtString = "@name('s0') select * from SupportSensorEvent#groupwin(type)#length(10000000)#weighted_avg(measurement, confidence)";
                env.compileDeploy(stmtString).addListener("s0");
            } else {
                // 0.53 sec for 100k
                for (int i = 0; i < 10; i++) {
                    String stmtString = "SELECT * FROM SupportSensorEvent(type='A" + i + "')#length(1000000)#weighted_avg(measurement,confidence)";
                    env.compileDeploy(stmtString).addListener("s0");
                }
            }

            // prime
            for (int i = 0; i < 100; i++) {
                env.sendEventBean(new SupportSensorEvent(0, "A", "1", (double) i, (double) i));
            }

            // measure
            long numEvents = 10000;
            long startTime = System.nanoTime();
            for (int i = 0; i < numEvents; i++) {
                //int modulo = i % 10;
                int modulo = 1;
                String type = "A" + modulo;
                env.sendEventBean(new SupportSensorEvent(0, type, "1", (double) i, (double) i));
            }
            long endTime = System.nanoTime();
            double delta = (endTime - startTime) / 1000d / 1000d / 1000d;
            // System.out.println("delta=" + delta);
            assertTrue(delta < 1);

            env.undeployAll();
        }
    }

    public static class ViewGroupReclaimWithFlipTime implements RegressionExecution {
        private final long flipTime;

        public ViewGroupReclaimWithFlipTime(long flipTime) {
            this.flipTime = flipTime;
        }

        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String epl = "@name('s0') @Hint('reclaim_group_aged=1,reclaim_group_freq=5') select * from SupportBean#groupwin(theString)#keepall";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 0));
            env.assertStatement("s0", statement -> {
                assertCount(statement, 1);
            });

            env.advanceTime(flipTime - 1);
            env.sendEventBean(new SupportBean("E2", 0));
            env.assertStatement("s0", statement -> {
                assertCount(statement, 2);
            });

            env.milestone(0);

            env.advanceTime(flipTime);
            env.sendEventBean(new SupportBean("E3", 0));
            env.assertStatement("s0", statement -> {
                assertCount(statement, 2);
            });

            env.undeployAll();
        }
    }

    public static class ViewGroupTimeAccum implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "price".split(",");
            env.advanceTime(1000);

            String text = "@name('s0') select irstream * from  SupportMarketDataBean#groupwin(symbol)#time_accum(10 sec)";
            env.compileDeploy(text).addListener("s0");

            // 1st event S1 group
            env.advanceTime(1000);
            sendEvent(env, "S1", 10);
            assertPrice(env, 10d);

            env.milestone(1);

            // 2nd event S1 group
            env.advanceTime(5000);
            sendEvent(env, "S1", 20);
            assertPrice(env, 20d);

            env.milestone(2);

            // 1st event S2 group
            env.advanceTime(10000);
            sendEvent(env, "S2", 30);
            assertPrice(env, 30d);

            env.milestone(3);

            env.advanceTime(15000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{10d}, {20d}});

            env.advanceTime(20000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{30d}});

            env.undeployAll();
        }
    }

    public static class ViewGroupTimeBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "price".split(",");
            env.advanceTime(1000);

            String text = "@name('s0') select irstream * from  SupportMarketDataBean#groupwin(symbol)#time_batch(10 sec)";
            env.compileDeploy(text).addListener("s0");

            // 1st event S1 group
            env.advanceTime(1000);
            sendEvent(env, "S1", 10);
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            // 2nd event S1 group
            env.advanceTime(5000);
            sendEvent(env, "S1", 20);
            env.assertListenerNotInvoked("s0");

            env.milestone(2);

            // 1st event S2 group
            env.advanceTime(10000);
            sendEvent(env, "S2", 30);
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            env.advanceTime(10999);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(11000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{10d}, {20d}}, null);

            env.milestone(4);

            env.advanceTime(20000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{30d}}, null);

            env.milestone(5);

            env.advanceTime(21000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{10d}, {20d}});

            env.undeployAll();
        }
    }

    public static class ViewGroupTimeOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "id".split(",");
            env.advanceTime(1000);

            String text = "@name('s0') select irstream * from SupportBeanTimestamp#groupwin(groupId)#time_order(timestamp, 10 sec)";
            env.compileDeploy(text).addListener("s0").milestone(0);

            // 1st event
            env.advanceTime(1000);
            sendEventTS(env, "E1", "G1", 3000);
            assertId(env, "E1");

            env.milestone(1);

            // 2nd event
            env.advanceTime(2000);
            sendEventTS(env, "E2", "G2", 2000);
            assertId(env, "E2");

            env.milestone(2);

            // 3rd event
            env.advanceTime(3000);
            sendEventTS(env, "E3", "G2", 3000);
            assertId(env, "E3");

            env.milestone(3);

            // 4th event
            env.advanceTime(4000);
            sendEventTS(env, "E4", "G1", 2500);
            assertId(env, "E4");

            env.milestone(4);

            // Window pushes out event E2
            env.advanceTime(11999);
            env.assertListenerNotInvoked("s0");
            env.advanceTime(12000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E2"}});

            env.milestone(5);

            // Window pushes out event E4
            env.advanceTime(12499);
            env.assertListenerNotInvoked("s0");
            env.advanceTime(12500);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{"E4"}});

            env.milestone(6);

            env.undeployAll();
        }
    }

    public static class ViewGroupTimeLengthBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "price".split(",");
            env.advanceTime(1000);

            String text = "@name('s0') select irstream * from  SupportMarketDataBean#groupwin(symbol)#time_length_batch(10 sec, 100)";
            env.compileDeploy(text).addListener("s0").milestone(0);

            // 1st event S1 group
            env.advanceTime(1000);
            sendEvent(env, "S1", 10);
            env.assertListenerNotInvoked("s0");

            env.milestone(1);

            // 2nd event S1 group
            env.advanceTime(5000);
            sendEvent(env, "S1", 20);
            env.assertListenerNotInvoked("s0");

            env.milestone(2);

            // 1st event S2 group
            env.advanceTime(10000);
            sendEvent(env, "S2", 30);
            env.assertListenerNotInvoked("s0");

            env.milestone(3);

            env.advanceTime(10999);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(11000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{10d}, {20d}}, null);

            env.milestone(4);

            env.advanceTime(20000);
            env.assertPropsPerRowIRPair("s0", fields, new Object[][]{{30d}}, null);

            env.milestone(5);

            env.advanceTime(21000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{10d}, {20d}});

            env.undeployAll();
        }
    }

    public static class ViewGroupLengthBatch implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "symbol,price".split(",");
            String text = "@name('s0') select irstream * from SupportMarketDataBean#groupwin(symbol)#length_batch(3) order by symbol asc";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(makeMarketDataEvent("S1", 1));

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("S2", 20));

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent("S2", 21));

            env.milestone(3);

            env.sendEventBean(makeMarketDataEvent("S1", 2));
            env.assertListenerNotInvoked("s0");

            env.milestone(4);

            // test iterator
            env.assertPropsPerRowIterator("s0", new String[]{"price"}, new Object[][]{{1.0}, {2.0}, {20.0}, {21.0}});

            env.sendEventBean(makeMarketDataEvent("S2", 22));
            env.assertPropsPerRowIRPairFlattened("s0", fields, new Object[][]{{"S2", 20.0}, {"S2", 21.0}, {"S2", 22.0}}, null);

            env.milestone(5);

            env.sendEventBean(makeMarketDataEvent("S2", 23));
            env.assertListenerNotInvoked("s0");

            env.milestone(6);

            env.sendEventBean(makeMarketDataEvent("S1", 3));
            env.assertPropsPerRowIRPairFlattened("s0", fields, new Object[][]{{"S1", 1.0}, {"S1", 2.0}, {"S1", 3.0}}, null);

            env.milestone(7);

            env.sendEventBean(makeMarketDataEvent("S2", 24));
            env.assertListenerNotInvoked("s0");

            env.milestone(8);

            env.sendEventBean(makeMarketDataEvent("S2", 25));
            env.assertPropsPerRowIRPairFlattened("s0", fields, new Object[][]{{"S2", 23.0}, {"S2", 24.0}, {"S2", 25.0}}, new Object[][]{{"S2", 20.0}, {"S2", 21.0}, {"S2", 22.0}});

            env.milestone(9);

            env.sendEventBean(makeMarketDataEvent("S1", 4));
            env.sendEventBean(makeMarketDataEvent("S1", 5));
            env.assertListenerNotInvoked("s0");

            env.milestone(10);

            env.sendEventBean(makeMarketDataEvent("S1", 6));
            env.assertPropsPerRowIRPairFlattened("s0", fields, new Object[][]{{"S1", 4.0}, {"S1", 5.0}, {"S1", 6.0}}, new Object[][]{{"S1", 1.0}, {"S1", 2.0}, {"S1", 3.0}});

            env.milestone(11);

            env.undeployAll();
        }
    }

    public static class ViewGroupTimeWin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "price".split(",");
            env.advanceTime(1000);

            String text = "@name('s0') select irstream * from  SupportMarketDataBean#groupwin(symbol)#time(10 sec)";
            env.compileDeploy(text).addListener("s0");

            // 1st event S1 group
            env.advanceTime(1000);
            sendEvent(env, "S1", 10);
            assertPrice(env, 10d);

            env.milestone(1);

            // 2nd event S1 group
            env.advanceTime(5000);
            sendEvent(env, "S1", 20);
            assertPrice(env, 20d);

            env.milestone(2);

            // 1st event S2 group
            env.advanceTime(10000);
            sendEvent(env, "S2", 30);
            assertPrice(env, 30d);

            env.milestone(3);

            env.advanceTime(10999);
            env.assertListenerNotInvoked("s0");

            env.advanceTime(11000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{10d}});

            env.milestone(4);

            env.advanceTime(15000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{20d}});

            env.milestone(5);

            env.advanceTime(20000);
            env.assertPropsPerRowIRPair("s0", fields, null, new Object[][]{{30d}});

            env.undeployAll();
        }
    }

    public static class ViewGroupLengthWin implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");

            env.milestone(0);

            String epl = "@Name('s0') select irstream theString as c0,intPrimitive as c1 from SupportBean#groupwin(theString)#length(3)";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(1);

            env.assertPropsPerRowIterator("s0", fields, new Object[0][]);
            sendSupportBean(env, "E1", 1);
            env.assertPropsNew("s0", fields, new Object[]{"E1", 1});

            env.milestone(2);

            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}});
            sendSupportBean(env, "E2", 20);
            env.assertPropsNew("s0", fields, new Object[]{"E2", 20});

            env.milestone(3);

            sendSupportBean(env, "E1", 2);
            sendSupportBean(env, "E2", 21);
            sendSupportBean(env, "E2", 22);
            sendSupportBean(env, "E1", 3);
            env.assertPropsPerRowNewFlattened("s0", fields, new Object[][]{{"E1", 2}, {"E2", 21}, {"E2", 22}, {"E1", 3}});

            env.milestone(4);

            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E1", 3}, {"E2", 20}, {"E2", 21}, {"E2", 22}});
            sendSupportBean(env, "E2", 23);
            env.assertPropsIRPair("s0", fields, new Object[]{"E2", 23}, new Object[]{"E2", 20});

            env.milestone(5);

            env.milestone(6);

            env.assertPropsPerRowIteratorAnyOrder("s0", fields, new Object[][]{{"E1", 1}, {"E1", 2}, {"E1", 3}, {"E2", 23}, {"E2", 21}, {"E2", 22}});
            sendSupportBean(env, "E1", -1);
            env.assertPropsIRPair("s0", fields, new Object[]{"E1", -1}, new Object[]{"E1", 1});

            env.undeployAll();
        }
    }

    private static void assertCount(EPStatement stmt, long count) {
        assertEquals(count, EPAssertionUtil.iteratorCount(stmt.iterator()));
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        sendEvent(env, symbol, price, -1);
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price, long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, volume, "");
        env.sendEventBean(theEvent);
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, String feed, long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, volume, feed);
        env.sendEventBean(theEvent);
    }

    private static SupportBeanTimestamp sendEventTS(RegressionEnvironment env, String id, String groupId, long timestamp) {
        SupportBeanTimestamp bean = new SupportBeanTimestamp(id, groupId, timestamp);
        env.sendEventBean(bean);
        return bean;
    }

    private static void sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol, double price) {
        return new SupportMarketDataBean(symbol, price, 0L, null);
    }

    private static void assertLastNewRow(RegressionEnvironment env, String statementName, String symbol, double average) {
        final String[] fields = "symbol,average".split(",");
        env.assertListener(statementName, listener -> EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{symbol, average}}));
    }

    private static void assertId(RegressionEnvironment env, String expected) {
        env.assertEqualsNew("s0", "id", expected);
    }

    private static void assertPrice(RegressionEnvironment env, double expected) {
        env.assertEqualsNew("s0", "price", expected);
    }

    public static class EventWithTags implements Serializable {
        private static final long serialVersionUID = -8159216554607296223L;
        private String name;
        private Map<String, String> tags;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }
    }
}
