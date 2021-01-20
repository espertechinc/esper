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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.*;
import com.espertech.esper.runtime.client.*;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;

import java.util.*;

import static org.junit.Assert.*;

public class ClientDeployRollout {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientDeployRolloutFourInterdepModulesWStmtId());
        execs.add(new ClientDeployRolloutTwoInterdepModules());
        execs.add(new ClientDeployRolloutInvalid());
        return execs;
    }

    private static class ClientDeployRolloutFourInterdepModulesWStmtId implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled base = env.compile("@name('basevar') @public create constant variable int basevar = 1");
            EPCompiled child0 = env.compile("@name('s0') select basevar from SupportBean", new RegressionPath().add(base));
            EPCompiled child1 = env.compile("@name('child1var') @public create constant variable int child1var = 2;\n" +
                "@name('s1') select basevar, child1var from SupportBean;\n", new RegressionPath().add(base));
            EPCompiled child11 = env.compile("@name('s2') select basevar, child1var from SupportBean;\n", new RegressionPath().add(base).add(child1));

            env.rollout(Arrays.asList(toRolloutItems(base, child0, child1, child11)), null);
            env.addListener("s0").addListener("s1").addListener("s2");

            sendAssert(env, "s1,s2");

            env.milestone(0);

            sendAssert(env, "s1,s2");
            assertStatementIds(env, "basevar,s0,child1var,s1,s2", 1, 2, 3, 4, 5);

            EPDeploymentRolloutCompiled item = new EPDeploymentRolloutCompiled(env.compile("@name('s3') select basevar, child1var from SupportBean", new RegressionPath().add(base).add(child1)), null);
            env.rollout(Collections.singletonList(item), null).addListener("s3");
            EPDeployment deploymentChild11 = env.deployment().getDeployment(env.deploymentId("s2"));
            EPAssertionUtil.assertEqualsAnyOrder(new String[]{env.deploymentId("basevar"), env.deploymentId("child1var")}, deploymentChild11.getDeploymentIdDependencies());

            env.milestone(1);

            sendAssert(env, "s1,s2,s3");
            assertStatementIds(env, "basevar,s0,child1var,s1,s2,s3", 1, 2, 3, 4, 5, 6);

            env.undeployAll();

            env.milestone(2);

            env.compileDeploy("@name('s1') select * from SupportBean");
            tryInvalidRollout(env, "A precondition is not satisfied: Required dependency variable 'basevar' cannot be found", 0, EPDeployPreconditionException.class, child0);
            assertStatementIds(env, "s1", 7);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.RUNTIMEOPS);
        }

        private void sendAssert(RegressionEnvironment env, String stmtNameCsv) {
            env.sendEventBean(new SupportBean());
            env.assertEqualsNew("s0", "basevar", 1);
            for (String stmtName : stmtNameCsv.split(",")) {
                EPAssertionUtil.assertProps(env.listener(stmtName).assertOneGetNewAndReset(), "basevar,child1var".split(","), new Object[]{1, 2});
            }
        }
    }

    private static class ClientDeployRolloutInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPCompiled type = env.compile("@name('s0') @public @buseventtype create schema MyEvent(p string)");
            EPCompiled selectMyEvent = env.compile("@name('s0') select * from MyEvent", new RegressionPath().add(type));
            EPCompiled selectSB = env.compile("@name('s0') select * from SupportBean");
            EPCompiled selectSBParameterized = env.compile("@name('s0') select * from SupportBean(theString = ?::string)");
            env.compileDeploy("@name('s1') select * from SupportBean");

            // dependency not found
            String msg = "A precondition is not satisfied: Required dependency event type 'MyEvent' cannot be found";
            tryInvalidRollout(env, msg, 1, EPDeployPreconditionException.class, selectSB, selectMyEvent);
            tryInvalidRollout(env, msg, 0, EPDeployPreconditionException.class, selectMyEvent);
            tryInvalidRollout(env, msg, 2, EPDeployPreconditionException.class, selectSB, selectSB, selectMyEvent);
            tryInvalidRollout(env, msg, 1, EPDeployPreconditionException.class, selectSB, selectMyEvent, selectSB, selectSB);

            // already defined
            tryInvalidRollout(env, "Event type by name 'MyEvent' already registered", 1, EPDeployException.class, type, type);

            // duplicate deployment id
            tryInvalidRollout(env, "Deployment id 'a' occurs multiple times in the rollout", 1, EPDeployException.class,
                new EPDeploymentRolloutCompiled(selectSB, new DeploymentOptions().setDeploymentId("a")),
                new EPDeploymentRolloutCompiled(selectSB, new DeploymentOptions().setDeploymentId("a")));

            // deployment id exists
            tryInvalidRollout(env, "Deployment by id '" + env.deploymentId("s1") + "' already exists", 1, EPDeployDeploymentExistsException.class,
                new EPDeploymentRolloutCompiled(selectSB, new DeploymentOptions().setDeploymentId("a")),
                new EPDeploymentRolloutCompiled(selectSB, new DeploymentOptions().setDeploymentId(env.deploymentId("s1"))));

            // substitution param problem
            tryInvalidRollout(env, "Substitution parameters have not been provided: Statement 's0' has 1 substitution parameters", 1, EPDeploySubstitutionParameterException.class,
                selectSB, selectSBParameterized);

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.INVALIDITY);
        }
    }

    private static class ClientDeployRolloutTwoInterdepModules implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String eplOne = "@name('type') @public @buseventtype create schema MyEvent(p string)";
            EPCompiled compiledOne = env.compile(eplOne, path);
            String eplTwo = "@name('s0') select * from MyEvent";
            EPCompiled compiledTwo = env.compile(eplTwo, path);

            List<EPDeploymentRolloutCompiled> items = new ArrayList<>();
            items.add(new EPDeploymentRolloutCompiled(compiledOne));
            items.add(new EPDeploymentRolloutCompiled(compiledTwo));

            EPDeploymentRollout rollout;
            try {
                rollout = env.deployment().rollout(items);
                env.addListener("s0");
            } catch (EPDeployException ex) {
                throw new RuntimeException(ex);
            }
            assertEquals(2, rollout.getItems().length);
            assertDeployment(env, rollout.getItems()[0].getDeployment(), "type");
            assertDeployment(env, rollout.getItems()[1].getDeployment(), "s0");

            assertSendAndReceive(env, "a");

            env.milestone(0);

            assertSendAndReceive(env, "b");

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.RUNTIMEOPS);
        }

        private void assertDeployment(RegressionEnvironment env, EPDeployment deployment, String statementName) {
            assertEquals(1, deployment.getStatements().length);
            assertEquals(statementName, deployment.getStatements()[0].getName());
            assertEquals(env.deploymentId(statementName), deployment.getDeploymentId());
        }

        private void assertSendAndReceive(RegressionEnvironment env, String value) {
            env.sendEventMap(Collections.singletonMap("p", value), "MyEvent");
            env.assertEqualsNew("s0", "p", value);
        }
    }

    private static EPDeploymentRolloutCompiled[] toRolloutItems(EPCompiled... compileds) {
        EPDeploymentRolloutCompiled[] items = new EPDeploymentRolloutCompiled[compileds.length];
        for (int i = 0; i < compileds.length; i++) {
            items[i] = new EPDeploymentRolloutCompiled(compileds[i]);
        }
        return items;
    }

    private static void tryInvalidRollout(RegressionEnvironment env, String expectedMsg, int rolloutNumber, Class exceptionType, EPCompiled... compileds) {
        tryInvalidRollout(env, expectedMsg, rolloutNumber, exceptionType, toRolloutItems(compileds));
    }

    private static void tryInvalidRollout(RegressionEnvironment env, String expectedMsg, int rolloutNumber, Class exceptionType, EPDeploymentRolloutCompiled... items) {
        try {
            env.runtime().getDeploymentService().rollout(Arrays.asList(items));
            fail();
        } catch (EPDeployException ex) {
            assertEquals(rolloutNumber, ex.getRolloutItemNumber());
            SupportMessageAssertUtil.assertMessage(ex.getMessage(), expectedMsg);
            assertEquals(exceptionType, ex.getClass());
        }

        try {
            env.deploymentId("s0");
            fail();
        } catch (Throwable t) {
            // expected
        }
        assertNotNull(env.deploymentId("s1"));
    }

    private static void assertStatementIds(RegressionEnvironment env, String nameCSV, int... statementIds) {
        String[] names = nameCSV.split(",");
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            env.assertStatement(names[i], statement -> {
                EPStatementSPI spi = (EPStatementSPI) statement;
                assertEquals(statementIds[index], spi.getStatementId());
            });
        }
    }
}
