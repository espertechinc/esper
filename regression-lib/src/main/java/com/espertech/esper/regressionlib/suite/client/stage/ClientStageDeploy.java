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
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esper.runtime.client.EPUndeployPreconditionException;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.stageIt;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.unstageIt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientStageDeploy {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientStageDeployInvalidUndeployWhileStaged());
        return execs;
    }

    private static class ClientStageDeployInvalidUndeployWhileStaged implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportBean");
            String deploymentId = env.deploymentId("s0");
            env.stageService().getStage("ST");
            stageIt(env, "ST", deploymentId);

            try {
                env.deployment().undeploy(deploymentId);
                fail();
            } catch (EPUndeployPreconditionException ex) {
                assertEquals(ex.getMessage(), "A precondition is not satisfied: Deployment id '" + deploymentId + "' is staged and cannot be undeployed");
            } catch (EPUndeployException ex) {
                throw new RuntimeException(ex);
            }

            unstageIt(env, "ST", deploymentId);
            env.undeploy(deploymentId);

            env.undeployAll();
        }
    }
}
