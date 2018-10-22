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
package com.espertech.esper.regressionlib.suite.rowrecog;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.rowrecog.SupportRecogBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class RowRecogGreedyness {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogReluctantZeroToOne());
        execs.add(new RowRecogReluctantZeroToMany());
        execs.add(new RowRecogReluctantOneToMany());
        return execs;
    }

    private static class RowRecogReluctantZeroToOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,b_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string " +
                "  pattern (A?? B?) " +
                "  define " +
                "   A as A.value = 1," +
                "   B as B.value = 1" +
                ")";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, "E1"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{null, "E1"}});

            env.undeployAll();
        }
    }

    private static class RowRecogReluctantZeroToMany implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a0,a1,a2,b,c".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A[0].theString as a0, A[1].theString as a1, A[2].theString as a2, B.theString as b, C.theString as c" +
                "  pattern (A*? B? C) " +
                "  define " +
                "   A as A.value = 1," +
                "   B as B.value in (1, 2)," +
                "   C as C.value = 3" +
                ")";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 1));
            env.sendEventBean(new SupportRecogBean("E2", 1));
            env.sendEventBean(new SupportRecogBean("E3", 1));
            env.sendEventBean(new SupportRecogBean("E4", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E2", null, "E3", "E4"}});

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E11", 1));
            env.sendEventBean(new SupportRecogBean("E12", 1));
            env.sendEventBean(new SupportRecogBean("E13", 1));
            env.sendEventBean(new SupportRecogBean("E14", 1));
            env.sendEventBean(new SupportRecogBean("E15", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E11", "E12", "E13", "E14", "E15"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E16", 1));
            env.sendEventBean(new SupportRecogBean("E17", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, null, null, "E16", "E17"}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E18", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, null, null, null, "E18"}});

            env.undeployAll();
        }
    }

    private static class RowRecogReluctantOneToMany implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a0,a1,a2,b,c".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A[0].theString as a0, A[1].theString as a1, A[2].theString as a2, B.theString as b, C.theString as c" +
                "  pattern (A+? B? C) " +
                "  define " +
                "   A as A.value = 1," +
                "   B as B.value in (1, 2)," +
                "   C as C.value = 3" +
                ")";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 1));
            env.sendEventBean(new SupportRecogBean("E2", 1));
            env.sendEventBean(new SupportRecogBean("E3", 1));
            env.sendEventBean(new SupportRecogBean("E4", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E2", null, "E3", "E4"}});

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E11", 1));
            env.sendEventBean(new SupportRecogBean("E12", 1));
            env.sendEventBean(new SupportRecogBean("E13", 1));
            env.sendEventBean(new SupportRecogBean("E14", 1));
            env.sendEventBean(new SupportRecogBean("E15", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E11", "E12", "E13", "E14", "E15"}});

            env.sendEventBean(new SupportRecogBean("E16", 1));
            env.sendEventBean(new SupportRecogBean("E17", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E16", null, null, null, "E17"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E18", 3));
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }
}
