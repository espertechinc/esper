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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.stage.EPStage;
import com.espertech.esper.runtime.client.stage.EPStageDestroyedException;
import com.espertech.esper.runtime.client.stage.EPStageException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.assertMessage;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.stageIt;
import static com.espertech.esper.regressionlib.support.stage.SupportStageUtil.unstageIt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ClientStageEPStage {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientStageEPStageDestroy());
        execs.add(new ClientStageEPStageStageInvalid());
        return execs;
    }

    private static class ClientStageEPStageStageInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStage stageA = env.stageService().getStage("ST");

            tryIllegalArgument(() -> stageA.stage(null));
            tryOp(() -> stageA.stage(Collections.emptyList()));
            tryIllegalArgument(() -> stageA.stage(Arrays.asList(new String[]{null})));
            tryIllegalArgument(() -> stageA.stage(Arrays.asList(new String[]{"a", null})));

            tryIllegalArgument(() -> stageA.unstage(null));
            tryOp(() -> stageA.unstage(Collections.emptyList()));
            tryIllegalArgument(() -> stageA.unstage(Arrays.asList(new String[]{null})));
            tryIllegalArgument(() -> stageA.unstage(Arrays.asList(new String[]{"a", null})));

            tryDeploymentNotFound(() -> stageA.stage(Arrays.asList(new String[]{"x"})), "Deployment 'x' was not found");
            tryDeploymentNotFound(() -> stageA.unstage(Arrays.asList(new String[]{"x"})), "Deployment 'x' was not found");
        }
    }

    private static class ClientStageEPStageDestroy implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStage stageA = env.stageService().getStage("ST");
            env.compileDeploy("@name('s0') select * from SupportBean");
            String deploymentId = env.deploymentId("s0");

            stageIt(env, "ST", deploymentId);
            try {
                stageA.destroy();
                fail();
            } catch (EPException ex) {
                assertEquals("Failed to destroy stage 'ST': The stage has existing deployments", ex.getMessage());
            }
            unstageIt(env, "ST", deploymentId);

            stageA.destroy();
            assertEquals("ST", stageA.getURI());

            tryInvalidDestroyed(() -> stageA.getEventService());
            tryInvalidDestroyed(() -> stageA.getDeploymentService());

            tryInvalidDestroyed(() -> {
                try {
                    stageA.stage(Collections.singletonList(deploymentId));
                } catch (EPStageException ex) {
                    throw new RuntimeException(ex);
                }
            });

            tryInvalidDestroyed(() -> {
                try {
                    stageA.unstage(Collections.singletonList(deploymentId));
                } catch (EPStageException ex) {
                    throw new RuntimeException(ex);
                }
            });

            env.undeployAll();
        }

    }

    private static void tryInvalidDestroyed(Runnable r) {
        try {
            r.run();
            fail();
        } catch (EPStageDestroyedException ex) {
            // expected
        }
    }

    private static void tryIllegalArgument(RunnableWException r) {
        try {
            r.run();
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void tryDeploymentNotFound(RunnableWException r, String expected) {
        try {
            r.run();
            fail();
        } catch (EPStageException ex) {
            assertMessage(ex.getMessage(), expected);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void tryOp(RunnableWException r) {
        try {
            r.run();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private interface RunnableWException {
        void run() throws Exception;
    }
}
