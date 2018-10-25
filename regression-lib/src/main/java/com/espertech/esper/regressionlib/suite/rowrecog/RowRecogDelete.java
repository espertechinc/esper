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

public class RowRecogDelete {

    // This test container is for
    //  (a) on-delete of events from a named window
    //  (b) a sorted window which also posts a remove stream that is out-of-order
    // ... also termed Out-Of-Sequence Delete (OOSD).
    //
    // The test is for out-of-sequence (and in-sequence) deletes:
    //  (1) Make sure that partial pattern matches get removed
    //  (2) Make sure that PREV is handled by order-of-arrival, and is not affected (by default) by delete (versus normal ordered remove stream).
    //      Since it is impossible to make guarantees as the named window could be entirely deleted, and "prev" depth is therefore unknown.
    //
    // Prev
    //    has OOSD
    //      update          PREV operates on original order-of-arrival; OOSD impacts matching: resequence only when partial matches deleted
    //      iterate         PREV operates on original order-of-arrival; OOSD impacts matching: iterator may present unseen-before matches after delete
    //    no OOSD
    //      update          PREV operates on original order-of-arrival; no resequencing when in-order deleted
    //      iterate         PREV operates on original order-of-arrival
    // No-Prev
    //    has OOSD
    //      update
    //      iterate
    //    no OOSD
    //      update
    //      iterate

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogNamedWindowOnDeleteOutOfSeq());
        execs.add(new RowRecogNamedWindowOutOfSequenceDelete());
        execs.add(new RowRecogNamedWindowInSequenceDelete());
        return execs;
    }

    private static class RowRecogNamedWindowOnDeleteOutOfSeq implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyNamedWindow#keepall as SupportRecogBean", path);
            env.compileDeploy("insert into MyNamedWindow select * from SupportRecogBean", path);
            env.compileDeploy("on SupportBean as d delete from MyNamedWindow w where d.intPrimitive = w.value", path);

            String[] fields = "a_string,b_string".split(",");
            String text = "@name('s0') select * from MyNamedWindow " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string" +
                "  all matches pattern (A B) " +
                "  define " +
                "    A as PREV(A.theString, 3) = 'P3' and PREV(A.theString, 2) = 'P2' and PREV(A.theString, 4) = 'P4'," +
                "    B as B.value in (PREV(B.value, 4), PREV(B.value, 2))" +
                ")";

            env.compileDeploy(text, path).addListener("s0");

            env.sendEventBean(new SupportRecogBean("P2", 1));
            env.sendEventBean(new SupportRecogBean("P1", 2));
            env.sendEventBean(new SupportRecogBean("P3", 3));
            env.sendEventBean(new SupportRecogBean("P4", 4));
            env.sendEventBean(new SupportRecogBean("P2", 1));
            env.sendEventBean(new SupportRecogBean("E1", 3));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("P4", 11));
            env.sendEventBean(new SupportRecogBean("P3", 12));
            env.sendEventBean(new SupportRecogBean("P2", 13));
            env.sendEventBean(new SupportRecogBean("xx", 4));
            env.sendEventBean(new SupportRecogBean("E2", -4));
            env.sendEventBean(new SupportRecogBean("E3", 12));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E2", "E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}});

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("P4", 21));
            env.sendEventBean(new SupportRecogBean("P3", 22));
            env.sendEventBean(new SupportRecogBean("P2", 23));
            env.sendEventBean(new SupportRecogBean("xx", -2));
            env.sendEventBean(new SupportRecogBean("E5", -1));
            env.sendEventBean(new SupportRecogBean("E6", -2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E5", "E6"}});

            env.milestone(2);

            // delete an PREV-referenced event: no effect as PREV is an order-of-arrival operator
            env.sendEventBean(new SupportBean("D1", 21));      // delete P4 of second batch
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}, {"E5", "E6"}});

            env.milestone(3);

            // delete an partial-match event
            env.sendEventBean(new SupportBean("D2", -1));      // delete E5 of second batch
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2", "E3"}});

            env.milestone(4);

            env.sendEventBean(new SupportBean("D3", 12));      // delete P3 and E3 of first batch
            assertFalse(env.statement("s0").iterator().hasNext());

            env.undeployAll();
        }
    }

    private static class RowRecogNamedWindowOutOfSequenceDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as SupportRecogBean", path);
            env.compileDeploy("insert into MyWindow select * from SupportRecogBean", path);
            env.compileDeploy("on SupportBean as s delete from MyWindow as w where s.theString = w.theString", path);

            String[] fields = "a0,a1,b0,b1,c".split(",");
            String text = "@name('s0') select * from MyWindow " +
                "match_recognize (" +
                "  measures A[0].theString as a0, A[1].theString as a1, B[0].theString as b0, B[1].theString as b1, C.theString as c" +
                "  pattern ( A+ B* C ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)," +
                "    C as (C.value = 3)" +
                ")";
            env.compileDeploy(text, path).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 1));
            env.sendEventBean(new SupportRecogBean("E2", 1));
            env.sendEventBean(new SupportBean("E2", 0));       // deletes E2
            env.sendEventBean(new SupportRecogBean("E3", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E1", null, null, null, "E3"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E1", null, null, null, "E3"}});

            env.milestone(0);

            env.sendEventBean(new SupportBean("E1", 0));       // deletes E1
            env.sendEventBean(new SupportBean("E4", 0));       // deletes E4

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E4", 1));
            env.sendEventBean(new SupportRecogBean("E5", 1));
            env.sendEventBean(new SupportBean("E4", 0));       // deletes E4
            env.sendEventBean(new SupportRecogBean("E6", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5", null, null, null, "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", null, null, null, "E6"}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E7", 1));
            env.sendEventBean(new SupportRecogBean("E8", 1));
            env.sendEventBean(new SupportRecogBean("E9", 2));
            env.sendEventBean(new SupportRecogBean("E10", 2));
            env.sendEventBean(new SupportRecogBean("E11", 2));
            env.sendEventBean(new SupportBean("E9", 0));       // deletes E9
            env.sendEventBean(new SupportRecogBean("E12", 3));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E7", "E8", "E10", "E11", "E12"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", null, null, null, "E6"}, {"E7", "E8", "E10", "E11", "E12"}});    // note interranking among per-event result

            env.milestone(3);

            env.sendEventBean(new SupportRecogBean("E13", 1));
            env.sendEventBean(new SupportRecogBean("E14", 1));
            env.sendEventBean(new SupportRecogBean("E15", 2));
            env.sendEventBean(new SupportRecogBean("E16", 2));
            env.sendEventBean(new SupportBean("E14", 0));       // deletes E14
            env.sendEventBean(new SupportBean("E15", 0));       // deletes E15
            env.sendEventBean(new SupportBean("E16", 0));       // deletes E16
            env.sendEventBean(new SupportBean("E13", 0));       // deletes E17
            env.sendEventBean(new SupportRecogBean("E18", 3));
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", null, null, null, "E6"}, {"E7", "E8", "E10", "E11", "E12"}});    // note interranking among per-event result

            env.undeployAll();
        }
    }

    private static class RowRecogNamedWindowInSequenceDelete implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as SupportRecogBean", path);
            env.compileDeploy("insert into MyWindow select * from SupportRecogBean", path);
            env.compileDeploy("on SupportBean as s delete from MyWindow as w where s.theString = w.theString", path);

            String[] fields = "a0,a1,b".split(",");
            String text = "@name('s0') select * from MyWindow " +
                "match_recognize (" +
                "  measures A[0].theString as a0, A[1].theString as a1, B.theString as b" +
                "  pattern ( A* B ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)" +
                ")";

            env.compileDeploy(text, path).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 1));
            env.sendEventBean(new SupportRecogBean("E2", 1));
            env.sendEventBean(new SupportBean("E1", 0));       // deletes E1
            env.sendEventBean(new SupportBean("E2", 0));       // deletes E2
            env.sendEventBean(new SupportRecogBean("E3", 3));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E4", 1));
            env.sendEventBean(new SupportRecogBean("E5", 1));
            env.sendEventBean(new SupportBean("E4", 0));       // deletes E4
            env.sendEventBean(new SupportRecogBean("E6", 1));
            env.sendEventBean(new SupportRecogBean("E7", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields,
                new Object[][]{{"E5", "E6", "E7"}});
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E5", "E6", "E7"}});

            env.undeployAll();
        }
    }
}