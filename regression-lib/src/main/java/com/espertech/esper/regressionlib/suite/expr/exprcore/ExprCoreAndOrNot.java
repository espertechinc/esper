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
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExprCoreAndOrNot {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreAndOrNotCombined());
        executions.add(new ExprCoreNotWithVariable());
        return executions;
    }

    private static class ExprCoreNotWithVariable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create variable string thing = \"Hello World\";" +
                "@name('s0') select not thing.contains(theString) as c0 from SupportBean;\n";
            env.compileDeploy(epl).addListener("s0");

            sendBeanAssert(env, "World", false);
            sendBeanAssert(env, "x", true);

            Map<DeploymentIdNamePair, Object> newValues = new HashMap<>();
            newValues.put(new DeploymentIdNamePair(env.deploymentId("s0"), "thing"), "5 x 5");
            env.runtime().getVariableService().setVariableValue(newValues);

            sendBeanAssert(env, "World", true);
            sendBeanAssert(env, "x", false);

            env.undeployAll();
        }
    }

    private static class ExprCoreAndOrNotCombined implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "(intPrimitive=1) or (intPrimitive=2) as c0, " +
                "(intPrimitive>0) and (intPrimitive<3) as c1," +
                "not(intPrimitive=2) as c2" +
                " from SupportBean";
            env.compileDeploy(epl).addListener("s0");

            sendBeanAssert(env, 1, new Object[]{true, true, true});

            sendBeanAssert(env, 2, new Object[]{true, true, false});

            env.milestone(0);

            sendBeanAssert(env, 3, new Object[]{false, false, true});

            env.undeployAll();
        }
    }

    private static void sendBeanAssert(RegressionEnvironment env, int intPrimitive, Object[] expected) {
        SupportBean bean = new SupportBean("", intPrimitive);
        env.sendEventBean(bean);
        final String[] FIELDS = "c0,c1,c2".split(",");
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, expected);
    }

    private static void sendBeanAssert(RegressionEnvironment env, String theString, boolean expected) {
        SupportBean bean = new SupportBean(theString, 0);
        env.sendEventBean(bean);
        assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
    }
}
