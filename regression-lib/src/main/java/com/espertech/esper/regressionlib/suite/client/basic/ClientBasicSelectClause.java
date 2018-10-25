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
package com.espertech.esper.regressionlib.suite.client.basic;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

public class ClientBasicSelectClause implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select intPrimitive from SupportBean";
        env.compileDeployAddListenerMileZero(epl, "s0");

        env.sendEventBean(new SupportBean("E1", 10));

        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "intPrimitive".split(","), new Object[]{10});

        env.undeployAll();
    }
}
