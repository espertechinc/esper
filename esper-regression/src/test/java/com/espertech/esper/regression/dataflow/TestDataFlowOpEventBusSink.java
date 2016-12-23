/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.dataflow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.dataflow.EPDataFlowEventCollector;
import com.espertech.esper.client.dataflow.EPDataFlowEventCollectorContext;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.dataflow.util.DefaultSupportGraphEventUtil;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Arrays;

public class TestDataFlowOpEventBusSink extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private SupportUpdateListener listenerTwo;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
        listenerTwo = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
        listenerTwo = null;
    }

    public void testAllTypes() throws Exception
    {
        DefaultSupportGraphEventUtil.addTypeConfiguration(epService);

        runAssertionAllTypes("MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents());
        runAssertionAllTypes("MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents());
        runAssertionAllTypes("MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents());
        runAssertionAllTypes("MyEvent", DefaultSupportGraphEventUtil.getPOJOEvents());

        // invalid: output stream
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "DF1", "create dataflow DF1 EventBusSink -> s1 {}",
                "Failed to instantiate data flow 'DF1': Failed initialization for operator 'EventBusSink': EventBusSink operator does not provide an output stream");

        epService.getEPAdministrator().createEPL("create schema SampleSchema(tagId string, locX double, locY double)");
        String docSmple = "create dataflow MyDataFlow\n" +
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
        epService.getEPAdministrator().createEPL(docSmple);
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow");
    }

    private void runAssertionAllTypes(String typeName, Object[] events) throws Exception
    {
        String graph = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> instream<" + typeName + ">{}" +
                "EventBusSink(instream) {}";
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(graph);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from " + typeName);
        stmt.addListener(listener);

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(events);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(source));
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyGraph", options);
        instance.run();

        EPAssertionUtil.assertPropsPerRow(listener.getNewDataListFlattened(), "myDouble,myInt,myString".split(","), new Object[][] {{1.1d, 1, "one"}, {2.2d, 2, "two"}});
        listener.reset();

        stmtGraph.destroy();
    }

    public void testBeacon() throws Exception {
        epService.getEPAdministrator().createEPL("create objectarray schema MyEvent(p0 string, p1 long)");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyEvent");
        stmt.addListener(listener);

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "" +
                "BeaconSource -> BeaconStream<MyEvent> {" +
                "  iterations : 3," +
                "  p0 : 'abc'," +
                "  p1 : 1," +
                "}" +
                "EventBusSink(BeaconStream) {}");

        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", null).start();
        listener.waitForInvocation(3000, 3);
        EventBean[] events = listener.getNewDataListFlattened();

        for (int i = 0; i < 3; i++) {
            assertEquals("abc", events[i].get("p0"));
            long val = (Long) events[i].get("p1");
            assertTrue(val > 0 && val < 10);
        }
    }

    public void testSendEventDynamicType() throws Exception {
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventOne(type string, p0 int, p1 string)");
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventTwo(type string, f0 string, f1 int)");

        epService.getEPAdministrator().createEPL("select * from MyEventOne").addListener(listener);
        epService.getEPAdministrator().createEPL("select * from MyEventTwo").addListener(listenerTwo);

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlow " +
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
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow", options).start();

        listener.waitForInvocation(3000, 1);
        listenerTwo.waitForInvocation(3000, 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "p0,p1".split(","), new Object[]{100, "abc"});
        EPAssertionUtil.assertProps(listenerTwo.assertOneGetNewAndReset(), "f0,f1".split(","), new Object[] {"GE", -1});
    }

    public static class MyTransformToEventBus implements EPDataFlowEventCollector {

        public void collect(EPDataFlowEventCollectorContext context) {
            Object[] eventObj = (Object[]) context.getEvent();
            if (eventObj[0].equals("type1")) {
                context.getEventBusCollector().sendEvent(eventObj, "MyEventOne");
            }
            else {
                context.getEventBusCollector().sendEvent(eventObj, "MyEventTwo");
            }
        }
    }
}
