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
import com.espertech.esper.common.internal.epl.rowrecog.state.RowRecogPartitionStateRepoGroup;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.rowrecog.SupportRecogBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RowRecogOps {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogConcatenation());
        execs.add(new RowRecogZeroToMany());
        execs.add(new RowRecogOneToMany());
        execs.add(new RowRecogZeroToOne());
        execs.add(new RowRecogPartitionBy());
        execs.add(new RowRecogUnlimitedPartition());
        execs.add(new RowRecogConcatWithinAlter());
        execs.add(new RowRecogAlterWithinConcat());
        execs.add(new RowRecogVariableMoreThenOnce());
        execs.add(new RowRecogRegex());
        return execs;
    }

    private static class RowRecogConcatenation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,b_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string " +
                "  all matches " +
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

    private static class RowRecogZeroToMany implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,b0_string,b1_string,b2_string,c_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, " +
                "    B[0].theString as b0_string, " +
                "    B[1].theString as b1_string, " +
                "    B[2].theString as b2_string, " +
                "    C.theString as c_string" +
                "  all matches " +
                "  pattern (A B* C) " +
                "  define \n" +
                "    A as A.value = 10,\n" +
                "    B as B.value > 10,\n" +
                "    C as C.value < 10\n" +
                ") " +
                "order by a_string, c_string";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 12));
            env.sendEventBean(new SupportRecogBean("E2", 10));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.sendEventBean(new SupportRecogBean("E3", 8));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}});

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E4", 10));
            env.sendEventBean(new SupportRecogBean("E5", 12));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}});

            env.sendEventBean(new SupportRecogBean("E6", 8));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5", null, null, "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}, {"E4", "E5", null, null, "E6"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E7", 10));
            env.sendEventBean(new SupportRecogBean("E8", 12));
            env.sendEventBean(new SupportRecogBean("E9", 12));
            env.sendEventBean(new SupportRecogBean("E10", 12));
            env.sendEventBean(new SupportRecogBean("E11", 9));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E8", "E9", "E10", "E11"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", null, null, null, "E3"}, {"E4", "E5", null, null, "E6"}, {"E7", "E8", "E9", "E10", "E11"}});

            env.undeployModuleContaining("s0");

            // Zero-to-many unfiltered
            String epl = "@name('s0') select * from SupportRecogBean match_recognize (" +
                "measures A as a, B as b, C as c " +
                "pattern (A C*? B) " +
                "define " +
                "A as typeof(A) = 'SupportRecogBeanTypeA'," +
                "B as typeof(B) = 'SupportRecogBeanTypeB'" +
                ")";
            env.compileDeploy(epl);
            env.undeployAll();
        }
    }

    private static class RowRecogOneToMany implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,b0_string,b1_string,b2_string,c_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, " +
                "    B[0].theString as b0_string, " +
                "    B[1].theString as b1_string, " +
                "    B[2].theString as b2_string, " +
                "    C.theString as c_string" +
                "  all matches " +
                "  pattern (A B+ C) " +
                "  define \n" +
                "    A as (A.value = 10),\n" +
                "    B as (B.value > 10),\n" +
                "    C as (C.value < 10)\n" +
                ") " +
                "order by a_string, c_string";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 12));
            env.sendEventBean(new SupportRecogBean("E2", 10));
            env.sendEventBean(new SupportRecogBean("E3", 8));

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E4", 10));
            env.sendEventBean(new SupportRecogBean("E5", 12));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.sendEventBean(new SupportRecogBean("E6", 8));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5", null, null, "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E4", "E5", null, null, "E6"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E7", 10));
            env.sendEventBean(new SupportRecogBean("E8", 12));
            env.sendEventBean(new SupportRecogBean("E9", 12));
            env.sendEventBean(new SupportRecogBean("E10", 12));
            env.sendEventBean(new SupportRecogBean("E11", 9));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E8", "E9", "E10", "E11"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E4", "E5", null, null, "E6"}, {"E7", "E8", "E9", "E10", "E11"}});

            env.undeployModuleContaining("s0");
        }
    }

    private static class RowRecogZeroToOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,b_string,c_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string, " +
                "    C.theString as c_string" +
                "  all matches " +
                "  pattern (A B? C) " +
                "  define \n" +
                "    A as (A.value = 10),\n" +
                "    B as (B.value > 10),\n" +
                "    C as (C.value < 10)\n" +
                ") " +
                "order by a_string";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 12));
            env.sendEventBean(new SupportRecogBean("E2", 10));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E3", 8));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", null, "E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", null, "E3"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E4", 10));
            env.sendEventBean(new SupportRecogBean("E5", 12));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", null, "E3"}});

            env.sendEventBean(new SupportRecogBean("E6", 8));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5", "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", null, "E3"}, {"E4", "E5", "E6"}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E7", 10));
            env.sendEventBean(new SupportRecogBean("E8", 12));
            env.sendEventBean(new SupportRecogBean("E9", 12));
            env.sendEventBean(new SupportRecogBean("E11", 9));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", null, "E3"}, {"E4", "E5", "E6"}});

            env.undeployModuleContaining("s0");

            // test optional event not defined
            String epl = "@name('s0') select * from SupportBean_A match_recognize (" +
                "measures A.id as id, B.id as b_id " +
                "pattern (A B?) " +
                "define " +
                " A as typeof(A) = 'SupportBean_A'" +
                ")";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean_A("A1"));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class RowRecogPartitionBy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,a_value,b_value".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  partition by theString" +
                "  measures A.theString as a_string, A.value as a_value, B.value as b_value " +
                "  all matches pattern (A B) " +
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

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("S1", 4));
            env.sendEventBean(new SupportRecogBean("S4", -1));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

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

            env.sendEventBean(new SupportRecogBean("S4", 11));
            assertFalse(env.listener("s0").isInvoked());      // since skip past last row
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("S3", 3));
            env.sendEventBean(new SupportRecogBean("S4", -2));
            env.sendEventBean(new SupportRecogBean("S3", 2));
            env.sendEventBean(new SupportRecogBean("S1", 4));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S4", -1, 10}});

            env.sendEventBean(new SupportRecogBean("S1", 7));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S1", 4, 7}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S4", -1, 10}});

            env.sendEventBean(new SupportRecogBean("S4", 12));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S4", -2, 12}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S4", -1, 10}, {"S4", -2, 12}});

            env.milestone(3);

            env.sendEventBean(new SupportRecogBean("S4", 12));
            env.sendEventBean(new SupportRecogBean("S1", 7));
            env.sendEventBean(new SupportRecogBean("S2", 4));
            env.sendEventBean(new SupportRecogBean("S1", 5));
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportRecogBean("S2", 5));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"S2", 4, 5}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"S1", 4, 6}, {"S1", 4, 7}, {"S2", 4, 5}, {"S4", -1, 10}, {"S4", -2, 12}});

            env.undeployAll();
        }
    }

    private static class RowRecogUnlimitedPartition implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  partition by value" +
                "  measures A.theString as a_string " +
                "  pattern (A B) " +
                "  define " +
                "    A as (A.theString = 'A')," +
                "    B as (B.theString = 'B')" +
                ")";

            env.compileDeploy(text).addListener("s0");

            for (int i = 0; i < 5 * RowRecogPartitionStateRepoGroup.INITIAL_COLLECTION_MIN; i++) {
                env.sendEventBean(new SupportRecogBean("A", i));
                env.sendEventBean(new SupportRecogBean("B", i));
                assertTrue(env.listener("s0").getAndClearIsInvoked());
            }

            env.milestone(0);

            for (int i = 0; i < 5 * RowRecogPartitionStateRepoGroup.INITIAL_COLLECTION_MIN; i++) {
                env.sendEventBean(new SupportRecogBean("A", i + 100000));
            }
            assertFalse(env.listener("s0").getAndClearIsInvoked());
            for (int i = 0; i < 5 * RowRecogPartitionStateRepoGroup.INITIAL_COLLECTION_MIN; i++) {
                env.sendEventBean(new SupportRecogBean("B", i + 100000));
                assertTrue(env.listener("s0").getAndClearIsInvoked());
            }

            env.undeployAll();
        }
    }

    private static class RowRecogConcatWithinAlter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,b_string,c_string,d_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string, C.theString as c_string, D.theString as d_string " +
                "  all matches pattern ( A B | C D ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)," +
                "    C as (C.value = 3)," +
                "    D as (D.value = 4)" +
                ")";
            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 3));
            env.sendEventBean(new SupportRecogBean("E2", 5));
            env.sendEventBean(new SupportRecogBean("E3", 4));
            env.sendEventBean(new SupportRecogBean("E4", 3));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E5", 4));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, null, "E4", "E5"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{null, null, "E4", "E5"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E1", 1));
            env.sendEventBean(new SupportRecogBean("E1", 1));
            env.sendEventBean(new SupportRecogBean("E2", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", "E2", null, null}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{null, null, "E4", "E5"}, {"E1", "E2", null, null}});

            env.undeployModuleContaining("s0");
        }
    }

    private static class RowRecogAlterWithinConcat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a_string,b_string,c_string,d_string".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string, C.theString as c_string, D.theString as d_string " +
                "  all matches pattern ( (A | B) (C | D) ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)," +
                "    C as (C.value = 3)," +
                "    D as (D.value = 4)" +
                ")";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 3));
            env.sendEventBean(new SupportRecogBean("E2", 1));
            env.sendEventBean(new SupportRecogBean("E3", 2));
            env.sendEventBean(new SupportRecogBean("E4", 5));
            env.sendEventBean(new SupportRecogBean("E5", 1));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E6", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5", null, "E6", null}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", null, "E6", null}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E7", 2));
            env.sendEventBean(new SupportRecogBean("E8", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{null, "E7", "E8", null}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", null, "E6", null}, {null, "E7", "E8", null}});

            env.undeployAll();
        }
    }

    private static class RowRecogVariableMoreThenOnce implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a0,b,a1".split(",");
            String text = "@name('s0') select * from SupportRecogBean#keepall " +
                "match_recognize (" +
                "  measures A[0].theString as a0, B.theString as b, A[1].theString as a1 " +
                "  all matches pattern ( A B A ) " +
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

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E7", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6", "E7"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E8", 2));
            env.sendEventBean(new SupportRecogBean("E9", 1));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}});

            env.sendEventBean(new SupportRecogBean("E10", 2));
            env.sendEventBean(new SupportRecogBean("E11", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E9", "E10", "E11"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}, {"E9", "E10", "E11"}});

            env.undeployAll();
        }
    }

    private static class RowRecogRegex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            assertTrue("aq".matches("^aq|^id"));
            assertTrue("id".matches("^aq|^id"));
            assertTrue("ad".matches("a(q|i)?d"));
            assertTrue("aqd".matches("a(q|i)?d"));
            assertTrue("aid".matches("a(q|i)?d"));
            assertFalse("aed".matches("a(q|i)?d"));
            assertFalse("a".matches("(a(b?)c)?"));
        }
    }
}
