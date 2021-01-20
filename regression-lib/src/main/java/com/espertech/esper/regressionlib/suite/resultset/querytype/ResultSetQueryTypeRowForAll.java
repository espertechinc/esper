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
package com.espertech.esper.regressionlib.suite.resultset.querytype;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.bean.SupportPriceEvent;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResultSetQueryTypeRowForAll {
    private final static String JOIN_KEY = "KEY";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeRowForAllSimple());
        execs.add(new ResultSetQueryTypeRowForAllSumMinMax());
        execs.add(new ResultSetQueryTypeRowForAllWWindowAgg());
        execs.add(new ResultSetQueryTypeRowForAllMinMaxWindowed());
        execs.add(new ResultSetQueryTypeRowForAllSumOneView());
        execs.add(new ResultSetQueryTypeRowForAllSumJoin());
        execs.add(new ResultSetQueryTypeRowForAllAvgPerSym());
        execs.add(new ResultSetQueryTypeRowForAllSelectStarStdGroupBy());
        execs.add(new ResultSetQueryTypeRowForAllSelectExprGroupWin());
        execs.add(new ResultSetQueryTypeRowForAllSelectAvgExprStdGroupBy());
        execs.add(new ResultSetQueryTypeRowForAllSelectAvgStdGroupByUni());
        execs.add(new ResultSetQueryTypeRowForAllNamedWindowWindow());
        execs.add(new ResultSetQueryTypeRowForAllStaticMethodDoubleNested());
        return execs;
    }

    private static class ResultSetQueryTypeRowForAllStaticMethodDoubleNested implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "import " + MyHelper.class.getName() + ";\n" +
                "@name('s0') select MyHelper.doOuter(MyHelper.doInner(last(theString))) as c0 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertEqualsNew("s0", "c0", "oiE1io");

            env.undeployAll();
        }
    }


    public static class ResultSetQueryTypeRowForAllSumMinMax implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2,c3".split(",");

            env.milestone(0);
            env.advanceTime(0);
            String epl = "@Name('s0') select theString as c0, sum(intPrimitive) as c1," +
                "min(intPrimitive) as c2, max(intPrimitive) as c3 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(1);

            sendEventSB(env, "E1", 10);
            env.assertPropsNew("s0", fields, new Object[]{"E1", 10, 10, 10});

            env.milestone(2);

            sendEventSB(env, "E2", 100);
            env.assertPropsNew("s0", fields, new Object[]{"E2", 10 + 100, 10, 100});

            env.milestone(3);

            sendEventSB(env, "E3", 11);
            env.assertPropsNew("s0", fields, new Object[]{"E3", 10 + 100 + 11, 10, 100});

            env.milestone(4);

            env.milestone(5);

            sendEventSB(env, "E4", 9);
            env.assertPropsNew("s0", fields, new Object[]{"E4", 10 + 100 + 11 + 9, 9, 100});

            sendEventSB(env, "E5", 120);
            env.assertPropsNew("s0", fields, new Object[]{"E5", 10 + 100 + 11 + 9 + 120, 9, 120});

            sendEventSB(env, "E6", 100);
            env.assertPropsNew("s0", fields, new Object[]{"E6", 10 + 100 + 11 + 9 + 120 + 100, 9, 120});

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeRowForAllWWindowAgg implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2".split(",");

            String epl = "@Name('s0') select irstream theString as c0, sum(intPrimitive) as c1," +
                "window(*) as c2 from SupportBean.win:length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            Object e1 = sendSupportBean(env, "E1", 10);
            env.assertPropsNew("s0", fields, new Object[]{"E1", 10, new Object[]{e1}});

            env.milestone(1);

            Object e2 = sendSupportBean(env, "E2", 100);
            env.assertPropsNew("s0", fields, new Object[]{"E2", 10 + 100, new Object[]{e1, e2}});

            env.milestone(2);

            Object e3 = sendSupportBean(env, "E3", 11);
            env.assertPropsIRPair("s0", fields, new Object[]{"E3", 100 + 11, new Object[]{e2, e3}}, new Object[]{"E1", 100 + 11, new Object[]{e2, e3}});

            env.milestone(3);

            env.milestone(4);

            Object e4 = sendSupportBean(env, "E4", 9);
            env.assertPropsIRPair("s0", fields, new Object[]{"E4", 11 + 9, new Object[]{e3, e4}}, new Object[]{"E2", 11 + 9, new Object[]{e3, e4}});

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeRowForAllSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream " +
                "avg(price) as avgPrice," +
                "sum(price) as sumPrice," +
                "min(price) as minPrice," +
                "max(price) as maxPrice," +
                "median(price) as medianPrice," +
                "stddev(price) as stddevPrice," +
                "avedev(price) as avedevPrice," +
                "count(*) as datacount, " +
                "count(distinct price) as countDistinctPrice " +
                "from SupportMarketDataBean";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.sendEventBean(makeMarketDataEvent(100));

            env.assertPropsNV("s0",
                new Object[][]{
                    {"avgPrice", 100d},
                    {"sumPrice", 100d},
                    {"minPrice", 100d},
                    {"maxPrice", 100d},
                    {"medianPrice", 100d},
                    {"stddevPrice", null},
                    {"avedevPrice", 0.0},
                    {"datacount", 1L},
                    {"countDistinctPrice", 1L},
                }, // new data
                new Object[][]{
                    {"avgPrice", null},
                    {"sumPrice", null},
                    {"minPrice", null},
                    {"maxPrice", null},
                    {"medianPrice", null},
                    {"stddevPrice", null},
                    {"avedevPrice", null},
                    {"datacount", 0L},
                    {"countDistinctPrice", 0L},
                } // old data
            );

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent(200));
            env.assertPropsNV("s0",
                new Object[][]{
                    {"avgPrice", (100 + 200) / 2.0},
                    {"sumPrice", 100 + 200d},
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                    {"medianPrice", 150d},
                    {"stddevPrice", 70.71067811865476},
                    {"avedevPrice", 50d},
                    {"datacount", 2L},
                    {"countDistinctPrice", 2L},
                }, // new data
                new Object[][]{
                    {"avgPrice", 100d},
                    {"sumPrice", 100d},
                    {"minPrice", 100d},
                    {"maxPrice", 100d},
                    {"medianPrice", 100d},
                    {"stddevPrice", null},
                    {"avedevPrice", 0.0},
                    {"datacount", 1L},
                    {"countDistinctPrice", 1L},
                } // old data
            );

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent(150));
            env.assertPropsNV("s0",
                new Object[][]{
                    {"avgPrice", (150 + 100 + 200) / 3.0},
                    {"sumPrice", 150 + 100 + 200d},
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                    {"medianPrice", 150d},
                    {"stddevPrice", 50d},
                    {"avedevPrice", 33 + 1 / 3d},
                    {"datacount", 3L},
                    {"countDistinctPrice", 3L},
                }, // new data
                new Object[][]{
                    {"avgPrice", (100 + 200) / 2.0},
                    {"sumPrice", 100 + 200d},
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                    {"medianPrice", 150d},
                    {"stddevPrice", 70.71067811865476},
                    {"avedevPrice", 50d},
                    {"datacount", 2L},
                    {"countDistinctPrice", 2L},
                } // old data
            );

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeRowForAllMinMaxWindowed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream " +
                "min(price) as minPrice," +
                "max(price) as maxPrice " +
                "from  SupportMarketDataBean#length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.sendEventBean(makeMarketDataEvent(100));
            env.assertPropsNV("s0",
                new Object[][]{
                    {"minPrice", 100d},
                    {"maxPrice", 100d},
                }, // new data
                new Object[][]{
                    {"minPrice", null},
                    {"maxPrice", null},
                } // old data
            );

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent(200));
            env.assertPropsNV("s0",
                new Object[][]{
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                }, // new data
                new Object[][]{
                    {"minPrice", 100d},
                    {"maxPrice", 100d},
                } // old data
            );

            env.milestone(2);

            env.sendEventBean(makeMarketDataEvent(150));
            env.assertPropsNV("s0",
                new Object[][]{
                    {"minPrice", 150d},
                    {"maxPrice", 200d},
                }, // new data
                new Object[][]{
                    {"minPrice", 100d},
                    {"maxPrice", 200d},
                } // old data
            );

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAllSumOneView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream sum(longBoxed) as mySum " +
                "from SupportBean#time(10 sec)";
            env.compileDeploy(epl).addListener("s0");

            sendTimerEvent(env, 0);

            tryAssert(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAllSumJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream sum(longBoxed) as mySum " +
                "from SupportBeanString#keepall as one, " +
                "SupportBean#time(10 sec) as two " +
                "where one.theString = two.theString";
            env.compileDeploy(epl).addListener("s0");

            sendTimerEvent(env, 0);

            env.sendEventBean(new SupportBeanString(JOIN_KEY));

            tryAssert(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAllAvgPerSym implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "sym,avgp".split(",");
            String epl = "@name('s0') select irstream avg(price) as avgp, sym " +
                "from SupportPriceEvent#groupwin(sym)#length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportPriceEvent(1, "A"));
            env.assertPropsNew("s0", fields, new Object[] {"A", 1.0});

            env.sendEventBean(new SupportPriceEvent(2, "B"));
            env.assertPropsNew("s0", fields, new Object[] {"B", 1.5});

            env.milestone(0);

            env.sendEventBean(new SupportPriceEvent(9, "A"));
            env.assertPropsNew("s0", fields, new Object[] {"A", (1 + 2 + 9) / 3.0});

            env.sendEventBean(new SupportPriceEvent(18, "B"));
            env.assertPropsNew("s0", fields, new Object[] {"B", (1 + 2 + 9 + 18) / 4.0});

            env.sendEventBean(new SupportPriceEvent(5, "A"));
            env.assertPropsIRPair("s0", fields, new Object[] {"A", (2 + 9 + 18 + 5) / 4.0}, new Object[] {"A", (5 + 2 + 9 + 18) / 4.0});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAllSelectStarStdGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream * from SupportMarketDataBean#groupwin(symbol)#length(2)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", 1);
            env.assertListener("s0", listener -> {
                assertEquals(1.0, listener.getLastNewData()[0].get("price"));
                assertTrue(listener.getLastNewData()[0].getUnderlying() instanceof SupportMarketDataBean);
            });

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAllSelectExprGroupWin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream price from SupportMarketDataBean#groupwin(symbol)#length(2)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", 1);
            env.assertListener("s0", listener -> assertEquals(1.0, env.listener("s0").getLastNewData()[0].get("price")));

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAllSelectAvgExprStdGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream avg(price) as aprice from SupportMarketDataBean"
                + "#groupwin(symbol)#length(2)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", 1);
            assertAPrice(env, 1.0);

            env.milestone(0);

            sendEvent(env, "B", 3);
            assertAPrice(env, 2.0);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAllSelectAvgStdGroupByUni implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream average as aprice from SupportMarketDataBean"
                + "#groupwin(symbol)#length(2)#uni(price)";
            env.compileDeploy(stmtText).addListener("s0");

            sendEvent(env, "A", 1);
            env.assertEqualsNew("s0", "aprice", 1.0);

            env.milestone(0);

            sendEvent(env, "B", 3);
            env.assertEqualsNew("s0", "aprice", 3.0);

            sendEvent(env, "A", 3);
            env.assertEqualsNew("s0", "aprice", 2.0);

            env.milestone(1);

            sendEvent(env, "A", 10);
            env.listenerReset("s0");

            sendEvent(env, "A", 20);
            env.assertEqualsNew("s0", "aprice", 15.0);

            env.undeployAll();
        }
    }

    private static void tryAssert(RegressionEnvironment env) {
        // assert select result type
        String[] fields = "mySum".split(",");
        env.assertStatement("s0", statement -> assertEquals(Long.class, statement.getEventType().getPropertyType("mySum")));
        env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"mySum"}, new Object[][]{{null}});

        sendTimerEvent(env, 0);
        sendEvent(env, 10);
        assertMySum(env, 10L);
        env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"mySum"}, new Object[][]{{10L}});

        sendTimerEvent(env, 5000);
        sendEvent(env, 15);
        assertMySum(env, 25L);
        env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"mySum"}, new Object[][]{{25L}});

        sendTimerEvent(env, 8000);
        sendEvent(env, -5);
        assertMySum(env, 20L);
        env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"mySum"}, new Object[][]{{20L}});

        sendTimerEvent(env, 10000);
        env.assertPropsIRPair("s0", fields, new Object[] {10L}, new Object[] {20L});
        env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"mySum"}, new Object[][]{{10L}});

        sendTimerEvent(env, 15000);
        env.assertPropsIRPair("s0", fields, new Object[] {-5L}, new Object[] {10L});
        env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"mySum"}, new Object[][]{{-5L}});

        sendTimerEvent(env, 18000);
        env.assertPropsIRPair("s0", fields, new Object[] {null}, new Object[] {-5L});
        env.assertPropsPerRowIteratorAnyOrder("s0", new String[]{"mySum"}, new Object[][]{{null}});
    }

    public static class ResultSetQueryTypeRowForAllNamedWindowWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1".split(",");
            String epl = "create window ABCWin.win:keepall() as SupportBean;\n" +
                "insert into ABCWin select * from SupportBean;\n" +
                "on SupportBean_A delete from ABCWin where theString = id;\n" +
                "@Name('s0') select irstream theString as c0, window(intPrimitive) as c1 from ABCWin;\n";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            sendSupportBean(env, "E1", 10);
            env.assertPropsNew("s0", fields, new Object[]{"E1", new Integer[]{10}});

            env.milestone(1);

            sendSupportBean(env, "E2", 100);
            env.assertPropsNew("s0", fields, new Object[]{"E2", new Integer[]{10, 100}});

            env.milestone(2);

            sendSupportBean_A(env, "E2");    // delete E2
            env.assertPropsOld("s0", fields, new Object[]{"E2", new Integer[]{10}});

            env.milestone(3);

            sendSupportBean(env, "E3", 50);
            env.assertPropsNew("s0", fields, new Object[]{"E3", new Integer[]{10, 50}});

            env.milestone(4);

            env.milestone(5);  // no change

            sendSupportBean_A(env, "E1");    // delete E1
            env.assertPropsOld("s0", fields, new Object[]{"E1", new Integer[]{50}});

            env.milestone(6);

            sendSupportBean(env, "E4", -1);
            env.assertPropsNew("s0", fields, new Object[]{"E4", new Integer[]{50, -1}});

            env.undeployAll();
        }
    }

    private static void sendSupportBean_A(RegressionEnvironment env, String id) {
        env.sendEventBean(new SupportBean_A(id));
    }

    private static SupportBean sendSupportBean(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        env.sendEventBean(sb);
        return sb;
    }

    private static Object sendEvent(RegressionEnvironment env, String symbol, double price) {
        Object theEvent = new SupportMarketDataBean(symbol, price, null, null);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed, int intBoxed, short shortBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(JOIN_KEY);
        bean.setLongBoxed(longBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setShortBoxed(shortBoxed);
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env, long longBoxed) {
        sendEvent(env, longBoxed, 0, (short) 0);
    }

    private static SupportMarketDataBean makeMarketDataEvent(double price) {
        return new SupportMarketDataBean("DELL", price, 0L, null);
    }

    private static void sendEventSB(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }

    private static void sendTimerEvent(RegressionEnvironment env, long msec) {
        env.advanceTime(msec);
    }

    private static void assertMySum(RegressionEnvironment env, long expected) {
        env.assertListener("s0", listener -> assertEquals(expected, listener.getAndResetLastNewData()[0].get("mySum")));
    }

    private static void assertAPrice(RegressionEnvironment env, double expected) {
        env.assertListener("s0", listener -> assertEquals(expected, listener.getAndResetLastNewData()[0].get("aprice")));
    }

    public static class MyHelper {
        public static String doOuter(String value) {
            return "o" + value + "o";
        }

        public static String doInner(String value) {
            return "i" + value + "i";
        }
    }
}
