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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanString;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportEventWithManyArray;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class ResultSetAggregateCountSum {

    private static final String SYMBOL_DELL = "DELL";
    private static final String SYMBOL_IBM = "IBM";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateCountSimple());
        execs.add(new ResultSetAggregateCountPlusStar());
        execs.add(new ResultSetAggregateCountHaving());
        execs.add(new ResultSetAggregateSumHaving());
        execs.add(new ResultSetAggregateCountOneViewOM());
        execs.add(new ResultSetAggregateGroupByCountNestedAggregationAvg());
        execs.add(new ResultSetAggregateCountOneViewCompile());
        execs.add(new ResultSetAggregateCountOneView());
        execs.add(new ResultSetAggregateCountJoin());
        execs.add(new ResultSetAggregateCountDistinctGrouped());
        execs.add(new ResultSetAggregateSumNamedWindowRemoveGroup());
        execs.add(new ResultSetAggregateCountDistinctMultikeyWArray());
        return execs;
    }

    private static class ResultSetAggregateCountDistinctMultikeyWArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select count(distinct intOne) as c0, count(distinct {intOne, intTwo}) as c1 from SupportEventWithManyArray#length(3)";
            env.compileDeploy(epl).addListener("s0");

            sendManyArrayAssert(env, new int[] {1, 2}, new int[] {1}, 1, 1);
            sendManyArrayAssert(env, new int[] {1, 2}, new int[] {1}, 1, 1);
            sendManyArrayAssert(env, new int[] {1, 3}, new int[] {1}, 2, 2);

            env.milestone(0);

            sendManyArrayAssert(env, new int[] {1, 4}, new int[] {1}, 3, 3);
            sendManyArrayAssert(env, new int[] {1, 3}, new int[] {2}, 2, 3);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateCountPlusStar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Test for ESPER-118
            String statementText = "@name('s0') select *, count(*) as cnt from SupportMarketDataBean";
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "S0", 1L);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            assertEquals(1, env.listener("s0").getLastNewData().length);
            assertEquals(1L, env.listener("s0").getLastNewData()[0].get("cnt"));
            assertEquals("S0", env.listener("s0").getLastNewData()[0].get("symbol"));

            sendEvent(env, "S1", 1L);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            assertEquals(1, env.listener("s0").getLastNewData().length);
            assertEquals(2L, env.listener("s0").getLastNewData()[0].get("cnt"));
            assertEquals("S1", env.listener("s0").getLastNewData()[0].get("symbol"));

            sendEvent(env, "S2", 1L);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            assertEquals(1, env.listener("s0").getLastNewData().length);
            assertEquals(3L, env.listener("s0").getLastNewData()[0].get("cnt"));
            assertEquals("S2", env.listener("s0").getLastNewData()[0].get("symbol"));

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateCountSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select count(*) as cnt from SupportMarketDataBean#time(1)";
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env, "DELL", 1L);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            assertEquals(1, env.listener("s0").getLastNewData().length);
            assertEquals(1L, env.listener("s0").getLastNewData()[0].get("cnt"));

            sendEvent(env, "DELL", 1L);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            assertEquals(1, env.listener("s0").getLastNewData().length);
            assertEquals(2L, env.listener("s0").getLastNewData()[0].get("cnt"));

            sendEvent(env, "DELL", 1L);
            assertTrue(env.listener("s0").getAndClearIsInvoked());
            assertEquals(1, env.listener("s0").getLastNewData().length);
            assertEquals(3L, env.listener("s0").getLastNewData()[0].get("cnt"));

            // test invalid distinct
            SupportMessageAssertUtil.tryInvalidCompile(env, "select count(distinct *) from SupportMarketDataBean",
                "Failed to validate select-clause expression 'count(distinct *)': Invalid use of the 'distinct' keyword with count and wildcard");

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateCountHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select irstream sum(intPrimitive) as mysum from SupportBean having sum(intPrimitive) = 2";
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env);
            assertFalse(env.listener("s0").getAndClearIsInvoked());
            sendEvent(env);
            assertEquals(2, env.listener("s0").assertOneGetNewAndReset().get("mysum"));
            sendEvent(env);
            assertEquals(2, env.listener("s0").assertOneGetOldAndReset().get("mysum"));

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateSumHaving implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String statementText = "@name('s0') select irstream count(*) as mysum from SupportBean having count(*) = 2";
            env.compileDeploy(statementText).addListener("s0");

            sendEvent(env);
            assertFalse(env.listener("s0").getAndClearIsInvoked());
            sendEvent(env);
            assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("mysum"));
            sendEvent(env);
            assertEquals(2L, env.listener("s0").assertOneGetOldAndReset().get("mysum"));

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateCountOneViewOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add("symbol")
                .add(Expressions.countStar(), "countAll")
                .add(Expressions.countDistinct("volume"), "countDistVol")
                .add(Expressions.count("volume"), "countVol"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getSimpleName()).addView("length", Expressions.constant(3))));
            model.setWhereClause(Expressions.or()
                .add(Expressions.eq("symbol", "DELL"))
                .add(Expressions.eq("symbol", "IBM"))
                .add(Expressions.eq("symbol", "GE")));
            model.setGroupByClause(GroupByClause.create("symbol"));
            model = SerializableObjectCopier.copyMayFail(model);

            String epl = "select irstream symbol, " +
                "count(*) as countAll, " +
                "count(distinct volume) as countDistVol, " +
                "count(volume) as countVol" +
                " from SupportMarketDataBean#length(3) " +
                "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                "group by symbol";
            assertEquals(epl, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            tryAssertionCount(env);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateGroupByCountNestedAggregationAvg implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test for ESPER-328
            String epl = "@name('s0') select symbol, count(*) as cnt, avg(count(*)) as val from SupportMarketDataBean#length(3)" +
                "group by symbol order by symbol asc";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, SYMBOL_DELL, 50L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "symbol,cnt,val".split(","), new Object[]{"DELL", 1L, 1d});

            sendEvent(env, SYMBOL_DELL, 51L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "symbol,cnt,val".split(","), new Object[]{"DELL", 2L, 1.5d});

            sendEvent(env, SYMBOL_DELL, 52L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "symbol,cnt,val".split(","), new Object[]{"DELL", 3L, 2d});

            sendEvent(env, "IBM", 52L);
            EventBean[] events = env.listener("s0").getLastNewData();
            EPAssertionUtil.assertProps(events[0], "symbol,cnt,val".split(","), new Object[]{"DELL", 2L, 2d});
            EPAssertionUtil.assertProps(events[1], "symbol,cnt,val".split(","), new Object[]{"IBM", 1L, 1d});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_DELL, 53L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "symbol,cnt,val".split(","), new Object[]{"DELL", 2L, 2.5d});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateCountOneViewCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol, " +
                "count(*) as countAll, " +
                "count(distinct volume) as countDistVol, " +
                "count(volume) as countVol" +
                " from SupportMarketDataBean#length(3) " +
                "where symbol=\"DELL\" or symbol=\"IBM\" or symbol=\"GE\" " +
                "group by symbol";
            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertionCount(env);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateCountOneView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol, " +
                "count(*) as countAll," +
                "count(distinct volume) as countDistVol," +
                "count(all volume) as countVol" +
                " from SupportMarketDataBean#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";

            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertionCount(env);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateCountJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol, " +
                "count(*) as countAll," +
                "count(distinct volume) as countDistVol," +
                "count(volume) as countVol " +
                " from SupportBeanString#length(100) as one, " +
                "SupportMarketDataBean#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "  and one.theString = two.symbol " +
                "group by symbol";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
            env.sendEventBean(new SupportBeanString(SYMBOL_IBM));

            env.milestone(0);

            tryAssertionCount(env);

            env.undeployAll();
        }
    }

    public static class ResultSetAggregateCountDistinctGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol, count(distinct price) as countDistinctPrice " +
                "from SupportMarketDataBean group by symbol";
            env.compileDeploy(epl).addListener("s0");

            env.milestone(0);

            env.sendEventBean(makeMarketDataEvent("ONE", 100));

            env.undeployAll();
        }
    }

    public static class ResultSetAggregateSumNamedWindowRemoveGroup implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,mysum".split(",");
            String epl = "create window MyWindow.win:keepall() as select * from SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "on SupportBean_A a delete from MyWindow w where w.theString = a.id;\n" +
                "on SupportBean_B delete from MyWindow;\n" +
                "@Name('s0') select theString, sum(intPrimitive) as mysum from MyWindow group by theString order by theString";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("A", 100));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 100});

            env.sendEventBean(new SupportBean("B", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 20});

            env.milestone(0);

            env.sendEventBean(new SupportBean("A", 101));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 201});

            env.milestone(1);

            env.sendEventBean(new SupportBean("B", 21));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 41});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 201}, {"B", 41}});

            env.milestone(2);

            env.sendEventBean(new SupportBean_A("A"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", null});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"B", 41}});

            env.milestone(3);

            env.sendEventBean(new SupportBean("A", 102));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 102});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 102}, {"B", 41}});

            env.milestone(4);

            env.sendEventBean(new SupportBean_A("B"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", null});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 102}});

            env.milestone(5);

            env.sendEventBean(new SupportBean("B", 22));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 22});
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"A", 102}, {"B", 22}});

            env.undeployAll();
        }
    }

    private static void tryAssertionCount(RegressionEnvironment env) {
        // assert select result type
        assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("symbol"));
        assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("countAll"));
        assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("countDistVol"));
        assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("countVol"));

        sendEvent(env, SYMBOL_DELL, 50L);
        assertEvents(env, SYMBOL_DELL, 0L, 0L, 0L,
            SYMBOL_DELL, 1L, 1L, 1L
        );

        sendEvent(env, SYMBOL_DELL, null);
        assertEvents(env, SYMBOL_DELL, 1L, 1L, 1L,
            SYMBOL_DELL, 2L, 1L, 1L
        );

        sendEvent(env, SYMBOL_DELL, 25L);
        assertEvents(env, SYMBOL_DELL, 2L, 1L, 1L,
            SYMBOL_DELL, 3L, 2L, 2L
        );

        sendEvent(env, SYMBOL_DELL, 25L);
        assertEvents(env, SYMBOL_DELL, 3L, 2L, 2L,
            SYMBOL_DELL, 3L, 1L, 2L
        );

        sendEvent(env, SYMBOL_DELL, 25L);
        assertEvents(env, SYMBOL_DELL, 3L, 1L, 2L,
            SYMBOL_DELL, 3L, 1L, 3L
        );

        sendEvent(env, SYMBOL_IBM, 1L);
        sendEvent(env, SYMBOL_IBM, null);
        sendEvent(env, SYMBOL_IBM, null);
        sendEvent(env, SYMBOL_IBM, null);
        assertEvents(env, SYMBOL_IBM, 3L, 1L, 1L,
            SYMBOL_IBM, 3L, 0L, 0L
        );
    }

    private static void assertEvents(RegressionEnvironment env, String symbolOld, Long countAllOld, Long countDistVolOld, Long countVolOld,
                                     String symbolNew, Long countAllNew, Long countDistVolNew, Long countVolNew) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        assertEquals(symbolOld, oldData[0].get("symbol"));
        assertEquals(countAllOld, oldData[0].get("countAll"));
        assertEquals(countDistVolOld, oldData[0].get("countDistVol"));
        assertEquals(countVolOld, oldData[0].get("countVol"));

        assertEquals(symbolNew, newData[0].get("symbol"));
        assertEquals(countAllNew, newData[0].get("countAll"));
        assertEquals(countDistVolNew, newData[0].get("countDistVol"));
        assertEquals(countVolNew, newData[0].get("countVol"));

        env.listener("s0").reset();
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, Long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "f1");
        env.sendEventBean(bean);
    }

    private static void sendEvent(RegressionEnvironment env) {
        SupportBean bean = new SupportBean("", 1);
        env.sendEventBean(bean);
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol, double price) {
        return new SupportMarketDataBean(symbol, price, 0L, null);
    }

    private static void sendManyArrayAssert(RegressionEnvironment env, int[] intOne, int[] intTwo, long expectedC0, long expectedC1) {
        env.sendEventBean(new SupportEventWithManyArray("id").withIntOne(intOne).withIntTwo(intTwo));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {expectedC0, expectedC1});
    }
}
