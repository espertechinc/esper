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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class EPLJoinStartStop {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinStartStopSceneOne());
        execs.add(new EPLJoinInvalidJoin());
        return execs;
    }

    private static class EPLJoinStartStopSceneOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select * from " +
                "SupportMarketDataBean(symbol='IBM')#length(3) s0, " +
                "SupportMarketDataBean(symbol='CSCO')#length(3) s1" +
                " where s0.volume=s1.volume";
            env.compileDeployAddListenerMileZero(joinStatement, "s0");

            Object[] setOne = new Object[5];
            Object[] setTwo = new Object[5];
            long[] volumesOne = new long[]{10, 20, 20, 40, 50};
            long[] volumesTwo = new long[]{10, 20, 30, 40, 50};
            for (int i = 0; i < setOne.length; i++) {
                setOne[i] = new SupportMarketDataBean("IBM", volumesOne[i], (long) i, "");
                setTwo[i] = new SupportMarketDataBean("CSCO", volumesTwo[i], (long) i, "");
            }

            sendEvent(env, setOne[0]);
            sendEvent(env, setTwo[0]);
            assertNotNull(env.listener("s0").getLastNewData());
            env.listener("s0").reset();

            SupportListener listener = env.listener("s0");
            env.undeployAll();
            sendEvent(env, setOne[1]);
            sendEvent(env, setTwo[1]);
            assertFalse(listener.isInvoked());

            env.compileDeploy(joinStatement).addListener("s0");
            sendEvent(env, setOne[2]);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
            sendEvent(env, setOne[3]);
            sendEvent(env, setOne[4]);
            sendEvent(env, setTwo[3]);

            env.compileDeploy(joinStatement).addListener("s0");
            sendEvent(env, setTwo[4]);
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLJoinInvalidJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String invalidJoin = "select * from SupportBean_A, SupportBean_B";
            tryInvalidCompile(env, invalidJoin,
                "Joins require that at least one view is specified for each stream, no view was specified for SupportBean_A");

            invalidJoin = "select * from SupportBean_A#time(5 min), SupportBean_B";
            tryInvalidCompile(env, invalidJoin,
                "Joins require that at least one view is specified for each stream, no view was specified for SupportBean_B");

            invalidJoin = "select * from SupportBean_A#time(5 min), pattern[SupportBean_A->SupportBean_B]";
            tryInvalidCompile(env, invalidJoin,
                "Joins require that at least one view is specified for each stream, no view was specified for pattern event stream");
        }
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }
}
