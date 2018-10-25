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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ExprCoreMathDivisionRules {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreMathRulesBigInt());
        executions.add(new ExprCoreMathRulesLong());
        executions.add(new ExprCoreMathRulesFloat());
        executions.add(new ExprCoreMathRulesDouble());
        executions.add(new ExprCoreMathRulesInt());
        return executions;
    }

    public static class ExprCoreMathRulesBigInt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select BigInteger.valueOf(4)/BigInteger.valueOf(2) as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(BigInteger.class, env.statement("s0").getEventType().getPropertyType("c0"));

            String[] fields = "c0".split(",");
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{BigInteger.valueOf(4).divide(BigInteger.valueOf(2))});

            env.undeployAll();
        }
    }

    public static class ExprCoreMathRulesLong implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select 10L/2L as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Long.class, env.statement("s0").getEventType().getPropertyType("c0"));

            String[] fields = "c0".split(",");
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5L});

            env.undeployAll();
        }
    }

    public static class ExprCoreMathRulesFloat implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select 10f/2f as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Float.class, env.statement("s0").getEventType().getPropertyType("c0"));

            String[] fields = "c0".split(",");
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{5f});

            env.undeployAll();
        }
    }

    public static class ExprCoreMathRulesDouble implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select 10d/0d as c0 from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            String[] fields = "c0".split(",");
            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{null});

            env.undeployAll();
        }
    }

    public static class ExprCoreMathRulesInt implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive/intBoxed as result from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("result"));

            sendEvent(env, 100, 3);
            assertEquals(33, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEvent(env, 100, null);
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("result"));

            sendEvent(env, 100, 0);
            assertEquals(null, env.listener("s0").assertOneGetNewAndReset().get("result"));

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, Integer intPrimitive, Integer intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }
}
