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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class EPLJoin2StreamInKeywordPerformance {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinInKeywordSingleIndexLookup());
        execs.add(new EPLJoinInKeywordMultiIndexLookup());
        return execs;
    }

    private static class EPLJoinInKeywordSingleIndexLookup implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive as val from SupportBean#keepall sb, SupportBean_S0 s0 unidirectional " +
                "where sb.theString in (s0.p00, s0.p01)";
            String[] fields = "val".split(",");
            env.compileDeployAddListenerMileZero(epl, "s0");

            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean("E" + i, i));
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBean_S0(1, "E645", "E8975"));
                EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{645}, {8975}});
            }
            long delta = System.currentTimeMillis() - startTime;
            assertTrue("delta=" + delta, delta < 500);
            log.info("delta=" + delta);

            env.undeployAll();
        }
    }

    private static class EPLJoinInKeywordMultiIndexLookup implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select id as val from SupportBean_S0#keepall s0, SupportBean sb unidirectional " +
                "where sb.theString in (s0.p00, s0.p01)";
            String[] fields = "val".split(",");
            env.compileDeployAddListenerMileZero(epl, "s0");

            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean_S0(i, "p00_" + i, "p01_" + i));
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                env.sendEventBean(new SupportBean("p01_645", 0));
                EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{645});
            }
            long delta = System.currentTimeMillis() - startTime;
            assertTrue("delta=" + delta, delta < 500);
            log.info("delta=" + delta);

            env.undeployAll();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(EPLJoin2StreamInKeywordPerformance.class);
}