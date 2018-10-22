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
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.util.SupportListenerTimerHRes;
import com.espertech.esper.runtime.client.DeploymentOptions;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.junit.Assert.assertTrue;

public class ClientRuntimeThreadedConfigTimer implements RegressionExecutionWithConfigure {
    private static final Logger log = LoggerFactory.getLogger(ClientRuntimeThreadedConfigTimer.class);

    @Override
    public boolean enableHATest() {
        return false;
    }

    public void configure(Configuration configuration) {
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getCompiler().getExpression().setUdfCache(false);
        configuration.getRuntime().getThreading().setThreadPoolTimerExec(true);
        configuration.getRuntime().getThreading().setThreadPoolTimerExecNumThreads(5);
        configuration.getCommon().addEventType("MyMap", new HashMap<>());
        configuration.getCommon().addImport(SupportStaticMethodLib.class.getName());
    }

    public void run(RegressionEnvironment env) {
        sendTimer(0, env);

        log.debug("Creating statements");
        int countStatements = 100;
        SupportListenerTimerHRes listener = new SupportListenerTimerHRes();
        EPCompiled compiled = env.compile("select SupportStaticMethodLib.sleep(10) from pattern[every MyMap -> timer:interval(1)]");
        for (int i = 0; i < countStatements; i++) {
            final String stmtName = "s" + i;
            env.deploy(compiled, new DeploymentOptions().setStatementNameRuntime(ctx -> stmtName));
            env.statement(stmtName).addListener(listener);
        }

        log.info("Sending trigger event");
        env.sendEventMap(new HashMap<>(), "MyMap");

        long start = System.nanoTime();
        sendTimer(1000, env);
        long end = System.nanoTime();
        long delta = (end - start) / 1000000;
        assertTrue("Delta is " + delta, delta < 100);

        // wait for delivery
        while (true) {
            int countDelivered = listener.getNewEvents().size();
            if (countDelivered == countStatements) {
                break;
            }

            log.info("Delivered " + countDelivered + ", waiting for more");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Assert.assertEquals(100, listener.getNewEvents().size());
        // analyze result
        //List<Pair<Long, EventBean[]>> events = listener.getNewEvents();
        //OccuranceResult result = OccuranceAnalyzer.analyze(events, new long[] {100 * 1000 * 1000L, 10*1000 * 1000L});
        //log.info(result);
    }

    private void sendTimer(long timeInMSec, RegressionEnvironment env) {
        env.advanceTime(timeInMSec);
    }
}
