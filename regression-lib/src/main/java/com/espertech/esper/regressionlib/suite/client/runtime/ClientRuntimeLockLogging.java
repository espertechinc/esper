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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.framework.RegressionFlag;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;

public class ClientRuntimeLockLogging implements RegressionExecutionWithConfigure {
    public void configure(Configuration configuration) {
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
        configuration.getRuntime().getLogging().setEnableLockActivity(true);
    }

    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.RUNTIMEOPS);
    }

    public void run(RegressionEnvironment env) {
        runAssertionLockLogging(env);
    }

    private void runAssertionLockLogging(RegressionEnvironment env) {
        String epl = "@Name('s0') select count(*) as c0 from SupportBean";
        env.compileDeploy(epl);

        env.sendEventBean(new SupportBean());
        env.assertSafeIterator("s0", it -> {
            assertEquals(1L, it.next().get("c0"));
            it.close();
        });

        env.undeployAll();
    }
}
