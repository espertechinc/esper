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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.option.StatementNameRuntimeContext;
import com.espertech.esper.runtime.client.option.StatementNameRuntimeOption;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.assertMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientRuntimeStatementName {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeStatementAllowNameDuplicate());
        execs.add(new ClientRuntimeSingleModuleTwoStatementsNoDep());
        execs.add(new ClientRuntimeStatementNameUnassigned());
        execs.add(new ClientRuntimeStatementNameRuntimeResolverDuplicate());
        return execs;
    }

    public static class ClientRuntimeStatementNameRuntimeResolverDuplicate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("select * from SupportBean;select * from SupportBean;");
            try {
                env.deployment().deploy(compiled, new DeploymentOptions().setStatementNameRuntime(new StatementNameRuntimeOption() {
                    public String getStatementName(StatementNameRuntimeContext env) {
                        return "x";
                    }
                }));
                fail();
            } catch (EPDeployException e) {
                assertMessage(e, "Duplicate statement name provide by statement name resolver for statement name 'x'");
            }
        }
    }

    public static class ClientRuntimeStatementNameUnassigned implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("select * from SupportBean;select * from SupportBean;");
            EPDeployment deployment;
            try {
                deployment = env.deployment().deploy(compiled);
            } catch (EPDeployException e) {
                throw new RuntimeException(e);
            }
            assertEquals("stmt-0", deployment.getStatements()[0].getName());
            assertEquals("stmt-1", deployment.getStatements()[1].getName());
            env.undeployAll();
        }
    }

    public static class ClientRuntimeStatementAllowNameDuplicate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("@name('a') select * from SupportBean;\n");
            env.deploy(compiled);
            env.deploy(compiled);
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
