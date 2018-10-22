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
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import static junit.framework.TestCase.assertEquals;

public class EPLDataflowAPIStartCaptive implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        String[] fields = "p0,p1".split(",");

        env.compileDeploy("@name('flow') create dataflow MyDataFlow " +
            "Emitter -> outstream<MyOAEventType> {name:'src1'}" +
            "DefaultSupportCaptureOp(outstream) {}");

        DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(captureOp));

        EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow", options);
        EPDataFlowInstanceCaptive captiveStart = instance.startCaptive();
        assertEquals(0, captiveStart.getRunnables().size());
        assertEquals(1, captiveStart.getEmitters().size());
        EPDataFlowEmitterOperator emitter = captiveStart.getEmitters().get("src1");
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

        env.undeployAll();

        // test doc sample
        String epl = "@name('flow') create dataflow HelloWorldDataFlow\n" +
            "  create schema SampleSchema(text string),\t// sample type\t\t\n" +
            "\t\n" +
            "  Emitter -> helloworld.stream<SampleSchema> { name: 'myemitter' }\n" +
            "  LogSink(helloworld.stream) {}";
        env.compileDeploy(epl);
        env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "HelloWorldDataFlow");

        env.undeployAll();
    }
}
