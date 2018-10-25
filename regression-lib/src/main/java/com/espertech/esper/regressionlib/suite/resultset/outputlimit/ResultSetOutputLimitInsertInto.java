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
package com.espertech.esper.regressionlib.suite.resultset.outputlimit;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;

public class ResultSetOutputLimitInsertInto {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ResultSetOutputLimitInsertFirst());
        execs.add(new ResultSetOutputLimitInsertSnapshot());
        return execs;
    }

    private static class ResultSetOutputLimitInsertSnapshot implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            env.compileDeploy("@name('s0') insert into MyStream select * from SupportBean#keepall output snapshot every 1 second;\n" +
                "@name('s1') select * from MyStream").addListener("s0").addListener("s1");

            env.sendEventBean(new SupportBean("E1", 0));

            env.advanceTime(1000);
            assertReceivedS0AndS1(env, new Object[][]{{"E1"}});

            env.sendEventBean(new SupportBean("E2", 0));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.listener("s1").isInvoked());

            env.advanceTime(2000);
            assertReceivedS0AndS1(env, new Object[][]{{"E1"}, {"E2"}});

            env.undeployAll();
        }
    }

    private static class ResultSetOutputLimitInsertFirst implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            env.compileDeploy("@name('s0') insert into MyStream select * from SupportBean output first every 1 second;\n" +
                "@name('s1') select * from MyStream").addListener("s0").addListener("s1");

            env.sendEventBean(new SupportBean("E1", 0));
            assertReceivedS0AndS1(env, new Object[][]{{"E1"}});

            env.sendEventBean(new SupportBean("E2", 0));
            assertFalse(env.listener("s0").isInvoked());
            assertFalse(env.listener("s1").isInvoked());

            env.advanceTime(1000);

            env.sendEventBean(new SupportBean("E2", 0));
            assertReceivedS0AndS1(env, new Object[][]{{"E2"}});

            env.undeployAll();
        }
    }

    private static void assertReceivedS0AndS1(RegressionEnvironment env, Object[][] props) {
        String[] fields = new String[]{"theString"};
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, props);
        EPAssertionUtil.assertPropsPerRow(env.listener("s1").getAndResetDataListsFlattened().getFirst(), fields, props);
    }
}
