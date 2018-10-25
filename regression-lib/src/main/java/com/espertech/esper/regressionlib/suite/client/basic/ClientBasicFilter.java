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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

public class ClientBasicFilter implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select * from SupportBean(intPrimitive = 1)";
        env.compileDeployAddListenerMileZero(epl, "s0");

        sendAssert(env, 1, true);
        sendAssert(env, 0, false);

        env.milestone(1);

        sendAssert(env, 1, true);
        sendAssert(env, 0, false);

        env.undeployAll();
    }

    private void sendAssert(RegressionEnvironment env, int intPrimitive, boolean expected) {
        env.sendEventBean(new SupportBean("E", intPrimitive))
            .listener("s0").assertInvokedFlagAndReset(expected);
    }
}
