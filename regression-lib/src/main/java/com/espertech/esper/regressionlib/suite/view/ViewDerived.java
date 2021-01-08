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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.view.derived.ViewFieldEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionlib.support.util.DoubleValueAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ViewDerived {
    private final static String SYMBOL = "CSCO.O";
    private final static String FEED = "feed1";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ViewSizeSceneOne());
        execs.add(new ViewSizeSceneTwo());
        execs.add(new ViewSizeAddProps());
        execs.add(new ViewDerivedAll());
        execs.add(new ViewDerivedLengthWUniSceneOne());
        execs.add(new ViewDerivedLengthWUniSceneTwo());
        execs.add(new ViewDerivedLengthWUniSceneThree());
        execs.add(new ViewDerivedLengthWWeightedAvgSceneOne());
        execs.add(new ViewDerivedLengthWWeightedAvgSceneTwo());
        execs.add(new ViewDerivedLengthWRegressionLinestSceneOne());
        execs.add(new ViewDerivedLengthWRegressionLinestSceneTwo());
        execs.add(new ViewDerivedLengthWCorrelation());
        return execs;
    }

    private static class ViewSizeSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream size from SupportMarketDataBean#size";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "DELL", 1L);
            assertSize(env, 1, 0);

            sendEvent(env, "DELL", 1L);
            assertSize(env, 2, 1);

            env.undeployAll();

            epl = "@name('s0') select size, symbol, feed from SupportMarketDataBean#size(symbol, feed)";
            env.compileDeployAddListenerMile(epl, "s0", 1);
            String[] fields = "size,symbol,feed".split(",");

            sendEvent(env, "DELL", 1L);
            env.assertPropsNew("s0", fields, new Object[]{1L, "DELL", "feed1"});

            sendEvent(env, "DELL", 1L);
            env.assertPropsNew("s0", fields, new Object[]{2L, "DELL", "feed1"});

            env.undeployAll();
        }
    }

    public static class ViewSizeSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#size()";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeMarketDataEvent("E1"));
            env.assertPropsNV("s0", new Object[][]{{"size", 1L}}, new Object[][]{{"size", 0L}});

            env.milestone(1);

            env.sendEventBean(makeMarketDataEvent("E2"));
            env.assertPropsNV("s0", new Object[][]{{"size", 2L}}, new Object[][]{{"size", 1L}});

            env.milestone(2);

            for (int i = 3; i < 10; i++) {
                env.sendEventBean(makeMarketDataEvent("E" + i));
                env.assertPropsNV("s0", new Object[][]{{"size", (long) i}}, // new data
                    new Object[][]{{"size", (long) i - 1}} //  old data
                );

                env.milestone(i);
            }

            // test iterator
            env.assertPropsPerRowIterator("s0", new String[]{"size"}, new Object[][]{{9L}});

            env.undeployAll();
        }
    }

    public static class ViewSizeAddProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from  SupportMarketDataBean#size(symbol)";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(makeMarketDataEvent("E1"));
            env.assertPropsNV("s0", new Object[][]{{"size", 1L}, {"symbol", "E1"}}, new Object[][]{{"size", 0L}, {"symbol", null}});

            env.milestone(0);

            env.sendEventBean(makeMarketDataEvent("E2"));
            env.assertPropsNV("s0", new Object[][]{{"size", 2L}, {"symbol", "E2"}}, new Object[][]{{"size", 1L}, {"symbol", "E1"}});

            env.assertPropsPerRowIterator("s0", new String[]{"size", "symbol"}, new Object[][]{{2L, "E2"}});

            env.undeployAll();
        }
    }

    public static class ViewDerivedLengthWUniSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from  SupportMarketDataBean#length(3)#uni(price)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeBean(50, "f1"));
            env.assertPropsNV("s0", new Object[][]{{"total", 50d}, {"datapoints", 1L}}, new Object[][]{{"total", 0.0}, {"datapoints", 0L}});

            env.milestone(1);

            env.sendEventBean(makeBean(25, "f2"));
            env.assertPropsNV("s0", new Object[][]{{"total", 75.0}, {"datapoints", 2L}}, new Object[][]{{"total", 50d}, {"datapoints", 1L}});

            env.milestone(2);

            env.sendEventBean(makeBean(25, "f3"));
            env.assertPropsNV("s0", new Object[][]{{"total", 100.0}, {"datapoints", 3L}}, new Object[][]{{"total", 75d}, {"datapoints", 2L}});

            env.milestone(3);

            // test iterator
            env.assertPropsPerRowIterator("s0", new String[]{"total", "datapoints"}, new Object[][]{{100.0, 3L}});

            env.sendEventBean(makeBean(1, "f4"));
            env.assertPropsNV("s0", new Object[][]{{"total", 51.0}, {"datapoints", 3L}}, new Object[][]{{"total", 100d}, {"datapoints", 3L}});

            env.milestone(4);

            env.undeployAll();
        }
    }

    public static class ViewDerivedLengthWUniSceneTwo implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream * from SupportMarketDataBean(symbol='" + SYMBOL + "')#length(3)#uni(price, symbol, feed)";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.assertStatement("s0", statement -> {
                assertEquals(Double.class, statement.getEventType().getPropertyType("average"));
                assertEquals(Double.class, statement.getEventType().getPropertyType("variance"));
                assertEquals(Long.class, statement.getEventType().getPropertyType("datapoints"));
                assertEquals(Double.class, statement.getEventType().getPropertyType("total"));
                assertEquals(Double.class, statement.getEventType().getPropertyType("stddev"));
                assertEquals(Double.class, statement.getEventType().getPropertyType("stddevpa"));
            });

            sendEvent(env, SYMBOL, 100);
            checkOld(env, true, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
            checkNew(env, 1, 100, 100, 0, Double.NaN, Double.NaN);

            sendEvent(env, SYMBOL, 100.5);
            checkOld(env, false, 1, 100, 100, 0, Double.NaN, Double.NaN);
            checkNew(env, 2, 200.5, 100.25, 0.25, 0.353553391, 0.125);

            sendEvent(env, "DUMMY", 100.5);
            env.assertListenerNotInvoked("s0");

            sendEvent(env, SYMBOL, 100.7);
            checkOld(env, false, 2, 200.5, 100.25, 0.25, 0.353553391, 0.125);
            checkNew(env, 3, 301.2, 100.4, 0.294392029, 0.360555128, 0.13);

            sendEvent(env, SYMBOL, 100.6);
            checkOld(env, false, 3, 301.2, 100.4, 0.294392029, 0.360555128, 0.13);
            checkNew(env, 3, 301.8, 100.6, 0.081649658, 0.1, 0.01);

            sendEvent(env, SYMBOL, 100.9);
            checkOld(env, false, 3, 301.8, 100.6, 0.081649658, 0.1, 0.01);
            checkNew(env, 3, 302.2, 100.733333333, 0.124721913, 0.152752523, 0.023333333);
            env.undeployAll();

            // test select-star
            String eplWildcard = "@name('s0') select * from SupportBean#length(3)#uni(intPrimitive, *)";
            env.compileDeployAddListenerMile(eplWildcard, "s0", 1);

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertEventNew("s0", event -> {
                assertEquals(1.0, event.get("average"));
                assertEquals("E1", event.get("theString"));
                assertEquals(1, event.get("intPrimitive"));
            });

            env.undeployAll();
        }
    }

    public static class ViewDerivedLengthWUniSceneThree implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#length(3)#uni(price, feed)";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(makeBean(50, "f1"));
            env.assertPropsNV("s0", new Object[][]{{"total", 50d}, {"datapoints", 1L}, {"feed", "f1"}}, new Object[][]{{"total", 0.0}, {"datapoints", 0L}, {"feed", null}});

            env.milestone(0);

            env.sendEventBean(makeBean(25, "f2"));
            env.assertPropsNV("s0", new Object[][]{{"total", 75.0}, {"datapoints", 2L}, {"feed", "f2"}}, new Object[][]{{"total", 50d}, {"datapoints", 1L}, {"feed", "f1"}});

            env.undeployAll();
        }
    }

    public static class ViewDerivedLengthWWeightedAvgSceneOne implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#length(3)#weighted_avg(price, volume)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeBean(10, 1000));
            env.assertPropsNV("s0", new Object[][]{{"average", 10d}}, new Object[][]{{"average", Double.NaN}});

            env.milestone(1);

            env.sendEventBean(makeBean(11, 2000));
            env.assertPropsNV("s0", new Object[][]{{"average", 10.666666666666666}}, new Object[][]{{"average", 10.0}});

            env.milestone(2);

            env.sendEventBean(makeBean(10.5, 1500));
            env.assertPropsNV("s0", new Object[][]{{"average", 10.61111111111111}}, new Object[][]{{"average", 10.666666666666666}});

            env.milestone(3);

            // test iterator
            env.assertPropsPerRowIterator("s0", new String[]{"average"}, new Object[][]{{10.61111111111111}});

            env.sendEventBean(makeBean(9.5, 600));
            env.assertPropsNV("s0", new Object[][]{{"average", 10.597560975609756}}, new Object[][]{{"average", 10.61111111111111}});

            env.milestone(4);

            env.undeployAll();
        }
    }

    public static class ViewDerivedLengthWWeightedAvgSceneTwo implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#length(3)#weighted_avg(price, volume, feed)";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(makeBean(10, 1000, "f1"));
            env.assertPropsNV("s0", new Object[][]{{"average", 10d}, {"feed", "f1"}}, new Object[][]{{"average", Double.NaN}, {"feed", null}});

            env.milestone(0);

            env.sendEventBean(makeBean(11, 2000, "f2"));
            env.assertPropsNV("s0", new Object[][]{{"average", 10.666666666666666}, {"feed", "f2"}}, new Object[][]{{"average", 10.0}, {"feed", "f1"}});

            env.milestone(1);

            env.sendEventBean(makeBean(10.5, 1500, "f3"));
            env.assertPropsNV("s0", new Object[][]{{"average", 10.61111111111111}, {"feed", "f3"}}, new Object[][]{{"average", 10.666666666666666}, {"feed", "f2"}});

            env.undeployAll();
        }
    }

    public static class ViewDerivedLengthWRegressionLinestSceneOne implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#length(3)#linest(price, volume)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeBean(70, 1000));
            env.assertPropsNV("s0", new Object[][]{{"slope", Double.NaN}, {"YIntercept", Double.NaN}}, new Object[][]{{"slope", Double.NaN}, {"YIntercept", Double.NaN}});
            env.milestone(1);

            env.sendEventBean(makeBean(70.5, 1500));
            env.assertPropsNV("s0", new Object[][]{{"slope", 1000.0}, {"YIntercept", -69000.0}}, new Object[][]{{"slope", Double.NaN}, {"YIntercept", Double.NaN}});
            env.milestone(2);

            // test iterator
            env.assertPropsPerRowIterator("s0", new String[]{"slope", "YIntercept"}, new Object[][]{{1000.0, -69000.0}});

            env.sendEventBean(makeBean(70.1, 1200));
            env.assertPropsNV("s0", new Object[][]{{"slope", 928.571428587354}, {"YIntercept", -63952.38095349892}}, new Object[][]{{"slope", 1000.0}, {"YIntercept", -69000.0}});
            env.milestone(3);

            env.sendEventBean(makeBean(70.25, 1000));
            env.assertPropsNV("s0", new Object[][]{{"slope", 877.5510204634593}, {"YIntercept", -60443.8775549068}}, new Object[][]{{"slope", 928.571428587354}, {"YIntercept", -63952.38095349892}});
            env.milestone(4);

            env.undeployAll();
        }
    }

    public static class ViewDerivedLengthWRegressionLinestSceneTwo implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#length(3)#linest(price, volume, feed)";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(makeBean(70, 1000, "f1"));
            env.assertPropsNV("s0", new Object[][]{{"slope", Double.NaN}, {"YIntercept", Double.NaN}, {"feed", "f1"}}, new Object[][]{{"slope", Double.NaN}, {"YIntercept", Double.NaN}, {"feed", null}});

            env.milestone(0);

            env.sendEventBean(makeBean(70.5, 1500, "f2"));
            env.assertPropsNV("s0", new Object[][]{{"slope", 1000.0}, {"YIntercept", -69000.0}, {"feed", "f2"}}, new Object[][]{{"slope", Double.NaN}, {"YIntercept", Double.NaN}, {"feed", "f1"}});

            // test iterator
            env.assertPropsPerRowIterator("s0", new String[]{"slope", "YIntercept", "feed"}, new Object[][]{{1000.0, -69000.0, "f2"}});

            env.sendEventBean(makeBean(70.1, 1200, "f3"));
            env.assertPropsNV("s0", new Object[][]{{"slope", 928.571428587354}, {"YIntercept", -63952.38095349892}, {"feed", "f3"}}, new Object[][]{{"slope", 1000.0}, {"YIntercept", -69000.0}, {"feed", "f2"}});

            env.undeployAll();
        }
    }

    public static class ViewDerivedLengthWCorrelation implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select irstream * from SupportMarketDataBean#length(3)#correl(price, volume)";
            env.compileDeployAddListenerMileZero(text, "s0");

            env.sendEventBean(makeBean(70, 1000));
            env.assertPropsNV("s0", new Object[][]{{"correlation", Double.NaN}}, new Object[][]{{"correlation", Double.NaN}});

            env.milestone(1);

            env.sendEventBean(makeBean(70.5, 1500));
            env.assertPropsNV("s0", new Object[][]{{"correlation", 1.0}}, new Object[][]{{"correlation", Double.NaN}});

            env.milestone(2);

            env.sendEventBean(makeBean(70.1, 1200));
            env.assertPropsNV("s0", new Object[][]{{"correlation", 0.9762210399358}}, new Object[][]{{"correlation", 1.0}});

            // test iterator
            env.assertPropsPerRowIterator("s0", "correlation".split(","), new Object[][]{{0.9762210399358}});

            env.milestone(3);

            env.sendEventBean(makeBean(70.25, 1000));
            env.assertPropsNV("s0", new Object[][]{{"correlation", 0.7046340397673054}}, new Object[][]{{"correlation", 0.9762210399358}});

            env.milestone(4);

            env.undeployAll();
        }
    }

    public static class ViewDerivedAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            // correlation
            String[] f1 = "correlation".split(",");
            epl = "@Name('S1') select irstream * from SupportMarketDataBean#correl(price, volume)";
            env.compileDeploy(epl).addListener("S1");

            // size
            String[] f2 = "size".split(",");
            epl = "@name('S2') select irstream * from SupportMarketDataBean#size()";
            env.compileDeploy(epl).addListener("S2");

            // regression
            String[] f3 = "slope,YIntercept".split(",");
            epl = "@name('S3') select irstream * from SupportMarketDataBean#linest(price, volume)";
            env.compileDeploy(epl).addListener("S3");

            // stat:uni
            String[] f4 = "total,datapoints".split(",");
            epl = "@name('S4') select irstream * from SupportMarketDataBean#uni(volume)";
            env.compileDeploy(epl).addListener("S4");

            // stat:weighted_avg
            String[] f5 = "average".split(",");
            epl = "@name('S5') select irstream * from SupportMarketDataBean#weighted_avg(price, volume)";
            env.compileDeploy(epl).addListener("S5");

            env.milestone(0);

            env.sendEventBean(makeBean(70, 1000));
            env.assertPropsNV("S1", new Object[][]{{"correlation", Double.NaN}}, new Object[][]{{"correlation", Double.NaN}});
            env.assertPropsIRPair("S2", f2, new Object[]{1L}, new Object[]{0L});
            env.assertPropsIRPair("S3", f3, new Object[]{Double.NaN, Double.NaN}, new Object[]{Double.NaN, Double.NaN});
            env.assertPropsIRPair("S4", f4, new Object[]{1000.0, 1L}, new Object[]{0.0, 0L});
            env.assertPropsIRPair("S5", f5, new Object[]{70.0}, new Object[]{Double.NaN});

            env.milestone(1);

            env.sendEventBean(makeBean(70.5, 1500));
            env.assertPropsNV("S1", new Object[][]{{"correlation", 1.0}}, new Object[][]{{"correlation", Double.NaN}});
            env.assertPropsIRPair("S2", f2, new Object[]{2L}, new Object[]{1L});
            env.assertPropsIRPair("S3", f3, new Object[]{1000.0, -69000.0}, new Object[]{Double.NaN, Double.NaN});
            env.assertPropsIRPair("S4", f4, new Object[]{2500.0, 2L}, new Object[]{1000.0, 1L});
            env.assertPropsIRPair("S5", f5, new Object[]{(70.0 * 1000 + 70.5 * 1500) / 2500.0}, new Object[]{70.0});

            env.milestone(2);

            env.sendEventBean(makeBean(70.1, 1200));
            env.assertPropsNV("S1", new Object[][]{{"correlation", 0.9762210399358}}, new Object[][]{{"correlation", 1.0}});
            env.assertPropsIRPair("S2", f2, new Object[]{3L}, new Object[]{2L});
            env.assertPropsIRPair("S3", f3, new Object[]{928.571428587354, -63952.38095349892}, new Object[]{1000.0, -69000.0});
            env.assertPropsIRPair("S4", f4, new Object[]{3700.0, 3L}, new Object[]{2500.0, 2L});
            env.assertPropsIRPair("S5", f5, new Object[]{(70.0 * 1000 + 70.5 * 1500 + 70.1 * 1200) / 3700.0}, new Object[]{(70.0 * 1000 + 70.5 * 1500) / 2500.0});

            // test iterator
            env.assertPropsPerRowIterator("S1", f1, new Object[][]{{0.9762210399358}});
            env.assertPropsPerRowIterator("S2", f2, new Object[][]{{3L}});
            env.assertPropsPerRowIterator("S3", f3, new Object[][]{{928.571428587354, -63952.38095349892}});
            env.assertPropsPerRowIterator("S4", f4, new Object[][]{{3700.0, 3L}});
            env.assertPropsPerRowIterator("S5", f5, new Object[][]{{(70.0 * 1000 + 70.5 * 1500 + 70.1 * 1200) / 3700.0}});

            env.milestone(3);

            env.milestone(4);

            env.sendEventBean(makeBean(70.25, 1000));
            env.assertPropsNV("S1", new Object[][]{{"correlation", 0.7865410694065471}}, new Object[][]{{"correlation", 0.9762210399358}});
            env.assertPropsIRPair("S2", f2, new Object[]{4L}, new Object[]{3L});
            env.assertPropsPerRowIterator("S3", f3, new Object[][]{{854.6255506976092, -58830.39647835589}});
            env.assertPropsIRPair("S4", f4, new Object[]{4700.0, 4L}, new Object[]{3700.0, 3L});
            env.assertPropsIRPair("S5", f5, new Object[]{(70.0 * 1000 + 70.5 * 1500 + 70.1 * 1200 + 70.25 * 1000) / 4700.0}, new Object[]{(70.0 * 1000 + 70.5 * 1500 + 70.1 * 1200) / 3700.0});

            env.undeployAll();
        }

        private SupportMarketDataBean makeBean(double price, long volume) {
            return new SupportMarketDataBean("", price, volume, "");
        }
    }

    private static void assertSize(RegressionEnvironment env, long newSize, long oldSize) {
        env.assertPropsPerRowIRPair("s0", "size".split(","), new Object[][]{{newSize}}, new Object[][]{{oldSize}});
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol) {
        return new SupportMarketDataBean(symbol, 0, 0L, null);
    }

    private static SupportMarketDataBean makeBean(double price, String feed) {
        return new SupportMarketDataBean("", price, -1L, feed);
    }

    private static void checkNew(RegressionEnvironment env, long countE, double sumE, double avgE, double stdevpaE, double stdevE, double varianceE) {
        env.assertIterator("s0", iterator -> {
            checkValues(iterator.next(), false, false, countE, sumE, avgE, stdevpaE, stdevE, varianceE);
            assertFalse(iterator.hasNext());
        });

        env.assertListener("s0", listener -> {
            assertEquals(1, listener.getLastNewData().length);
            EventBean childViewValues = listener.getLastNewData()[0];
            checkValues(childViewValues, false, false, countE, sumE, avgE, stdevpaE, stdevE, varianceE);
            listener.reset();
        });
    }

    private static void checkOld(RegressionEnvironment env, boolean isFirst, long countE, double sumE, double avgE, double stdevpaE, double stdevE, double varianceE) {
        env.assertListener("s0", listener -> {
            assertEquals(1, listener.getLastOldData().length);
            EventBean childViewValues = listener.getLastOldData()[0];
            checkValues(childViewValues, isFirst, false, countE, sumE, avgE, stdevpaE, stdevE, varianceE);
        });
    }

    private static void checkValues(EventBean values, boolean isFirst, boolean isNewData, long countE, double sumE, double avgE, double stdevpaE, double stdevE, double varianceE) {
        long count = getLongValue(ViewFieldEnum.UNIVARIATE_STATISTICS__DATAPOINTS, values);
        double sum = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__TOTAL, values);
        double avg = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__AVERAGE, values);
        double stdevpa = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEVPA, values);
        double stdev = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__STDDEV, values);
        double variance = getDoubleValue(ViewFieldEnum.UNIVARIATE_STATISTICS__VARIANCE, values);

        assertEquals(count, countE);
        assertTrue(DoubleValueAssertionUtil.equals(sum, sumE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(avg, avgE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(stdevpa, stdevpaE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(stdev, stdevE, 6));
        assertTrue(DoubleValueAssertionUtil.equals(variance, varianceE, 6));
        if (isFirst && !isNewData) {
            assertEquals(null, values.get("symbol"));
            assertEquals(null, values.get("feed"));
        } else {
            assertEquals(SYMBOL, values.get("symbol"));
            assertEquals(FEED, values.get("feed"));
        }
    }

    private static double getDoubleValue(ViewFieldEnum field, EventBean values) {
        return (Double) values.get(field.getName());
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, price, 0L, FEED);
        env.sendEventBean(theEvent);
    }

    private static SupportMarketDataBean makeBean(double price, long volume) {
        return new SupportMarketDataBean("", price, volume, "");
    }

    private static long getLongValue(ViewFieldEnum field, EventBean values) {
        return (Long) values.get(field.getName());
    }

    private static SupportMarketDataBean makeBean(double price, long volume, String feed) {
        return new SupportMarketDataBean("", price, volume, feed);
    }
}
