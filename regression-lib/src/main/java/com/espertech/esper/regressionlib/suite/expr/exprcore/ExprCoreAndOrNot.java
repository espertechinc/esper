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

public class ExprCoreAndOrNot implements RegressionExecution {
    private final static String[] FIELDS = "c0,c1,c2".split(",");

    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select " +
            "(intPrimitive=1) or (intPrimitive=2) as c0, " +
            "(intPrimitive>0) and (intPrimitive<3) as c1," +
            "not(intPrimitive=2) as c2" +
            " from SupportBean";
        env.compileDeploy(epl).addListener("s0");

        makeSendBeanAssert(env, 1, new Object[]{true, true, true});

        makeSendBeanAssert(env, 2, new Object[]{true, true, false});

        env.milestone(0);

        makeSendBeanAssert(env, 3, new Object[]{false, false, true});

        env.undeployAll();
    }

    private void makeSendBeanAssert(RegressionEnvironment env, int intPrimitive, Object[] expected) {
        SupportBean bean = new SupportBean("", intPrimitive);
        env.sendEventBean(bean);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), FIELDS, expected);
    }
}
