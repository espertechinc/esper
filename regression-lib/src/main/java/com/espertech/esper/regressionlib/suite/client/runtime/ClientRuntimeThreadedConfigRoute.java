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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.util.SupportListenerTimerHRes;
import com.espertech.esper.runtime.client.DeploymentOptions;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class ClientRuntimeThreadedConfigRoute implements RegressionExecutionWithConfigure {
    private static final Logger log = LoggerFactory.getLogger(ClientRuntimeThreadedConfigRoute.class);

    public void configure(Configuration configuration) {
        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
        configuration.getCompiler().getExpression().setUdfCache(false);
        configuration.getRuntime().getThreading().setThreadPoolRouteExec(true);
        configuration.getRuntime().getThreading().setThreadPoolRouteExecNumThreads(5);
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
        configuration.getCommon().addImport(SupportStaticMethodLib.class.getName());
    }

    @Override
    public boolean enableHATest() {
        return false;
    }

    public void run(RegressionEnvironment env) {
        log.debug("Creating statements");
        int countStatements = 100;
        SupportListenerTimerHRes listener = new SupportListenerTimerHRes();
        EPCompiled compiled = env.compile("select SupportStaticMethodLib.sleep(10) from SupportBean");
        for (int i = 0; i < countStatements; i++) {
            final String stmtName = "s" + i;
            env.deploy(compiled, new DeploymentOptions().setStatementNameRuntime(ctx -> stmtName));
            env.statement(stmtName).addListener(listener);
        }

        log.info("Sending trigger event");
        long start = System.nanoTime();
        env.sendEventBean(new SupportBean());
        long end = System.nanoTime();
        long delta = (end - start) / 1000000;
        assertTrue("Delta is " + delta, delta < 100);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(100, listener.getNewEvents().size());
        listener.getNewEvents().clear();

        // destroy all statements
        env.undeployAll();

        env.compileDeploy("@name('s0') select SupportStaticMethodLib.sleep(10) from SupportBean, SupportBean");
        env.statement("s0").addListener(listener);
        env.sendEventBean(new SupportBean());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(1, listener.getNewEvents().size());

        env.undeployAll();
    }
}
