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
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventBeanCollector;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventBeanCollectorContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphEventUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphParamProvider;
import com.espertech.esper.common.internal.event.core.EventServiceSendEventCommon;
import com.espertech.esper.common.internal.event.core.SendableEvent;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib.sleep;
import static org.junit.Assert.*;

public class EPLDataflowOpEventBusSource {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowAllTypes());
        execs.add(new EPLDataflowSchemaObjectArray());
        return execs;
    }

    private static class EPLDataflowAllTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionAllTypes(env, DefaultSupportGraphEventUtil.EVENTTYPENAME, DefaultSupportGraphEventUtil.getPOJOEventsSendable());
            runAssertionAllTypes(env, "MyMapEvent", DefaultSupportGraphEventUtil.getMapEventsSendable());
            runAssertionAllTypes(env, "MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEventsSendable());
            runAssertionAllTypes(env, "MyOAEvent", DefaultSupportGraphEventUtil.getOAEventsSendable());

            // invalid: no output stream
            tryInvalidCompile(env, "create dataflow DF1 EventBusSource {}",
                "Failed to obtain operator 'EventBusSource': EventBusSource operator requires one output stream but produces 0 streams");

            // invalid: type not found
            tryInvalidCompile(env, "create dataflow DF1 EventBusSource -> ABC {}",
                "Failed to obtain operator 'EventBusSource': EventBusSource operator requires an event type declated for the output stream");

            // test doc samples
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SampleSchema(tagId string, locX double, locY double)", path);
            String epl = "@name('flow') create dataflow MyDataFlow\n" +
                "\n" +
                "  // Receive all SampleSchema events from the event bus.\n" +
                "  // No transformation.\n" +
                "  EventBusSource -> stream.one<SampleSchema> {}\n" +
                "  \n" +
                "  // Receive all SampleSchema events with tag id '001' from the event bus.\n" +
                "  // No transformation.\n" +
                "  EventBusSource -> stream.one<SampleSchema> {\n" +
                "    filter : tagId = '001'\n" +
                "  }\n" +
                "\n" +
                "  // Receive all SampleSchema events from the event bus.\n" +
                "  // With collector that performs transformation.\n" +
                "  EventBusSource -> stream.two<SampleSchema> {\n" +
                "    collector : {\n" +
                "      class : '" + MyDummyCollector.class.getName() + "'\n" +
                "    },\n" +
                "  }";
            env.compileDeploy(epl, path);
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow");

            env.undeployAll();
        }
    }

    private static void runAssertionAllTypes(RegressionEnvironment env, String typeName, SendableEvent[] events) {
        env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
            "EventBusSource -> ReceivedStream<" + typeName + "> {} " +
            "DefaultSupportCaptureOp(ReceivedStream) {}");

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
            .operatorProvider(new DefaultSupportGraphOpProvider(future));
        EventServiceSendEventCommon eventService = (EventServiceSendEventCommon) env.eventService();

        events[0].send(eventService);
        assertEquals(0, future.getCurrent().length);

        EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

        events[0].send(eventService);
        assertEquals(0, future.getCurrent().length);

        df.start();

        // send events
        for (int i = 0; i < events.length; i++) {
            events[i].send(eventService);
        }

        // assert
        future.waitForInvocation(200, events.length);
        Object[] rows = future.getCurrentAndReset();
        assertEquals(events.length, rows.length);
        for (int i = 0; i < events.length; i++) {
            assertSame(events[i].getUnderlying(), rows[i]);
        }

        df.cancel();

        events[0].send(eventService);
        sleep(50);
        assertEquals(0, future.getCurrent().length);

        env.undeployAll();
    }

    private static class EPLDataflowSchemaObjectArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            EPCompiled compiled = env.compileWBusPublicType("create objectarray schema MyEventOA(p0 string, p1 long)");
            env.deploy(compiled);
            path.add(compiled);

            runAssertionOA(env, path, false);
            runAssertionOA(env, path, true);

            // test collector
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "EventBusSource -> ReceivedStream<MyEventOA> {filter: p0 like 'A%'} " +
                "DefaultSupportCaptureOp(ReceivedStream) {}", path);

            MyCollector collector = new MyCollector();
            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future))
                .parameterProvider(new DefaultSupportGraphParamProvider(Collections.singletonMap("collector", collector)));

            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            instance.start();

            env.sendEventObjectArray(new Object[]{"B", 100L}, "MyEventOA");
            sleep(50);
            assertNull(collector.getLast());

            env.sendEventObjectArray(new Object[]{"A", 101L}, "MyEventOA");
            future.waitForInvocation(100, 1);
            assertNotNull(collector.getLast().getEmitter());
            assertEquals("MyEventOA", collector.getLast().getEvent().getEventType().getName());
            assertEquals(false, collector.getLast().isSubmitEventBean());

            instance.cancel();

            env.undeployAll();
        }
    }

    private static void runAssertionOA(RegressionEnvironment env, RegressionPath path, boolean underlying) {
        env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
            "EventBusSource -> ReceivedStream<" + (underlying ? "MyEventOA" : "EventBean<MyEventOA>") + "> {} " +
            "DefaultSupportCaptureOp(ReceivedStream) {}", path);

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
            .operatorProvider(new DefaultSupportGraphOpProvider(future));

        EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
        instance.start();

        env.sendEventObjectArray(new Object[]{"abc", 100L}, "MyEventOA");
        Object[] rows = new Object[0];
        try {
            rows = future.get(1, TimeUnit.SECONDS);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        assertEquals(1, rows.length);
        if (underlying) {
            EPAssertionUtil.assertEqualsExactOrder((Object[]) rows[0], new Object[]{"abc", 100L});
        } else {
            EPAssertionUtil.assertProps((EventBean) rows[0], "p0,p1".split(","), new Object[]{"abc", 100L});
        }

        instance.cancel();
        env.undeployModuleContaining("flow");
    }

    public static class MyCollector implements EPDataFlowEventBeanCollector {
        private EPDataFlowEventBeanCollectorContext last;

        public void collect(EPDataFlowEventBeanCollectorContext context) {
            this.last = context;
            context.getEmitter().submit(context.getEvent());
        }

        public EPDataFlowEventBeanCollectorContext getLast() {
            return last;
        }
    }

    public static class MyDummyCollector implements EPDataFlowEventBeanCollector {
        public void collect(EPDataFlowEventBeanCollectorContext context) {

        }
    }
}
