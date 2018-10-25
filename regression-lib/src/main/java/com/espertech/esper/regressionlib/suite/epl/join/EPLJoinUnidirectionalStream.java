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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.common.internal.type.OuterJoinType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.runtime.client.EPStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.*;

public class EPLJoinUnidirectionalStream {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinPatternUnidirectionalOuterJoinNoOn());
        execs.add(new EPLJoin2TableJoinGrouped());
        execs.add(new EPLJoin2TableJoinRowForAll());
        execs.add(new EPLJoin3TableOuterJoinVar1());
        execs.add(new EPLJoin3TableOuterJoinVar2());
        execs.add(new EPLJoinPatternJoin());
        execs.add(new EPLJoinPatternJoinOutputRate());
        execs.add(new EPLJoin3TableJoinVar1());
        execs.add(new EPLJoin3TableJoinVar2A());
        execs.add(new EPLJoin3TableJoinVar2B());
        execs.add(new EPLJoin3TableJoinVar3());
        execs.add(new EPLJoin2TableFullOuterJoin());
        execs.add(new EPLJoin2TableFullOuterJoinCompile());
        execs.add(new EPLJoin2TableFullOuterJoinOM());
        execs.add(new EPLJoin2TableFullOuterJoinBackwards());
        execs.add(new EPLJoin2TableJoin());
        execs.add(new EPLJoin2TableBackwards());
        execs.add(new EPLJoinInvalid());
        return execs;
    }

    private static class EPLJoinPatternUnidirectionalOuterJoinNoOn implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // test 2-stream left outer join and SODA
            //
            AtomicInteger milestone = new AtomicInteger();
            env.advanceTime(1000);

            String stmtTextLO = "@name('s0') select sum(intPrimitive) as c0, count(*) as c1 " +
                "from pattern [every timer:interval(1)] unidirectional " +
                "left outer join " +
                "SupportBean#keepall";
            env.compileDeployAddListenerMile(stmtTextLO, "s0", milestone.getAndIncrement());

            tryAssertionPatternUniOuterJoinNoOn(env, 0);

            env.undeployAll();

            env.eplToModelCompileDeploy(stmtTextLO).addListener("s0").milestone(milestone.getAndIncrement());

            tryAssertionPatternUniOuterJoinNoOn(env, 100000);

            env.undeployAll();

            // test 2-stream inner join
            //
            String[] fieldsIJ = "c0,c1".split(",");
            String stmtTextIJ = "@name('s0') select sum(intPrimitive) as c0, count(*) as c1 " +
                "from SupportBean_S0 unidirectional " +
                "inner join " +
                "SupportBean#keepall";
            env.compileDeployAddListenerMile(stmtTextIJ, "s0", milestone.getAndIncrement());

            env.sendEventBean(new SupportBean_S0(1, "S0_1"));
            env.sendEventBean(new SupportBean("E1", 100));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(2, "S0_2"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsIJ, new Object[]{100, 1L});

            env.sendEventBean(new SupportBean("E2", 200));

            env.sendEventBean(new SupportBean_S0(3, "S0_3"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fieldsIJ, new Object[]{300, 2L});
            env.undeployAll();

            // test 2-stream inner join with group-by
            tryAssertion2StreamInnerWGroupBy(env);

            // test 3-stream inner join
            //
            String[] fields3IJ = "c0,c1".split(",");
            String stmtText3IJ = "@name('s0') select sum(intPrimitive) as c0, count(*) as c1 " +
                "from " +
                "SupportBean_S0#keepall " +
                "inner join " +
                "SupportBean_S1#keepall " +
                "inner join " +
                "SupportBean#keepall";
            env.compileDeployAddListenerMile(stmtText3IJ, "s0", milestone.getAndIncrement());

            env.sendEventBean(new SupportBean_S0(1, "S0_1"));
            env.sendEventBean(new SupportBean("E1", 50));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(10, "S1_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields3IJ, new Object[]{50, 1L});

            env.sendEventBean(new SupportBean("E2", 51));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields3IJ, new Object[]{101, 2L});

            env.undeployAll();

            // test 3-stream full outer join
            //
            String[] fields3FOJ = "p00,p10,theString".split(",");
            String stmtText3FOJ = "@name('s0') select p00, p10, theString " +
                "from " +
                "SupportBean_S0#keepall " +
                "full outer join " +
                "SupportBean_S1#keepall " +
                "full outer join " +
                "SupportBean#keepall";
            env.compileDeployAddListenerMile(stmtText3FOJ, "s0", milestone.getAndIncrement());

            env.sendEventBean(new SupportBean_S0(1, "S0_1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields3FOJ, new Object[]{"S0_1", null, null});

            env.sendEventBean(new SupportBean("E10", 0));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields3FOJ, new Object[]{null, null, "E10"});

            env.sendEventBean(new SupportBean_S0(2, "S0_2"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields3FOJ, new Object[][]{{"S0_2", null, null}});

            env.sendEventBean(new SupportBean_S1(1, "S1_0"));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields3FOJ, new Object[][]{{"S0_1", "S1_0", "E10"}, {"S0_2", "S1_0", "E10"}});

            env.sendEventBean(new SupportBean_S0(2, "S0_3"));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields3FOJ, new Object[][]{{"S0_3", "S1_0", "E10"}});

            env.sendEventBean(new SupportBean("E11", 0));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields3FOJ, new Object[][]{{"S0_1", "S1_0", "E11"}, {"S0_2", "S1_0", "E11"}, {"S0_3", "S1_0", "E11"}});
            assertEquals(6, EPAssertionUtil.iteratorCount(env.iterator("s0")));

            env.undeployAll();

            // test 3-stream full outer join with where-clause
            //
            String[] fields3FOJW = "p00,p10,theString".split(",");
            String stmtText3FOJW = "@name('s0') select p00, p10, theString " +
                "from " +
                "SupportBean_S0#keepall as s0 " +
                "full outer join " +
                "SupportBean_S1#keepall as s1 " +
                "full outer join " +
                "SupportBean#keepall as sb " +
                "where s0.p00 = s1.p10";
            env.compileDeployAddListenerMile(stmtText3FOJW, "s0", milestone.getAndIncrement());

            env.sendEventBean(new SupportBean_S0(1, "X1"));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S1(1, "Y1"));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean_S0(1, "Y1"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields3FOJW, new Object[]{"Y1", "Y1", null});

            env.undeployAll();
        }
    }

    private static class EPLJoin2TableJoinGrouped implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select irstream symbol, count(*) as cnt " +
                "from SupportMarketDataBean unidirectional, SupportBean#keepall " +
                "where theString = symbol group by theString, symbol";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            // send event, expect result
            sendEventMD(env, "E1", 1L);
            String[] fields = "symbol,cnt".split(",");
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "E1", 10);
            assertFalse(env.listener("s0").isInvoked());

            sendEventMD(env, "E1", 2L);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"E1", 1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"E1", 0L});
            env.listener("s0").reset();

            sendEvent(env, "E1", 20);
            assertFalse(env.listener("s0").isInvoked());

            sendEventMD(env, "E1", 3L);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"E1", 2L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"E1", 0L});
            env.listener("s0").reset();

            try {
                env.statement("s0").iterator();
                fail();
            } catch (UnsupportedOperationException ex) {
                assertEquals("Iteration over a unidirectional join is not supported", ex.getMessage());
            }
            // assure lock given up by sending more events

            sendEvent(env, "E2", 40);
            sendEventMD(env, "E2", 4L);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{"E2", 1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{"E2", 0L});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class EPLJoin2TableJoinRowForAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select irstream count(*) as cnt " +
                "from SupportMarketDataBean unidirectional, SupportBean#keepall " +
                "where theString = symbol";
            env.compileDeployAddListenerMileZero(stmtText, "s0");
            tryUnsupportedIterator(env.statement("s0"));

            // send event, expect result
            sendEventMD(env, "E1", 1L);
            String[] fields = "cnt".split(",");
            assertFalse(env.listener("s0").isInvoked());

            sendEvent(env, "E1", 10);
            assertFalse(env.listener("s0").isInvoked());

            sendEventMD(env, "E1", 2L);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{0L});
            env.listener("s0").reset();

            sendEvent(env, "E1", 20);
            assertFalse(env.listener("s0").isInvoked());

            sendEventMD(env, "E1", 3L);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{2L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{0L});
            env.listener("s0").reset();

            sendEvent(env, "E2", 40);
            sendEventMD(env, "E2", 4L);
            EPAssertionUtil.assertProps(env.listener("s0").getLastNewData()[0], fields, new Object[]{1L});
            EPAssertionUtil.assertProps(env.listener("s0").getLastOldData()[0], fields, new Object[]{0L});

            env.undeployAll();
        }
    }

    private static class EPLJoin3TableOuterJoinVar1 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s0.id, s1.id, s2.id " +
                "from SupportBean_S0 as s0 unidirectional " +
                " full outer join SupportBean_S1#keepall as s1" +
                " on p00 = p10 " +
                " full outer join SupportBean_S2#keepall as s2" +
                " on p10 = p20";
            env.compileDeployAddListenerMileZero(stmtText, "s0");
            try3TableOuterJoin(env);
            env.undeployAll();
        }
    }

    private static class EPLJoin3TableOuterJoinVar2 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s0.id, s1.id, s2.id from SupportBean_S0 as s0 unidirectional " +
                " left outer join SupportBean_S1#keepall as s1 " +
                " on p00 = p10 " +
                " left outer join SupportBean_S2#keepall as s2 " +
                " on p10 = p20";
            env.compileDeployAddListenerMileZero(stmtText, "s0");
            try3TableOuterJoin(env);
            env.undeployAll();
        }
    }

    private static class EPLJoinPatternJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(1000);

            // no iterator allowed
            String stmtText = "@name('s0') select count(*) as num " +
                "from pattern [every timer:at(*/1,*,*,*,*)] unidirectional,\n" +
                "SupportBean(intPrimitive=1)#unique(theString) a,\n" +
                "SupportBean(intPrimitive=2)#unique(theString) b\n" +
                "where a.theString = b.theString";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            sendEvent(env, "A", 1);
            sendEvent(env, "A", 2);
            sendEvent(env, "B", 1);
            sendEvent(env, "B", 2);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(70000);
            assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("num"));

            env.advanceTime(140000);
            assertEquals(2L, env.listener("s0").assertOneGetNewAndReset().get("num"));

            env.undeployAll();
        }
    }

    private static class EPLJoinPatternJoinOutputRate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(1000);

            // no iterator allowed
            String stmtText = "@name('s0') select count(*) as num " +
                "from pattern [every timer:at(*/1,*,*,*,*)] unidirectional,\n" +
                "SupportBean(intPrimitive=1)#unique(theString) a,\n" +
                "SupportBean(intPrimitive=2)#unique(theString) b\n" +
                "where a.theString = b.theString output every 2 minutes";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            sendEvent(env, "A", 1);
            sendEvent(env, "A", 2);
            sendEvent(env, "B", 1);
            sendEvent(env, "B", 2);
            assertFalse(env.listener("s0").isInvoked());

            env.advanceTime(70000);
            env.advanceTime(140000);

            env.advanceTime(210000);
            assertEquals(2L, env.listener("s0").getLastNewData()[0].get("num"));
            assertEquals(2L, env.listener("s0").getLastNewData()[1].get("num"));

            env.undeployAll();
        }
    }

    private static class EPLJoin3TableJoinVar1 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s0.id, s1.id, s2.id " +
                "from " +
                "SupportBean_S0 as s0 unidirectional, " +
                "SupportBean_S1#keepall as s1, " +
                "SupportBean_S2#keepall as s2 " +
                "where p00 = p10 and p10 = p20";
            env.compileDeployAddListenerMileZero(stmtText, "s0");
            try3TableJoin(env);
            env.undeployAll();
        }
    }

    private static class EPLJoin3TableJoinVar2A implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s0.id, s1.id, s2.id " +
                "from " +
                "SupportBean_S1#keepall as s1, " +
                "SupportBean_S0 as s0 unidirectional, " +
                "SupportBean_S2#keepall as s2 " +
                "where p00 = p10 and p10 = p20";
            env.compileDeployAddListenerMileZero(stmtText, "s0");
            try3TableJoin(env);
            env.undeployAll();
        }
    }

    private static class EPLJoin3TableJoinVar2B implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s0.id, s1.id, s2.id " +
                "from " +
                "SupportBean_S2#keepall as s2, " +
                "SupportBean_S0 as s0 unidirectional, " +
                "SupportBean_S1#keepall as s1 " +
                "where p00 = p10 and p10 = p20";
            env.compileDeployAddListenerMileZero(stmtText, "s0");
            try3TableJoin(env);
            env.undeployAll();
        }
    }

    private static class EPLJoin3TableJoinVar3 implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select s0.id, s1.id, s2.id " +
                "from " +
                "SupportBean_S1#keepall as s1, " +
                "SupportBean_S2#keepall as s2, " +
                "SupportBean_S0 as s0 unidirectional " +
                "where p00 = p10 and p10 = p20";
            env.compileDeployAddListenerMileZero(stmtText, "s0");
            try3TableJoin(env);
            env.undeployAll();
        }
    }

    private static class EPLJoin2TableFullOuterJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, theString, intPrimitive " +
                "from SupportMarketDataBean unidirectional " +
                "full outer join SupportBean#keepall on theString = symbol";
            env.compileDeployAddListenerMileZero(stmtText, "s0");
            tryFullOuterPassive2Stream(env);
            env.undeployAll();
        }
    }

    private static class EPLJoin2TableFullOuterJoinCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, theString, intPrimitive " +
                "from SupportMarketDataBean unidirectional " +
                "full outer join SupportBean#keepall on theString = symbol";
            env.eplToModelCompileDeploy(stmtText).addListener("s0");

            tryFullOuterPassive2Stream(env);

            env.undeployAll();
        }
    }

    private static class EPLJoin2TableFullOuterJoinOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("symbol", "volume", "theString", "intPrimitive"));
            model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getSimpleName()).unidirectional(true)));
            model.getFromClause().add(FilterStream.create(SupportBean.class.getSimpleName()).addView("keepall"));
            model.getFromClause().add(OuterJoinQualifier.create("theString", OuterJoinType.FULL, "symbol"));

            String stmtText = "select symbol, volume, theString, intPrimitive " +
                "from SupportMarketDataBean unidirectional " +
                "full outer join SupportBean" +
                "#keepall on theString = symbol";
            assertEquals(stmtText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            tryFullOuterPassive2Stream(env);

            env.undeployAll();
        }
    }

    private static class EPLJoin2TableFullOuterJoinBackwards implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, theString, intPrimitive " +
                "from SupportBean#keepall full outer join " +
                "SupportMarketDataBean unidirectional " +
                "on theString = symbol";
            env.compileDeployAddListenerMileZero(stmtText, "s0");

            tryFullOuterPassive2Stream(env);

            env.undeployAll();
        }
    }

    private static class EPLJoin2TableJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, theString, intPrimitive " +
                "from SupportMarketDataBean unidirectional, SupportBean" +
                "#keepall where theString = symbol";

            tryJoinPassive2Stream(env, stmtText);
        }
    }

    private static class EPLJoin2TableBackwards implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select symbol, volume, theString, intPrimitive " +
                "from SupportBean#keepall, SupportMarketDataBean unidirectional " +
                "where theString = symbol";

            tryJoinPassive2Stream(env, stmtText);
        }
    }

    private static class EPLJoinInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "select * from SupportBean unidirectional " +
                "full outer join SupportMarketDataBean#keepall unidirectional " +
                "on theString = symbol";
            tryInvalidCompile(env, text, "The unidirectional keyword requires that no views are declared onto the stream (applies to stream 1)");

            text = "select * from SupportBean#length(2) unidirectional " +
                "full outer join SupportMarketDataBean#keepall " +
                "on theString = symbol";
            tryInvalidCompile(env, text, "The unidirectional keyword requires that no views are declared onto the stream");
        }
    }

    private static void tryFullOuterPassive2Stream(RegressionEnvironment env) {
        tryUnsupportedIterator(env.statement("s0"));

        // send event, expect result
        sendEventMD(env, "E1", 1L);
        String[] fields = "symbol,volume,theString,intPrimitive".split(",");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1L, null, null});

        sendEvent(env, "E1", 10);
        assertFalse(env.listener("s0").isInvoked());

        sendEventMD(env, "E1", 2L);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 2L, "E1", 10});

        sendEvent(env, "E1", 20);
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void tryJoinPassive2Stream(RegressionEnvironment env, String stmtText) {
        env.compileDeployAddListenerMileZero(stmtText, "s0");
        tryUnsupportedIterator(env.statement("s0"));

        // send event, expect result
        sendEventMD(env, "E1", 1L);
        String[] fields = "symbol,volume,theString,intPrimitive".split(",");
        assertFalse(env.listener("s0").isInvoked());

        sendEvent(env, "E1", 10);
        assertFalse(env.listener("s0").isInvoked());

        sendEventMD(env, "E1", 2L);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 2L, "E1", 10});

        sendEvent(env, "E1", 20);
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private static void sendEvent(RegressionEnvironment env, String s, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendEventMD(RegressionEnvironment env, String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        env.sendEventBean(bean);
    }

    private static void tryAssertionPatternUniOuterJoinNoOn(RegressionEnvironment env, long startTime) {
        String[] fields = "c0,c1".split(",");
        env.advanceTime(startTime + 2000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, 1L});

        env.sendEventBean(new SupportBean("E1", 10));
        assertFalse(env.listener("s0").isInvoked());

        env.advanceTime(startTime + 3000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 1L});

        env.sendEventBean(new SupportBean("E2", 11));

        env.advanceTime(startTime + 4000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{21, 2L});

        env.sendEventBean(new SupportBean("E3", 12));

        env.advanceTime(startTime + 5000);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{33, 3L});
    }

    private static void tryAssertion2StreamInnerWGroupBy(RegressionEnvironment env) {
        String epl = "create objectarray schema E1 (id string, grp string, value int);\n" +
            "create objectarray schema E2 (id string, value2 int);\n" +
            "@name('s0') select count(*) as c0, sum(E1.value) as c1, E1.id as c2 " +
            "from E1 unidirectional inner join E2#keepall on E1.id = E2.id group by E1.grp";
        env.compileDeployWBusPublicType(epl, new RegressionPath());
        env.addListener("s0");
        String[] fields = "c0,c1,c2".split(",");

        env.sendEventObjectArray(new Object[]{"A", 100}, "E2");
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventObjectArray(new Object[]{"A", "X", 10}, "E1");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L, 10, "A"});

        env.sendEventObjectArray(new Object[]{"A", "Y", 20}, "E1");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L, 20, "A"});

        env.undeployAll();
    }

    private static void try3TableOuterJoin(RegressionEnvironment env) {
        String[] fields = "s0.id,s1.id,s2.id".split(",");

        env.sendEventBean(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1, null, null});
        env.sendEventBean(new SupportBean_S1(2, "E1"));
        env.sendEventBean(new SupportBean_S2(3, "E1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(20, "E2"));
        env.sendEventBean(new SupportBean_S0(10, "E2"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 20, null});
        env.sendEventBean(new SupportBean_S2(30, "E2"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(300, "E3"));
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportBean_S0(100, "E3"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100, null, null});
        env.sendEventBean(new SupportBean_S1(200, "E3"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(31, "E4"));
        env.sendEventBean(new SupportBean_S1(21, "E4"));
        assertFalse(env.listener("s0").isInvoked());
        env.sendEventBean(new SupportBean_S0(11, "E4"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11, 21, 31});

        env.sendEventBean(new SupportBean_S2(32, "E4"));
        env.sendEventBean(new SupportBean_S1(22, "E4"));
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void try3TableJoin(RegressionEnvironment env) {
        env.sendEventBean(new SupportBean_S0(1, "E1"));
        env.sendEventBean(new SupportBean_S1(2, "E1"));
        env.sendEventBean(new SupportBean_S2(3, "E1"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S1(20, "E2"));
        env.sendEventBean(new SupportBean_S0(10, "E2"));
        env.sendEventBean(new SupportBean_S2(30, "E2"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(300, "E3"));
        env.sendEventBean(new SupportBean_S0(100, "E3"));
        env.sendEventBean(new SupportBean_S1(200, "E3"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S2(31, "E4"));
        env.sendEventBean(new SupportBean_S1(21, "E4"));
        assertFalse(env.listener("s0").isInvoked());

        env.sendEventBean(new SupportBean_S0(11, "E4"));
        String[] fields = "s0.id,s1.id,s2.id".split(",");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{11, 21, 31});

        env.sendEventBean(new SupportBean_S2(32, "E4"));
        env.sendEventBean(new SupportBean_S1(22, "E4"));
        assertFalse(env.listener("s0").isInvoked());
    }

    private static void tryUnsupportedIterator(EPStatement stmt) {
        try {
            stmt.iterator();
            fail();
        } catch (UnsupportedOperationException ex) {
            assertEquals("Iteration over a unidirectional join is not supported", ex.getMessage());
        }
    }
}
