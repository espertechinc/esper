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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventCollector;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventCollectorContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphEventUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.dataflow.MyObjectArrayGraphSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EPLDataflowOpEventBusSink {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowAllTypes());
        execs.add(new EPLDataflowBeacon());
        execs.add(new EPLDataflowSendEventDynamicType());
        return execs;
    }

    private static class EPLDataflowAllTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionAllTypes(env, "MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents());
            runAssertionAllTypes(env, "MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents());
            runAssertionAllTypes(env, "MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents());
            runAssertionAllTypes(env, DefaultSupportGraphEventUtil.EVENTTYPENAME, DefaultSupportGraphEventUtil.getPOJOEvents());

            // invalid: output stream
            tryInvalidCompile(env, "create dataflow DF1 EventBusSink -> s1 {}",
                "Failed to obtain operator 'EventBusSink': EventBusSink operator does not provide an output stream");

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SampleSchema(tagId string, locX double, locY double)", path);
            String docSmple = "@name('s0') create dataflow MyDataFlow\n" +
                "BeaconSource -> instream<SampleSchema> {} // produces sample stream to\n" +
                "//demonstrate below\n" +
                "// Send SampleSchema events produced by beacon to the event bus.\n" +
                "EventBusSink(instream) {}\n" +
                "\n" +
                "// Send SampleSchema events produced by beacon to the event bus.\n" +
                "// With collector that performs transformation.\n" +
                "EventBusSink(instream) {\n" +
                "collector : {\n" +
                "class : '" + MyTransformToEventBus.class.getName() + "'\n" +
                "}\n" +
                "}";
            env.compileDeploy(docSmple, path);
            env.runtime().getDataFlowService().instantiate(env.deploymentId("s0"), "MyDataFlow");

            env.undeployAll();
        }
    }

    private static void runAssertionAllTypes(RegressionEnvironment env, String typeName, Object[] events) {
        String graph = "@name('flow') create dataflow MyGraph " +
            "DefaultSupportSourceOp -> instream<" + typeName + ">{}" +
            "EventBusSink(instream) {}";
        env.compileDeploy(graph);

        env.compileDeploy("@name('s0') select * from " + typeName).addListener("s0");

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(events);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(source));
        EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyGraph", options);
        instance.run();

        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(), "myDouble,myInt,myString".split(","), new Object[][]{{1.1d, 1, "one"}, {2.2d, 2, "two"}});

        env.undeployAll();
    }

    private static class EPLDataflowBeacon implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create objectarray schema MyEventBeacon(p0 string, p1 long)", path);
            env.compileDeploy("@name('s0') select * from MyEventBeacon", path).addListener("s0");
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "" +
                "BeaconSource -> BeaconStream<MyEventBeacon> {" +
                "  iterations : 3," +
                "  p0 : 'abc'," +
                "  p1 : 1," +
                "}" +
                "EventBusSink(BeaconStream) {}", path);

            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne").start();
            env.listener("s0").waitForInvocation(3000, 3);
            EventBean[] events = env.listener("s0").getNewDataListFlattened();

            for (int i = 0; i < 3; i++) {
                assertEquals("abc", events[i].get("p0"));
                long val = (Long) events[i].get("p1");
                assertTrue(val > 0 && val < 10);
            }

            env.undeployAll();
        }
    }

    private static class EPLDataflowSendEventDynamicType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String schemaEPL =
                "create objectarray schema MyEventOne(type string, p0 int, p1 string);\n" +
                    "create objectarray schema MyEventTwo(type string, f0 string, f1 int);\n";
            env.compileDeployWBusPublicType(schemaEPL, path);

            env.compileDeploy("@name('s0') select * from MyEventOne", path).addListener("s0");
            env.compileDeploy("@name('s1') select * from MyEventTwo", path).addListener("s1");

            env.compileDeploy("@name('flow') create dataflow MyDataFlow " +
                "MyObjectArrayGraphSource -> OutStream<?> {}" +
                "EventBusSink(OutStream) {" +
                "  collector : {" +
                "    class: '" + MyTransformToEventBus.class.getName() + "'" +
                "  }" +
                "}");

            MyObjectArrayGraphSource source = new MyObjectArrayGraphSource(Arrays.asList(
                new Object[]{"type1", 100, "abc"},
                new Object[]{"type2", "GE", -1}
            ).iterator());
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(source));
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow", options).start();

            env.listener("s0").waitForInvocation(3000, 1);
            env.listener("s1").waitForInvocation(3000, 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "p0,p1".split(","), new Object[]{100, "abc"});
            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "f0,f1".split(","), new Object[]{"GE", -1});

            env.undeployAll();
        }
    }

    public static class MyTransformToEventBus implements EPDataFlowEventCollector {

        public void collect(EPDataFlowEventCollectorContext context) {
            if (!(context.getEvent() instanceof Object[])) {
                return; // ignoring other types of events
            }
            Object[] eventObj = (Object[]) context.getEvent();
            if (eventObj[0].equals("type1")) {
                context.getSender().sendEventObjectArray(eventObj, "MyEventOne");
            } else {
                context.getSender().sendEventObjectArray(eventObj, "MyEventTwo");
            }
        }
    }
}
