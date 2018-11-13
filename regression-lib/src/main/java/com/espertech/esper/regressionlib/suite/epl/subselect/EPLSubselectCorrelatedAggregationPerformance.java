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
package com.espertech.esper.regressionlib.suite.epl.subselect;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class EPLSubselectCorrelatedAggregationPerformance implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        String stmtText = "@name('s0') select p00, " +
            "(select sum(intPrimitive) from SupportBean#keepall where theString = s0.p00) as sump00 " +
            "from SupportBean_S0 as s0";
        env.compileDeploy(stmtText).addListener("s0");

        String[] fields = "p00,sump00".split(",");

        // preload
        int max = 50000;
        for (int i = 0; i < max; i++) {
            env.sendEventBean(new SupportBean("T" + i, -i));
            env.sendEventBean(new SupportBean("T" + i, 10));
        }

        // exercise
        long start = System.currentTimeMillis();
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            int index = random.nextInt(max);
            env.sendEventBean(new SupportBean_S0(0, "T" + index));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"T" + index, -index + 10});
        }
        long end = System.currentTimeMillis();
        long delta = end - start;

        //System.out.println("delta=" + delta);
        assertTrue("delta=" + delta, delta < 500);

        env.undeployAll();
    }
}
