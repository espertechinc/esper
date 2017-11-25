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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.dataflow.EPDataFlowEventBeanCollector;
import com.espertech.esper.client.dataflow.EPDataFlowEventBeanCollectorContext;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphEventUtil;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.dataflow.util.DefaultSupportGraphParamProvider;
import com.espertech.esper.event.SendableEvent;
import com.espertech.esper.supportregression.dataflow.SupportDataFlowAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecDataflowOpEventBusSource implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionAllTypes(epService);
        runAssertionSchemaObjectArray(epService);
    }

    private void runAssertionAllTypes(EPServiceProvider epService) throws Exception {
        DefaultSupportGraphEventUtil.addTypeConfiguration(epService);

        runAssertionAllTypes(epService, "MyMapEvent", DefaultSupportGraphEventUtil.getMapEventsSendable());
        runAssertionAllTypes(epService, "MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEventsSendable());
        runAssertionAllTypes(epService, "MyOAEvent", DefaultSupportGraphEventUtil.getOAEventsSendable());
        runAssertionAllTypes(epService, "MyEvent", DefaultSupportGraphEventUtil.getPOJOEventsSendable());

        // invalid: no output stream
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "DF1", "create dataflow DF1 EventBusSource {}",
                "Failed to instantiate data flow 'DF1': Failed initialization for operator 'EventBusSource': EventBusSource operator requires one output stream but produces 0 streams");

        // invalid: type not found
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "DF1", "create dataflow DF1 EventBusSource -> ABC {}",
                "Failed to instantiate data flow 'DF1': Failed initialization for operator 'EventBusSource': EventBusSource operator requires an event type declated for the output stream");

        // test doc samples
        epService.getEPAdministrator().createEPL("create schema SampleSchema(tagId string, locX double, locY double)");
        String epl = "create dataflow MyDataFlow\n" +
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
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAllTypes(EPServiceProvider epService, String typeName, SendableEvent[] events) throws Exception {
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "EventBusSource -> ReceivedStream<" + typeName + "> {} " +
                "DefaultSupportCaptureOp(ReceivedStream) {}");

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future));

        events[0].send(epService.getEPRuntime());
        assertEquals(0, future.getCurrent().length);

        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        events[0].send(epService.getEPRuntime());
        assertEquals(0, future.getCurrent().length);

        df.start();

        // send events
        for (int i = 0; i < events.length; i++) {
            events[i].send(epService.getEPRuntime());
        }

        // assert
        future.waitForInvocation(200, events.length);
        Object[] rows = future.getCurrentAndReset();
        assertEquals(events.length, rows.length);
        for (int i = 0; i < events.length; i++) {
            assertSame(events[i].getUnderlying(), rows[i]);
        }

        df.cancel();

        events[0].send(epService.getEPRuntime());
        Thread.sleep(50);
        assertEquals(0, future.getCurrent().length);

        stmtGraph.destroy();
    }

    private void runAssertionSchemaObjectArray(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventOA(p0 string, p1 long)");

        runAssertionOA(epService, false);
        runAssertionOA(epService, true);

        // test collector
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "EventBusSource -> ReceivedStream<MyEventOA> {filter: p0 like 'A%'} " +
                "DefaultSupportCaptureOp(ReceivedStream) {}");

        MyCollector collector = new MyCollector();
        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future))
                .parameterProvider(new DefaultSupportGraphParamProvider(Collections.singletonMap("collector", collector)));

        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        instance.start();

        epService.getEPRuntime().sendEvent(new Object[]{"B", 100L}, "MyEventOA");
        Thread.sleep(50);
        assertNull(collector.getLast());

        epService.getEPRuntime().sendEvent(new Object[]{"A", 101L}, "MyEventOA");
        future.waitForInvocation(100, 1);
        assertNotNull(collector.getLast().getEmitter());
        assertEquals("MyEventOA", collector.getLast().getEvent().getEventType().getName());
        assertEquals(false, collector.getLast().isSubmitEventBean());

        instance.cancel();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOA(EPServiceProvider epService, boolean underlying) throws Exception {
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "EventBusSource -> ReceivedStream<" + (underlying ? "MyEventOA" : "EventBean<MyEventOA>") + "> {} " +
                "DefaultSupportCaptureOp(ReceivedStream) {}");

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future));

        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        instance.start();

        epService.getEPRuntime().sendEvent(new Object[]{"abc", 100L}, "MyEventOA");
        Object[] rows = future.get(1, TimeUnit.SECONDS);
        assertEquals(1, rows.length);
        if (underlying) {
            EPAssertionUtil.assertEqualsExactOrder((Object[]) rows[0], new Object[]{"abc", 100L});
        } else {
            EPAssertionUtil.assertProps((EventBean) rows[0], "p0,p1".split(","), new Object[]{"abc", 100L});
        }

        instance.cancel();
        stmtGraph.destroy();
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
