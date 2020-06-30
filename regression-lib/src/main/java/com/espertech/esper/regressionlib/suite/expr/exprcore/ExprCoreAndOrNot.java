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

import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypesAllSame;
import static org.junit.Assert.assertEquals;

public class ExprCoreAndOrNot {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreAndOrNotCombined());
        executions.add(new ExprCoreNotWithVariable());
        executions.add(new ExprCoreAndOrNotNull());
        return executions;
    }

    private static class ExprCoreAndOrNotNull implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expression(fields[0], "cast(null, boolean) and cast(null, boolean)")
                .expression(fields[1], "boolPrimitive and boolBoxed")
                .expression(fields[2], "boolBoxed and boolPrimitive")
                .expression(fields[3], "boolPrimitive or boolBoxed")
                .expression(fields[4], "boolBoxed or boolPrimitive")
                .expression(fields[5], "not boolBoxed");
            builder.statementConsumer(stmt -> assertTypesAllSame(stmt.getEventType(), fields, EPTypePremade.BOOLEANBOXED.getEPType()));
            builder.assertion(makeSB("E1", true, true)).expect(fields, null, true, true, true, true, false);
            builder.assertion(makeSB("E2", false, false)).expect(fields, null, false, false, false, false, true);
            builder.assertion(makeSB("E3", false, null)).expect(fields, null, false, false, null, null, null);
            builder.assertion(makeSB("E4", true, null)).expect(fields, null, null, null, true, true, null);
            builder.assertion(makeSB("E5", true, false)).expect(fields, null, false, false, true, true, true);
            builder.assertion(makeSB("E6", false, true)).expect(fields, null, false, false, true, true, false);
            builder.run(env);
            env.undeployAll();
        }

        private SupportBean makeSB(String theString, boolean boolPrimitive, Boolean boolBoxed) {
            SupportBean sb = new SupportBean(theString, 0);
            sb.setBoolPrimitive(boolPrimitive);
            sb.setBoolBoxed(boolBoxed);
            return sb;
        }
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
            String[] fields = "c0,c1,c2".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean")
                .expressions(fields, "(intPrimitive=1) or (intPrimitive=2)", "(intPrimitive>0) and (intPrimitive<3)",
                    "not(intPrimitive=2)");
            builder.assertion(new SupportBean("E1", 1)).expect(fields, true, true, true);
            builder.assertion(new SupportBean("E2", 2)).expect(fields, true, true, false);
            builder.assertion(new SupportBean("E3", 3)).expect(fields, false, false, true);
            builder.run(env);
            env.undeployAll();
        }
    }

    private static void sendBeanAssert(RegressionEnvironment env, String theString, boolean expected) {
        SupportBean bean = new SupportBean(theString, 0);
        env.sendEventBean(bean);
        assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
    }
}
