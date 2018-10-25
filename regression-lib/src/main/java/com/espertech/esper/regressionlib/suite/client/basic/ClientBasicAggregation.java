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

public class ClientBasicAggregation implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select count(*) as cnt from SupportBean";
        env.compileDeployAddListenerMileZero(epl, "s0");

        sendAssert(env, 1);

        env.milestone(1);

        sendAssert(env, 2);

        env.milestone(2);

        sendAssert(env, 3);

        env.undeployAll();
    }

    private void sendAssert(RegressionEnvironment env, long expected) {
        env.sendEventBean(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "cnt".split(","), new Object[]{expected});
    }
}
