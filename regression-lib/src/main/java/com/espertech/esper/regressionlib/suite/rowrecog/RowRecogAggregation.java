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

public class RowRecogAggregation {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogMeasureAggregation());
        execs.add(new RowRecogMeasureAggregationPartitioned());
        return execs;
    }

    private static class RowRecogMeasureAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, " +
                "       C.theString as c_string, " +
                "       max(B.value) as maxb, " +
                "       min(B.value) as minb, " +
                "       2*min(B.value) as minb2x, " +
                "       last(B.value) as lastb, " +
                "       first(B.value) as firstb," +
                "       count(B.value) as countb " +
                "  all matches pattern (A B* C) " +
                "  define " +
                "   A as (A.value = 0)," +
                "   B as (B.value != 1)," +
                "   C as (C.value = 1)" +
                ") " +
                "order by a_string";

            env.compileDeploy(text).addListener("s0");

            String[] fields = "a_string,c_string,maxb,minb,minb2x,firstb,lastb,countb".split(",");
            env.sendEventBean(new SupportRecogBean("E1", 0));
            env.sendEventBean(new SupportRecogBean("E2", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E2", null, null, null, null, null, 0L}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E1", "E2", null, null, null, null, null, 0L}});

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E3", 0));
            env.sendEventBean(new SupportRecogBean("E4", 5));

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E5", 3));
            env.sendEventBean(new SupportRecogBean("E6", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E3", "E6", 5, 3, 6, 5, 3, 2L}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E1", "E2", null, null, null, null, null, 0L}, {"E3", "E6", 5, 3, 6, 5, 3, 2L}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E7", 0));
            env.sendEventBean(new SupportRecogBean("E8", 4));
            env.sendEventBean(new SupportRecogBean("E9", -1));

            env.milestone(3);

            env.sendEventBean(new SupportRecogBean("E10", 7));
            env.sendEventBean(new SupportRecogBean("E11", 2));
            env.sendEventBean(new SupportRecogBean("E12", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E12", 7, -1, -2, 4, 2, 4L}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E1", "E2", null, null, null, null, null, 0L},
                    {"E3", "E6", 5, 3, 6, 5, 3, 2L},
                    {"E7", "E12", 7, -1, -2, 4, 2, 4L},
                });

            env.undeployAll();
        }
    }

    private static class RowRecogMeasureAggregationPartitioned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  partition by cat" +
                "  measures A.cat as cat, A.theString as a_string, " +
                "       D.theString as d_string, " +
                "       sum(C.value) as sumc, " +
                "       sum(B.value) as sumb, " +
                "       sum(B.value + A.value) as sumaplusb, " +
                "       sum(C.value + A.value) as sumaplusc " +
                "  all matches pattern (A B B C C D) " +
                "  define " +
                "   A as (A.value >= 10)," +
                "   B as (B.value > 1)," +
                "   C as (C.value < -1)," +
                "   D as (D.value = 999)" +
                ") order by cat";

            env.compileDeploy(text).addListener("s0");

            String[] fields = "a_string,d_string,sumb,sumc,sumaplusb,sumaplusc".split(",");
            env.sendEventBean(new SupportRecogBean("E1", "x", 10));
            env.sendEventBean(new SupportRecogBean("E2", "y", 20));

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E3", "x", 7));     // B
            env.sendEventBean(new SupportRecogBean("E4", "y", 5));
            env.sendEventBean(new SupportRecogBean("E5", "x", 8));
            env.sendEventBean(new SupportRecogBean("E6", "y", 2));

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E7", "x", -2));    // C
            env.sendEventBean(new SupportRecogBean("E8", "y", -7));
            env.sendEventBean(new SupportRecogBean("E9", "x", -5));
            env.sendEventBean(new SupportRecogBean("E10", "y", -4));

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E11", "y", 999));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", "E11", 7, -11, 47, 29}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E11", 7, -11, 47, 29}});

            env.milestone(3);

            env.sendEventBean(new SupportRecogBean("E12", "x", 999));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E12", 15, -7, 35, 13}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E1", "E12", 15, -7, 35, 13}, {"E2", "E11", 7, -11, 47, 29}});

            env.undeployAll();
        }
    }
}
