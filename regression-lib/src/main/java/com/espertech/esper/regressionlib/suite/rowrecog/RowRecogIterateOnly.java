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
import static org.junit.Assert.assertTrue;

public class RowRecogIterateOnly {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new RowRecogNoListenerMode());
        execs.add(new RowRecogPrev());
        execs.add(new RowRecogPrevPartitioned());
        return execs;
    }

    private static class RowRecogNoListenerMode implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a".split(",");
            String text = "@name('s0') @Hint('iterate_only') select * from SupportRecogBean#length(1) " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  all matches " +
                "  pattern (A) " +
                "  define A as SupportStaticMethodLib.sleepReturnTrue(mySleepDuration)" +
                ")";

            env.compileDeploy(text).addListener("s0");

            // this should not block
            long start = System.currentTimeMillis();
            for (int i = 0; i < 50; i++) {
                env.sendEventBean(new SupportRecogBean("E1", 1));
            }
            long end = System.currentTimeMillis();
            assertTrue((end - start) <= 100);
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportRecogBean("E2", 2));
            env.runtime().getVariableService().setVariableValue(null, "mySleepDuration", 0);
            assertFalse(env.listener("s0").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E2"}});

            env.undeployAll();
        }
    }

    private static class RowRecogPrev implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a".split(",");
            String text = "@Hint('iterate_only') @name('s0') select * from SupportRecogBean#lastevent " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  all matches " +
                "  pattern (A) " +
                "  define A as prev(A.value, 2) = value" +
                ")";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", 1));
            env.sendEventBean(new SupportRecogBean("E2", 2));

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E3", 3));
            env.sendEventBean(new SupportRecogBean("E4", 4));
            env.sendEventBean(new SupportRecogBean("E5", 2));
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E6", 4));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E6"}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E7", 2));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E7"}});
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class RowRecogPrevPartitioned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a,cat".split(",");
            String text = "@name('s0') @Hint('iterate_only') select * from SupportRecogBean#lastevent " +
                "match_recognize (" +
                "  partition by cat" +
                "  measures A.theString as a, A.cat as cat" +
                "  all matches " +
                "  pattern (A) " +
                "  define A as prev(A.value, 2) = value" +
                ")";

            env.compileDeploy(text).addListener("s0");

            env.sendEventBean(new SupportRecogBean("E1", "A", 1));
            env.sendEventBean(new SupportRecogBean("E2", "B", 1));

            env.milestone(0);

            env.sendEventBean(new SupportRecogBean("E3", "B", 3));
            env.sendEventBean(new SupportRecogBean("E4", "A", 4));
            env.sendEventBean(new SupportRecogBean("E5", "B", 2));
            assertFalse(env.statement("s0").iterator().hasNext());

            env.milestone(1);

            env.sendEventBean(new SupportRecogBean("E6", "A", 1));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E6", "A"}});

            env.milestone(2);

            env.sendEventBean(new SupportRecogBean("E7", "B", 3));
            EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), fields,
                new Object[][]{{"E7", "B"}});
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }
}
