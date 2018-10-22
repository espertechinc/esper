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
package com.espertech.esper.regressionlib.suite.resultset.aggregate;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ResultSetAggregateMedianAndDeviation {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateStmt());
        execs.add(new ResultSetAggregateStmtJoinOM());
        execs.add(new ResultSetAggregateStmtJoin());
        execs.add(new ResultSetAggregateStmt());
        return execs;
    }

    private static class ResultSetAggregateStmt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            String epl = "@name('s0') select irstream symbol," +
                "median(all price) as myMedian," +
                "median(distinct price) as myDistMedian," +
                "stddev(all price) as myStdev," +
                "avedev(all price) as myAvedev " +
                "from SupportMarketDataBean#length(5) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionStmt(env, milestone);

            // Test NaN sensitivity
            env.undeployAll();

            epl = "@name('s0') select stddev(price) as val from SupportMarketDataBean#length(3)";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "A", Double.NaN);
            sendEvent(env, "B", Double.NaN);
            sendEvent(env, "C", Double.NaN);

            env.milestoneInc(milestone);

            sendEvent(env, "D", 1d);
            sendEvent(env, "E", 2d);
            env.listener("s0").reset();

            env.milestoneInc(milestone);

            sendEvent(env, "F", 3d);
            Double result = (Double) env.listener("s0").assertOneGetNewAndReset().get("val");
            assertTrue(result.isNaN());

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateStmtJoinOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("symbol")
                .add(Expressions.median("price"), "myMedian")
                .add(Expressions.medianDistinct("price"), "myDistMedian")
                .add(Expressions.stddev("price"), "myStdev")
                .add(Expressions.avedev("price"), "myAvedev")
                .streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
            );
            FromClause fromClause = FromClause.create(
                FilterStream.create(SupportBeanString.class.getSimpleName(), "one").addView(View.create("length", Expressions.constant(100))),
                FilterStream.create(SupportMarketDataBean.class.getSimpleName(), "two").addView(View.create("length", Expressions.constant(5))));
            model.setFromClause(fromClause);
            model.setWhereClause(Expressions.and().add(
                Expressions.or()
                    .add(Expressions.eq("symbol", "DELL"))
                    .add(Expressions.eq("symbol", "IBM"))
                    .add(Expressions.eq("symbol", "GE"))
            )
                .add(Expressions.eqProperty("one.theString", "two.symbol")));
            model.setGroupByClause(GroupByClause.create("symbol"));
            model = SerializableObjectCopier.copyMayFail(model);

            String epl = "select irstream symbol, " +
                "median(price) as myMedian, " +
                "median(distinct price) as myDistMedian, " +
                "stddev(price) as myStdev, " +
                "avedev(price) as myAvedev " +
                "from SupportBeanString#length(100) as one, " +
                "SupportMarketDataBean#length(5) as two " +
                "where (symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\") " +
                "and one.theString=two.symbol " +
                "group by symbol";
            assertEquals(epl, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
            env.sendEventBean(new SupportBeanString(SYMBOL_IBM));
            env.sendEventBean(new SupportBeanString("AAA"));

            tryAssertionStmt(env, new AtomicInteger());

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateStmtJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol," +
                "median(price) as myMedian," +
                "median(distinct price) as myDistMedian," +
                "stddev(price) as myStdev," +
                "avedev(price) as myAvedev " +
                "from SupportBeanString#length(100) as one, " +
                "SupportMarketDataBean#length(5) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "       and one.theString = two.symbol " +
                "group by symbol";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
            env.sendEventBean(new SupportBeanString(SYMBOL_IBM));
            env.sendEventBean(new SupportBeanString("AAA"));

            tryAssertionStmt(env, new AtomicInteger());

            env.undeployAll();
        }
    }

    private static void tryAssertionStmt(RegressionEnvironment env, AtomicInteger milestone) {
        // assert select result type
        assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("myMedian"));
        assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("myDistMedian"));
        assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("myStdev"));
        assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("myAvedev"));

        sendEvent(env, SYMBOL_DELL, 10);
        assertEvents(env, SYMBOL_DELL,
            null, null, null, null,
            10d, 10d, null, 0d);

        env.milestoneInc(milestone);

        sendEvent(env, SYMBOL_DELL, 20);
        assertEvents(env, SYMBOL_DELL,
            10d, 10d, null, 0d,
            15d, 15d, 7.071067812d, 5d);

        sendEvent(env, SYMBOL_DELL, 20);
        assertEvents(env, SYMBOL_DELL,
            15d, 15d, 7.071067812d, 5d,
            20d, 15d, 5.773502692, 4.444444444444444);

        env.milestoneInc(milestone);

        sendEvent(env, SYMBOL_DELL, 90);
        assertEvents(env, SYMBOL_DELL,
            20d, 15d, 5.773502692, 4.444444444444444,
            20d, 20d, 36.96845502d, 27.5d);

        sendEvent(env, SYMBOL_DELL, 5);
        assertEvents(env, SYMBOL_DELL,
            20d, 20d, 36.96845502d, 27.5d,
            20d, 15d, 34.71310992d, 24.4d);

        sendEvent(env, SYMBOL_DELL, 90);
        assertEvents(env, SYMBOL_DELL,
            20d, 15d, 34.71310992d, 24.4d,
            20d, 20d, 41.53311931d, 36d);

        env.milestoneInc(milestone);

        sendEvent(env, SYMBOL_DELL, 30);
        assertEvents(env, SYMBOL_DELL,
            20d, 20d, 41.53311931d, 36d,
            30d, 25d, 40.24922359d, 34.4d);
    }

    private static void assertEvents(RegressionEnvironment env, String symbol,
                                     Double oldMedian, Double oldDistMedian, Double oldStdev, Double oldAvedev,
                                     Double newMedian, Double newDistMedian, Double newStdev, Double newAvedev
    ) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbol, oldData[0].get("symbol"));
        assertEquals("oldData.myMedian wrong", oldMedian, oldData[0].get("myMedian"));
        assertEquals("oldData.myDistMedian wrong", oldDistMedian, oldData[0].get("myDistMedian"));
        assertEquals("oldData.myAvedev wrong", oldAvedev, oldData[0].get("myAvedev"));

        Double oldStdevResult = (Double) oldData[0].get("myStdev");
        if (oldStdevResult == null) {
            assertNull(oldStdev);
        } else {
            assertEquals("oldData.myStdev wrong", Math.round(oldStdev * 1000), Math.round(oldStdevResult * 1000));
        }

        assertEquals(symbol, newData[0].get("symbol"));
        assertEquals("newData.myMedian wrong", newMedian, newData[0].get("myMedian"));
        assertEquals("newData.myDistMedian wrong", newDistMedian, newData[0].get("myDistMedian"));
        assertEquals("newData.myAvedev wrong", newAvedev, newData[0].get("myAvedev"));

        Double newStdevResult = (Double) newData[0].get("myStdev");
        if (newStdevResult == null) {
            assertNull(newStdev);
        } else {
            assertEquals("newData.myStdev wrong", Math.round(newStdev * 1000), Math.round(newStdevResult * 1000));
        }

        env.listener("s0").reset();
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }
}
