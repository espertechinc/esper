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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean;
import com.espertech.esper.runtime.client.EPDeployException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientRuntimeStatementName {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeStatementNameDuplicate());
        execs.add(new ClientRuntimeSingleModuleTwoStatementsNoDep());
        return execs;
    }

    public static class ClientRuntimeStatementNameDuplicate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("@name('a') select * from SupportBean;\n");

            try {
                env.deployment().deploy(compiled);

                env.milestone(0);

                env.deployment().deploy(compiled);
            } catch (EPDeployException ex) {
                fail(ex.getMessage());
            }

            env.undeployAll();
        }
    }

    public static class ClientRuntimeSingleModuleTwoStatementsNoDep implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "@name('s0') select intPrimitive from SupportBean;" +
                    "@name('s1') select theString from SupportBean;";
            EPCompiled compiled = env.compile(epl);

            env.deploy(compiled).addListener("s0").addListener("s1").milestone(0);

            sendAssert(env, "E1", 10);
            env.milestone(1);

            sendAssert(env, "E2", 20);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String theString, int intPrimitive) {
            env.sendEventBean(new SupportBean(theString, intPrimitive));
            assertEquals(intPrimitive, env.listener("s0").assertOneGetNewAndReset().get("intPrimitive"));
            assertEquals(theString, env.listener("s1").assertOneGetNewAndReset().get("theString"));
        }
    }
}
