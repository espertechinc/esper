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
package com.espertech.esper.regression.dataflow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EPStatementState;
import com.espertech.esper.client.dataflow.EPDataFlowDescriptor;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationException;
import com.espertech.esper.client.dataflow.EPDataFlowRuntime;
import com.espertech.esper.client.deploy.Module;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.UuidGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecDataflowAPICreateStartStopDestroy implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCreateStartStop(epService);
        runAssertionDeploymentAdmin(epService);
    }

    private void runAssertionCreateStartStop(EPServiceProvider epService) throws Exception {
        String epl = "@Name('Create-A-Flow') create dataflow MyGraph Emitter -> outstream<?> {}";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);

        EPDataFlowRuntime dfruntime = epService.getEPRuntime().getDataFlowRuntime();
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"MyGraph"}, dfruntime.getDataFlows());
        EPDataFlowDescriptor desc = dfruntime.getDataFlow("MyGraph");
        assertEquals("MyGraph", desc.getDataFlowName());
        assertEquals(EPStatementState.STARTED, desc.getStatementState());
        assertEquals("Create-A-Flow", desc.getStatementName());

        dfruntime.instantiate("MyGraph");

        // test duplicate
        tryInvalidCompile(epService, epl, "Error starting statement: Data flow by name 'MyGraph' has already been declared [");

        // stop - can no longer instantiate but still exists
        stmt.stop();    // not removed
        assertEquals(EPStatementState.STOPPED, dfruntime.getDataFlow("MyGraph").getStatementState());
        tryInvalidCompile(epService, epl, "Error starting statement: Data flow by name 'MyGraph' has already been declared [");
        tryInstantiate(epService, "MyGraph", "Data flow by name 'MyGraph' is currently in STOPPED statement state");
        tryInstantiate(epService, "DUMMY", "Data flow by name 'DUMMY' has not been defined");

        // destroy - should be gone
        stmt.destroy(); // removed, create again
        assertEquals(null, dfruntime.getDataFlow("MyGraph"));
        assertEquals(0, dfruntime.getDataFlows().length);
        tryInstantiate(epService, "MyGraph", "Data flow by name 'MyGraph' has not been defined");
        try {
            stmt.start();
            fail();
        } catch (IllegalStateException ex) {
            assertEquals("Cannot start statement, statement is in destroyed state", ex.getMessage());
        }

        // new one, try start-stop-start
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.stop();
        stmt.start();
        dfruntime.instantiate("MyGraph");
    }

    private void runAssertionDeploymentAdmin(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportSourceOp.class.getPackage().getName() + ".*");
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String eplModule = "create dataflow TheGraph\n" +
                "create schema ABC as " + SupportBean.class.getName() + "," +
                "DefaultSupportSourceOp -> outstream<SupportBean> {}\n" +
                "Select(outstream) -> selectedData {select: (select theString, intPrimitive from outstream) }\n" +
                "DefaultSupportCaptureOp(selectedData) {};";
        Module module = epService.getEPAdministrator().getDeploymentAdmin().parse(eplModule);
        assertEquals(1, module.getItems().size());
        epService.getEPAdministrator().getDeploymentAdmin().deploy(module, null);

        epService.getEPRuntime().getDataFlowRuntime().instantiate("TheGraph");
    }

    private void tryInvalidCompile(EPServiceProvider epService, String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl, UuidGenerator.generate());
            fail();
        } catch (EPStatementException ex) {
            assertException(message, ex.getMessage());
        }
    }

    private void tryInstantiate(EPServiceProvider epService, String graph, String message) {
        try {
            epService.getEPRuntime().getDataFlowRuntime().instantiate(graph);
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
