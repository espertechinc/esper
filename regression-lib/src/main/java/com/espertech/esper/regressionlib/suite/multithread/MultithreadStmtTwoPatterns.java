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
package com.espertech.esper.regressionlib.suite.multithread;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil;
import com.espertech.esper.regressionlib.support.multithread.TwoPatternRunnable;
import com.espertech.esper.runtime.client.EPDeployment;

import static org.junit.Assert.assertFalse;

/**
 * Test for multithread-safety for case of 2 patterns:
 * 1. Thread 1 starts pattern "every event1=SupportTradeEvent(userID in ('100','101'), amount>=1000)"
 * 2. Thread 1 repeats sending 100 events and tests 5% received
 * 3. Main thread starts pattern:
 * ( every event1=SupportTradeEvent(userID in ('100','101')) ->
 * (SupportTradeEvent(userID in ('100','101'), direction = event1.direction ) ->
 * SupportTradeEvent(userID in ('100','101'), direction = event1.direction )
 * ) where timer:within(8 hours)
 * and not eventNC=SupportTradeEvent(userID in ('100','101'), direction!= event1.direction )
 * ) -> eventFinal=SupportTradeEvent(userID in ('100','101'), direction != event1.direction ) where timer:within(1 hour)
 * 4. Main thread waits for 2 seconds and stops all threads
 */
public class MultithreadStmtTwoPatterns implements RegressionExecutionWithConfigure {

    public void configure(Configuration configuration) {
    }

    public boolean haWithCOnly() {
        return true;
    }

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        String statementTwo = "select * from pattern[( every event1=SupportTradeEvent(userId in ('100','101')) ->\n" +
            "         (SupportTradeEvent(userId in ('100','101'), direction = event1.direction ) ->\n" +
            "          SupportTradeEvent(userId in ('100','101'), direction = event1.direction )\n" +
            "         ) where timer:within(8 hours)\n" +
            "         and not eventNC=SupportTradeEvent(userId in ('100','101'), direction!= event1.direction )\n" +
            "        ) -> eventFinal=SupportTradeEvent(userId in ('100','101'), direction != event1.direction ) where timer:within(1 hour)]";
        EPCompiled compiledTwo = env.compile(statementTwo);

        TwoPatternRunnable runnable = new TwoPatternRunnable(env);
        Thread t = new Thread(runnable, MultithreadStmtTwoPatterns.class.getSimpleName());
        t.start();
        SupportCompileDeployUtil.threadSleep(100);

        // Create a second pattern, wait 500 msec, destroy second pattern in a loop
        int numRepeats = env.isHA() ? 1 : 10;
        for (int i = 0; i < numRepeats; i++) {
            try {
                EPDeployment deployed = env.deployment().deploy(compiledTwo);
                SupportCompileDeployUtil.threadSleep(200);
                env.undeploy(deployed.getDeploymentId());
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        runnable.setShutdown(true);
        SupportCompileDeployUtil.threadSleep(1000);
        assertFalse(t.isAlive());

        env.undeployAll();
    }
}
