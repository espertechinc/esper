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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;

public class ResultSetAggregateFiltered {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetAggregateBlackWhitePercent());
        execs.add(new ResultSetAggregateCountVariations());
        execs.add(new ResultSetAggregateAllAggFunctions());
        execs.add(new ResultSetAggregateFirstLastEver());
        execs.add(new ResultSetAggregateInvalid());
        return execs;
    }

    private static class ResultSetAggregateBlackWhitePercent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "cb,cnb,c,pct".split(",");
            String epl = "@name('s0') select count(*,boolPrimitive) as cb, count(*,not boolPrimitive) as cnb, count(*) as c, count(*,boolPrimitive)/count(*) as pct from SupportBean#length(3)";
            env.compileDeploy(epl).addListener("s0");
            assertStatelessStmt(env, "s0", false);

            env.sendEventBean(makeSB(true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L, 0L, 1L, 1d});

            env.milestone(0);

            env.sendEventBean(makeSB(false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L, 1L, 2L, 0.5d});

            env.sendEventBean(makeSB(false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L, 2L, 3L, 1 / 3d});

            env.milestone(1);

            env.sendEventBean(makeSB(false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0L, 3L, 3L, 0d});

            env.undeployAll();

            env.eplToModelCompileDeploy(epl);

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateCountVariations implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c1,c2".split(",");
            String epl = "@name('s0') select " +
                "count(intBoxed, boolPrimitive) as c1," +
                "count(distinct intBoxed, boolPrimitive) as c2 " +
                "from SupportBean#length(3)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeBean(100, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L, 1L});

            env.milestone(0);

            env.sendEventBean(makeBean(100, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L, 1L});

            env.sendEventBean(makeBean(101, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L, 1L});

            env.sendEventBean(makeBean(102, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{2L, 2L});

            env.sendEventBean(makeBean(103, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L, 1L});

            env.sendEventBean(makeBean(104, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{1L, 1L});

            env.sendEventBean(makeBean(105, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0L, 0L});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateAllAggFunctions implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String[] fields;
            String epl;

            fields = "cavedev,cavg,cmax,cmedian,cmin,cstddev,csum,cfmaxever,cfminever".split(",");
            epl = "@name('s0') select " +
                "avedev(intBoxed, boolPrimitive) as cavedev," +
                "avg(intBoxed, boolPrimitive) as cavg, " +
                "fmax(intBoxed, boolPrimitive) as cmax, " +
                "median(intBoxed, boolPrimitive) as cmedian, " +
                "fmin(intBoxed, boolPrimitive) as cmin, " +
                "stddev(intBoxed, boolPrimitive) as cstddev, " +
                "sum(intBoxed, boolPrimitive) as csum," +
                "fmaxever(intBoxed, boolPrimitive) as cfmaxever, " +
                "fminever(intBoxed, boolPrimitive) as cfminever " +
                "from SupportBean#length(3)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(makeBean(100, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null, null, null, null});

            env.milestoneInc(milestone);

            env.sendEventBean(makeBean(10, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0.0d, 10.0, 10, 10.0, 10, null, 10, 10, 10});

            env.sendEventBean(makeBean(11, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0.0d, 10.0, 10, 10.0, 10, null, 10, 10, 10});

            env.milestoneInc(milestone);

            env.sendEventBean(makeBean(20, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5.0d, 15.0, 20, 15.0, 10, 7.0710678118654755, 30, 20, 10});

            env.sendEventBean(makeBean(30, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5.0d, 25.0, 30, 25.0, 20, 7.0710678118654755, 50, 30, 10});

            // Test all remaining types of "sum"
            env.undeployAll();

            fields = "c1,c2,c3,c4".split(",");
            epl = "@name('s0') select " +
                "sum(floatPrimitive, boolPrimitive) as c1," +
                "sum(doublePrimitive, boolPrimitive) as c2, " +
                "sum(longPrimitive, boolPrimitive) as c3, " +
                "sum(shortPrimitive, boolPrimitive) as c4 " +
                "from SupportBean#length(2)";
            env.compileDeploy(epl).addListener("s0");
            env.milestoneInc(milestone);

            env.sendEventBean(makeBean(2f, 3d, 4L, (short) 5, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

            env.sendEventBean(makeBean(3f, 4d, 5L, (short) 6, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{3f, 4d, 5L, 6});

            env.milestoneInc(milestone);

            env.sendEventBean(makeBean(4f, 5d, 6L, (short) 7, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{7f, 9d, 11L, 13});

            env.sendEventBean(makeBean(1f, 1d, 1L, (short) 1, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5f, 6d, 7L, 8});

            // Test min/max-ever
            env.undeployAll();
            fields = "c1,c2".split(",");
            epl = "@name('s0') select " +
                "fmax(intBoxed, boolPrimitive) as c1," +
                "fmin(intBoxed, boolPrimitive) as c2 " +
                "from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            assertStatelessStmt(env, "s0", false);

            env.sendEventBean(makeBean(10, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, 10});

            env.sendEventBean(makeBean(20, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, 10});

            env.sendEventBean(makeBean(8, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, 10});

            env.sendEventBean(makeBean(7, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, 7});

            env.sendEventBean(makeBean(30, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{20, 7});

            env.milestoneInc(milestone);

            env.sendEventBean(makeBean(40, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{40, 7});

            // test big decimal big integer
            env.undeployAll();
            fields = "c1,c2,c3".split(",");
            epl = "@name('s0') select " +
                "avg(bigdec, bigint < 100) as c1," +
                "sum(bigdec, bigint < 100) as c2, " +
                "sum(bigint, bigint < 100) as c3 " +
                "from SupportBeanNumeric#length(2)";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBeanNumeric(new BigInteger("10"), new BigDecimal(20)));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{new BigDecimal(20), new BigDecimal(20), new BigInteger("10")});

            env.sendEventBean(new SupportBeanNumeric(new BigInteger("101"), new BigDecimal(101)));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{new BigDecimal(20), new BigDecimal(20), new BigInteger("10")});

            env.milestoneInc(milestone);

            env.sendEventBean(new SupportBeanNumeric(new BigInteger("20"), new BigDecimal(40)));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{new BigDecimal(40), new BigDecimal(40), new BigInteger("20")});

            env.sendEventBean(new SupportBeanNumeric(new BigInteger("30"), new BigDecimal(50)));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{new BigDecimal(45), new BigDecimal(90), new BigInteger("50")});

            env.undeployAll();
            epl = "@name('s0') select " +
                "avedev(distinct intBoxed,boolPrimitive) as cavedev, " +
                "avg(distinct intBoxed,boolPrimitive) as cavg, " +
                "fmax(distinct intBoxed,boolPrimitive) as cmax, " +
                "median(distinct intBoxed,boolPrimitive) as cmedian, " +
                "fmin(distinct intBoxed,boolPrimitive) as cmin, " +
                "stddev(distinct intBoxed,boolPrimitive) as cstddev, " +
                "sum(distinct intBoxed,boolPrimitive) as csum " +
                "from SupportBean#length(3)";
            env.compileDeploy(epl).addListener("s0");

            tryAssertionDistinct(env, milestone);

            // test SODA
            env.undeployAll();

            env.eplToModelCompileDeploy(epl).addListener("s0");

            tryAssertionDistinct(env, milestone);

            env.undeployAll();
        }

        private void tryAssertionDistinct(RegressionEnvironment env, AtomicInteger milestone) {

            String[] fields = "cavedev,cavg,cmax,cmedian,cmin,cstddev,csum".split(",");
            env.sendEventBean(makeBean(100, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0d, 100d, 100, 100d, 100, null, 100});

            env.milestoneInc(milestone);

            env.sendEventBean(makeBean(100, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0d, 100d, 100, 100d, 100, null, 100});

            env.sendEventBean(makeBean(200, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{50d, 150d, 200, 150d, 100, 70.71067811865476, 300});

            env.milestoneInc(milestone);

            env.sendEventBean(makeBean(200, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{50d, 150d, 200, 150d, 100, 70.71067811865476, 300});

            env.sendEventBean(makeBean(200, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{0d, 200d, 200, 200d, 200, null, 200});
        }
    }

    private static class ResultSetAggregateFirstLastEver implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            tryAssertionFirstLastEver(env, true, milestone);
            tryAssertionFirstLastEver(env, false, milestone);
        }

        private void tryAssertionFirstLastEver(RegressionEnvironment env, boolean soda, AtomicInteger milestone) {
            String[] fields = "c1,c2,c3".split(",");
            String epl = "@name('s0') select " +
                "firstever(intBoxed,boolPrimitive) as c1, " +
                "lastever(intBoxed,boolPrimitive) as c2, " +
                "countever(*,boolPrimitive) as c3 " +
                "from SupportBean#length(3)";
            env.compileDeploy(soda, epl).addListener("s0");

            env.sendEventBean(makeBean(100, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null, null, 0L});

            env.milestoneInc(milestone);

            env.sendEventBean(makeBean(100, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100, 100, 1L});

            env.sendEventBean(makeBean(200, true));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100, 200, 2L});

            env.milestoneInc(milestone);

            env.sendEventBean(makeBean(201, false));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100, 200, 2L});

            env.undeployAll();
        }
    }

    private static class ResultSetAggregateInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "select count(*, intPrimitive) from SupportBean",
                "Failed to validate select-clause expression 'count(*,intPrimitive)': Invalid filter expression parameter to the aggregation function 'count' is expected to return a boolean value but returns java.lang.Integer [select count(*, intPrimitive) from SupportBean]");

            tryInvalidCompile(env, "select fmin(intPrimitive) from SupportBean",
                "Failed to validate select-clause expression 'min(intPrimitive)': MIN-filtered aggregation function must have a filter expression as a second parameter [select fmin(intPrimitive) from SupportBean]");
        }
    }

    private static SupportBean makeBean(float floatPrimitive, double doublePrimitive, long longPrimitive, short shortPrimitive, boolean boolPrimitive) {
        SupportBean sb = new SupportBean();
        sb.setFloatPrimitive(floatPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        sb.setLongPrimitive(longPrimitive);
        sb.setShortPrimitive(shortPrimitive);
        sb.setBoolPrimitive(boolPrimitive);
        return sb;
    }

    private static SupportBean makeBean(Integer intBoxed, boolean boolPrimitive) {
        SupportBean sb = new SupportBean();
        sb.setIntBoxed(intBoxed);
        sb.setBoolPrimitive(boolPrimitive);
        return sb;
    }

    private static SupportBean makeSB(boolean boolPrimitive) {
        SupportBean sb = new SupportBean("E", 0);
        sb.setBoolPrimitive(boolPrimitive);
        return sb;
    }
}
