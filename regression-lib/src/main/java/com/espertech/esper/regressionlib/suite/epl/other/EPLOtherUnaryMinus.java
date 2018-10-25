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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static org.junit.Assert.assertEquals;

public class EPLOtherUnaryMinus implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        env.compileDeploy("create variable double v = 1.0;\n;" +
            "@name('s0') select -intPrimitive as c0, -v as c1 from SupportBean;\n").addListener("s0");

        env.sendEventBean(new SupportBean("E1", 10));

        assertEquals(1d, env.runtime().getVariableService().getVariableValue(env.deploymentId("s0"), "v"));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{-10, -1d});

        env.undeployAll();
    }
}
