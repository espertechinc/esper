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
package com.espertech.esperio.amqp.regression;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOpForge;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.common.internal.util.SerializerUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esperio.amqp.AMQPSource;
import com.espertech.esperio.amqp.AMQPSupportReceiveCallback;
import com.espertech.esperio.amqp.AMQPSupportReceiveRunnable;
import com.espertech.esperio.amqp.AMQPSupportSendRunnable;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestAMQPGraphs extends TestCase {
    private EPRuntime runtime;

    protected void setUp() {
        Configuration configuration = new Configuration();
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getCommon().addImport(DefaultSupportCaptureOpForge.class.getPackage().getName() + ".*");
        configuration.getCommon().addImport(AMQPSource.class.getPackage().getName() + ".*");
        runtime = EPRuntimeProvider.getDefaultRuntime(configuration);
        runtime.initialize();
    }

    public void testAMQPInput() throws Exception {

        String queueName = TestAMQPGraphs.class.getSimpleName() + "-InputQueue";
        String[] fields = "myString,myInt,myDouble".split(",");
        compileDeploy("@public @buseventtype create schema MyMapEvent(myString string, myInt int, myDouble double)");

        String graph = "create dataflow ReadAMQPGraph " +
            "AMQPSource -> outstream<MyMapEvent> {" +
            "  host: 'localhost'," +
            "  queueName: '" + queueName + "', " +
            "  collector: {class: 'AMQPToObjectCollectorSerializable'}," +
            "  logMessages: true" +
            "}" +
            "DefaultSupportCaptureOp(outstream) {}";
        EPDeployment deployed = compileDeploy(graph);

        DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>(3);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(captureOp));
        EPDataFlowInstance df = runtime.getDataFlowService().instantiate(deployed.getDeploymentId(), "ReadAMQPGraph", options);
        df.start();

        AMQPSupportSendRunnable runnable = new AMQPSupportSendRunnable("localhost", queueName, getEvents(3), 0);
        runnable.run();

        Object[] received = captureOp.get(3, TimeUnit.SECONDS);
        EPAssertionUtil.assertPropsPerRow(received, fields, new Object[][]{{"E10", 0, 0d}, {"E11", 1, 1d}, {"E12", 2, 2d}});

        df.cancel();
    }

    public void testAMQPOutput() throws Exception {

        String queueName = TestAMQPGraphs.class.getSimpleName() + "-OutputQueue";
        String[] fields = "myString,myInt,myDouble".split(",");
        compileDeploy("@public @buseventtype create schema MyMapEvent(myString string, myInt int, myDouble double)");

        String graph = "create dataflow WriteAMQPGraph " +
            "DefaultSupportSourceOp -> outstream<MyMapEvent> {}" +
            "AMQPSink(outstream) {" +
            "  host: 'localhost', " +
            "  queueName: '" + queueName + "', " +
            "  collector: {class: 'ObjectToAMQPCollectorSerializable'}, " +
            "}";
        EPDeployment deployed = compileDeploy(graph);

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{makeEvent("E10", 0, 0), makeEvent("E11", 1, 1), makeEvent("E12", 2, 2)});
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(source));
        EPDataFlowInstance df = runtime.getDataFlowService().instantiate(deployed.getDeploymentId(), "WriteAMQPGraph", options);
        df.start();

        ReceiverHelper receiverHelper = new ReceiverHelper();
        AMQPSupportReceiveRunnable runnable = new AMQPSupportReceiveRunnable("localhost", queueName, 20000, receiverHelper);
        Thread runner = new Thread(runnable);
        runner.start();

        receiverHelper.getWaitReceived(3);
        EPAssertionUtil.assertPropsPerRow(toMapArray(receiverHelper.getReceived()), fields, new Object[][]{{"E10", 0, 0d}, {"E11", 1, 1d}, {"E12", 2, 2d}});

        runner.interrupt();
        runner.join();

        df.cancel();
    }

    private Map[] toMapArray(List<Object> rows) {
        Map[] maps = new Map[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            maps[i] = (Map) rows.get(i);
        }
        return maps;
    }

    private List<Object> getEvents(int count) {
        List<Object> events = new ArrayList<Object>();
        for (int i = 0; i < count; i++) {
            events.add(makeEvent("E" + (i + 10), i, (double) i));
        }
        return events;
    }

    private Map<String, Object> makeEvent(String myString, int myInt, double myDouble) {
        Map<String, Object> scoreEvent = new HashMap<String, Object>();
        scoreEvent.put("myString", myString);
        scoreEvent.put("myInt", myInt);
        scoreEvent.put("myDouble", myDouble);
        return scoreEvent;
    }

    public static class ReceiverHelper implements AMQPSupportReceiveCallback {
        private List<Object> received = new ArrayList<Object>();

        public void handleMessage(byte[] bytes) {
            Object message = SerializerUtil.byteArrToObject(bytes);
            received.add(message);
        }

        public List<Object> getReceived() {
            return received;
        }

        public List<Object> getWaitReceived(int numEvents) {
            while (received.size() < numEvents) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return received;
        }
    }

    private EPDeployment compileDeploy(String epl) {
        try {
            CompilerArguments args = new CompilerArguments(runtime.getConfigurationDeepCopy());
            args.getPath().add(runtime.getRuntimePath());
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            return runtime.getDeploymentService().deploy(compiled);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
