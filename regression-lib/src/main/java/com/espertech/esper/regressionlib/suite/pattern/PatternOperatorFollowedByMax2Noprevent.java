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
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;

import static com.espertech.esper.regressionlib.suite.pattern.PatternOperatorFollowedByMax4Prevent.assertContextEnginePool;
import static com.espertech.esper.regressionlib.suite.pattern.PatternOperatorFollowedByMax4Prevent.getExpectedCountMap;

public class PatternOperatorFollowedByMax2Noprevent implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        SupportConditionHandlerFactory.SupportConditionHandler handler = SupportConditionHandlerFactory.getLastHandler();

        String expression = "@Name('A') select a.id as a, b.id as b from pattern [every a=SupportBean_A -> b=SupportBean_B]";
        env.compileDeploy(expression).addListener("A");

        env.sendEventBean(new SupportBean_A("A1"));
        env.sendEventBean(new SupportBean_A("A2"));

        handler.getContexts().clear();
        env.sendEventBean(new SupportBean_A("A3"));
        assertContextEnginePool(env, env.statement("A"), handler.getContexts(), 2, getExpectedCountMap("A", 2));

        handler.getContexts().clear();
        env.sendEventBean(new SupportBean_A("A4"));
        assertContextEnginePool(env, env.statement("A"), handler.getContexts(), 2, getExpectedCountMap("A", 3));

        String[] fields = new String[]{"a", "b"};
        env.sendEventBean(new SupportBean_B("B1"));
        EPAssertionUtil.assertPropsPerRow(env.listener("A").getAndResetLastNewData(), fields, new Object[][]{{"A1", "B1"}, {"A2", "B1"}, {"A3", "B1"}, {"A4", "B1"}});

        env.undeployAll();
    }
}
