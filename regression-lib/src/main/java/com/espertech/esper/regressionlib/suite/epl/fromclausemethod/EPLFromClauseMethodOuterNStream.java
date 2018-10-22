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
package com.espertech.esper.regressionlib.suite.epl.fromclausemethod;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanInt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertFalse;

public class EPLFromClauseMethodOuterNStream {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLFromClauseMethod1Stream2HistStarSubordinateLeftRight());
        execs.add(new EPLFromClauseMethod1Stream2HistStarSubordinateInner());
        execs.add(new EPLFromClauseMethod1Stream2HistForwardSubordinate());
        execs.add(new EPLFromClauseMethod1Stream3HistForwardSubordinate());
        execs.add(new EPLFromClauseMethod1Stream3HistForwardSubordinateChain());
        execs.add(new EPLFromClauseMethodInvalid());
        execs.add(new EPLFromClauseMethod2Stream1HistStarSubordinateLeftRight());
        execs.add(new EPLFromClauseMethod1Stream2HistStarNoSubordinateLeftRight());
        return execs;
    }

    private static class EPLFromClauseMethod1Stream2HistStarSubordinateLeftRight implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;
            AtomicInteger milestone = new AtomicInteger();

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt(id like 'E%')#keepall as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " on s0.p02 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index" +
                " order by valh0, valh1";
            tryAssertionOne(env, expression, milestone);

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " right outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p03 = h1.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " on s0.p02 = h0.index" +
                " order by valh0, valh1";
            tryAssertionOne(env, expression, milestone);

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " right outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p02 = h0.index" +
                " left outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index " +
                " order by valh0, valh1";
            tryAssertionOne(env, expression, milestone);

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " full outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p02 = h0.index" +
                " full outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index " +
                " order by valh0, valh1";
            tryAssertionOne(env, expression, milestone);
        }
    }

    private static class EPLFromClauseMethod1Stream2HistStarSubordinateInner implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt(id like 'E%')#keepall as s0 " +
                " inner join " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " on s0.p02 = h0.index " +
                " inner join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index" +
                " order by valh0, valh1";
            tryAssertionTwo(env, expression);

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " inner join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p02 = h0.index " +
                " inner join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index" +
                " order by valh0, valh1";
            tryAssertionTwo(env, expression);
        }
    }

    private static class EPLFromClauseMethod1Stream2HistForwardSubordinate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt(id like 'E%')#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0 " +
                " on s0.p02 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                " on h0.index = h1.index" +
                " order by valh0, valh1";
            tryAssertionThree(env, expression);
        }

        private static void tryAssertionThree(RegressionEnvironment env, String expression) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E1", 0, 0, 1);
            Object[][] result = new Object[][]{{"E1", null, null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E2", 0, 1, 1);
            result = new Object[][]{{"E2", null, null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E3", 1, 0, 1);
            result = new Object[][]{{"E3", "H01", null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E4", 1, 1, 1);
            result = new Object[][]{{"E4", "H01", "H11"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E5", 4, 4, 2);
            result = new Object[][]{{"E5", "H02", "H12"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod1Stream3HistForwardSubordinate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt(id like 'E%')#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0 " +
                " on s0.p03 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                " on h0.index = h1.index" +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H2', p02) as h2 " +
                " on h1.index = h2.index" +
                " order by valh0, valh1, valh2";
            tryAssertionFour(env, expression);

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0 " +
                " right outer join " +
                "SupportBeanInt(id like 'E%')#lastevent as s0 " +
                " on s0.p03 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                " on h0.index = h1.index" +
                " full outer join " +
                "method:SupportJoinMethods.fetchVal('H2', p02) as h2 " +
                " on h1.index = h2.index" +
                " order by valh0, valh1, valh2";
            tryAssertionFour(env, expression);
        }

        private static void tryAssertionFour(RegressionEnvironment env, String expression) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1,valh2".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E1", 0, 0, 0, 1);
            Object[][] result = new Object[][]{{"E1", null, null, null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E2", 0, 1, 1, 1);
            result = new Object[][]{{"E2", null, null, null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E3", 1, 1, 1, 1);
            result = new Object[][]{{"E3", "H01", "H11", "H21"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E4", 1, 0, 1, 1);
            result = new Object[][]{{"E4", "H01", null, null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E5", 4, 4, 4, 2);
            result = new Object[][]{{"E5", "H02", "H12", "H22"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod1Stream3HistForwardSubordinateChain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt(id like 'E%')#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(s0.id || '-H0', p00) as h0 " +
                " on s0.p03 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(h0.val || '-H1', p01) as h1 " +
                " on h0.index = h1.index" +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(h1.val || '-H2', p02) as h2 " +
                " on h1.index = h2.index" +
                " order by valh0, valh1, valh2";
            tryAssertionFive(env, expression);
        }

        private static void tryAssertionFive(RegressionEnvironment env, String expression) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1,valh2".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E1", 0, 0, 0, 1);
            Object[][] result = new Object[][]{{"E1", null, null, null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E2", 0, 1, 1, 1);
            result = new Object[][]{{"E2", null, null, null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E3", 1, 1, 1, 1);
            result = new Object[][]{{"E3", "E3-H01", "E3-H01-H11", "E3-H01-H11-H21"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E4", 1, 0, 1, 1);
            result = new Object[][]{{"E4", "E4-H01", null, null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            sendBeanInt(env, "E5", 4, 4, 4, 2);
            result = new Object[][]{{"E5", "E5-H02", "E5-H02-H12", "E5-H02-H12-H22"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;
            // Invalid dependency order: a historical depends on it's own outer join child or descendant
            //              S0
            //      H0  (depends H1)
            //      H1
            expression = "@name('s0') select * from " +
                "SupportBeanInt#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(h1.val, 1) as h0 " +
                " on s0.p00 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', 1) as h1 " +
                " on h0.index = h1.index";
            tryInvalidCompile(env, expression, "Historical stream 1 parameter dependency originating in stream 2 cannot or may not be satisfied by the join");

            // Optimization conflict : required streams are always executed before optional streams
            //              S0
            //  full outer join H0 to S0
            //  left outer join H1 to S0 (H1 depends on H0)
            expression = "@name('s0') select * from " +
                "SupportBeanInt#lastevent as s0 " +
                " full outer join " +
                "method:SupportJoinMethods.fetchVal('x', 1) as h0 " +
                " on s0.p00 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(h0.val, 1) as h1 " +
                " on s0.p00 = h1.index";
            tryInvalidCompile(env, expression, "Historical stream 2 parameter dependency originating in stream 1 cannot or may not be satisfied by the join");
        }
    }

    private static class EPLFromClauseMethod2Stream1HistStarSubordinateLeftRight implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            //   S1 -> S0 -> H0
            expression = "@name('s0') select s0.id as s0id, s1.id as s1id, h0.val as valh0 from " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(s0.id || 'H0', s0.p00) as h0 " +
                " on s0.p01 = h0.index " +
                " right outer join " +
                "SupportBeanInt(id like 'F%')#keepall as s1 " +
                " on s1.p01 = s0.p01";
            tryAssertionSix(env, expression);

            expression = "@name('s0') select s0.id as s0id, s1.id as s1id, h0.val as valh0 from " +
                "SupportBeanInt(id like 'F%')#keepall as s1 " +
                " left outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s1.p01 = s0.p01" +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(s0.id || 'H0', s0.p00) as h0 " +
                " on s0.p01 = h0.index ";
            tryAssertionSix(env, expression);

            expression = "@name('s0') select s0.id as s0id, s1.id as s1id, h0.val as valh0 from " +
                "method:SupportJoinMethods.fetchVal(s0.id || 'H0', s0.p00) as h0 " +
                " right outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p01 = h0.index " +
                " right outer join " +
                "SupportBeanInt(id like 'F%')#keepall as s1 " +
                " on s1.p01 = s0.p01";
            tryAssertionSix(env, expression);
        }

        private static void tryAssertionSix(RegressionEnvironment env, String expression) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "s0id,s1id,valh0".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E1", 1, 1);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "F1", 1, 1);
            Object[][] resultOne = new Object[][]{{"E1", "F1", "E1H01"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultOne);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, resultOne);

            sendBeanInt(env, "F2", 2, 2);
            Object[][] resultTwo = new Object[][]{{null, "F2", null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultTwo);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo));

            sendBeanInt(env, "E2", 2, 2);
            Object[][] resultThree = new Object[][]{{"E2", "F2", "E2H02"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultThree);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultThree));

            sendBeanInt(env, "F3", 3, 3);
            Object[][] resultFour = new Object[][]{{null, "F3", null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultFour);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultThree, resultFour));

            sendBeanInt(env, "E3", 0, 3);
            Object[][] resultFive = new Object[][]{{"E3", "F3", null}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultFive);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultThree, resultFive));

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod1Stream2HistStarNoSubordinateLeftRight implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            expression = "@name('s0') select s0.id as s0id, h0.val as valh0, h1.val as valh1 from " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " right outer join " +
                "method:SupportJoinMethods.fetchVal('H0', 2) as h0 " +
                " on s0.p00 = h0.index " +
                " right outer join " +
                "method:SupportJoinMethods.fetchVal('H1', 2) as h1 " +
                " on s0.p00 = h1.index";
            tryAssertionSeven(env, expression);

            expression = "@name('s0') select s0.id as s0id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchVal('H1', 2) as h1 " +
                " left outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p00 = h1.index" +
                " right outer join " +
                "method:SupportJoinMethods.fetchVal('H0', 2) as h0 " +
                " on s0.p00 = h0.index ";
            tryAssertionSeven(env, expression);
        }
    }

    private static void tryAssertionSeven(RegressionEnvironment env, String expression) {
        env.compileDeploy(expression).addListener("s0");

        String[] fields = "s0id,valh0,valh1".split(",");
        Object[][] resultOne = new Object[][]{{null, "H01", null}, {null, "H02", null}, {null, null, "H11"}, {null, null, "H12"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, resultOne);

        sendBeanInt(env, "E1", 0);
        assertFalse(env.listener("s0").isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, resultOne);

        sendBeanInt(env, "E2", 2);
        Object[][] resultTwo = new Object[][]{{"E2", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultTwo);
        Object[][] resultIt = new Object[][]{{null, "H01", null}, {null, null, "H11"}, {"E2", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, resultIt);

        sendBeanInt(env, "E3", 1);
        resultTwo = new Object[][]{{"E3", "H01", "H11"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultTwo);
        resultIt = new Object[][]{{"E3", "H01", "H11"}, {"E2", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, resultIt);

        sendBeanInt(env, "E4", 1);
        resultTwo = new Object[][]{{"E4", "H01", "H11"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultTwo);
        resultIt = new Object[][]{{"E3", "H01", "H11"}, {"E4", "H01", "H11"}, {"E2", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, resultIt);

        env.undeployAll();
    }

    private static void tryAssertionOne(RegressionEnvironment env, String expression, AtomicInteger milestone) {
        env.compileDeploy(expression).addListener("s0");

        String[] fields = "id,valh0,valh1".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

        sendBeanInt(env, "E1", 0, 0, 0, 0, 1, 1);
        Object[][] resultOne = new Object[][]{{"E1", null, null}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultOne);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, resultOne);

        sendBeanInt(env, "E2", 1, 1, 1, 1, 1, 1);
        Object[][] resultTwo = new Object[][]{{"E2", "H01_0", "H11_0"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultTwo);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo));

        env.milestoneInc(milestone);

        sendBeanInt(env, "E3", 5, 5, 3, 4, 1, 1);
        Object[][] resultThree = new Object[][]{{"E3", "H03_0", "H14_0"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultThree);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree));

        sendBeanInt(env, "E4", 0, 5, 3, 4, 1, 1);
        Object[][] resultFour = new Object[][]{{"E4", null, "H14_0"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultFour);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree, resultFour));

        sendBeanInt(env, "E5", 2, 0, 2, 1, 1, 1);
        Object[][] resultFive = new Object[][]{{"E5", "H02_0", null}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultFive);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree, resultFour, resultFive));

        // set 2 rows for H0
        sendBeanInt(env, "E6", 2, 2, 2, 2, 2, 1);
        Object[][] resultSix = new Object[][]{{"E6", "H02_0", "H12_0"}, {"E6", "H02_1", "H12_0"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultSix);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree, resultFour, resultFive, resultSix));

        sendBeanInt(env, "E7", 10, 10, 4, 5, 1, 2);
        Object[][] resultSeven = new Object[][]{{"E7", "H04_0", "H15_0"}, {"E7", "H04_0", "H15_1"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultSeven);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree, resultFour, resultFive, resultSix, resultSeven));

        env.undeployAll();
    }

    private static void tryAssertionTwo(RegressionEnvironment env, String expression) {
        env.compileDeploy(expression).addListener("s0");

        String[] fields = "id,valh0,valh1".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

        sendBeanInt(env, "E1", 0, 0, 0, 0, 1, 1);
        assertFalse(env.listener("s0").isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

        sendBeanInt(env, "E2", 1, 1, 1, 1, 1, 1);
        Object[][] resultTwo = new Object[][]{{"E2", "H01_0", "H11_0"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultTwo);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo));

        sendBeanInt(env, "E3", 5, 5, 3, 4, 1, 1);
        Object[][] resultThree = new Object[][]{{"E3", "H03_0", "H14_0"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultThree);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree));

        sendBeanInt(env, "E4", 0, 5, 3, 4, 1, 1);
        assertFalse(env.listener("s0").isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree));

        sendBeanInt(env, "E5", 2, 0, 2, 1, 1, 1);
        assertFalse(env.listener("s0").isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree));

        // set 2 rows for H0
        sendBeanInt(env, "E6", 2, 2, 2, 2, 2, 1);
        Object[][] resultSix = new Object[][]{{"E6", "H02_0", "H12_0"}, {"E6", "H02_1", "H12_0"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultSix);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree, resultSix));

        sendBeanInt(env, "E7", 10, 10, 4, 5, 1, 2);
        Object[][] resultSeven = new Object[][]{{"E7", "H04_0", "H15_0"}, {"E7", "H04_0", "H15_1"}};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultSeven);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree, resultSix, resultSeven));

        env.undeployAll();
    }

    private static void sendBeanInt(RegressionEnvironment env, String id, int p00, int p01, int p02, int p03, int p04, int p05) {
        env.sendEventBean(new SupportBeanInt(id, p00, p01, p02, p03, p04, p05));
    }

    private static void sendBeanInt(RegressionEnvironment env, String id, int p00, int p01, int p02, int p03) {
        sendBeanInt(env, id, p00, p01, p02, p03, -1, -1);
    }

    private static void sendBeanInt(RegressionEnvironment env, String id, int p00, int p01, int p02) {
        sendBeanInt(env, id, p00, p01, p02, -1);
    }

    private static void sendBeanInt(RegressionEnvironment env, String id, int p00, int p01) {
        sendBeanInt(env, id, p00, p01, -1, -1);
    }

    private static void sendBeanInt(RegressionEnvironment env, String id, int p00) {
        sendBeanInt(env, id, p00, -1, -1, -1);
    }
}
