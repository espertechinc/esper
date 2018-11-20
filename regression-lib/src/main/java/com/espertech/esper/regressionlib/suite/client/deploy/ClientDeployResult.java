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
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.client.SupportDeploymentStateListener;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.client.option.StatementNameRuntimeContext;
import com.espertech.esper.runtime.client.option.StatementNameRuntimeOption;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.compiler.internal.parse.ParseHelper.NEWLINE;
import static org.junit.Assert.*;

public class ClientDeployResult {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientDeployResultSimple());
        execs.add(new ClientDeployStateListener());
        execs.add(new ClientDeployGetStmtByDepIdAndName());
        execs.add(new ClientDeploySameDeploymentId());
        return execs;
    }

    private static class ClientDeploySameDeploymentId implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.compile("select * from SupportBean");
            env.deploy(compiled, new DeploymentOptions().setDeploymentId("ABC"));

            try {
                env.runtime().getDeploymentService().deploy(compiled, new DeploymentOptions().setDeploymentId("ABC"));
                fail();
            } catch (EPDeployDeploymentExistsException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Deployment by id 'ABC' already exists");
            } catch (EPDeployException ex) {
                throw new RuntimeException(ex);
            }

            env.undeployAll();
        }
    }

    private static class ClientDeployResultSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled compiled = env.readCompile("regression/test_module_9.epl");

            EPDeployment result;
            try {
                result = env.runtime().getDeploymentService().deploy(compiled);
            } catch (EPDeployException ex) {
                throw new RuntimeException(ex);
            }

            assertTrue(env.runtime().getDeploymentService().isDeployed(result.getDeploymentId()));
            assertNotNull(result.getDeploymentId());
            assertEquals(2, result.getStatements().length);
            assertEquals(1, env.deployment().getDeployments().length);
            assertEquals("@Name(\"StmtOne\")" + NEWLINE +
                "create schema MyEvent(id String, val1 int, val2 int)", env.statement("StmtOne").getProperty(StatementProperty.EPL));
            assertEquals("@Name(\"StmtTwo\")" + NEWLINE +
                "select * from MyEvent", env.statement("StmtTwo").getProperty(StatementProperty.EPL));
            assertEquals(0, result.getDeploymentIdDependencies().length);

            env.undeployAll();

            assertFalse(env.runtime().getDeploymentService().isDeployed(result.getDeploymentId()));
        }
    }

    private static class ClientDeployStateListener implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportDeploymentStateListener.getEvents().clear();
            SupportDeploymentStateListener listener = new SupportDeploymentStateListener();
            env.deployment().addDeploymentStateListener(listener);

            env.compileDeploy("@name('s0') select * from SupportBean");
            String deploymentId = env.deploymentId("s0");

            DeploymentStateEventDeployed deployed = (DeploymentStateEventDeployed) SupportDeploymentStateListener.getEvents().get(0);
            SupportDeploymentStateListener.getEvents().clear();
            assertEquals(deploymentId, deployed.getDeploymentId());
            assertEquals("default", deployed.getRuntimeURI());
            assertEquals(1, deployed.getStatements().length);

            env.undeployAll();
            DeploymentStateEventUndeployed undeployed = (DeploymentStateEventUndeployed) SupportDeploymentStateListener.getEvents().get(0);
            SupportDeploymentStateListener.getEvents().clear();
            assertEquals(deploymentId, undeployed.getDeploymentId());
            assertEquals("default", undeployed.getRuntimeURI());
            assertEquals(1, undeployed.getStatements().length);

            env.deployment().getDeploymentStateListeners().next();
            env.deployment().removeDeploymentStateListener(listener);
            assertFalse(env.deployment().getDeploymentStateListeners().hasNext());

            env.deployment().addDeploymentStateListener(listener);
            env.deployment().removeAllDeploymentStateListeners();
            assertFalse(env.deployment().getDeploymentStateListeners().hasNext());

            env.undeployAll();
        }
    }

    private static class ClientDeployGetStmtByDepIdAndName implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] deploymentIds = "A,B,C,D,E".split(",");
            String[] names = "s1,s2,s3--0,s3,s3".split(",");

            EPStatement[] stmts = createStmts(env, deploymentIds, names);
            for (int i = 0; i < stmts.length; i++) {
                assertSame(stmts[i], env.deployment().getStatement(deploymentIds[i], names[i]));
            }

            // test statement name trim
            env.compileDeploy("@name(' stmt0  ') select * from SupportBean_S0");
            assertNotNull(env.deployment().getStatement(env.deploymentId("stmt0"), "stmt0"));

            try {
                env.deployment().getStatement(null, null);
                fail();
            } catch (IllegalArgumentException ex) {
                assertEquals("Missing deployment-id parameter", ex.getMessage());
            }

            try {
                env.deployment().getStatement("x", null);
                fail();
            } catch (IllegalArgumentException ex) {
                assertEquals("Missing statement-name parameter", ex.getMessage());
            }

            assertNull(env.deployment().getStatement("x", "y"));
            assertNull(env.deployment().getStatement(env.deploymentId("stmt0"), "y"));
            assertNull(env.deployment().getStatement("x", "stmt0"));
            assertNotNull(env.deployment().getStatement(env.deploymentId("stmt0"), "stmt0"));

            env.undeployAll();
        }
    }

    private static EPStatement[] createStmts(RegressionEnvironment env, String[] deploymentIds, String[] statementNames) {
        assertEquals(deploymentIds.length, statementNames.length);
        EPStatement[] statements = new EPStatement[statementNames.length];
        EPCompiled compiled = env.compile("select * from SupportBean");

        for (int i = 0; i < statementNames.length; i++) {
            final int num = i;
            try {
                EPDeployment deployed = env.deployment().deploy(compiled, new DeploymentOptions().setDeploymentId(deploymentIds[i]).setStatementNameRuntime(new StatementNameRuntimeOption() {
                    public String getStatementName(StatementNameRuntimeContext env) {
                        return statementNames[num];
                    }
                }));
                statements[i] = deployed.getStatements()[0];
            } catch (EPDeployException e) {
                throw new RuntimeException(e);
            }
        }
        return statements;
    }
}
