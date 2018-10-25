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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.rowrecog.SupportRecogBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RowRecogDataWin {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogUnboundStreamNoIterator());
        execs.add(new RowRecogTimeWindow());
        execs.add(new RowRecogTimeBatchWindow());
        execs.add(new RowRecogDataWinNamedWindow());
        execs.add(new RowRecogDataWinTimeBatch());
        return execs;
    }

    private static class RowRecogUnboundStreamNoIterator implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "string,value".split(",");
            String text = "@name('s0') select * from SupportRecogBean " +
                "match_recognize (" +
                "  measures A.theString as string, A.value as value" +
                "  all matches pattern (A) " +
                "  define " +
                "    A as PREV(A.theString, 1) = theString" +
                ")";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("s1", 1));

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("s2", 2));
            env.sendEventBean(new SupportRecogBean("s1", 3));
            env.sendEventBean(new SupportRecogBean("s3", 4));

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("s2", 5));
            env.sendEventBean(new SupportRecogBean("s1", 6));
            assertFalse(env.statement("s0").iterator().hasNext());
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportRecogBean("s1", 7));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"s1", 7}});
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        /*
          Optionally send some more events.

        for (int i = 0; i < 100000; i++)
        {
            env.sendEventBean(new SupportRecogBean("P2", 1));
        }
        env.sendEventBean(new SupportRecogBean("P2", 1));
         */
        }
    }

    private static class RowRecogTimeWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(0, env);
            String[] fields = "a_string,b_string,c_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#time(5 sec) " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string, C.theString as c_string" +
                "  all matches pattern ( A B C ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)," +
                "    C as (C.value = 3)" +
                ")";

            env.compileDeploy(text).addListener("s0");

            env.milestone(0);

            sendTimer(50, env);
            env.sendEventBean(new SupportRecogBean("E1", 1));

            env.milestone(1);

            sendTimer(1000, env);
            env.sendEventBean(new SupportRecogBean("E2", 2));
            assertFalse(env.statement("s0").iterator().hasNext());
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(2);

            sendTimer(6000, env);
            env.sendEventBean(new SupportRecogBean("E3", 3));
            assertFalse(env.statement("s0").iterator().hasNext());
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(3);

            sendTimer(7000, env);
            env.sendEventBean(new SupportRecogBean("E4", 1));

            env.milestone(4);

            sendTimer(8000, env);
            env.sendEventBean(new SupportRecogBean("E5", 2));

            env.milestone(5);

            sendTimer(11500, env);
            env.sendEventBean(new SupportRecogBean("E6", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5", "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E4", "E5", "E6"}});

            env.milestone(6);

            sendTimer(11999, env);
            assertTrue(env.statement("s0").iterator().hasNext());

            env.milestone(7);

            sendTimer(12000, env);
            assertFalse(env.statement("s0").iterator().hasNext());
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class RowRecogTimeBatchWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            sendTimer(0, env);
            String[] fields = "a_string,b_string,c_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#time_batch(5 sec) " +
                "match_recognize (" +
                "  partition by cat " +
                "  measures A.theString as a_string, B.theString as b_string, C.theString as c_string" +
                "  all matches pattern ( (A | B) C ) " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    B as B.theString like 'B%'," +
                "    C as C.theString like 'C%' and C.value in (A.value, B.value)" +
                ") order by a_string";

            env.compileDeploy(text).addListener("s0");

            env.milestone(0);

            sendTimer(50, env);
            env.sendEventBean(new SupportRecogBean("A1", "001", 1));
            env.sendEventBean(new SupportRecogBean("B1", "002", 1));
            env.sendEventBean(new SupportRecogBean("B2", "002", 4));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(1);

            sendTimer(4000, env);
            env.sendEventBean(new SupportRecogBean("C1", "002", 4));
            env.sendEventBean(new SupportRecogBean("C2", "002", 5));
            env.sendEventBean(new SupportRecogBean("B3", "003", -1));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{null, "B2", "C1"}});

            env.milestone(2);

            sendTimer(5050, env);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, "B2", "C1"}});
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(3);

            sendTimer(6000, env);
            env.sendEventBean(new SupportRecogBean("C3", "003", -1));
            env.sendEventBean(new SupportRecogBean("C4", "001", 1));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(4);

            sendTimer(10050, env);
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(5);

            sendTimer(14000, env);
            env.sendEventBean(new SupportRecogBean("A2", "002", 0));
            env.sendEventBean(new SupportRecogBean("B4", "003", 10));
            env.sendEventBean(new SupportRecogBean("C5", "002", 0));
            env.sendEventBean(new SupportRecogBean("C6", "003", 10));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{null, "B4", "C6"}, {"A2", null, "C5"}});

            env.milestone(6);

            sendTimer(15050, env);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, "B4", "C6"}, {"A2", null, "C5"}});
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        }
    }

    public static class RowRecogDataWinNamedWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@Name('createwindow') create window MyWindow#keepall as select * from SupportBean", path);
            env.compileDeploy("@Name('insertwindow') insert into MyWindow select * from SupportBean", path);

            env.sendEventBean(new SupportBean("A", 1));
            env.sendEventBean(new SupportBean("A", 2));
            env.sendEventBean(new SupportBean("B", 1));
            env.sendEventBean(new SupportBean("C", 3));

            String text = "@Name('S1') select * from MyWindow " +
                "match_recognize (" +
                "  partition by theString " +
                "  measures A.theString as ast, A.intPrimitive as ai, B.intPrimitive as bi" +
                "  all matches pattern ( A B ) " +
                "  define " +
                "    B as (B.intPrimitive = A.intPrimitive)" +
                ")";

            String[] fields = "ast,ai,bi".split(",");
            env.compileDeploy(text, path).addListener("S1");

            env.milestone(0);

            env.sendEventBean(new SupportBean("C", 3));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields,
                new Object[]{"C", 3, 3});

            env.sendEventBean(new SupportBean("E", 5));

            env.milestone(1);

            env.sendEventBean(new SupportBean("E", 5));
            EPAssertionUtil.assertProps(env.listener("S1").assertOneGetNewAndReset(), fields,
                new Object[]{"E", 5, 5});

            env.undeployAll();
        }
    }

    public static class RowRecogDataWinTimeBatch implements RegressionExecution {

        public void run(RegressionEnvironment env) {

            env.advanceTime(0);

            String[] fields = "a_string,b_string,c_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#time_batch(5 sec) " +
                "match_recognize (" +
                "  partition by cat " +
                "  measures A.theString as a_string, B.theString as b_string, C.theString as c_string" +
                "  all matches pattern ( (A | B) C ) " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    B as B.theString like 'B%'," +
                "    C as C.theString like 'C%' and C.value in (A.value, B.value)" +
                ") order by a_string";
            env.compileDeploy(text).addListener("s0");

            env.advanceTime(50);
            env.sendEventBean(new SupportRecogBean("A1", "001", 1));
            env.sendEventBean(new SupportRecogBean("B1", "002", 1));
            env.sendEventBean(new SupportRecogBean("B2", "002", 4));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.iterator("s0").hasNext());

            env.milestone(0);

            env.advanceTime(4000);
            env.sendEventBean(new SupportRecogBean("C1", "002", 4));
            env.sendEventBean(new SupportRecogBean("C2", "002", 5));
            env.sendEventBean(new SupportRecogBean("B3", "003", -1));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields,
                new Object[][]{{null, "B2", "C1"}});

            env.milestone(1);

            env.advanceTime(5050);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, "B2", "C1"}});
            assertFalse(env.iterator("s0").hasNext());

            env.milestone(2);

            env.advanceTime(6000);
            env.sendEventBean(new SupportRecogBean("C3", "003", -1));
            env.sendEventBean(new SupportRecogBean("C4", "001", 1));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.iterator("s0").hasNext());

            env.milestone(3);

            env.advanceTime(10050);
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.iterator("s0").hasNext());

            env.milestone(4);

            env.advanceTime(14000);
            env.sendEventBean(new SupportRecogBean("A2", "002", 0));
            env.sendEventBean(new SupportRecogBean("B4", "003", 10));
            env.sendEventBean(new SupportRecogBean("C5", "002", 0));
            env.sendEventBean(new SupportRecogBean("C6", "003", 10));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields,
                new Object[][]{{null, "B4", "C6"}, {"A2", null, "C5"}});

            env.milestone(5);

            env.advanceTime(15050);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, "B4", "C6"}, {"A2", null, "C5"}});
            assertFalse(env.iterator("s0").hasNext());

            env.undeployAll();
        }
    }

    private static void sendTimer(long time, RegressionEnvironment env) {
        env.advanceTime(time);
    }
}
