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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;

public class RowRecogAfter {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogAfterCurrentRow());
        execs.add(new RowRecogAfterNextRow());
        execs.add(new RowRecogSkipToNextRow());
        execs.add(new RowRecogVariableMoreThenOnce());
        execs.add(new RowRecogSkipToNextRowPartitioned());
        execs.add(new RowRecogAfterSkipPastLast());
        return execs;
    }

    private static class RowRecogAfterCurrentRow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                " measures A.theString as a, B[0].theString as b0, B[1].theString as b1" +
                " after match skip to current row" +
                " pattern (A B*)" +
                " define" +
                " A as A.theString like \"A%\"," +
                " B as B.theString like \"B%\"" +
                ")";
            env.compileDeploy(text).addListener("s0");
            tryAssertionAfterCurrentRow(env, milestone);
            env.undeployAll();

            env.eplToModelCompileDeploy(text).addListener("s0");
            tryAssertionAfterCurrentRow(env, milestone);
            env.undeployAll();
        }

        private void tryAssertionAfterCurrentRow(RegressionEnvironment env, AtomicInteger milestone) {
            String[] fields = "a,b0,b1".split(",");

            env.sendEventBean(new SupportRecogBean("A1", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"A1", null, null}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"A1", null, null}});

            env.milestoneInc(milestone);

            // since the first match skipped past A, we do not match again
            env.sendEventBean(new SupportRecogBean("B1", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"A1", "B1", null}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"A1", "B1", null}});
        }
    }

    private static class RowRecogAfterNextRow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a,b0,b1".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a, B[0].theString as b0, B[1].theString as b1" +
                "  AFTER MATCH SKIP TO NEXT ROW " +
                "  pattern (A B*) " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    B as B.theString like 'B%'" +
                ")";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("A1", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"A1", null, null}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"A1", null, null}});

            env.milestone(0);

            // since the first match skipped past A, we do not match again
            env.sendEventBean(new SupportRecogBean("B1", 2));
            assertFalse(env.listener("s0").isInvoked());  // incremental skips to next
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"A1", "B1", null}});

            env.undeployAll();
        }
    }

    private static class RowRecogSkipToNextRow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,b_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string " +
                "  all matches " +
                "  after match skip to next row " +
                "  pattern (A B) " +
                "  define B as B.value > A.value" +
                ") " +
                "order by a_string, b_string";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 5));

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E2", 3));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E3", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", "E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E4", 4));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}});

            env.milestone(3);

            env.sendEventBean(new SupportRecogBean("E5", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}});

            env.milestone(4);

            env.sendEventBean(new SupportRecogBean("E6", 10));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}, {"E5", "E6"}});

            env.milestone(5);

            env.sendEventBean(new SupportRecogBean("E7", 9));

            env.milestone(6);

            env.sendEventBean(new SupportRecogBean("E8", 4));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}, {"E5", "E6"}});

            env.undeployAll();
        }
    }

    private static class RowRecogVariableMoreThenOnce implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a0,b,a1".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A[0].theString as a0, B.theString as b, A[1].theString as a1 " +
                "  all matches " +
                "  after match skip to next row " +
                "  pattern ( A B A ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)" +
                ")";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 3));
            env.sendEventBean(new SupportRecogBean("E2", 1));
            env.sendEventBean(new SupportRecogBean("E3", 2));
            env.sendEventBean(new SupportRecogBean("E4", 5));
            env.sendEventBean(new SupportRecogBean("E5", 1));
            env.sendEventBean(new SupportRecogBean("E6", 2));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.sendEventBean(new SupportRecogBean("E7", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6", "E7"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}});

            env.sendEventBean(new SupportRecogBean("E8", 2));
            env.sendEventBean(new SupportRecogBean("E9", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E8", "E9"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}, {"E7", "E8", "E9"}});

            env.undeployAll();
        }
    }

    private static class RowRecogSkipToNextRowPartitioned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,a_value,b_value".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  partition by theString" +
                "  measures A.theString as a_string, A.value as a_value, B.value as b_value " +
                "  all matches " +
                "  after match skip to next row " +
                "  pattern (A B) " +
                "  define B as (B.value > A.value)" +
                ")" +
                " order by a_string";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("S1", 5));
            env.sendEventBean(new SupportRecogBean("S2", 6));
            env.sendEventBean(new SupportRecogBean("S3", 3));
            env.sendEventBean(new SupportRecogBean("S4", 4));
            env.sendEventBean(new SupportRecogBean("S1", 5));
            env.sendEventBean(new SupportRecogBean("S2", 5));
            env.sendEventBean(new SupportRecogBean("S1", 4));
            env.sendEventBean(new SupportRecogBean("S4", -1));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("S1", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S1", 4, 6}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("S4", 10));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S4", -1, 10}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("S4", 11));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S4", 10, 11}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}, {"S4", 10, 11}});

            env.milestone(3);

            env.sendEventBean(new SupportRecogBean("S3", 3));
            env.sendEventBean(new SupportRecogBean("S4", -1));
            env.sendEventBean(new SupportRecogBean("S3", 2));
            env.sendEventBean(new SupportRecogBean("S1", 4));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}, {"S4", 10, 11}});

            env.milestone(4);

            env.sendEventBean(new SupportRecogBean("S1", 7));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S1", 4, 7}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S4", -1, 10}, {"S4", 10, 11}});

            env.milestone(5);

            env.sendEventBean(new SupportRecogBean("S4", 12));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S4", -1, 12}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S4", -1, 10}, {"S4", 10, 11}, {"S4", -1, 12}});

            env.milestone(6);

            env.sendEventBean(new SupportRecogBean("S4", 12));
            env.sendEventBean(new SupportRecogBean("S1", 7));
            env.sendEventBean(new SupportRecogBean("S2", 4));
            env.sendEventBean(new SupportRecogBean("S1", 5));
            assertFalse(env.listener("s0").isInvoked());

            env.milestone(7);

            env.sendEventBean(new SupportRecogBean("S2", 5));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S2", 4, 5}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S2", 4, 5}, {"S4", -1, 10}, {"S4", 10, 11}, {"S4", -1, 12}});

            env.undeployAll();
        }
    }

    private static class RowRecogAfterSkipPastLast implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,b_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string " +
                "  all matches " +
                "  after match skip past last row" +
                "  pattern (A B) " +
                "  define B as B.value > A.value" +
                ") " +
                "order by a_string, b_string";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 5));
            env.sendEventBean(new SupportRecogBean("E2", 3));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E3", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", "E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E4", 4));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}});

            env.sendEventBean(new SupportRecogBean("E5", 6));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E6", 10));
            assertFalse(env.listener("s0").isInvoked());      // E5-E6 not a match since "skip past last row"
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}});

            env.sendEventBean(new SupportRecogBean("E7", 9));
            env.sendEventBean(new SupportRecogBean("E8", 4));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E4", "E5"}});

            env.undeployModuleContaining("s0");
        }
    }
}
