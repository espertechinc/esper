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

import com.espertech.esper.client.*;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationChoice;
import junit.framework.TestCase;
import org.apache.avro.generic.GenericData;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestDataFlowOpBeaconSource extends TestCase {

    private EPServiceProvider epService;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testBeaconWithBeans() throws Exception {

        ConfigurationEventTypeLegacy legacy = new ConfigurationEventTypeLegacy();
        legacy.setCodeGeneration(ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
        epService.getEPAdministrator().getConfiguration().addEventType("MyLegacyEvent", MyLegacyEvent.class.getName(), legacy);
        MyLegacyEvent resultLegacy = (MyLegacyEvent) runAssertionBeans("MyLegacyEvent");
        assertEquals("abc", resultLegacy.getMyfield());

        epService.getEPAdministrator().getConfiguration().addEventType("MyEventNoDefaultCtor", MyEventNoDefaultCtor.class);
        MyEventNoDefaultCtor resultNoDefCtor = (MyEventNoDefaultCtor) runAssertionBeans("MyEventNoDefaultCtor");
        assertEquals("abc", resultNoDefCtor.getMyfield());
    }

    public void testVariable() throws Exception {
        epService.getEPAdministrator().createEPL("create Schema SomeEvent()");
        epService.getEPAdministrator().createEPL("create variable int var_iterations=3");
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream<SomeEvent> {" +
                "  iterations : var_iterations" +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(3);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future));
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        df.start();
        Object[] output = future.get(2, TimeUnit.SECONDS);
        assertEquals(3, output.length);
        stmtGraph.destroy();
    }

    private Object runAssertionBeans(String typeName) throws Exception {
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "" +
                "BeaconSource -> BeaconStream<" + typeName + "> {" +
                "  myfield : 'abc', iterations : 1" +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future));
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        df.start();
        Object[] output = future.get(2, TimeUnit.SECONDS);
        assertEquals(1, output.length);
        stmtGraph.destroy();
        return output[0];
    }

    public void testBeaconFields() throws Exception {
        for (EventRepresentationChoice rep : new EventRepresentationChoice[] {EventRepresentationChoice.AVRO}) {
            runAssertionFields(rep, true);
            runAssertionFields(rep, false);
        }

        // test doc samples
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("generateTagId", this.getClass().getName(), "generateTagId");
        String epl = "create dataflow MyDataFlow\n" +
                "  create schema SampleSchema(tagId string, locX double, locY double)," +
                "  " +
                "  // BeaconSource that produces empty object-array events without delay or interval\n" +
                "  // until cancelled.\n" +
                "  BeaconSource -> stream.one {}\n" +
                "  \n" +
                "  // BeaconSource that produces one RFIDSchema event populating event properties\n" +
                "  // from a user-defined function \"generateTagId\" and values.\n" +
                "  BeaconSource -> stream.two<SampleSchema> {\n" +
                "    iterations : 1,\n" +
                "    tagId : generateTagId(),\n" +
                "    locX : 10,\n" +
                "    locY : 20 \n" +
                "  }\n" +
                "  \n" +
                "  // BeaconSource that produces 10 object-array events populating the price property \n" +
                "  // with a random value.\n" +
                "  BeaconSource -> stream.three {\n" +
                "    iterations : 1,\n" +
                "    interval : 10, // every 10 seconds\n" +
                "    initialDelay : 5, // start after 5 seconds\n" +
                "    price : Math.random() * 100,\n" +
                "  }";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow");

        // test options-provided beacon field
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        String eplMinimal = "create dataflow MyGraph " +
                "BeaconSource -> outstream<SupportBean> {iterations:1} " +
                "EventBusSink(outstream) {}";
        epService.getEPAdministrator().createEPL(eplMinimal);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.addParameterURI("BeaconSource/theString", "E1");
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyGraph", options);

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from SupportBean").addListener(listener);
        instance.run();
        Thread.sleep(200);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString".split(","), new Object[]{"E1"});

        // invalid: no output stream
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "DF1", "create dataflow DF1 BeaconSource {}",
                "Failed to instantiate data flow 'DF1': Failed initialization for operator 'BeaconSource': BeaconSource operator requires one output stream but produces 0 streams");
    }

    private void runAssertionFields(EventRepresentationChoice representationEnum, boolean eventbean) throws Exception {
        EPDataFlowInstantiationOptions options;

        epService.getEPAdministrator().createEPL("create " + representationEnum.getOutputTypeCreateSchemaName() + " schema MyEvent(p0 string, p1 long, p2 double)");
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "" +
                "BeaconSource -> BeaconStream<" + (eventbean ? "EventBean<MyEvent>" : "MyEvent") + "> {" +
                "  iterations : 3," +
                "  p0 : 'abc'," +
                "  p1 : Math.round(Math.random() * 10) + 1," +
                "  p2 : 1d," +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(3);
        options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future));
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        df.start();
        Object[] output = future.get(2, TimeUnit.SECONDS);
        assertEquals(3, output.length);
        for (int i = 0; i < 3; i++) {

            if (!eventbean) {
                if (representationEnum.isObjectArrayEvent()) {
                    Object[] row = (Object[]) output[i];
                    assertEquals("abc", row[0]);
                    long val = (Long) row[1];
                    assertTrue("val=" + val, val >= 0 && val <= 11);
                    assertEquals(1d, row[2]);
                }
                else if (representationEnum.isMapEvent()) {
                    Map row = (Map) output[i];
                    assertEquals("abc", row.get("p0"));
                    long val = (Long) row.get("p1");
                    assertTrue("val=" + val, val >= 0 && val <= 11);
                    assertEquals(1d, row.get("p2"));
                }
                else {
                    GenericData.Record row = (GenericData.Record) output[i];
                    assertEquals("abc", row.get("p0"));
                    long val = (Long) row.get("p1");
                    assertTrue("val=" + val, val >= 0 && val <= 11);
                    assertEquals(1d, row.get("p2"));
                }
            }
            else {
                EventBean row = (EventBean) output[i];
                assertEquals("abc", row.get("p0"));
            }
        }
        
        stmtGraph.destroy();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyEvent", true);
    }

    public void testBeaconNoType() throws Exception {

        EPDataFlowInstantiationOptions options;
        Object[] output;

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "BeaconSource -> BeaconStream {}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

        int countExpected = 10;
        DefaultSupportCaptureOp<Object> futureAtLeast = new DefaultSupportCaptureOp<Object>(countExpected);
        options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(futureAtLeast));
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        df.start();
        output = futureAtLeast.get(1, TimeUnit.SECONDS);
        assertTrue(countExpected <= output.length);
        df.cancel();

        // BeaconSource with given number of iterations
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowTwo " +
                "BeaconSource -> BeaconStream {" +
                "  iterations: 5" +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

        DefaultSupportCaptureOp<Object> futureExactTwo = new DefaultSupportCaptureOp<Object>(5);
        options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(futureExactTwo));
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowTwo", options).start();
        output = futureExactTwo.get(1, TimeUnit.SECONDS);
        assertEquals(5, output.length);

        // BeaconSource with delay
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowThree " +
                "BeaconSource -> BeaconStream {" +
                "  iterations: 2," +
                "  initialDelay: 0.5" +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

        DefaultSupportCaptureOp<Object> futureExactThree = new DefaultSupportCaptureOp<Object>(2);
        options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(futureExactThree));
        long start = System.currentTimeMillis();
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowThree", options).start();
        output = futureExactThree.get(1, TimeUnit.SECONDS);
        long end = System.currentTimeMillis();
        assertEquals(2, output.length);
        assertTrue("delta=" + (end - start), end - start > 490);

        // BeaconSource with period
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowFour " +
                "BeaconSource -> BeaconStream {" +
                "  interval: 0.5" +
                "}" +
                "DefaultSupportCaptureOp(BeaconStream) {}");

        DefaultSupportCaptureOp<Object> futureFour = new DefaultSupportCaptureOp<Object>(2);
        options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(futureFour));
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowFour", options).start();
        output = futureFour.get(1, TimeUnit.SECONDS);
        assertEquals(2, output.length);

        // test Beacon with define typed
        epService.getEPAdministrator().createEPL("create objectarray schema MyTestOAType(p1 string)");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowFive " +
                "BeaconSource -> BeaconStream<MyTestOAType> {" +
                "  interval: 0.5," +
                "  p1 : 'abc'" +
                "}");
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowFive");
    }

    public static String generateTagId() {
        return "";
    }

    public static class MyEventNoDefaultCtor {
        private String myfield;

        public MyEventNoDefaultCtor(String someOtherfield, int someOtherValue) {
        }

        public String getMyfield() {
            return myfield;
        }

        public void setMyfield(String myfield) {
            this.myfield = myfield;
        }
    }

    public static class MyLegacyEvent {
        private String myfield;

        public MyLegacyEvent() {
        }

        public String getMyfield() {
            return myfield;
        }

        public void setMyfield(String myfield) {
            this.myfield = myfield;
        }
    }
}
