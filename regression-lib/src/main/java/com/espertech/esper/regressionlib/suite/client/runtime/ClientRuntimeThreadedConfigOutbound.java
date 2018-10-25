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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.util.SupportListenerSleeping;
import org.junit.Assert;

import static org.junit.Assert.assertTrue;

public class ClientRuntimeThreadedConfigOutbound implements RegressionExecutionWithConfigure {
    public void configure(Configuration configuration) {
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getCompiler().getExpression().setUdfCache(false);
        configuration.getRuntime().getThreading().setThreadPoolOutbound(true);
        configuration.getRuntime().getThreading().setThreadPoolOutboundNumThreads(5);
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
    }

    public void run(RegressionEnvironment env) {
        SupportListenerSleeping listener = new SupportListenerSleeping(200);
        env.compileDeploy("@name('s0') select * from SupportBean").statement("s0").addListener(listener);

        long start = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            env.sendEventBean(new SupportBean());
        }
        long end = System.nanoTime();
        long delta = (end - start) / 1000000;
        assertTrue("Delta is " + delta, delta < 100);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(5, listener.getNewEvents().size());

        env.undeployAll();
    }
}
