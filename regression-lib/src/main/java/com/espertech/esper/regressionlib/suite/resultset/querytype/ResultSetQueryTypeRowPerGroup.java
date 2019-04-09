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
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.*;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ResultSetQueryTypeRowPerGroup {
    private final static String SYMBOL_DELL = "DELL";
    private final static String SYMBOL_IBM = "IBM";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetQueryTypeRowPerGroupSimple());
        execs.add(new ResultSetQueryTypeRowPerGroupSumOneView());
        execs.add(new ResultSetQueryTypeRowPerGroupSumJoin());
        execs.add(new ResultSetQueryTypeCriteriaByDotMethod());
        execs.add(new ResultSetQueryTypeNamedWindowDelete());
        execs.add(new ResultSetQueryTypeUnboundStreamUnlimitedKey());
        execs.add(new ResultSetQueryTypeAggregateGroupedProps());
        execs.add(new ResultSetQueryTypeAggregateGroupedPropsPerGroup());
        execs.add(new ResultSetQueryTypeAggregationOverGroupedProps());
        execs.add(new ResultSetQueryTypeUniqueInBatch());
        execs.add(new ResultSetQueryTypeSelectAvgExprGroupBy());
        execs.add(new ResultSetQueryTypeUnboundStreamIterate());
        execs.add(new ResultSetQueryTypeReclaimSideBySide());
        execs.add(new ResultSetQueryTypeRowPerGrpMultikeyWArray(false, true));
        execs.add(new ResultSetQueryTypeRowPerGrpMultikeyWArray(false, false));
        execs.add(new ResultSetQueryTypeRowPerGrpMultikeyWArray(true, false));
        execs.add(new ResultSetQueryTypeRowPerGrpMultikeyWReclaim());
        return execs;
    }

    public static class ResultSetQueryTypeRowPerGrpMultikeyWReclaim implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            String epl = "@Hint('reclaim_group_aged=10,reclaim_group_freq=1') @name('s0') select theString, intPrimitive, sum(longPrimitive) as thesum from SupportBean group by theString, intPrimitive";
            env.compileDeploy(epl).addListener("s0");

            sendEventSBAssert(env, "A", 0, 100, 100);
            sendEventSBAssert(env, "A", 0, 101, 201);

            env.milestone(0);
            env.advanceTime(11000);

            sendEventSBAssert(env, "A", 0, 104, 104);

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeRowPerGrpMultikeyWArray implements RegressionExecution {
        private final boolean join;
        private final boolean unbound;

        public ResultSetQueryTypeRowPerGrpMultikeyWArray(boolean join, boolean unbound) {
            this.join = join;
            this.unbound = unbound;
        }

        public void run(RegressionEnvironment env) {
            String epl = join ?
                "@Name('s0') select sum(value) as thesum from SupportEventWithIntArray#keepall, SupportBean#keepall group by array" :
                (unbound ?
                    "@Name('s0') select sum(value) as thesum from SupportEventWithIntArray group by array" :
                    "@Name('s0') select sum(value) as thesum from SupportEventWithIntArray#keepall group by array"
                    );

            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean());

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

    public static class ResultSetQueryTypeRowPerGroupSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String[] fields = "c0,c1,c2,c3".split(",");

            String epl = "@Name('s0') select theString as c0, sum(intPrimitive) as c1," +
                "min(intPrimitive) as c2, max(intPrimitive) as c3 from SupportBean group by theString";
            env.compileDeploy(epl).addListener("s0");

            sendEventSB(env, "E1", 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 10, 10, 10});

            env.milestone(1);

            sendEventSB(env, "E2", 100);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 100, 100, 100});

            env.milestone(2);

            sendEventSB(env, "E1", 11);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 21, 10, 11});

            env.milestone(3);

            sendEventSB(env, "E1", 9);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 30, 9, 11});

            env.milestone(4);

            sendEventSB(env, "E2", 99);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 199, 99, 100});

            env.milestone(5);

            sendEventSB(env, "E2", 97);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 296, 97, 100});

            env.milestone(6);

            sendEventSB(env, "E3", 1000);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 1000, 1000, 1000});

            env.milestone(7);

            sendEventSB(env, "E2", 96);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 392, 96, 100});

            env.milestone(8);

            env.milestone(9);

            sendEventSB(env, "E2", 101);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 493, 96, 101});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeSelectAvgExprGroupBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select istream avg(price) as aprice, symbol from SupportMarketDataBean"
                + "#length(2) group by symbol";
            env.compileDeploy(stmtText).addListener("s0");

            String[] fields = "aprice,symbol".split(",");

            sendEvent(env, "A", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1.0, "A"});

            sendEvent(env, "B", 3);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3.0, "B"});

            env.milestone(0);

            sendEvent(env, "B", 5);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{null, "A"}, {4.0, "B"}});

            env.milestone(1);

            sendEvent(env, "A", 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{10.0, "A"}, {5.0, "B"}});

            sendEvent(env, "A", 20);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{15.0, "A"}, {null, "B"}});

            env.undeployAll();
        }
    }

    public static class ResultSetQueryTypeReclaimSideBySide implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            String eplOne = "@Name('S0') @Hint('disable_reclaim_group') select sum(intPrimitive) as val from SupportBean.win:keepall() group by theString";
            env.compileDeploy(eplOne).addListener("S0");
            String eplTwo = "@Name('S1') @Hint('disable_reclaim_group') select window(intPrimitive) as val from SupportBean.win:keepall() group by theString";
            env.compileDeploy(eplTwo).addListener("S1");
            String eplThree = "@Name('S2') @Hint('disable_reclaim_group') select sum(intPrimitive) as val1, window(intPrimitive) as val2 from SupportBean.win:keepall() group by theString";
            env.compileDeploy(eplThree).addListener("S2");
            String eplFour = "@Name('S3') @Hint('reclaim_group_aged=10,reclaim_group_freq=5') select sum(intPrimitive) as val1, window(intPrimitive) as val2 from SupportBean.win:keepall() group by theString";
            env.compileDeploy(eplFour).addListener("S3");

            String[] fieldsOne = "val".split(",");
            String[] fieldsTwo = "val".split(",");
            String[] fieldsThree = "val1,val2".split(",");
            String[] fieldsFour = "val1,val2".split(",");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("S0").assertOneGetNewAndReset(), fieldsOne, new Object[]{1});
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fieldsTwo, new Object[]{new int[]{1}});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fieldsThree, new Object[]{1, new int[]{1}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fieldsFour, new Object[]{1, new int[]{1}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 2));
            EPAssertionUtil.assertProps(env.listener("S0").assertOneGetNewAndReset(), fieldsOne, new Object[]{3});
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fieldsTwo, new Object[]{new int[]{1, 2}});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fieldsThree, new Object[]{3, new int[]{1, 2}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fieldsFour, new Object[]{3, new int[]{1, 2}});

            env.sendEventBean(new SupportBean("E2", 4));
            EPAssertionUtil.assertProps(env.listener("S0").assertOneGetNewAndReset(), fieldsOne, new Object[]{4});
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fieldsTwo, new Object[]{new int[]{4}});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fieldsThree, new Object[]{4, new int[]{4}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fieldsFour, new Object[]{4, new int[]{4}});

            env.milestone(1);

            env.sendEventBean(new SupportBean("E2", 5));
            EPAssertionUtil.assertProps(env.listener("S0").assertOneGetNewAndReset(), fieldsOne, new Object[]{9});
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fieldsTwo, new Object[]{new int[]{4, 5}});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fieldsThree, new Object[]{9, new int[]{4, 5}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fieldsFour, new Object[]{9, new int[]{4, 5}});

            env.sendEventBean(new SupportBean("E1", 6));
            EPAssertionUtil.assertProps(env.listener("S0").assertOneGetNewAndReset(), fieldsOne, new Object[]{9});
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fieldsTwo, new Object[]{new int[]{1, 2, 6}});
            EPAssertionUtil.assertProps(env.listener("S2").assertOneGetNewAndReset(), fieldsThree, new Object[]{9, new int[]{1, 2, 6}});
            EPAssertionUtil.assertProps(env.listener("S3").assertOneGetNewAndReset(), fieldsFour, new Object[]{9, new int[]{1, 2, 6}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeCriteriaByDotMethod implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select sb.getTheString() as c0, sum(intPrimitive) as c1 " +
                "from SupportBean#length_batch(2) as sb group by sb.getTheString()";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E1", 20));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{"E1", 30});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnboundStreamIterate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            AtomicInteger milestone = new AtomicInteger();

            // with output snapshot
            String epl = "@name('s0') select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString " +
                "output snapshot every 3 events";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 10}});
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 10}, {"E2", 20}});
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E1", 11));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E0", 30));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}, {"E0", 30}});
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();

            // with order-by
            epl = "@name('s0') select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString " +
                "output snapshot every 3 events order by theString asc";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            env.sendEventBean(new SupportBean("E2", 20));
            env.sendEventBean(new SupportBean("E1", 11));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1", 21}, {"E2", 20}});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E0", 30));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E0", 30}, {"E1", 21}, {"E2", 20}});
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E3", 40));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E0", 30}, {"E1", 21}, {"E2", 20}, {"E3", 40}});
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();

            // test un-grouped case
            epl = "@name('s0') select null as c0, sum(intPrimitive) as c1 from SupportBean output snapshot every 3 events";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{null, 10}});
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("E2", 20));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{null, 30}});
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBean("E1", 11));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{null, 41}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{null, 41}});

            env.undeployAll();

            // test reclaim
            env.advanceTime(1000);
            epl = "@name('s0') @Hint('reclaim_group_aged=1,reclaim_group_freq=1') select theString as c0, sum(intPrimitive) as c1 from SupportBean group by theString " +
                "output snapshot every 3 events";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));

            env.milestoneInc(milestone);

            env.advanceTime(1500);
            env.sendEventBean(new SupportBean("E0", 11));

            env.milestoneInc(milestone);

            env.advanceTime(1800);
            env.sendEventBean(new SupportBean("E2", 12));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1", 10}, {"E0", 11}, {"E2", 12}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E1", 10}, {"E0", 11}, {"E2", 12}});

            env.milestoneInc(milestone);

            env.advanceTime(2200);
            env.sendEventBean(new SupportBean("E2", 13));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"E0", 11}, {"E2", 25}});

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeNamedWindowDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            RegressionPath path = new RegressionPath();
            String epl = "create window MyWindow#keepall as select * from SupportBean;\n" +
                "insert into MyWindow select * from SupportBean;\n" +
                "on SupportBean_A a delete from MyWindow w where w.theString = a.id;\n" +
                "on SupportBean_B delete from MyWindow;\n";
            env.compileDeploy(epl, path);

            epl = "@Hint('DISABLE_RECLAIM_GROUP') @name('s0') select theString, sum(intPrimitive) as mysum from MyWindow group by theString order by theString";
            env.compileDeploy(epl, path).addListener("s0");
            String[] fields = "theString,mysum".split(",");

            tryAssertionNamedWindowDelete(env, fields, milestone);

            env.undeployModuleContaining("s0");
            env.sendEventBean(new SupportBean_B("delete"));

            epl = "@name('s0') select theString, sum(intPrimitive) as mysum from MyWindow group by theString order by theString";
            env.compileDeploy(epl, path).addListener("s0");

            tryAssertionNamedWindowDelete(env, fields, milestone);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUnboundStreamUnlimitedKey implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            // ESPER-396 Unbound stream and aggregating/grouping by unlimited key (i.e. timestamp) configurable state drop
            sendTimer(env, 0);

            // After the oldest group is 60 second old, reclaim group older then  30 seconds
            String epl = "@name('s0') @Hint('reclaim_group_aged=30,reclaim_group_freq=5') select longPrimitive, count(*) from SupportBean group by longPrimitive";
            env.compileDeploy(epl).addListener("s0");

            for (int i = 0; i < 1000; i++) {
                sendTimer(env, 1000 + i * 1000); // reduce factor if sending more events
                SupportBean theEvent = new SupportBean();
                theEvent.setLongPrimitive(i * 1000);
                env.sendEventBean(theEvent);

                //if (i % 100000 == 0)
                //{
                //    System.out.println("Sending event number " + i);
                //}
            }

            env.listener("s0").reset();

            for (int i = 0; i < 964; i++) {
                SupportBean theEvent = new SupportBean();
                theEvent.setLongPrimitive(i * 1000);
                env.sendEventBean(theEvent);
                Assert.assertEquals("Failed at " + i, 1L, env.listener("s0").assertOneGetNewAndReset().get("count(*)"));
            }

            for (int i = 965; i < 1000; i++) {
                SupportBean theEvent = new SupportBean();
                theEvent.setLongPrimitive(i * 1000);
                env.sendEventBean(theEvent);
                Assert.assertEquals("Failed at " + i, 2L, env.listener("s0").assertOneGetNewAndReset().get("count(*)"));
            }

            env.undeployAll();

            // no frequency provided
            epl = "@name('s0') @Hint('reclaim_group_aged=30') select longPrimitive, count(*) from SupportBean group by longPrimitive";
            env.compileDeploy(epl).addListener("s0");
            env.sendEventBean(new SupportBean());
            env.undeployAll();

            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('define-age') create variable int myAge = 10;\n" +
                "@name('define-freq') create variable int myFreq = 10;\n", path);
            String deploymentIdVariables = env.deploymentId("define-age");

            epl = "@name('s0') @Hint('reclaim_group_aged=myAge,reclaim_group_freq=myFreq') select longPrimitive, count(*) from SupportBean group by longPrimitive";
            env.compileDeploy(epl, path).addListener("s0");

            for (int i = 0; i < 1000; i++) {
                sendTimer(env, 2000000 + 1000 + i * 1000); // reduce factor if sending more events
                SupportBean theEvent = new SupportBean();
                theEvent.setLongPrimitive(i * 1000);
                env.sendEventBean(theEvent);

                if (i == 500) {
                    env.runtime().getVariableService().setVariableValue(deploymentIdVariables, "myAge", 60);
                    env.runtime().getVariableService().setVariableValue(deploymentIdVariables, "myFreq", 90);
                }

                if (i % 100000 == 0) {
                    System.out.println("Sending event number " + i);
                }
            }

            env.listener("s0").reset();

            for (int i = 0; i < 900; i++) {
                SupportBean theEvent = new SupportBean();
                theEvent.setLongPrimitive(i * 1000);
                env.sendEventBean(theEvent);
                Assert.assertEquals("Failed at " + i, 1L, env.listener("s0").assertOneGetNewAndReset().get("count(*)"));
            }

            for (int i = 900; i < 1000; i++) {
                SupportBean theEvent = new SupportBean();
                theEvent.setLongPrimitive(i * 1000);
                env.sendEventBean(theEvent);
                Assert.assertEquals("Failed at " + i, 2L, env.listener("s0").assertOneGetNewAndReset().get("count(*)"));
            }

            env.undeployAll();

            // invalid tests
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "@Hint('reclaim_group_aged=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Failed to parse hint parameter value 'xyz' as a double-typed seconds value or variable name [@Hint('reclaim_group_aged=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "@Hint('reclaim_group_aged=30,reclaim_group_freq=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Failed to parse hint parameter value 'xyz' as a double-typed seconds value or variable name [@Hint('reclaim_group_aged=30,reclaim_group_freq=xyz') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "@Hint('reclaim_group_aged=MyVar') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Variable type of variable 'MyVar' is not numeric [@Hint('reclaim_group_aged=MyVar') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
            SupportMessageAssertUtil.tryInvalidCompile(env, path, "@Hint('reclaim_group_aged=-30,reclaim_group_freq=30') select longPrimitive, count(*) from SupportBean group by longPrimitive",
                "Hint parameter value '-30' is an invalid value, expecting a double-typed seconds value or variable name [@Hint('reclaim_group_aged=-30,reclaim_group_freq=30') select longPrimitive, count(*) from SupportBean group by longPrimitive]");
        }
    }

    private static class ResultSetQueryTypeAggregateGroupedProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test for ESPER-185
            String[] fields = "mycount".split(",");
            String epl = "@name('s0') select irstream count(price) as mycount " +
                "from SupportMarketDataBean#length(5) " +
                "group by price";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, SYMBOL_DELL, 10);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1L}});
            env.listener("s0").reset();

            env.milestone(0);

            sendEvent(env, SYMBOL_DELL, 11);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1L}, {1L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_IBM, 10);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{2L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{2L}, {1L}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAggregateGroupedPropsPerGroup implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test for ESPER-185
            String[] fields = "mycount".split(",");
            String epl = "@name('s0') select irstream count(price) as mycount " +
                "from SupportMarketDataBean#length(5) " +
                "group by symbol, price";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, SYMBOL_DELL, 10);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_DELL, 11);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{1L}, {1L}});
            env.listener("s0").reset();

            env.milestone(0);

            sendEvent(env, SYMBOL_DELL, 10);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{2L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{2L}, {1L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_IBM, 10);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{2L}, {1L}, {1L}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeAggregationOverGroupedProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test for ESPER-185
            String[] fields = "symbol,price,mycount".split(",");
            String epl = "@name('s0') select irstream symbol,price,count(price) as mycount " +
                "from SupportMarketDataBean#length(5) " +
                "group by symbol, price order by symbol asc";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, SYMBOL_DELL, 10);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"DELL", 10.0, 1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"DELL", 10.0, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 10.0, 1L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_DELL, 11);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"DELL", 11.0, 1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"DELL", 11.0, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 10.0, 1L}, {"DELL", 11.0, 1L}});
            env.listener("s0").reset();

            env.milestone(0);

            sendEvent(env, SYMBOL_DELL, 10);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"DELL", 10.0, 2L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"DELL", 10.0, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 10.0, 2L}, {"DELL", 11.0, 1L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_IBM, 5);
            Assert.assertEquals(1, env.listener("s0").getNewDataList().size());
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"IBM", 5.0, 1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"IBM", 5.0, 0L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 10.0, 2L}, {"DELL", 11.0, 1L}, {"IBM", 5.0, 1L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_IBM, 5);
            Assert.assertEquals(1, env.listener("s0").getLastNewData().length);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"IBM", 5.0, 2L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"IBM", 5.0, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 10.0, 2L}, {"DELL", 11.0, 1L}, {"IBM", 5.0, 2L}});
            env.listener("s0").reset();

            env.milestone(1);

            sendEvent(env, SYMBOL_IBM, 5);
            Assert.assertEquals(2, env.listener("s0").getLastNewData().length);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[1], fields, new Object[]{"IBM", 5.0, 3L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[1], fields, new Object[]{"IBM", 5.0, 2L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"DELL", 10.0, 1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"DELL", 10.0, 2L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 11.0, 1L}, {"DELL", 10.0, 1L}, {"IBM", 5.0, 3L}});
            env.listener("s0").reset();

            sendEvent(env, SYMBOL_IBM, 5);
            Assert.assertEquals(2, env.listener("s0").getLastNewData().length);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[1], fields, new Object[]{"IBM", 5.0, 4L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[1], fields, new Object[]{"IBM", 5.0, 3L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"DELL", 11.0, 0L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"DELL", 11.0, 1L});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 10.0, 1L}, {"IBM", 5.0, 4L}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeUniqueInBatch implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(env, 0);
            String epl = "insert into MyStream select symbol, price from SupportMarketDataBean#time_batch(1 sec);\n" +
                "@name('s0') select symbol " +
                "from MyStream#time_batch(1 sec)#unique(symbol) " +
                "group by symbol";
            env.compileDeployAddListenerMileZero(epl, "s0");

            sendEvent(env, "IBM", 100);
            sendEvent(env, "IBM", 101);

            env.milestone(1);

            sendEvent(env, "IBM", 102);
            sendTimer(env, 1000);
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(env, 2000);
            UniformPair<EventBean[]> received = env.listener("s0").getDataListsFlattened();
            Assert.assertEquals("IBM", received.getFirst()[0].get("symbol"));

            env.undeployAll();
        }
    }

    private static void tryAssertionNamedWindowDelete(RegressionEnvironment env, String[] fields, AtomicInteger milestone) {
        env.sendEventBean(new SupportBean("A", 100));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 100});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("B", 20));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 20});

        env.sendEventBean(new SupportBean("A", 101));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 201});

        env.sendEventBean(new SupportBean("B", 21));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 41});
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A", 201}, {"B", 41}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_A("A"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", null});
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"B", 41}});

        env.sendEventBean(new SupportBean("A", 102));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", 102});
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A", 102}, {"B", 41}});

        env.sendEventBean(new SupportBean_A("B"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", null});
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A", 102}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("B", 22));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"B", 22});
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A", 102}, {"B", 22}});
    }

    private static void assertEvents(RegressionEnvironment env, String symbol,
                                     Double oldSum, Double oldAvg,
                                     Double newSum, Double newAvg) {
        EventBean[] oldData = env.listener("s0").getLastOldData();
        EventBean[] newData = env.listener("s0").getLastNewData();

        assertEquals(1, oldData.length);
        assertEquals(1, newData.length);

        Assert.assertEquals(symbol, oldData[0].get("symbol"));
        Assert.assertEquals(oldSum, oldData[0].get("mySum"));
        Assert.assertEquals(oldAvg, oldData[0].get("myAvg"));

        Assert.assertEquals(symbol, newData[0].get("symbol"));
        Assert.assertEquals(newSum, newData[0].get("mySum"));
        Assert.assertEquals("newData myAvg wrong", newAvg, newData[0].get("myAvg"));

        env.listener("s0").reset();
        assertFalse(env.listener("s0").isInvoked());
    }

    private static class ResultSetQueryTypeRowPerGroupSumJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from SupportBeanString#length(100) as one, " +
                "SupportMarketDataBean#length(3) as two " +
                "where (symbol='DELL' or symbol='IBM' or symbol='GE') " +
                "       and one.theString = two.symbol " +
                "group by symbol";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBeanString(SYMBOL_DELL));
            env.sendEventBean(new SupportBeanString(SYMBOL_IBM));
            env.sendEventBean(new SupportBeanString("AAA"));

            tryAssertionSum(env);

            env.undeployAll();
        }
    }

    private static class ResultSetQueryTypeRowPerGroupSumOneView implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select irstream symbol," +
                "sum(price) as mySum," +
                "avg(price) as myAvg " +
                "from SupportMarketDataBean#length(3) " +
                "where symbol='DELL' or symbol='IBM' or symbol='GE' " +
                "group by symbol";
            env.compileDeployAddListenerMileZero(epl, "s0");

            tryAssertionSum(env);

            env.undeployAll();
        }
    }

    private static void tryAssertionSum(RegressionEnvironment env) {
        String[] fields = new String[]{"symbol", "mySum", "myAvg"};
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, null);

        // assert select result type
        assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("symbol"));
        assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("mySum"));
        assertEquals(Double.class, env.statement("s0").getEventType().getPropertyType("myAvg"));

        sendEvent(env, SYMBOL_DELL, 10);
        assertEvents(env, SYMBOL_DELL,
            null, null,
            10d, 10d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 10d, 10d}});

        env.milestone(1);

        sendEvent(env, SYMBOL_DELL, 20);
        assertEvents(env, SYMBOL_DELL,
            10d, 10d,
            30d, 15d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 30d, 15d}});

        env.milestone(2);

        sendEvent(env, SYMBOL_DELL, 100);
        assertEvents(env, SYMBOL_DELL,
            30d, 15d,
            130d, 130d / 3d);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 130d, 130d / 3d}});

        env.milestone(3);

        sendEvent(env, SYMBOL_DELL, 50);
        assertEvents(env, SYMBOL_DELL,
            130d, 130 / 3d,
            170d, 170 / 3d);    // 20 + 100 + 50
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 170d, 170d / 3d}});

        env.milestone(4);

        sendEvent(env, SYMBOL_DELL, 5);
        assertEvents(env, SYMBOL_DELL,
            170d, 170 / 3d,
            155d, 155 / 3d);    // 100 + 50 + 5
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 155d, 155d / 3d}});

        env.milestone(5);

        sendEvent(env, "AAA", 1000);
        assertEvents(env, SYMBOL_DELL,
            155d, 155d / 3,
            55d, 55d / 2);    // 50 + 5
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 55d, 55d / 2d}});

        env.milestone(6);

        sendEvent(env, SYMBOL_IBM, 70);
        assertEvents(env, SYMBOL_DELL,
            55d, 55 / 2d,
            5, 5,
            SYMBOL_IBM,
            null, null,
            70, 70);    // Dell:5
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"DELL", 5d, 5d}, {"IBM", 70d, 70d}});

        env.milestone(7);

        sendEvent(env, "AAA", 2000);
        assertEvents(env, SYMBOL_DELL,
            5d, 5d,
            null, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, new Object[][]{{"IBM", 70d, 70d}});

        env.milestone(8);

        sendEvent(env, "AAA", 3000);
        assertFalse(env.listener("s0").isInvoked());

        sendEvent(env, "AAA", 4000);
        assertEvents(env, SYMBOL_IBM,
            70d, 70d,
            null, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, null);
    }

    private static void sendEvent(RegressionEnvironment env, String symbol, double price) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, price, 0L, null);
        env.sendEventBean(bean);
    }

    private static void assertEvents(RegressionEnvironment env, String symbolOne,
                                     Double oldSumOne, Double oldAvgOne,
                                     double newSumOne, double newAvgOne,
                                     String symbolTwo,
                                     Double oldSumTwo, Double oldAvgTwo,
                                     double newSumTwo, double newAvgTwo) {
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetDataListsFlattened(),
            "mySum,myAvg".split(","),
            new Object[][]{{newSumOne, newAvgOne}, {newSumTwo, newAvgTwo}},
            new Object[][]{{oldSumOne, oldAvgOne}, {oldSumTwo, oldAvgTwo}});
    }

    private static void sendEventSB(RegressionEnvironment env, String theString, int intPrimitive) {
        env.sendEventBean(new SupportBean(theString, intPrimitive));
    }

    private static void sendEventSBAssert(RegressionEnvironment env, String theString, int intPrimitive, int longPrimitive, long expected) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setLongPrimitive(longPrimitive);
        env.sendEventBean(sb);
        assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("thesum"));
    }

    private static void sendTimer(RegressionEnvironment env, long timeInMSec) {
        env.advanceTime(timeInMSec);
    }

    private static void sendAssertIntArray(RegressionEnvironment env, String id, int[] array, int value, int expected) {
        final String[] fields = "thesum".split(",");
        env.sendEventBean(new SupportEventWithIntArray(id, array, value));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[] {expected});
    }
}
