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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowSubquery {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraSubqueryTwoConsumerWindow());
        execs.add(new InfraSubqueryLateConsumerAggregation());
        execs.add(new InfraSubqueryWithFilterInParens());
        return execs;
    }

    private static class InfraSubqueryWithFilterInParens implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create window MyWindow#keepall as SupportBean;\n" +
                         "@name('insert') insert into MyWindow select * from SupportBean;\n" +
                         "@name('s0') select exists (select * from MyWindow(theString='E1')) as c0 from SupportBean_S0;\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssert(env, false);

            env.sendEventBean(new SupportBean("E2", 1));
            sendAssert(env, false);

            env.sendEventBean(new SupportBean("E1", 1));
            sendAssert(env, true);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, boolean expected) {
            env.sendEventBean(new SupportBean_S0(0));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
    }

    private static class InfraSubqueryTwoConsumerWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "\n create window MyWindowTwo#length(1) as (mycount long);" +
                    "\n @Name('insert-count') insert into MyWindowTwo select 1L as mycount from SupportBean;" +
                    "\n create variable long myvar = 0;" +
                    "\n @Name('assign') on MyWindowTwo set myvar = (select mycount from MyWindowTwo);";
            env.compileDeploy(epl);

            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals(1L, env.runtime().getVariableService().getVariableValue(env.deploymentId("assign"), "myvar"));   // if the subquery-consumer executes first, this will be null

            env.undeployAll();
        }
    }

    private static class InfraSubqueryLateConsumerAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindow#keepall as SupportBean", path);
            env.compileDeploy("insert into MyWindow select * from SupportBean", path);

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 1));

            env.compileDeploy("@name('s0') select * from MyWindow where (select count(*) from MyWindow) > 0", path).addListener("s0");

            env.sendEventBean(new SupportBean("E3", 1));
            assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }
}
