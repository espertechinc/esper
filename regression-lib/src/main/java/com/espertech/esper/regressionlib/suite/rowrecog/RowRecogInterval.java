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
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.rowrecog.SupportRecogBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;

public class RowRecogInterval {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogIntervalSimple());
        execs.add(new RowRecogPartitioned());
        execs.add(new RowRecogMultiCompleted());
        execs.add(new RowRecogMonthScoped());
        return execs;
    }

    private static class RowRecogIntervalSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(0, env);
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1, last(B.theString) as lastb" +
                " pattern (A B*)" +
                " interval 10 seconds" +
                " define" +
                " A as A.theString like \"A%\"," +
                " B as B.theString like \"B%\"" +
                ") order by a, b0, b1, lastb";

            AtomicInteger milestone = new AtomicInteger();
            env.compileDeploy(text).addListener("s0");
            tryAssertionInterval(env, milestone);
            env.undeployAll();

            env.eplToModelCompileDeploy(text).addListener("s0");
            tryAssertionInterval(env, milestone);
            env.undeployAll();
        }

        private void tryAssertionInterval(RegressionEnvironment env, AtomicInteger milestone) {

            String[] fields = "a,b0,b1,lastb".split(",");
            sendTimer(1000, env);
            env.sendEventBean(new SupportRecogBean("A1", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimer(10999, env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A1", null, null, null}});

            env.milestoneInc(milestone);

            sendTimer(11000, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A1", null, null, null}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A1", null, null, null}});

            env.milestoneInc(milestone);

            sendTimer(13000, env);
            env.sendEventBean(new SupportRecogBean("A2", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimer(15000, env);
            env.sendEventBean(new SupportRecogBean("B1", 3));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimer(22999, env);
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimer(23000, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A1", null, null, null}, {"A2", "B1", null, "B1"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A2", "B1", null, "B1"}});

            env.milestoneInc(milestone);

            sendTimer(25000, env);
            env.sendEventBean(new SupportRecogBean("A3", 4));
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(26000, env);
            env.sendEventBean(new SupportRecogBean("B2", 5));
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(29000, env);
            env.sendEventBean(new SupportRecogBean("B3", 6));
            assertFalse(env.listener("s0").isInvoked());

            env.milestoneInc(milestone);

            sendTimer(34999, env);
            env.sendEventBean(new SupportRecogBean("B4", 7));
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(35000, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A1", null, null, null}, {"A2", "B1", null, "B1"}, {"A3", "B2", "B3", "B4"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A3", "B2", "B3", "B4"}});
        }
    }

    private static class RowRecogPartitioned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(0, env);
            String[] fields = "a,b0,b1,lastb".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  partition by cat " +
                "  measures A.theString as a, B[0].theString as b0, B[1].theString as b1, last(B.theString) as lastb" +
                "  pattern (A B*) " +
                "  INTERVAL 10 seconds " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    B as B.theString like 'B%'" +
                ") order by a, b0, b1, lastb";

            env.compileDeploy(text).addListener("s0");

            sendTimer(1000, env);
            env.sendEventBean(new SupportRecogBean("A1", "C1", 1));

            env.milestone(0);

            sendTimer(1000, env);
            env.sendEventBean(new SupportRecogBean("A2", "C2", 2));

            env.milestone(1);

            sendTimer(2000, env);
            env.sendEventBean(new SupportRecogBean("A3", "C3", 3));

            env.milestone(2);

            sendTimer(3000, env);
            env.sendEventBean(new SupportRecogBean("A4", "C4", 4));

            env.milestone(3);

            env.sendEventBean(new SupportRecogBean("B1", "C3", 5));
            env.sendEventBean(new SupportRecogBean("B2", "C1", 6));
            env.sendEventBean(new SupportRecogBean("B3", "C1", 7));
            env.sendEventBean(new SupportRecogBean("B4", "C4", 7));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{
                {"A1", "B2", "B3", "B3"}, {"A2", null, null, null}, {"A3", "B1", null, "B1"}, {"A4", "B4", null, "B4"}});

            env.milestone(4);

            sendTimer(10999, env);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(5);

            sendTimer(11000, env);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A1", "B2", "B3", "B3"}, {"A2", null, null, null}});

            sendTimer(11999, env);
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);

            sendTimer(12000, env);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A3", "B1", null, "B1"}});

            sendTimer(13000, env);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A4", "B4", null, "B4"}});

            env.undeployAll();
        }
    }

    private static class RowRecogMultiCompleted implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(0, env);
            String[] fields = "a,b0,b1,lastb".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a, B[0].theString as b0, B[1].theString as b1, last(B.theString) as lastb" +
                "  pattern (A B*) " +
                "  interval 10 seconds " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    B as B.theString like 'B%'" +
                ") order by a, b0, b1, lastb";

            env.compileDeploy(text).addListener("s0");

            sendTimer(1000, env);
            env.sendEventBean(new SupportRecogBean("A1", 1));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(0);

            sendTimer(5000, env);
            env.sendEventBean(new SupportRecogBean("A2", 2));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(1);

            sendTimer(10999, env);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A1", null, null, null}, {"A2", null, null, null}});

            env.milestone(2);

            sendTimer(11000, env);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A1", null, null, null}});

            env.milestone(3);

            sendTimer(15000, env);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A2", null, null, null}});

            env.milestone(4);

            sendTimer(21000, env);
            env.sendEventBean(new SupportRecogBean("A3", 3));
            assertFalse(env.listener("s0").isInvoked());

            sendTimer(22000, env);
            env.sendEventBean(new SupportRecogBean("A4", 4));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(5);

            sendTimer(23000, env);
            env.sendEventBean(new SupportRecogBean("B1", 5));
            env.sendEventBean(new SupportRecogBean("B2", 6));
            env.sendEventBean(new SupportRecogBean("B3", 7));
            env.sendEventBean(new SupportRecogBean("B4", 8));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);

            sendTimer(31000, env);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A3", null, null, null}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields, new Object[][]{{"A1", null, null, null}, {"A2", null, null, null}, {"A3", null, null, null}, {"A4", "B1", "B2", "B4"}});

            env.milestone(7);

            sendTimer(32000, env);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"A4", "B1", "B2", "B4"}});

            env.undeployAll();
        }
    }

    private static class RowRecogMonthScoped implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            sendCurrentTime(env, "2002-02-01T09:00:00.000");
            String text = "@name('s0') select * from SupportBean " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1 " +
                " pattern (A B*)" +
                " interval 1 month" +
                " define" +
                " A as A.theString like \"A%\"," +
                " B as B.theString like \"B%\"" +
                ")";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportBean("A1", 0));
            env.sendEventBean(new SupportBean("B1", 0));
            sendCurrentTimeWithMinus(env, "2002-03-01T09:00:00.000", 1);
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.milestone(0);

            sendCurrentTime(env, "2002-03-01T09:00:00.000");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "a,b0,b1".split(","),
                new Object[][]{{"A1", "B1", null}});

            env.undeployAll();
        }
    }

    private static void sendCurrentTime(RegressionEnvironment env, String time) {
        env.advanceTime(DateTime.parseDefaultMSec(time));
    }

    private static void sendCurrentTimeWithMinus(RegressionEnvironment env, String time, long minus) {
        env.advanceTime(DateTime.parseDefaultMSec(time) - minus);
    }

    private static void sendTimer(long time, RegressionEnvironment env) {
        env.advanceTime(time);
    }
}
