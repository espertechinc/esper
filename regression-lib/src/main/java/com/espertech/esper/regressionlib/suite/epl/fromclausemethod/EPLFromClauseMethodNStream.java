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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanInt;
import com.espertech.esper.regressionlib.support.bean.SupportTradeEventWithSide;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;

public class EPLFromClauseMethodNStream {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLFromClauseMethod1Stream2HistStarSubordinateCartesianLast());
        execs.add(new EPLFromClauseMethod1Stream2HistStarSubordinateJoinedKeepall());
        execs.add(new EPLFromClauseMethod1Stream2HistForwardSubordinate());
        execs.add(new EPLFromClauseMethod1Stream3HistStarSubordinateCartesianLast());
        execs.add(new EPLFromClauseMethod1Stream3HistForwardSubordinate());
        execs.add(new EPLFromClauseMethod1Stream3HistChainSubordinate());
        execs.add(new EPLFromClauseMethod2Stream2HistStarSubordinate());
        execs.add(new EPLFromClauseMethod3Stream1HistSubordinate());
        execs.add(new EPLFromClauseMethod3HistPureNoSubordinate());
        execs.add(new EPLFromClauseMethod3Hist1Subordinate());
        execs.add(new EPLFromClauseMethod3Hist2SubordinateChain());
        execs.add(new EPLFromClauseMethod3Stream1HistStreamNWTwice());
        return execs;
    }

    private static class EPLFromClauseMethod3Stream1HistStreamNWTwice implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window AllTrades#keepall as SupportTradeEventWithSide", path);
            env.compileDeploy("insert into AllTrades select * from SupportTradeEventWithSide", path);

            String epl = "@name('s0') select us, them, corr.correlation as crl " +
                "from AllTrades as us, AllTrades as them," +
                "method:" + EPLFromClauseMethodNStream.class.getName() + ".computeCorrelation(us, them) as corr\n" +
                "where us.side != them.side and corr.correlation > 0";
            env.compileDeploy(epl, path).addListener("s0");

            SupportTradeEventWithSide one = new SupportTradeEventWithSide("T1", "B");
            env.sendEventBean(one);
            assertFalse(env.listener("s0").isInvoked());

            SupportTradeEventWithSide two = new SupportTradeEventWithSide("T2", "S");
            env.sendEventBean(two);

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "us,them,crl".split(","), new Object[][]{{one, two, 1}, {two, one, 1}});
            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod1Stream2HistStarSubordinateCartesianLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#lastevent as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                "order by h0.val, h1.val";
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E1", 1, 1);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1", "H01", "H11"}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H01", "H11"}});

            sendBeanInt(env, "E2", 2, 0);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            env.milestone(0);

            sendBeanInt(env, "E3", 0, 1);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E3", 2, 2);
            Object[][] result = new Object[][]{{"E3", "H01", "H11"}, {"E3", "H01", "H12"}, {"E3", "H02", "H11"}, {"E3", "H02", "H12"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            env.milestone(0);

            sendBeanInt(env, "E4", 2, 1);
            result = new Object[][]{{"E4", "H01", "H11"}, {"E4", "H02", "H11"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod1Stream2HistStarSubordinateJoinedKeepall implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                "where h0.index = h1.index and h0.index = p02";
            tryAssertionOne(env, expression);

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1   from " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "SupportBeanInt#keepall as s0 " +
                "where h0.index = h1.index and h0.index = p02";
            tryAssertionOne(env, expression);
        }

        private static void tryAssertionOne(RegressionEnvironment env, String expression) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E1", 20, 20, 3);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1", "H03", "H13"}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H03", "H13"}});

            sendBeanInt(env, "E2", 20, 20, 21);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H03", "H13"}});

            sendBeanInt(env, "E3", 4, 4, 2);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E3", "H02", "H12"}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H03", "H13"}, {"E3", "H02", "H12"}});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod1Stream2HistForwardSubordinate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;
            AtomicInteger milestone = new AtomicInteger();

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal(h0.val, p01) as h1 " +
                "order by h0.val, h1.val";
            tryAssertionTwo(env, expression, milestone);

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchVal(h0.val, p01) as h1, " +
                "SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0 " +
                "order by h0.val, h1.val";
            tryAssertionTwo(env, expression, milestone);
        }

        private static void tryAssertionTwo(RegressionEnvironment env, String expression, AtomicInteger milestone) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E1", 1, 1);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1", "H01", "H011"}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H01", "H011"}});

            sendBeanInt(env, "E2", 0, 1);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H01", "H011"}});

            env.milestoneInc(milestone);

            sendBeanInt(env, "E3", 1, 0);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H01", "H011"}});

            sendBeanInt(env, "E4", 2, 2);
            Object[][] result = {{"E4", "H01", "H011"}, {"E4", "H01", "H012"}, {"E4", "H02", "H021"}, {"E4", "H02", "H022"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(result, new Object[][]{{"E1", "H01", "H011"}}));

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod1Stream3HistStarSubordinateCartesianLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt#lastevent as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal('H2', p02) as h2 " +
                "order by h0.val, h1.val, h2.val";
            tryAssertionThree(env, expression);

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H2', p02) as h2, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "SupportBeanInt#lastevent as s0 " +
                "order by h0.val, h1.val, h2.val";
            tryAssertionThree(env, expression);
        }

        private static void tryAssertionThree(RegressionEnvironment env, String expression) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1,valh2".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E1", 1, 1, 1);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1", "H01", "H11", "H21"}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H01", "H11", "H21"}});

            sendBeanInt(env, "E2", 1, 1, 2);
            Object[][] result = new Object[][]{{"E2", "H01", "H11", "H21"}, {"E2", "H01", "H11", "H22"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, result);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod1Stream3HistForwardSubordinate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal(h0.val||'H2', p02) as h2 " +
                " where h0.index = h1.index and h1.index = h2.index and h2.index = p03";
            tryAssertionFour(env, expression);

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal(h0.val||'H2', p02) as h2, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                " where h0.index = h1.index and h1.index = h2.index and h2.index = p03";
            tryAssertionFour(env, expression);
        }

        private static void tryAssertionFour(RegressionEnvironment env, String expression) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1,valh2".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E1", 2, 2, 2, 1);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E1", "H01", "H11", "H01H21"}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H01", "H11", "H01H21"}});

            sendBeanInt(env, "E2", 4, 4, 4, 3);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E2", "H03", "H13", "H03H23"}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E1", "H01", "H11", "H01H21"}, {"E2", "H03", "H13", "H03H23"}});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod1Stream3HistChainSubordinate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression;

            expression = "@name('s0') select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal(h0.val||'H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal(h1.val||'H2', p02) as h2 " +
                " where h0.index = h1.index and h1.index = h2.index and h2.index = p03";
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "id,valh0,valh1,valh2".split(",");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "E2", 4, 4, 4, 3);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"E2", "H03", "H03H13", "H03H13H23"}});

            sendBeanInt(env, "E2", 4, 4, 4, 5);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, null);

            env.milestone(0);

            sendBeanInt(env, "E2", 4, 4, 0, 1);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, null);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"E2", "H03", "H03H13", "H03H13H23"}});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod2Stream2HistStarSubordinate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select s0.id as ids0, s1.id as ids1, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt(id like 'S0%')#keepall as s0, " +
                "SupportBeanInt(id like 'S1%')#lastevent as s1, " +
                "method:SupportJoinMethods.fetchVal(s0.id||'H1', s0.p00) as h0, " +
                "method:SupportJoinMethods.fetchVal(s1.id||'H2', s1.p00) as h1 " +
                "order by s0.id asc";
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "ids0,ids1,valh0,valh1".split(",");
            sendBeanInt(env, "S00", 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBeanInt(env, "S10", 1);
            Object[][] resultOne = new Object[][]{{"S00", "S10", "S00H11", "S10H21"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultOne);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, resultOne);

            sendBeanInt(env, "S01", 1);
            Object[][] resultTwo = new Object[][]{{"S01", "S10", "S01H11", "S10H21"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultTwo);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo));

            env.milestone(0);

            sendBeanInt(env, "S11", 1);
            Object[][] resultThree = new Object[][]{{"S00", "S11", "S00H11", "S11H21"}, {"S01", "S11", "S01H11", "S11H21"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultThree);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultThree));

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod3Stream1HistSubordinate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String expression = "@name('s0') select s0.id as ids0, s1.id as ids1, s2.id as ids2, h0.val as valh0 " +
                "from SupportBeanInt(id like 'S0%')#keepall as s0, " +
                "SupportBeanInt(id like 'S1%')#lastevent as s1, " +
                "SupportBeanInt(id like 'S2%')#lastevent as s2, " +
                "method:SupportJoinMethods.fetchVal(s1.id||s2.id||'H1', s0.p00) as h0 " +
                "order by s0.id, s1.id, s2.id, h0.val";
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "ids0,ids1,ids2,valh0".split(",");
            sendBeanInt(env, "S00", 2);
            sendBeanInt(env, "S10", 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBeanInt(env, "S20", 1);
            Object[][] resultOne = new Object[][]{{"S00", "S10", "S20", "S10S20H11"}, {"S00", "S10", "S20", "S10S20H12"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultOne);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, resultOne);

            sendBeanInt(env, "S01", 1);
            Object[][] resultTwo = new Object[][]{{"S01", "S10", "S20", "S10S20H11"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultTwo);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo));

            env.milestone(0);

            sendBeanInt(env, "S21", 1);
            Object[][] resultThree = new Object[][]{{"S00", "S10", "S21", "S10S21H11"}, {"S00", "S10", "S21", "S10S21H12"}, {"S01", "S10", "S21", "S10S21H11"}};
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, resultThree);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, EPAssertionUtil.concatenateArray2Dim(resultThree));

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod3HistPureNoSubordinate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("on SupportBeanInt set var1=p00, var2=p01, var3=p02, var4=p03");
            AtomicInteger milestone = new AtomicInteger();

            String expression;
            expression = "@name('s0') select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0," +
                "method:SupportJoinMethods.fetchVal('H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal('H2', var3) as h2";
            tryAssertionFive(env, expression, milestone);

            expression = "@name('s0') select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H2', var3) as h2," +
                "method:SupportJoinMethods.fetchVal('H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0";
            tryAssertionFive(env, expression, milestone);

            env.undeployAll();
        }

        private static void tryAssertionFive(RegressionEnvironment env, String expression, AtomicInteger milestone) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "valh0,valh1,valh2".split(",");

            sendBeanInt(env, "S00", 1, 1, 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"H01", "H11", "H21"}});

            sendBeanInt(env, "S01", 0, 1, 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            env.milestoneInc(milestone);

            sendBeanInt(env, "S02", 1, 1, 0);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "S03", 1, 1, 2);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"H01", "H11", "H21"}, {"H01", "H11", "H22"}});

            sendBeanInt(env, "S04", 2, 2, 1);
            Object[][] result = new Object[][]{{"H01", "H11", "H21"}, {"H02", "H11", "H21"}, {"H01", "H12", "H21"}, {"H02", "H12", "H21"}};
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            env.undeployModuleContaining("s0");
        }
    }

    private static class EPLFromClauseMethod3Hist1Subordinate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("on SupportBeanInt set var1=p00, var2=p01, var3=p02, var4=p03");

            String expression;
            expression = "@name('s0') select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0," +
                "method:SupportJoinMethods.fetchVal('H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal(h0.val||'-H2', var3) as h2";
            tryAssertionSix(env, expression);

            expression = "@name('s0') select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal(h0.val||'-H2', var3) as h2," +
                "method:SupportJoinMethods.fetchVal('H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0";
            tryAssertionSix(env, expression);

            env.undeployAll();
        }

        private static void tryAssertionSix(RegressionEnvironment env, String expression) {
            env.compileDeploy(expression).addListener("s0");

            String[] fields = "valh0,valh1,valh2".split(",");

            sendBeanInt(env, "S00", 1, 1, 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"H01", "H11", "H01-H21"}});

            sendBeanInt(env, "S01", 0, 1, 1);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "S02", 1, 1, 0);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendBeanInt(env, "S03", 1, 1, 2);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"H01", "H11", "H01-H21"}, {"H01", "H11", "H01-H22"}});

            sendBeanInt(env, "S04", 2, 2, 1);
            Object[][] result = new Object[][]{{"H01", "H11", "H01-H21"}, {"H02", "H11", "H02-H21"}, {"H01", "H12", "H01-H21"}, {"H02", "H12", "H02-H21"}};
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

            env.undeployModuleContaining("s0");
        }
    }

    private static class EPLFromClauseMethod3Hist2SubordinateChain implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("on SupportBeanInt set var1=p00, var2=p01, var3=p02, var4=p03");
            AtomicInteger milestone = new AtomicInteger();

            String expression;
            expression = "@name('s0') select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0," +
                "method:SupportJoinMethods.fetchVal(h0.val||'-H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal(h1.val||'-H2', var3) as h2";
            tryAssertionSeven(env, expression, milestone);

            expression = "@name('s0') select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal(h1.val||'-H2', var3) as h2," +
                "method:SupportJoinMethods.fetchVal(h0.val||'-H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0";
            tryAssertionSeven(env, expression, milestone);

            env.undeployAll();
        }
    }

    private static void tryAssertionSeven(RegressionEnvironment env, String expression, AtomicInteger milestone) {
        env.compileDeploy(expression).addListener("s0");

        String[] fields = "valh0,valh1,valh2".split(",");

        sendBeanInt(env, "S00", 1, 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"H01", "H01-H11", "H01-H11-H21"}});

        sendBeanInt(env, "S01", 0, 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

        env.milestoneInc(milestone);

        sendBeanInt(env, "S02", 1, 1, 0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

        sendBeanInt(env, "S03", 1, 1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{"H01", "H01-H11", "H01-H11-H21"}, {"H01", "H01-H11", "H01-H11-H22"}});

        sendBeanInt(env, "S04", 2, 2, 1);
        Object[][] result = new Object[][]{{"H01", "H01-H11", "H01-H11-H21"}, {"H02", "H02-H11", "H02-H11-H21"}, {"H01", "H01-H12", "H01-H12-H21"}, {"H02", "H02-H12", "H02-H12-H21"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, result);

        env.undeployModuleContaining("s0");
    }

    private static void sendBeanInt(RegressionEnvironment env, String id, int p00, int p01, int p02, int p03) {
        env.sendEventBean(new SupportBeanInt(id, p00, p01, p02, p03, -1, -1));
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

    public static ComputeCorrelationResult computeCorrelation(SupportTradeEventWithSide us, SupportTradeEventWithSide them) {
        return new ComputeCorrelationResult(us != null && them != null ? 1 : 0);
    }

    public static class ComputeCorrelationResult implements Serializable {
        private final int correlation;

        public ComputeCorrelationResult(int correlation) {
            this.correlation = correlation;
        }

        public int getCorrelation() {
            return correlation;
        }
    }

}
