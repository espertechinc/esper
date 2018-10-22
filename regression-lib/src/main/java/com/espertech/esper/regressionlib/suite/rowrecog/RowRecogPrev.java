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

public class RowRecogPrev {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogTimeWindowPartitionedSimple());
        execs.add(new RowRecogPartitionBy2FieldsKeepall());
        execs.add(new RowRecogUnpartitionedKeepAll());
        execs.add(new RowRecogTimeWindowUnpartitioned());
        execs.add(new RowRecogTimeWindowPartitioned());
        return execs;
    }

    private static class RowRecogTimeWindowUnpartitioned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(0, env);
            String[] fields = "a_string,b_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#time(5) " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string" +
                "  all matches pattern (A B) " +
                "  define " +
                "    A as PREV(A.theString, 3) = 'P3' and PREV(A.theString, 2) = 'P2' and PREV(A.theString, 4) = 'P4' and Math.abs(prev(A.value, 0)) >= 0," +
                "    B as B.value in (PREV(B.value, 4), PREV(B.value, 2))" +
                ")";

            env.compileDeploy(text).addListener("s0");

            sendTimer(1000, env);
            env.sendEventBean(new SupportRecogBean("P2", 1));
            env.sendEventBean(new SupportRecogBean("P1", 2));
            env.sendEventBean(new SupportRecogBean("P3", 3));
            env.sendEventBean(new SupportRecogBean("P4", 4));

            env.milestone(0);

            sendTimer(2000, env);
            env.sendEventBean(new SupportRecogBean("P2", 1));
            env.sendEventBean(new SupportRecogBean("E1", 3));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(1);

            sendTimer(3000, env);
            env.sendEventBean(new SupportRecogBean("P4", 11));
            env.sendEventBean(new SupportRecogBean("P3", 12));
            env.sendEventBean(new SupportRecogBean("P2", 13));

            env.milestone(2);

            sendTimer(4000, env);
            env.sendEventBean(new SupportRecogBean("xx", 4));
            env.sendEventBean(new SupportRecogBean("E2", -1));
            env.sendEventBean(new SupportRecogBean("E3", 12));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", "E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}});

            env.milestone(3);

            sendTimer(5000, env);
            env.sendEventBean(new SupportRecogBean("P4", 21));
            env.sendEventBean(new SupportRecogBean("P3", 22));

            env.milestone(4);

            sendTimer(6000, env);
            env.sendEventBean(new SupportRecogBean("P2", 23));
            env.sendEventBean(new SupportRecogBean("xx", -2));
            env.sendEventBean(new SupportRecogBean("E5", -1));
            env.sendEventBean(new SupportRecogBean("E6", -2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E5", "E6"}});

            env.milestone(5);

            sendTimer(8500, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E5", "E6"}});

            sendTimer(9500, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", "E6"}});

            env.milestone(6);

            sendTimer(10500, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", "E6"}});

            sendTimer(11500, env);
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        }
    }

    private static class RowRecogTimeWindowPartitioned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(0, env);
            String[] fields = "cat,a_string,b_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#time(5) " +
                "match_recognize (" +
                "  partition by cat" +
                "  measures A.cat as cat, A.theString as a_string, B.theString as b_string" +
                "  all matches pattern (A B) " +
                "  define " +
                "    A as PREV(A.theString, 3) = 'P3' and PREV(A.theString, 2) = 'P2' and PREV(A.theString, 4) = 'P4'," +
                "    B as B.value in (PREV(B.value, 4), PREV(B.value, 2))" +
                ") order by cat";

            env.compileDeploy(text).addListener("s0");


            sendTimer(1000, env);
            env.sendEventBean(new SupportRecogBean("P4", "c2", 1));
            env.sendEventBean(new SupportRecogBean("P3", "c1", 2));
            env.sendEventBean(new SupportRecogBean("P2", "c2", 3));
            env.sendEventBean(new SupportRecogBean("xx", "c1", 4));

            env.milestone(0);

            sendTimer(2000, env);
            env.sendEventBean(new SupportRecogBean("P2", "c1", 1));
            env.sendEventBean(new SupportRecogBean("E1", "c1", 3));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(1);

            sendTimer(3000, env);
            env.sendEventBean(new SupportRecogBean("P4", "c1", 11));
            env.sendEventBean(new SupportRecogBean("P3", "c1", 12));
            env.sendEventBean(new SupportRecogBean("P2", "c1", 13));

            env.milestone(2);

            sendTimer(4000, env);
            env.sendEventBean(new SupportRecogBean("xx", "c1", 4));
            env.sendEventBean(new SupportRecogBean("E2", "c1", -1));
            env.sendEventBean(new SupportRecogBean("E3", "c1", 12));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"c1", "E2", "E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"c1", "E2", "E3"}});

            env.milestone(3);

            sendTimer(5000, env);
            env.sendEventBean(new SupportRecogBean("P4", "c2", 21));
            env.sendEventBean(new SupportRecogBean("P3", "c2", 22));

            env.milestone(4);

            sendTimer(6000, env);
            env.sendEventBean(new SupportRecogBean("P2", "c2", 23));
            env.sendEventBean(new SupportRecogBean("xx", "c2", -2));
            env.sendEventBean(new SupportRecogBean("E5", "c2", -1));
            env.sendEventBean(new SupportRecogBean("E6", "c2", -2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"c2", "E5", "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"c1", "E2", "E3"}, {"c2", "E5", "E6"}});

            env.milestone(5);

            sendTimer(8500, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"c1", "E2", "E3"}, {"c2", "E5", "E6"}});

            sendTimer(9500, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"c2", "E5", "E6"}});

            env.milestone(6);

            sendTimer(10500, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"c2", "E5", "E6"}});

            sendTimer(11500, env);
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        }
    }

    private static class RowRecogTimeWindowPartitionedSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(0, env);
            String[] fields = "a_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#time(5 sec) " +
                "match_recognize (" +
                "  partition by cat " +
                "  measures A.cat as cat, A.theString as a_string" +
                "  all matches pattern (A) " +
                "  define " +
                "    A as PREV(A.value) = (A.value - 1)" +
                ") order by a_string";

            env.compileDeploy(text).addListener("s0");

            env.milestone(0);

            sendTimer(1000, env);
            env.sendEventBean(new SupportRecogBean("E1", "S1", 100));

            sendTimer(2000, env);
            env.sendEventBean(new SupportRecogBean("E2", "S3", 100));

            env.milestone(1);

            sendTimer(2500, env);
            env.sendEventBean(new SupportRecogBean("E3", "S2", 102));

            env.milestone(2);

            sendTimer(6200, env);
            env.sendEventBean(new SupportRecogBean("E4", "S1", 101));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E4"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E4"}});

            env.milestone(3);

            sendTimer(6500, env);
            env.sendEventBean(new SupportRecogBean("E5", "S3", 101));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E4"}, {"E5"}});

            env.milestone(4);

            sendTimer(7000, env);
            env.sendEventBean(new SupportRecogBean("E6", "S1", 102));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}});

            env.milestone(5);

            sendTimer(10000, env);
            env.sendEventBean(new SupportRecogBean("E7", "S2", 103));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E7"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}});

            env.sendEventBean(new SupportRecogBean("E8", "S2", 102));
            env.sendEventBean(new SupportRecogBean("E8", "S1", 101));
            env.sendEventBean(new SupportRecogBean("E8", "S2", 104));
            env.sendEventBean(new SupportRecogBean("E8", "S1", 105));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(6);

            sendTimer(11199, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}, {"E7"}});

            env.milestone(7);

            sendTimer(11200, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5"}, {"E6"}, {"E7"}});

            sendTimer(11600, env);
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E6"}, {"E7"}});

            env.milestone(8);

            sendTimer(16000, env);
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        }
    }

    private static class RowRecogPartitionBy2FieldsKeepall implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,a_cat,a_value,b_value".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  partition by theString, cat" +
                "  measures A.theString as a_string, A.cat as a_cat, A.value as a_value, B.value as b_value " +
                "  all matches pattern (A B) " +
                "  define " +
                "    A as (A.value > PREV(A.value))," +
                "    B as (B.value > PREV(B.value))" +
                ") order by a_string, a_cat";

            env.compileDeploy(text).addListener("s0");
            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("S1", "T1", 5));
            env.sendEventBean(new SupportRecogBean("S2", "T1", 110));
            env.sendEventBean(new SupportRecogBean("S1", "T2", 21));
            env.sendEventBean(new SupportRecogBean("S1", "T1", 7));
            env.sendEventBean(new SupportRecogBean("S2", "T1", 111));
            env.sendEventBean(new SupportRecogBean("S1", "T2", 20));

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("S2", "T1", 110));
            env.sendEventBean(new SupportRecogBean("S2", "T2", 1000));
            env.sendEventBean(new SupportRecogBean("S2", "T2", 1001));
            env.sendEventBean(new SupportRecogBean("S1", null, 9));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("S1", "T1", 9));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S1", "T1", 7, 9}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", "T1", 7, 9}});

            env.milestone(3);

            env.sendEventBean(new SupportRecogBean("S2", "T2", 1001));
            env.sendEventBean(new SupportRecogBean("S2", "T1", 109));
            env.sendEventBean(new SupportRecogBean("S1", "T2", 25));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", "T1", 7, 9}});

            env.milestone(4);

            env.sendEventBean(new SupportRecogBean("S2", "T2", 1002));
            env.sendEventBean(new SupportRecogBean("S2", "T2", 1003));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S2", "T2", 1002, 1003}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", "T1", 7, 9}, {"S2", "T2", 1002, 1003}});

            env.milestone(5);

            env.sendEventBean(new SupportRecogBean("S1", "T2", 28));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S1", "T2", 25, 28}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", "T1", 7, 9}, {"S1", "T2", 25, 28}, {"S2", "T2", 1002, 1003}});

            env.undeployAll();
        }
    }

    private static class RowRecogUnpartitionedKeepAll implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string" +
                "  all matches pattern (A) " +
                "  define A as (A.value > PREV(A.value))" +
                ") " +
                "order by a_string";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 5));
            env.sendEventBean(new SupportRecogBean("E2", 3));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E3", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E4", 4));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3"}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E5", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3"}, {"E5"}});

            env.milestone(3);

            env.sendEventBean(new SupportRecogBean("E6", 10));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3"}, {"E5"}, {"E6"}});

            env.milestone(4);

            env.sendEventBean(new SupportRecogBean("E7", 9));

            env.milestone(5);

            env.sendEventBean(new SupportRecogBean("E8", 4));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3"}, {"E5"}, {"E6"}});

            env.undeployModuleContaining("s0");

            text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string" +
                "  all matches pattern (A) " +
                "  define A as (PREV(A.value, 2) = 5)" +
                ") " +
                "order by a_string";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 5));
            env.sendEventBean(new SupportRecogBean("E2", 4));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(6);

            env.sendEventBean(new SupportRecogBean("E3", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3"}});

            env.milestone(7);

            env.sendEventBean(new SupportRecogBean("E4", 3));
            env.sendEventBean(new SupportRecogBean("E5", 3));
            env.sendEventBean(new SupportRecogBean("E5", 5));
            env.sendEventBean(new SupportRecogBean("E6", 5));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3"}});

            env.milestone(8);

            env.sendEventBean(new SupportRecogBean("E7", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E7"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3"}, {"E7"}});

            env.milestone(9);

            env.sendEventBean(new SupportRecogBean("E8", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E8"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E3"}, {"E7"}, {"E8"}});

            env.undeployAll();
        }
    }

    private static void sendTimer(long time, RegressionEnvironment env) {
        env.advanceTime(time);
    }
}
