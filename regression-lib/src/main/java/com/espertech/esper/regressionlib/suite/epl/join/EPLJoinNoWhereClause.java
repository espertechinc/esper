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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EPLJoinNoWhereClause {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLJoinJoinWInnerKeywordWOOnClause());
        execs.add(new EPLJoinJoinNoWhereClause());
        return execs;
    }

    private static class EPLJoinJoinWInnerKeywordWOOnClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "a.theString,b.theString".split(",");
            String epl = "@name('s0') select * from SupportBean(theString like 'A%')#length(3) as a inner join SupportBean(theString like 'B%')#length(3) as b " +
                "where a.intPrimitive = b.intPrimitive";
            env.compileDeploy(epl).addListener("s0");

            sendEvent(env, "A1", 1);
            sendEvent(env, "A2", 2);
            sendEvent(env, "A3", 3);
            sendEvent(env, "B2", 2);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A2", "B2"});

            env.undeployAll();
        }
    }

    private static class EPLJoinJoinNoWhereClause implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = new String[]{"stream_0.volume", "stream_1.longBoxed"};
            String joinStatement = "@name('s0') select * from " +
                "SupportMarketDataBean#length(3)," +
                "SupportBean#length(3)";
            env.compileDeploy(joinStatement).addListener("s0");

            Object[] setOne = new Object[5];
            Object[] setTwo = new Object[5];
            for (int i = 0; i < setOne.length; i++) {
                setOne[i] = new SupportMarketDataBean("IBM", 0, (long) i, "");

                SupportBean theEvent = new SupportBean();
                theEvent.setLongBoxed((long) i);
                setTwo[i] = theEvent;
            }

            // Send 2 events, should join on second one
            sendEvent(env, setOne[0]);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields, null);

            sendEvent(env, setTwo[0]);
            assertEquals(1, env.listener("s0").getLastNewData().length);
            assertEquals(setOne[0], env.listener("s0").getLastNewData()[0].get("stream_0"));
            assertEquals(setTwo[0], env.listener("s0").getLastNewData()[0].get("stream_1"));
            env.listener("s0").reset();
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{{0L, 0L}});

            sendEvent(env, setOne[1]);
            sendEvent(env, setOne[2]);
            sendEvent(env, setTwo[1]);
            assertEquals(3, env.listener("s0").getLastNewData().length);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("s0").iterator(), fields,
                new Object[][]{{0L, 0L},
                    {1L, 0L},
                    {2L, 0L},
                    {0L, 1L},
                    {1L, 1L},
                    {2L, 1L}});

            env.undeployAll();
        }
    }

    private static void sendEvent(RegressionEnvironment env, String theString, int intPrimitive) {
        sendEvent(env, new SupportBean(theString, intPrimitive));
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }
}
