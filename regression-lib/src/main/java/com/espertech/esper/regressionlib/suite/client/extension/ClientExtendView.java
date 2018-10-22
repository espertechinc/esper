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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertFalse;

public class ClientExtendView implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        runAssertionPlugInViewTrend(env);
        runAssertionPlugInViewFlushed(env);
        runAssertionInvalid(env);
    }

    private void runAssertionPlugInViewFlushed(RegressionEnvironment env) {
        String text = "@name('s0') select * from SupportMarketDataBean.mynamespace:flushedsimple(price)";
        env.compileDeploy(text).addListener("s0");

        sendEvent(env, 1);
        sendEvent(env, 2);
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private void runAssertionPlugInViewTrend(RegressionEnvironment env) {
        String text = "@name('s0') select irstream * from SupportMarketDataBean.mynamespace:trendspotter(price)";
        env.compileDeploy(text).addListener("s0");

        sendEvent(env, 10);
        assertReceived(env, 1L, null);

        sendEvent(env, 11);
        assertReceived(env, 2L, 1L);

        sendEvent(env, 12);
        assertReceived(env, 3L, 2L);

        sendEvent(env, 11);
        assertReceived(env, 0L, 3L);

        sendEvent(env, 12);
        assertReceived(env, 1L, 0L);

        sendEvent(env, 0);
        assertReceived(env, 0L, 1L);

        sendEvent(env, 0);
        assertReceived(env, 0L, 0L);

        sendEvent(env, 1);
        assertReceived(env, 1L, 0L);

        sendEvent(env, 1);
        assertReceived(env, 1L, 1L);

        sendEvent(env, 2);
        assertReceived(env, 2L, 1L);

        sendEvent(env, 2);
        assertReceived(env, 2L, 2L);

        env.undeployAll();
    }

    private void runAssertionInvalid(RegressionEnvironment env) {
        tryInvalidCompile(env, "select * from SupportMarketDataBean.mynamespace:xxx()",
            "Failed to validate data window declaration: View name 'mynamespace:xxx' is not a known view name");
        tryInvalidCompile(env, "select * from SupportMarketDataBean.mynamespace:invalid()",
            "Failed to validate data window declaration: Error casting view factory instance to " + ViewFactoryForge.class.getName() + " interface for view 'invalid'");
    }

    private void sendEvent(RegressionEnvironment env, double price) {
        env.sendEventBean(new SupportMarketDataBean("", price, null, null));
    }

    private void assertReceived(RegressionEnvironment env, Long newTrendCount, Long oldTrendCount) {
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").assertInvokedAndReset(), "trendcount", new Object[]{newTrendCount}, new Object[]{oldTrendCount});
    }
}
