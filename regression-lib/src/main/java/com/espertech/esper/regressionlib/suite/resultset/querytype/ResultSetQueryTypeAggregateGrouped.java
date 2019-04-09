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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithIntArray;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class ResultSetQueryTypeAggregateGrouped {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeCriteriaByDotMethod());
        execs.add(new ResultSetQueryTypeIterateUnbound());
        execs.add(new ResultSetQueryTypeUnaggregatedHaving());
        execs.add(new ResultSetQueryTypeWildcard());
        execs.add(new ResultSetQueryTypeAggregationOverGroupedProps());
        execs.add(new ResultSetQueryTypeSumOneView());
        execs.add(new ResultSetQueryTypeSumJoin());
        execs.add(new ResultSetQueryTypeInsertInto());
        execs.add(new ResultSetQueryTypeMultikeyWArray());
        return execs;
    }

    private static class ResultSetQueryTypeMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select id, sum(value) as thesum from SupportEventWithIntArray group by array";
            env.compileDeploy(epl).addListener("s0");

            sendAssertIntArray(env, "E1", new int[] {1, 2}, 5, 5);

            env.milestone(0);

            sendAssertIntArray(env, "E2", new int[] {1, 2}, 10, 15);
            sendAssertIntArray(env, "E3", new int[] {1}, 11, 11);
            sendAssertIntArray(env, "E4", new int[] {1, 3}, 12, 12);

            env.milestone(1);

            sendAssertIntArray(env, "E5", new int[] {1}, 13, 24);
            sendAssertIntArray(env, "E6", new int[] {1, 3}, 15, 27);
            sendAssertIntArray(env, "E7", new int[] {1, 2}, 16, 31);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeCriteriaByDotMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select sb.getLongPrimitive() as c0, sum(intPrimitive) as c1 from SupportBean#length_batch(2) as sb group by sb.getTheString()";
            env.compileDeploy(epl).addListener("s0");

            makeSendSupportBean(env, "E1", 10, 100L);

            env.milestone(0);

            makeSendSupportBean(env, "E1", 20, 200L);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "c0,c1".split(","),
                new Object[][]{{100L, 30}, {200L, 30}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeIterateUnbound implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            String epl = "@name('s0') @IterableUnbound select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));

            env.milestone(0);

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 10}, {"E2", 20}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E1", 11));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnaggregatedHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select theString from SupportBean group by theString having intPrimitive > 5";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 3));
            env.sendEventBean(new SupportBean("E2", 5));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 6));
            Assert.assertEquals("E1", env.listener("s0").assertOneGetNewAndReset().get("theString"));

            env.sendEventBean(new SupportBean("E3", 7));
            Assert.assertEquals("E3", env.listener("s0").assertOneGetNewAndReset().get("theString"));

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test no output limit
            String[] fields = "theString, intPrimitive, minval".split(",");
            String epl = "@name('s0') select *, min(intPrimitive) as minval from SupportBean#length(2) group by theString";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("G1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 10, 10});

            env.sendEventBean(new SupportBean("G1", 9));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 9, 9});

            env.milestone(0);

            env.sendEventBean(new SupportBean("G1", 11));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"G1", 11, 9});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAggregationOverGroupedProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test for ESPER-185
            String[] fields = "volume,symbol,price,mycount".split(",");
            String epl = "@name('s0') select irstream volume,symbol,price,count(price) as mycount " +
                "from SupportMarketDataBean#length(5) " +
                "group by symbol, price";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, SYMBOL_DELL, 1000, 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1000L, "DELL", 10.0, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 1L}});

            env.milestone(0);

            sendEvent(env, SYMBOL_DELL, 900, 11);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{900L, "DELL", 11.0, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 1L}, {900L, "DELL", 11.0, 1L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_DELL, 1500, 10);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{1500L, "DELL", 10.0, 2L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 2L}, {900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 2L}});
            env.listener("s0").reset();

            env.milestone(1);

            sendEvent(env, SYMBOL_IBM, 500, 5);
            Assert.assertEquals(1, env.listener("s0").getNewDataList().size());
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{500L, "IBM", 5.0, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 2L}, {900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 2L}, {500L, "IBM", 5.0, 1L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_IBM, 600, 5);
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{600L, "IBM", 5.0, 2L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1000L, "DELL", 10.0, 2L}, {900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 2L}, {500L, "IBM", 5.0, 2L}, {600L, "IBM", 5.0, 2L}});
            env.listener("s0").reset();

            env.milestone(2);

            sendEvent(env, SYMBOL_IBM, 500, 5);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{500L, "IBM", 5.0, 3L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{1000L, "DELL", 10.0, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{900L, "DELL", 11.0, 1L}, {1500L, "DELL", 10.0, 1L}, {500L, "IBM", 5.0, 3L}, {600L, "IBM", 5.0, 3L}, {500L, "IBM", 5.0, 3L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_IBM, 600, 5);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{600L, "IBM", 5.0, 4L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{900L, "DELL", 11.0, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1500L, "DELL", 10.0, 1L}, {500L, "IBM", 5.0, 4L}, {600L, "IBM", 5.0, 4L}, {500L, "IBM", 5.0, 4L}, {600L, "IBM", 5.0, 4L}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSumOneView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select irstream symbol, volume, sum(price) as mySum " +
                "from SupportMarketDataBean#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionSum(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSumJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Every event generates a new row, this time we sum the price by symbol and output volume
            String epl = "@name('s0') select irstream symbol, volume, sum(price) as mySum " +
                "from SupportBeanString#length(100) as one, " +
                "SupportMarketDataBean#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
            env.sendEventBean(new SupportBeanString(SYMBOL_IBM));

            tryAssertionSum(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmt = "@name('s0') select symbol as symbol, avg(price) as average, sum(volume) as sumation from SupportMarketDataBean#length(3000)";
            env.compileDeploy(stmt).addListener("s0");

            env.sendEventBean(new SupportMarketDataBean("IBM", 10D, 20000L, null));
            EventBean eventBean = env.listener("s0").getLastNewData()[0];
            Assert.assertEquals("IBM", eventBean.get("symbol"));
            Assert.assertEquals(10d, eventBean.get("average"));
            Assert.assertEquals(20000L, eventBean.get("sumation"));

            // create insert into statements
            stmt = "@name('s1') insert into StockAverages select symbol as symbol, avg(price) as average, sum(volume) as sumation " +
                "from SupportMarketDataBean#length(3000);\n" +
                "@name('s2') select * from StockAverages";
            env.compileDeploy(stmt).addListener("s1").addListener("s2");

            // send event
            env.sendEventBean(new SupportMarketDataBean("IBM", 20D, 40000L, null));
            eventBean = env.listener("s0").getLastNewData()[0];
            Assert.assertEquals("IBM", eventBean.get("symbol"));
            Assert.assertEquals(15d, eventBean.get("average"));
            Assert.assertEquals(60000L, eventBean.get("sumation"));

            Assert.assertEquals(1, env.listener("s2").getNewDataList().size());
            Assert.assertEquals(1, env.listener("s2").getLastNewData().length);
            eventBean = env.listener("s2").getLastNewData()[0];
            Assert.assertEquals("IBM", eventBean.get("symbol"));
            Assert.assertEquals(20d, eventBean.get("average"));
            Assert.assertEquals(40000L, eventBean.get("sumation"));

            env.undeployAll();
        }
    }

    private static void tryAssertionSum(RegressionEnvironment env) {
        String[] fields = new String[]{"symbol", "volume", "mySum"};
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, null);

        // assert select result type
        Assert.assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("symbol"));
        Assert.assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("volume"));
        Assert.assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("mySum"));

        sendEvent(env, SYMBOL_DELL, 10000, 51);
        assertEvents(env, SYMBOL_DELL, 10000, 51);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{
            {"DELL", 10000L, 51d}});

        env.milestone(0);

        sendEvent(env, SYMBOL_DELL, 20000, 52);
        assertEvents(env, SYMBOL_DELL, 20000, 103);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{
            {"DELL", 10000L, 103d}, {"DELL", 20000L, 103d}});

        sendEvent(env, SYMBOL_IBM, 30000, 70);
        assertEvents(env, SYMBOL_IBM, 30000, 70);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{
            {"DELL", 10000L, 103d}, {"DELL", 20000L, 103d}, {"IBM", 30000L, 70d}});

        env.milestone(1);

        sendEvent(env, SYMBOL_IBM, 10000, 20);
        assertEvents(env, SYMBOL_DELL, 10000, 52, SYMBOL_IBM, 10000, 90);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{
            {"DELL", 20000L, 52d}, {"IBM", 30000L, 90d}, {"IBM", 10000L, 90d}});

        sendEvent(env, SYMBOL_DELL, 40000, 45);
        assertEvents(env, SYMBOL_DELL, 20000, 45, SYMBOL_DELL, 40000, 45);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{
            {"IBM", 10000L, 90d}, {"IBM", 30000L, 90d}, {"DELL", 40000L, 45d}});
    }

    private static void assertEvents(RegressionEnvironment env, String symbol, long volume, double sum) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

        assertNull(oldData);
        assertEquals(1, newData.length);

        Assert.assertEquals(symbol, newData[0].get("symbol"));
        Assert.assertEquals(volume, newData[0].get("volume"));
        Assert.assertEquals(sum, newData[0].get("mySum"));

        env.listener("s0").reset();
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void assertEvents(RegressionEnvironment env, String symbolOld, long volumeOld, double sumOld,
                                     String symbolNew, long volumeNew, double sumNew) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        Assert.assertEquals(symbolOld, oldData[0].get("symbol"));
        Assert.assertEquals(volumeOld, oldData[0].get("volume"));
        Assert.assertEquals(sumOld, oldData[0].get("mySum"));

        Assert.assertEquals(symbolNew, newData[0].get("symbol"));
        Assert.assertEquals(volumeNew, newData[0].get("volume"));
        Assert.assertEquals(sumNew, newData[0].get("mySum"));

        env.listener("s0").reset();
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, long volume, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, volume, null);
        env.sendEventBean(bean);
    }

    private static SupportBean makeSendSupportBean(RegressionEnvironment env, String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
        return bean;
    }

    private static void sendAssertIntArray(RegressionEnvironment env, String id, int[] array, int value, int expected) {
        final String[] fields = "id,thesum".split(",");
        env.sendEventBean(new SupportEventWithIntArray(id, array, value));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[] {id, expected});
    }
}
