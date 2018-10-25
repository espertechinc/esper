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
package com.espertech.esper.regressionlib.suite.resultset.aggregate;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertFalse;

public class ResultSetAggregateNTh implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        AtomicInteger milestone = new AtomicInteger();

        String epl = "@name('s0') select " +
            "theString, " +
            "nth(intPrimitive,0) as int1, " +  // current
            "nth(intPrimitive,1) as int2 " +   // one before
            "from SupportBean#keepall group by theString output last every 3 events order by theString";
        env.compileDeploy(epl).addListener("s0");

        runAssertion(env, milestone);

        env.milestoneInc(milestone);
        env.undeployAll();

        env.eplToModelCompileDeploy(epl).addListener("s0");

        runAssertion(env, milestone);

        env.undeployAll();

        tryInvalidCompile(env, "select nth() from SupportBean",
            "Failed to validate select-clause expression 'nth(*)': The nth aggregation function requires two parameters, an expression returning aggregation values and a numeric index constant [select nth() from SupportBean]");
    }

    private static void runAssertion(RegressionEnvironment env, AtomicInteger milestone) {
        String[] fields = "theString,int1,int2".split(",");

        env.sendEventBean(new SupportBean("G1", 10));
        env.sendEventBean(new SupportBean("G2", 11));
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("G1", 12));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"G1", 12, 10}, {"G2", 11, null}});

        env.sendEventBean(new SupportBean("G2", 30));
        env.sendEventBean(new SupportBean("G2", 20));
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("G2", 25));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"G2", 25, 20}});

        env.sendEventBean(new SupportBean("G1", -1));
        env.sendEventBean(new SupportBean("G1", -2));
        assertFalse(env.listener("s0").isInvoked());

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean("G2", 8));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"G1", -2, -1}, {"G2", 8, 25}});
    }
}