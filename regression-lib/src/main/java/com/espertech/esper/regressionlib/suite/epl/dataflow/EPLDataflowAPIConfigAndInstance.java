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

import com.espertech.esper.common.client.dataflow.core.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class EPLDataflowAPIConfigAndInstance implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        EPDataFlowService dataFlowRuntime = env.runtime().getDataFlowService();
        assertEquals(0, dataFlowRuntime.getSavedConfigurations().length);
        assertNull(dataFlowRuntime.getSavedConfiguration("MyFirstFlow"));
        assertFalse(dataFlowRuntime.removeSavedConfiguration("MyFirstFlow"));
        try {
            dataFlowRuntime.instantiateSavedConfiguration("MyFirstFlow");
            fail();
        } catch (EPDataFlowInstantiationException ex) {
            assertEquals("Dataflow saved configuration 'MyFirstFlow' could not be found", ex.getMessage());
        }
        try {
            dataFlowRuntime.saveConfiguration("MyFirstFlow", "x", "MyDataflow", null);
            fail();
        } catch (EPDataFlowNotFoundException ex) {
            assertEquals("Failed to locate data flow 'MyDataflow'", ex.getMessage());
        }

        // finally create one
        RegressionPath path = new RegressionPath();
        String epl = "create objectarray schema MyEvent ();\n" +
            "@name('df') create dataflow MyDataflow " +
            "BeaconSource -> outdata<MyEvent> {" +
            "  iterations:1" +
            "}" +
            "EventBusSink(outdata) {};\n";
        env.compileDeploy(epl, path);

        // add it
        String deploymentId = env.deploymentId("df");
        dataFlowRuntime.saveConfiguration("MyFirstFlow", deploymentId, "MyDataflow", null);
        assertEquals(1, dataFlowRuntime.getSavedConfigurations().length);
        EPDataFlowSavedConfiguration savedConfiguration = dataFlowRuntime.getSavedConfiguration(dataFlowRuntime.getSavedConfigurations()[0]);
        assertEquals("MyFirstFlow", savedConfiguration.getSavedConfigurationName());
        assertEquals("MyDataflow", savedConfiguration.getDataflowName());
        try {
            dataFlowRuntime.saveConfiguration("MyFirstFlow", deploymentId, "MyDataflow", null);
            fail();
        } catch (EPDataFlowAlreadyExistsException ex) {
            assertEquals("Data flow saved configuration by name 'MyFirstFlow' already exists", ex.getMessage());
        }

        // remove it
        assertTrue(dataFlowRuntime.removeSavedConfiguration("MyFirstFlow"));
        assertFalse(dataFlowRuntime.removeSavedConfiguration("MyFirstFlow"));
        assertEquals(0, dataFlowRuntime.getSavedConfigurations().length);
        assertNull(dataFlowRuntime.getSavedConfiguration("MyFirstFlow"));

        // add once more to instantiate
        dataFlowRuntime.saveConfiguration("MyFirstFlow", deploymentId, "MyDataflow", null);
        EPDataFlowInstance instance = dataFlowRuntime.instantiateSavedConfiguration("MyFirstFlow");
        env.compileDeploy("@name('s0') select * from MyEvent", path).addListener("s0");
        instance.run();
        assertTrue(env.listener("s0").getAndClearIsInvoked());
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"MyFirstFlow"}, dataFlowRuntime.getSavedConfigurations());
        assertNotNull(dataFlowRuntime.getSavedConfiguration("MyFirstFlow"));

        // add/remove instance
        dataFlowRuntime.saveInstance("F1", instance);
        EPAssertionUtil.assertEqualsExactOrder(new String[]{"F1"}, dataFlowRuntime.getSavedInstances());
        EPDataFlowInstance instanceFromSvc = dataFlowRuntime.getSavedInstance("F1");
        assertEquals(deploymentId, instanceFromSvc.getDataFlowDeploymentId());
        assertEquals("MyDataflow", instanceFromSvc.getDataFlowName());
        try {
            dataFlowRuntime.saveInstance("F1", instance);
            fail();
        } catch (EPDataFlowAlreadyExistsException ex) {
            // expected
            assertEquals("Data flow instance name 'F1' already saved", ex.getMessage());
        }
        assertTrue(dataFlowRuntime.removeSavedInstance("F1"));
        assertFalse(dataFlowRuntime.removeSavedInstance("F1"));

        env.undeployAll();
    }
}
