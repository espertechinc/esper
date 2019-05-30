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
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
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
import com.espertech.esperio.amqp.*;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public void testAMQPInputMap() throws Exception {
        runAssertionInput("MyMapEvent", "map", "AMQPToObjectCollectorSerializable", getEventsSerializedMap(3));
    }

    public void testAMQPInputJson() throws Exception {
        runAssertionInput("MyJsonEvent", "json", "AMQPToObjectCollectorJson", getEventsJsonDefaultEncoding(3));
    }

    public void testAMQPOutputMap() throws Exception {
        Function<byte[], Object> deserializer = bytes -> SerializerUtil.byteArrToObject(bytes);
        Consumer<List<Object>> assertion = received -> EPAssertionUtil.assertPropsPerRow(toMapArray(received), "myString,myInt,myDouble".split(","), new Object[][]{{"E10", 0, 0d}, {"E11", 1, 1d}, {"E12", 2, 2d}});

        runAssertionOutput("MyMapEvent", "map", "ObjectToAMQPCollectorSerializable",
            new Object[]{makeEventMap("E10", 0, 0), makeEventMap("E11", 1, 1), makeEventMap("E12", 2, 2)},
            deserializer, assertion);
    }

    public void testAMQPOutputJSON() throws Exception {
        Function<byte[], Object> deserializer = bytes -> new String(bytes);
        Consumer<List<Object>> assertion = received -> {
            EPAssertionUtil.assertEqualsExactOrder(received.toArray(), new Object[] {
                "{\"myString\":\"E10\",\"myInt\":0,\"myDouble\":0}",
                "{\"myString\":\"E11\",\"myInt\":1,\"myDouble\":1}",
                "{\"myString\":\"E12\",\"myInt\":2,\"myDouble\":2}"});
        };

        runAssertionOutput("MyJsonEvent", "json", "ObjectToAMQPCollectorJson",
            new Object[]{makeEventJson("E10", 0, 0), makeEventJson("E11", 1, 1), makeEventJson("E12", 2, 2)},
            deserializer, assertion);
    }

    private void runAssertionOutput(String eventTypeName, String underlyingTypeName, String collectorClassName,
                                    Object[] events,
                                    Function<byte[], Object> deserializer,
                                    Consumer<List<Object>> assertion) throws Exception {

        String queueName = TestAMQPGraphs.class.getSimpleName() + "-OutputQueue";
        compileDeploy("@public @buseventtype create " + underlyingTypeName + " schema " + eventTypeName + "(myString string, myInt int, myDouble double)");

        String graph = "create dataflow WriteAMQPGraph " +
            "DefaultSupportSourceOp -> outstream<" + eventTypeName + "> {}" +
            "AMQPSink(outstream) {" +
            "  host: 'localhost', " +
            "  queueName: '" + queueName + "', " +
            "  collector: {class: '" + collectorClassName + "'}, " +
            "}";
        EPDeployment deployed = compileDeploy(graph);

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(events);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(source));
        EPDataFlowInstance df = runtime.getDataFlowService().instantiate(deployed.getDeploymentId(), "WriteAMQPGraph", options);
        df.start();

        ReceiverHelper receiverHelper = new ReceiverHelper(deserializer);
        AMQPSupportReceiveRunnable runnable = new AMQPSupportReceiveRunnable("localhost", queueName, 20000, receiverHelper);
        Thread runner = new Thread(runnable);
        runner.start();

        receiverHelper.getWaitReceived(3);
        assertion.accept(receiverHelper.getReceived());

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

    private List<byte[]> getEventsSerializedMap(int count) {
        List<byte[]> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> event = makeEventMap("E" + (i + 10), i, (double) i);
            byte[] serialized = SerializerUtil.objectToByteArr(event);
            events.add(serialized);
        }
        return events;
    }

    private List<byte[]> getEventsJsonDefaultEncoding(int count) {
        List<byte[]> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String event = makeEventJson("E" + (i + 10), i, (double) i);
            byte[] serialized = event.getBytes();
            events.add(serialized);
        }
        return events;
    }

    private Map<String, Object> makeEventMap(String myString, int myInt, double myDouble) {
        Map<String, Object> scoreEvent = new HashMap<String, Object>();
        scoreEvent.put("myString", myString);
        scoreEvent.put("myInt", myInt);
        scoreEvent.put("myDouble", myDouble);
        return scoreEvent;
    }

    private String makeEventJson(String myString, int myInt, double myDouble) {
        return new JsonObject().add("myString", myString).add("myInt", myInt).add("myDouble", myDouble).toString();
    }

    public static class ReceiverHelper implements AMQPSupportReceiveCallback {
        private final Function<byte[], Object> deserializer;
        private List<Object> received = new ArrayList<Object>();

        public ReceiverHelper(Function<byte[], Object> deserializer) {
            this.deserializer = deserializer;
        }

        public void handleMessage(byte[] bytes) {
            Object message = deserializer.apply(bytes);
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

    private void runAssertionInput(String eventTypeName, String underlyingType, String collectorClassName, List<byte[]> bytes) throws Exception {
        String queueName = TestAMQPGraphs.class.getSimpleName() + "-InputQueue";
        String[] fields = "myString,myInt,myDouble".split(",");
        compileDeploy("@public @buseventtype create " + underlyingType + " schema " + eventTypeName + "(myString string, myInt int, myDouble double)");

        String graph = "create dataflow ReadAMQPGraph " +
            "AMQPSource -> outstream<" + eventTypeName + "> {" +
            "  host: 'localhost'," +
            "  queueName: '" + queueName + "', " +
            "  collector: {class: '" + collectorClassName + "'}," +
            "  logMessages: true" +
            "}" +
            "DefaultSupportCaptureOp(outstream) {}";
        EPDeployment deployed = compileDeploy(graph);

        DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>(3);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(captureOp));
        EPDataFlowInstance df = runtime.getDataFlowService().instantiate(deployed.getDeploymentId(), "ReadAMQPGraph", options);
        df.start();

        AMQPSupportSendRunnable runnable = new AMQPSupportSendRunnable("localhost", queueName, bytes, 0);
        runnable.run();

        Object[] received = captureOp.get(3, TimeUnit.SECONDS);
        EPAssertionUtil.assertPropsPerRow(received, fields, new Object[][]{{"E10", 0, 0d}, {"E11", 1, 1d}, {"E12", 2, 2d}});

        df.cancel();
    }
}
