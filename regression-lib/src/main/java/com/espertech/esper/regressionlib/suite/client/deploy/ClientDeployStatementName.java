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
package com.espertech.esper.regressionlib.suite.client.deploy;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.option.StatementNameRuntimeContext;
import com.espertech.esper.runtime.client.option.StatementNameRuntimeOption;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ClientDeployStatementName {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientDeployStatementNameResolveContext());
        return execs;
    }

    private static class ClientDeployStatementNameResolveContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            MyStatementNameRuntimeResolver.getContexts().clear();
            String epl = "@name('s0') select * from SupportBean";
            EPCompiled compiled = env.compile(epl);
            DeploymentOptions options = new DeploymentOptions();
            options.setStatementNameRuntime(new MyStatementNameRuntimeResolver());

            try {
                env.deployment().deploy(compiled, options);
            } catch (EPDeployException e) {
                fail(e.getMessage());
            }

            StatementNameRuntimeContext ctx = MyStatementNameRuntimeResolver.getContexts().get(0);
            assertEquals("s0", ctx.getStatementName());
            assertEquals(env.deploymentId("hello"), ctx.getDeploymentId());
            assertSame(env.statement("hello").getAnnotations(), ctx.getAnnotations());
            assertEquals(epl, ctx.getEpl());
            assertEquals("hello", env.statement("hello").getName());

            env.milestone(0);

            assertEquals("hello", env.statement("hello").getName());

            env.undeployAll();
        }
    }

    private static class MyStatementNameRuntimeResolver implements StatementNameRuntimeOption {
        private static List<StatementNameRuntimeContext> contexts = new ArrayList<>();

        public static List<StatementNameRuntimeContext> getContexts() {
            return contexts;
        }

        public String getStatementName(StatementNameRuntimeContext env) {
            contexts.add(env);
            return "hello";
        }
    }
}
