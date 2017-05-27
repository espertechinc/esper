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
import com.espertech.esper.client.dataflow.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.dataflow.ops.Emitter;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecDataflowAPIStartCaptive implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportCaptureOp.class.getName());

        String[] fields = "p0,p1".split(",");
        epService.getEPAdministrator().getConfiguration().addEventType("MyOAEventType", fields, new Object[]{String.class, int.class});

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlow " +
                "Emitter -> outstream<MyOAEventType> {name:'src1'}" +
                "DefaultSupportCaptureOp(outstream) {}");

        DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(captureOp));

        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow", options);
        EPDataFlowInstanceCaptive captiveStart = instance.startCaptive();
        assertEquals(0, captiveStart.getRunnables().size());
        assertEquals(1, captiveStart.getEmitters().size());
        Emitter emitter = captiveStart.getEmitters().get("src1");
        assertEquals(EPDataFlowState.RUNNING, instance.getState());

        emitter.submit(new Object[]{"E1", 10});
        EPAssertionUtil.assertPropsPerRow(captureOp.getCurrent(), fields, new Object[][]{{"E1", 10}});

        emitter.submit(new Object[]{"E2", 20});
        EPAssertionUtil.assertPropsPerRow(captureOp.getCurrent(), fields, new Object[][]{{"E1", 10}, {"E2", 20}});

        emitter.submitSignal(new EPDataFlowSignalFinalMarker() {
        });
        EPAssertionUtil.assertPropsPerRow(captureOp.getCurrent(), fields, new Object[0][]);
        EPAssertionUtil.assertPropsPerRow(captureOp.getAndReset().get(0).toArray(), fields, new Object[][]{{"E1", 10}, {"E2", 20}});

        emitter.submit(new Object[]{"E3", 30});
        EPAssertionUtil.assertPropsPerRow(captureOp.getCurrent(), fields, new Object[][]{{"E3", 30}});

        // stays running until cancelled (no transition to complete)
        assertEquals(EPDataFlowState.RUNNING, instance.getState());

        instance.cancel();
        assertEquals(EPDataFlowState.CANCELLED, instance.getState());

        // test doc sample
        String epl = "create dataflow HelloWorldDataFlow\n" +
                "  create schema SampleSchema(text string),\t// sample type\t\t\n" +
                "\t\n" +
                "  Emitter -> helloworld.stream<SampleSchema> { name: 'myemitter' }\n" +
                "  LogSink(helloworld.stream) {}";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().getDataFlowRuntime().instantiate("HelloWorldDataFlow");
    }
}
