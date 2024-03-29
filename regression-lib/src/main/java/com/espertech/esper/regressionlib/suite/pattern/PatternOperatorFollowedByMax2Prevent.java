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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.regressionlib.suite.pattern.PatternOperatorFollowedByMax4Prevent.assertContextEnginePool;
import static com.espertech.esper.regressionlib.suite.pattern.PatternOperatorFollowedByMax4Prevent.getExpectedCountMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternOperatorFollowedByMax2Prevent implements RegressionExecution {
    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.STATICHOOK);
    }

    public void run(RegressionEnvironment env) {
        AtomicInteger milestone = new AtomicInteger();
        String expression = "@Name('A') select a.id as a, b.id as b from pattern [every a=SupportBean_A -> b=SupportBean_B]";
        env.compileDeploy(expression).addListener("A");

        env.sendEventBean(new SupportBean_A("A1"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_A("A2"));

        SupportConditionHandlerFactory.getLastHandler().getContexts().clear();
        env.sendEventBean(new SupportBean_A("A3"));
        assertContextEnginePool(env, env.statement("A"), SupportConditionHandlerFactory.getLastHandler().getContexts(), 2, getExpectedCountMap("A", 2));

        env.milestoneInc(milestone);

        String[] fields = new String[]{"a", "b"};
        env.sendEventBean(new SupportBean_B("B1"));
        EPAssertionUtil.assertPropsPerRow(env.listener("A").getAndResetLastNewData(), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}});

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_A("A4"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_B("B2"));
        EPAssertionUtil.assertPropsPerRow(env.listener("A").getAndResetLastNewData(), fields, new Object[][]{{"A4", "B2"}});
        assertTrue(SupportConditionHandlerFactory.getLastHandler().getContexts().isEmpty());

        env.milestoneInc(milestone);

        for (int i = 5; i < 9; i++) {
            env.sendEventBean(new SupportBean_A("A" + i));
            if (i >= 7) {
                assertContextEnginePool(env, env.statement("A"), SupportConditionHandlerFactory.getLastHandler().getContexts(), 2, getExpectedCountMap("A", 2));
            }
        }

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_B("B3"));
        EPAssertionUtil.assertPropsPerRow(env.listener("A").getAndResetLastNewData(), fields, new Object[][]{{"A5", "B3"}, {"A6", "B3"}});

        env.sendEventBean(new SupportBean_B("B4"));
        assertFalse(env.listener("A").isInvoked());

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_A("A20"));
        env.sendEventBean(new SupportBean_A("A21"));

        env.milestoneInc(milestone);

        env.sendEventBean(new SupportBean_B("B5"));
        EPAssertionUtil.assertPropsPerRow(env.listener("A").getAndResetLastNewData(), fields, new Object[][]{{"A20", "B5"}, {"A21", "B5"}});
        assertTrue(SupportConditionHandlerFactory.getLastHandler().getContexts().isEmpty());

        env.undeployAll();
    }
}
