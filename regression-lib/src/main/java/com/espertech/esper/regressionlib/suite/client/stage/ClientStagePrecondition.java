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
package com.espertech.esper.regressionlib.suite.client.stage;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.runtime.client.stage.EPStage;
import com.espertech.esper.runtime.client.stage.EPStageException;
import com.espertech.esper.runtime.client.stage.EPStagePreconditionException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.assertMessage;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.stageIt;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.unstageIt;
import static org.junit.Assert.fail;

public class ClientStagePrecondition {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        // Precondition checking of dependencies does not require each object type as general dependency reporting is tested elsewhere
        execs.add(new ClientStageStagePreconditionNamedWindow());
        execs.add(new ClientStageStagePreconditionContext());
        execs.add(new ClientStageStagePreconditionVariable());
        execs.add(new ClientStageUnstagePrecondition());
        return execs;
    }

    private static class ClientStageUnstagePrecondition implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('context') @public create context MyContext initiated by SupportBean", path);
            env.compileDeploy("@name('stmt') context MyContext select count(*) from SupportBean_S0", path);
            String idCreate = env.deploymentId("context");
            String idStmt = env.deploymentId("stmt");

            env.runtime().getStageService().getStage("ST");
            stageIt(env, "ST", idCreate, idStmt);

            tryInvalidUnstage(env, idCreate);
            tryInvalidUnstage(env, idStmt);

            unstageIt(env, "ST", idCreate, idStmt);

            env.undeployAll();
        }
    }

    private static class ClientStageStagePreconditionVariable implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('variable') @public create variable int MyVariable", path);
            env.compileDeploy("@name('stmt') select MyVariable from SupportBean_S0", path);

            EPStage stage = env.runtime().getStageService().getStage("S1");
            String idCreate = env.deploymentId("variable");
            String idStmt = env.deploymentId("stmt");

            tryInvalidStageProvides(env, stage, idCreate, idStmt, "variable 'MyVariable'");
            tryInvalidStageConsumes(env, stage, idStmt, idCreate, "variable 'MyVariable'");

            env.undeployAll();
        }
    }

    private static class ClientStageStagePreconditionContext implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('context') @public create context MyContext initiated by SupportBean", path);
            env.compileDeploy("@name('stmt') context MyContext select count(*) from SupportBean_S0", path);

            EPStage stage = env.runtime().getStageService().getStage("S1");
            String idCreate = env.deploymentId("context");
            String idStmt = env.deploymentId("stmt");

            tryInvalidStageProvides(env, stage, idCreate, idStmt, "context 'MyContext'");
            tryInvalidStageConsumes(env, stage, idStmt, idCreate, "context 'MyContext'");

            env.compileDeploy("@name('stmt-2') context MyContext select count(*) from SupportBean_S1", path);
            String idStmt2 = env.deploymentId("stmt-2");

            tryInvalidStage(env, stage, new String[]{idCreate, idStmt});
            tryInvalidStage(env, stage, new String[]{idStmt2, idStmt});
            tryInvalidStage(env, stage, new String[]{idStmt2, idCreate});

            env.undeployAll();
        }
    }

    private static class ClientStageStagePreconditionNamedWindow implements ClientStageRegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('create') @public create window MyWindow#keepall as SupportBean", path);
            EPStage stage = env.runtime().getStageService().getStage("S1");
            String idCreate = env.deploymentId("create");

            String namedWindowObjectName = "named window 'MyWindow'";
            String eventTypeObjectName = "event type 'MyWindow'";
            String[][] namedWindowDependencies = new String[][]{
                {"select * from MyWindow", namedWindowObjectName},
                {"insert into MyWindow select * from SupportBean", namedWindowObjectName},
                {"select (select count(*) from MyWindow) from SupportBean", namedWindowObjectName},
                {"on SupportBean delete from MyWindow", namedWindowObjectName},
                {"select * from pattern[every MyWindow]", eventTypeObjectName},
                {"on MyWindow merge MyWindow where 1=1 when not matched then insert into ABC select *", namedWindowObjectName}
            };
            for (String[] line : namedWindowDependencies) {
                assertPrecondition(env, path, stage, idCreate, line[1], line[0]);
            }

            env.undeployAll();
        }
    }

    private static void assertPrecondition(RegressionEnvironment env, RegressionPath path, EPStage stage, String idCreate, String objectName, String epl) {
        env.compileDeploy("@name('tester') " + epl, path);
        String idTester = env.deploymentId("tester");
        tryInvalidStageProvides(env, stage, idCreate, idTester, objectName);
        tryInvalidStageConsumes(env, stage, idTester, idCreate, objectName);
        env.undeployModuleContaining("tester");
    }

    private static void tryInvalidStageProvides(RegressionEnvironment env, EPStage stage, String idStaged, String idConsuming, String objectName) {
        String expected = "Failed to stage deployment '" + idStaged + "': Deployment provides " + objectName + " to deployment '" + idConsuming + "' and must therefore also be staged";
        tryInvalidStage(env, stage, new String[]{idStaged}, expected);
    }

    private static void tryInvalidStageConsumes(RegressionEnvironment env, EPStage stage, String idStaged, String idProviding, String objectName) {
        String expected = "Failed to stage deployment '" + idStaged + "': Deployment consumes " + objectName + " from deployment '" + idProviding + "' and must therefore also be staged";
        tryInvalidStage(env, stage, new String[]{idStaged}, expected);
    }

    private static void tryInvalidStage(RegressionEnvironment env, EPStage stage, String[] idsStaged) {
        tryInvalidStage(env, stage, idsStaged, "skip");
    }

    private static void tryInvalidStage(RegressionEnvironment env, EPStage stage, String[] idsStaged, String message) {
        try {
            stage.stage(Arrays.asList(idsStaged));
            fail();
        } catch (EPStagePreconditionException ex) {
            if (!message.equals("skip")) {
                assertMessage(ex.getMessage(), message);
            }
        } catch (EPStageException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void tryInvalidUnstage(RegressionEnvironment env, String id) {
        try {
            env.stageService().getStage("ST").unstage(Arrays.asList(id));
            fail();
        } catch (EPStageException ex) {
            // expected
        }
    }
}
