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
package com.espertech.esper.regressionlib.suite.epl.dataflow;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowDescriptor;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationException;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowService;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EPLDataflowAPICreateStartStopDestroy {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowCreateStartStop());
        execs.add(new EPLDataflowDeploymentAdmin());
        return execs;
    }


    private static class EPLDataflowCreateStartStop implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@Name('flow') create dataflow MyGraph Emitter -> outstream<?> {}";
            EPCompiled compiledGraph = env.compile(epl);
            try {
                env.deployment().deploy(compiledGraph, new DeploymentOptions().setDeploymentId("DEP1"));
            } catch (EPDeployException ex) {
                throw new RuntimeException(ex);
            }

            EPDataFlowService dfruntime = env.runtime().getDataFlowService();
            EPAssertionUtil.assertEqualsAnyOrder(new DeploymentIdNamePair[]{new DeploymentIdNamePair(env.deploymentId("flow"), "MyGraph")}, dfruntime.getDataFlows());
            EPDataFlowDescriptor desc = dfruntime.getDataFlow("DEP1", "MyGraph");
            assertEquals("MyGraph", desc.getDataFlowName());
            assertEquals("flow", desc.getStatementName());

            dfruntime.instantiate(env.deploymentId("flow"), "MyGraph");

            // stop - can no longer instantiate but still exists
            env.undeployModuleContaining("flow");
            tryInstantiate(env, "DEP1", "MyGraph", "Data flow by name 'MyGraph' for deployment id 'DEP1' has not been defined");
            tryInstantiate(env, "DEP1", "DUMMY", "Data flow by name 'DUMMY' for deployment id 'DEP1' has not been defined");

            // destroy - should be gone
            assertEquals(null, dfruntime.getDataFlow("DEP1", "MyGraph"));
            assertEquals(0, dfruntime.getDataFlows().length);
            tryInstantiate(env, "DEP1", "MyGraph", "Data flow by name 'MyGraph' for deployment id 'DEP1' has not been defined");

            // new one, try start-stop-start
            env.compileDeploy(epl);
            dfruntime.instantiate(env.deploymentId("flow"), "MyGraph");
            env.undeployAll();
        }
    }

    private static class EPLDataflowDeploymentAdmin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            String epl = "@name('flow') create dataflow TheGraph\n" +
                "create schema ABC as " + SupportBean.class.getName() + "," +
                "DefaultSupportSourceOp -> outstream<SupportBean> {}\n" +
                "Select(outstream) -> selectedData {select: (select theString, intPrimitive from outstream) }\n" +
                "DefaultSupportCaptureOp(selectedData) {};";

            Module module = null;
            try {
                module = EPCompilerProvider.getCompiler().parseModule(epl);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            assertEquals(1, module.getItems().size());
            env.compileDeploy(epl);

            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "TheGraph");

            env.undeployAll();
        }
    }

    private static void tryInstantiate(RegressionEnvironment env, String deploymentId, String graph, String message) {
        try {
            env.runtime().getDataFlowService().instantiate(deploymentId, graph);
            fail();
        } catch (EPDataFlowInstantiationException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private void assertException(String expected, String message) {
        String received = message.substring(0, message.indexOf("[") + 1);
        assertEquals(expected, received);
    }
}
