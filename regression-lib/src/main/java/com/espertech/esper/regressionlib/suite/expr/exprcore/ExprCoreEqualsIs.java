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
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExprCoreEqualsIs {

    public static Collection<RegressionExecution> executions() {
        List<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreEqualsIsCoercion());
        executions.add(new ExprCoreEqualsIsCoercionSameType());
        return executions;
    }

    private static class ExprCoreEqualsIsCoercion implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive=longPrimitive as c0, intPrimitive is longPrimitive as c1 from SupportBean";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "c0,c1".split(",");

            makeSendBean(env, 1, 1L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true});

            makeSendBean(env, 1, 2L);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, false});

            env.undeployAll();
        }
    }

    private static class ExprCoreEqualsIsCoercionSameType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select p00 = p01 as c0, id = id as c1, p02 is not null as c2 from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");
            String[] fields = "c0,c1,c2".split(",");

            env.sendEventBean(new SupportBean_S0(1, "a", "a", "a"));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{true, true, true});

            env.sendEventBean(new SupportBean_S0(1, "a", "b", null));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{false, true, false});

            env.undeployAll();
        }
    }

    private static void makeSendBean(RegressionEnvironment env, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        env.sendEventBean(bean);
    }
}
