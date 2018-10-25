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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertEquals;
import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertSame;

public class ClientBasicLengthWindow implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@name('s0') select irstream * from SupportBean#length(2)";
        env.compileDeploy(epl).addListener("s0");

        SupportBean sb0 = sendAssertNoRStream(env, "E1");

        SupportBean sb1 = sendAssertNoRStream(env, "E2");

        env.milestone(1);

        sendAssertIR(env, "E3", sb0);

        env.milestone(2);

        sendAssertIR(env, "E4", sb1);

        env.undeployAll();
    }

    private void sendAssertIR(RegressionEnvironment env, String theString, SupportBean rstream) {
        SupportBean sb = sendBean(env, theString);
        UniformPair<EventBean> pair = env.listener("s0").assertPairGetIRAndReset();
        assertEquals(rstream, pair.getSecond().getUnderlying());
        assertSame(sb, pair.getFirst().getUnderlying());
    }

    private SupportBean sendAssertNoRStream(RegressionEnvironment env, String theString) {
        SupportBean sb = sendBean(env, theString);
        assertSame(sb, env.listener("s0").assertOneGetNewAndReset().getUnderlying());
        return sb;
    }

    private SupportBean sendBean(RegressionEnvironment env, String theString) {
        SupportBean sb = new SupportBean(theString, 0);
        env.sendEventBean(sb);
        return sb;
    }
}
