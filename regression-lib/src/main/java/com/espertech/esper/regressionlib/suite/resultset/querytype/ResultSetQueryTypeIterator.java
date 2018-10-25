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
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResultSetQueryTypeIterator {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypePatternNoWindow());
        execs.add(new ResultSetQueryTypePatternWithWindow());
        execs.add(new ResultSetQueryTypeOrderByWildcard());
        execs.add(new ResultSetQueryTypeOrderByProps());
        execs.add(new ResultSetQueryTypeFilter());
        execs.add(new ResultSetQueryTypeRowPerGroupOrdered());
        execs.add(new ResultSetQueryTypeRowPerGroup());
        execs.add(new ResultSetQueryTypeRowPerGroupHaving());
        execs.add(new ResultSetQueryTypeRowPerGroupComplex());
        execs.add(new ResultSetQueryTypeAggregateGroupedOrdered());
        execs.add(new ResultSetQueryTypeAggregateGrouped());
        execs.add(new ResultSetQueryTypeAggregateGroupedHaving());
        execs.add(new ResultSetQueryTypeRowPerEvent());
        execs.add(new ResultSetQueryTypeRowPerEventOrdered());
        execs.add(new ResultSetQueryTypeRowPerEventHaving());
        execs.add(new ResultSetQueryTypeRowForAll());
        execs.add(new ResultSetQueryTypeRowForAllHaving());
        return execs;
    }

    private static class ResultSetQueryTypePatternNoWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test for Esper-115
            String epl = "@name('s0') @IterableUnbound select * from pattern " +
                "[every ( addressInfo = SupportBean(theString='address') " +
                "-> txnWD = SupportBean(theString='txn') ) ] " +
                "where addressInfo.intBoxed = txnWD.intBoxed";
            env.compileDeploy(epl).addListener("s0");

            SupportBean myEventBean1 = new SupportBean();
            myEventBean1.setTheString("address");
            myEventBean1.setIntBoxed(9001);
            env.sendEventBean(myEventBean1);
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            SupportBean myEventBean2 = new SupportBean();
            myEventBean2.setTheString("txn");
            myEventBean2.setIntBoxed(9001);
            env.sendEventBean(myEventBean2);
            assertTrue(env.statement("s0").iterator().hasNext());

            env.milestone(1);

            Iterator<EventBean> itr = env.statement("s0").iterator();
            EventBean theEvent = itr.next();
            Assert.assertEquals(myEventBean1, theEvent.get("addressInfo"));
            Assert.assertEquals(myEventBean2, theEvent.get("txnWD"));

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypePatternWithWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern " +
                "[every ( addressInfo = SupportBean(theString='address') " +
                "-> txnWD = SupportBean(theString='txn') ) ]#lastevent " +
                "where addressInfo.intBoxed = txnWD.intBoxed";
            env.compileDeploy(epl).addListener("s0");

            SupportBean myEventBean1 = new SupportBean();
            myEventBean1.setTheString("address");
            myEventBean1.setIntBoxed(9001);
            env.sendEventBean(myEventBean1);

            SupportBean myEventBean2 = new SupportBean();
            myEventBean2.setTheString("txn");
            myEventBean2.setIntBoxed(9001);
            env.sendEventBean(myEventBean2);

            env.milestone(0);

            Iterator<EventBean> itr = env.statement("s0").iterator();
            EventBean theEvent = itr.next();
            Assert.assertEquals(myEventBean1, theEvent.get("addressInfo"));
            Assert.assertEquals(myEventBean2, theEvent.get("txnWD"));

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeOrderByWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * from SupportMarketDataBean#length(5) order by symbol, volume";
            env.compileDeploy(stmtText).addListener("s0");

            assertFalse(env.statement("s0").iterator().hasNext());

            Object eventOne = sendEvent(env, "SYM", 1);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventOne}, env.statement("s0").iterator());

            Object eventTwo = sendEvent(env, "OCC", 2);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventOne}, env.statement("s0").iterator());

            Object eventThree = sendEvent(env, "TOC", 3);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventOne, eventThree}, env.statement("s0").iterator());

            Object eventFour = sendEvent(env, "SYM", 0);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventFour, eventOne, eventThree}, env.statement("s0").iterator());

            Object eventFive = sendEvent(env, "SYM", 10);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventFour, eventOne, eventFive, eventThree}, env.statement("s0").iterator());

            Object eventSix = sendEvent(env, "SYM", 4);
            EPAssertionUtil.assertEqualsExactOrderUnderlying(new Object[]{eventTwo, eventFour, eventSix, eventFive, eventThree}, env.statement("s0").iterator());

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeOrderByProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "volume"};
            String stmtText = "@name('s0') select symbol, volume from SupportMarketDataBean#length(3) order by symbol, volume";
            env.compileDeploy(stmtText).addListener("s0");

            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", 1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 1L}});

            sendEvent(env, "OCC", 2);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"OCC", 2L}, {"SYM", 1L}});

            sendEvent(env, "SYM", 0);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"OCC", 2L}, {"SYM", 0L}, {"SYM", 1L}});

            sendEvent(env, "OCC", 3);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"OCC", 2L}, {"OCC", 3L}, {"SYM", 0L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "vol"};
            String stmtText = "@name('s0') select symbol, volume * 10 as vol from SupportMarketDataBean#length(5)" +
                " where volume < 0";
            env.compileDeploy(stmtText).addListener("s0");

            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", 100);
            assertFalse(env.statement("s0").iterator().hasNext());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, null);

            sendEvent(env, "SYM", -1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -10L}});

            sendEvent(env, "SYM", -6);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -10L}, {"SYM", -60L}});

            env.milestone(0);

            sendEvent(env, "SYM", 1);
            sendEvent(env, "SYM", 16);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -10L}, {"SYM", -60L}});

            sendEvent(env, "SYM", -9);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -10L}, {"SYM", -60L}, {"SYM", -90L}});

            env.milestone(1);

            sendEvent(env, "SYM", 2);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -60L}, {"SYM", -90L}});

            sendEvent(env, "SYM", 3);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -90L}});

            env.milestone(2);

            sendEvent(env, "SYM", 4);
            sendEvent(env, "SYM", 5);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -90L}});

            env.milestone(3);

            sendEvent(env, "SYM", 6);
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowPerGroupOrdered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "sumVol"};
            String stmtText = "@name('s0') select symbol, sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(5) " +
                "group by symbol " +
                "order by symbol";
            env.compileDeploy(stmtText).addListener("s0");

            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            sendEvent(env, "SYM", 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 100L}});

            env.milestone(1);

            sendEvent(env, "OCC", 5);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"OCC", 5L}, {"SYM", 100L}});

            sendEvent(env, "SYM", 10);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"OCC", 5L}, {"SYM", 110L}});

            sendEvent(env, "OCC", 6);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"OCC", 11L}, {"SYM", 110L}});

            env.milestone(2);

            sendEvent(env, "ATB", 8);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"ATB", 8L}, {"OCC", 11L}, {"SYM", 110L}});

            env.milestone(3);

            sendEvent(env, "ATB", 7);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"ATB", 15L}, {"OCC", 11L}, {"SYM", 10L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowPerGroup implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "sumVol"};
            String stmtText = "@name('s0') select symbol, sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(5) " +
                "group by symbol";
            env.compileDeploy(stmtText).addListener("s0");

            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 100L}});

            sendEvent(env, "SYM", 10);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 110L}});

            env.milestone(0);

            sendEvent(env, "TAC", 1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 110L}, {"TAC", 1L}});

            sendEvent(env, "SYM", 11);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 121L}, {"TAC", 1L}});

            env.milestone(1);

            sendEvent(env, "TAC", 2);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 121L}, {"TAC", 3L}});

            sendEvent(env, "OCC", 55);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 21L}, {"TAC", 3L}, {"OCC", 55L}});

            env.milestone(2);

            sendEvent(env, "OCC", 4);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"TAC", 3L}, {"SYM", 11L}, {"OCC", 59L}});

            sendEvent(env, "OCC", 3);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 11L}, {"TAC", 2L}, {"OCC", 62L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowPerGroupHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "sumVol"};
            String stmtText = "@name('s0') select symbol, sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(5) " +
                "group by symbol having sum(volume) > 10";

            env.compileDeploy(stmtText).addListener("s0");
            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 100L}});

            sendEvent(env, "SYM", 5);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 105L}});

            env.milestone(0);

            sendEvent(env, "TAC", 1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 105L}});

            sendEvent(env, "SYM", 3);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 108L}});

            env.milestone(1);

            sendEvent(env, "TAC", 12);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 108L}, {"TAC", 13L}});

            sendEvent(env, "OCC", 55);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"TAC", 13L}, {"OCC", 55L}});

            sendEvent(env, "OCC", 4);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"TAC", 13L}, {"OCC", 59L}});

            env.milestone(2);

            sendEvent(env, "OCC", 3);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"TAC", 12L}, {"OCC", 62L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowPerGroupComplex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "msg"};
            String stmtText = "@name('s0') insert into Cutoff " +
                "select symbol, (String.valueOf(count(*)) || 'x1000.0') as msg " +
                "from SupportMarketDataBean#groupwin(symbol)#length(1) " +
                "where price - volume >= 1000.0 group by symbol having count(*) = 1";
            env.compileDeploy(stmtText).addListener("s0");
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportMarketDataBean("SYM", -1, -1L, null));
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(1);

            env.sendEventBean(new SupportMarketDataBean("SYM", 100000d, 0L, null));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", "1x1000.0"}});

            env.sendEventBean(new SupportMarketDataBean("SYM", 1d, 1L, null));
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAggregateGroupedOrdered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "price", "sumVol"};
            String stmtText = "@name('s0') select symbol, price, sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(5) " +
                "group by symbol " +
                "order by symbol";
            env.compileDeploy(stmtText).addListener("s0");
            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", -1, 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -1d, 100L}});

            env.milestone(0);

            sendEvent(env, "TAC", -2, 12);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 12L}});

            sendEvent(env, "TAC", -3, 13);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

            env.milestone(1);

            sendEvent(env, "SYM", -4, 1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"SYM", -4d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

            env.milestone(2);

            sendEvent(env, "OCC", -5, 99);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"OCC", -5d, 99L}, {"SYM", -1d, 101L}, {"SYM", -4d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

            sendEvent(env, "TAC", -6, 2);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"OCC", -5d, 99L}, {"SYM", -4d, 1L}, {"TAC", -2d, 27L}, {"TAC", -3d, 27L}, {"TAC", -6d, 27L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAggregateGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "price", "sumVol"};
            String stmtText = "@name('s0') select symbol, price, sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(5) " +
                "group by symbol";

            env.compileDeploy(stmtText).addListener("s0");
            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", -1, 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -1d, 100L}});

            sendEvent(env, "TAC", -2, 12);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 12L}});

            env.milestone(0);

            sendEvent(env, "TAC", -3, 13);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

            env.milestone(1);

            sendEvent(env, "SYM", -4, 1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}, {"SYM", -4d, 101L}});

            sendEvent(env, "OCC", -5, 99);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}, {"SYM", -4d, 101L}, {"OCC", -5d, 99L}});

            sendEvent(env, "TAC", -6, 2);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"TAC", -2d, 27L}, {"TAC", -3d, 27L}, {"SYM", -4d, 1L}, {"OCC", -5d, 99L}, {"TAC", -6d, 27L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAggregateGroupedHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "price", "sumVol"};
            String stmtText = "@name('s0') select symbol, price, sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(5) " +
                "group by symbol having sum(volume) > 20";

            env.compileDeploy(stmtText).addListener("s0");
            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", -1, 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", -1d, 100L}});

            sendEvent(env, "TAC", -2, 12);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}});

            env.milestone(0);

            sendEvent(env, "TAC", -3, 13);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 100L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}});

            sendEvent(env, "SYM", -4, 1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}, {"SYM", -4d, 101L}});

            env.milestone(1);

            sendEvent(env, "OCC", -5, 99);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"SYM", -1d, 101L}, {"TAC", -2d, 25L}, {"TAC", -3d, 25L}, {"SYM", -4d, 101L}, {"OCC", -5d, 99L}});

            env.milestone(2);

            sendEvent(env, "TAC", -6, 2);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"TAC", -2d, 27L}, {"TAC", -3d, 27L}, {"OCC", -5d, 99L}, {"TAC", -6d, 27L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowPerEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "sumVol"};
            String stmtText = "@name('s0') select symbol, sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(3) ";

            env.compileDeploy(stmtText).addListener("s0");
            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 100L}});

            sendEvent(env, "TAC", 1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 101L}, {"TAC", 101L}});

            env.milestone(0);

            sendEvent(env, "MOV", 3);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 104L}, {"TAC", 104L}, {"MOV", 104L}});

            sendEvent(env, "SYM", 10);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"TAC", 14L}, {"MOV", 14L}, {"SYM", 14L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowPerEventOrdered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "sumVol"};
            String stmtText = "@name('s0') select irstream symbol, sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(3) " +
                " order by symbol asc";
            env.compileDeploy(stmtText).addListener("s0");
            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 100L}});

            sendEvent(env, "TAC", 1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 101L}, {"TAC", 101L}});

            sendEvent(env, "MOV", 3);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"MOV", 104L}, {"SYM", 104L}, {"TAC", 104L}});

            env.milestone(0);

            sendEvent(env, "SYM", 10);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"MOV", 14L}, {"SYM", 14L}, {"TAC", 14L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowPerEventHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"symbol", "sumVol"};
            String stmtText = "@name('s0') select symbol, sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(3) having sum(volume) > 100";

            env.compileDeploy(stmtText).addListener("s0");

            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, "SYM", 100);
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            sendEvent(env, "TAC", 1);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 101L}, {"TAC", 101L}});

            env.milestone(1);

            sendEvent(env, "MOV", 3);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"SYM", 104L}, {"TAC", 104L}, {"MOV", 104L}});

            sendEvent(env, "SYM", 10);
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"sumVol"};
            String stmtText = "@name('s0') select sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(3) ";

            env.compileDeploy(stmtText).addListener("s0");
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{null}});

            env.milestone(0);

            sendEvent(env, 100);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{100L}});

            env.milestone(1);

            sendEvent(env, 50);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{150L}});

            sendEvent(env, 25);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{175L}});

            env.milestone(2);

            sendEvent(env, 10);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{85L}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowForAllHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"sumVol"};
            String stmtText = "@name('s0') select sum(volume) as sumVol " +
                "from SupportMarketDataBean#length(3) having sum(volume) > 100";

            env.compileDeploy(stmtText).addListener("s0");
            assertFalse(env.statement("s0").iterator().hasNext());

            sendEvent(env, 100);
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            sendEvent(env, 50);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{150L}});

            sendEvent(env, 25);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{175L}});

            env.milestone(1);

            sendEvent(env, 10);
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price, long volume) {
        env.sendEventBean(new SupportMarketDataBean(symbol, price, volume, null));
    }

    private static SupportMarketDataBean sendEvent(RegressionEnvironment env, String symbol, long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, volume, null);
        env.sendEventBean(theEvent);
        return theEvent;
    }

    private static void sendEvent(RegressionEnvironment env, long volume) {
        env.sendEventBean(new SupportMarketDataBean("SYM", 0, volume, null));
    }
}
