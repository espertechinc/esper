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
import com.espertech.esper.client.dataflow.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.dataflow.ops.Emitter;
import com.espertech.esper.dataflow.util.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.CollectionUtil;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecDataflowOpSelect implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionDocSamples(epService);
        runAssertionInvalid(epService);
        runAssertionIterateFinalMarker(epService);
        runAssertionOutputRateLimit(epService);
        runAssertionTimeWindowTriggered(epService);
        runAssertionOuterJoinMultirow(epService);
        runAssertionFromClauseJoinOrder(epService);
        runAssertionAllTypes(epService);
        runAssertionSelectPerformance(epService);
    }

    private void runAssertionDocSamples(EPServiceProvider epService) {
        String epl = "create dataflow MyDataFlow\n" +
                "  create schema SampleSchema(tagId string, locX double),\t// sample type\t\t\t\n" +
                "  BeaconSource -> instream<SampleSchema> {}  // sample stream\n" +
                "  BeaconSource -> secondstream<SampleSchema> {}  // sample stream\n" +
                "  \n" +
                "  // Simple continuous count of events\n" +
                "  Select(instream) -> outstream {\n" +
                "    select: (select count(*) from instream)\n" +
                "  }\n" +
                "  \n" +
                "  // Demonstrate use of alias\n" +
                "  Select(instream as myalias) -> outstream {\n" +
                "    select: (select count(*) from myalias)\n" +
                "  }\n" +
                "  \n" +
                "  // Output only when the final marker arrives\n" +
                "  Select(instream as myalias) -> outstream {\n" +
                "    select: (select count(*) from myalias),\n" +
                "    iterate: true\n" +
                "  }\n" +
                "\n" +
                "  // Same input port for the two sample streams.\n" +
                "  Select( (instream, secondstream) as myalias) -> outstream {\n" +
                "    select: (select count(*) from myalias)\n" +
                "  }\n" +
                "\n" +
                "  // A join with multiple input streams,\n" +
                "  // joining the last event per stream forming pairs\n" +
                "  Select(instream, secondstream) -> outstream {\n" +
                "    select: (select a.tagId, b.tagId \n" +
                "                 from instream#lastevent as a, secondstream#lastevent as b)\n" +
                "  }\n" +
                "  \n" +
                "  // A join with multiple input streams and using aliases.\n" +
                "  @Audit Select(instream as S1, secondstream as S2) -> outstream {\n" +
                "    select: (select a.tagId, b.tagId \n" +
                "                 from S1#lastevent as a, S2#lastevent as b)\n" +
                "  }";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow");
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportSourceOp.class.getPackage().getName() + ".*");
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        tryInvalidInstantiate(epService, "insert into ABC select theString from ME", false,
                "Failed to instantiate data flow 'MySelect': Failed validation for operator 'Select': Insert-into clause is not supported");

        tryInvalidInstantiate(epService, "select irstream theString from ME", false,
                "Failed to instantiate data flow 'MySelect': Failed validation for operator 'Select': Selecting remove-stream is not supported");

        tryInvalidInstantiate(epService, "select theString from pattern[SupportBean]", false,
                "Failed to instantiate data flow 'MySelect': Failed validation for operator 'Select': From-clause must contain only streams and cannot contain patterns or other constructs");

        tryInvalidInstantiate(epService, "select theString from DUMMY", false,
                "Failed to instantiate data flow 'MySelect': Failed validation for operator 'Select': Failed to find stream 'DUMMY' among input ports, input ports are [ME]");

        tryInvalidInstantiate(epService, "select theString from ME output every 10 seconds", true,
                "Failed to instantiate data flow 'MySelect': Failed validation for operator 'Select': Output rate limiting is not supported with 'iterate'");

        tryInvalidInstantiate(epService, "select (select * from SupportBean#lastevent) from ME", false,
                "Failed to instantiate data flow 'MySelect': Failed validation for operator 'Select': Subselects are not supported");
    }

    private void tryInvalidInstantiate(EPServiceProvider epService, String select, boolean iterate, String message) {
        String graph = "create dataflow MySelect\n" +
                "DefaultSupportSourceOp -> instream<SupportBean>{}\n" +
                "Select(instream as ME) -> outstream {select: (" + select + "), iterate: " + iterate + "}\n" +
                "DefaultSupportCaptureOp(outstream) {}";
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(graph);

        try {
            epService.getEPRuntime().getDataFlowRuntime().instantiate("MySelect");
            fail();
        } catch (EPDataFlowInstantiationException ex) {
            assertEquals(message, ex.getMessage());
        } finally {
            stmtGraph.destroy();
        }
    }

    private void runAssertionIterateFinalMarker(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String graph = "create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean>{name: 'emitterS0'}\n" +
                "@Audit Select(instream_s0 as ALIAS) -> outstream {\n" +
                "  select: (select theString, sum(intPrimitive) as sumInt from ALIAS group by theString order by theString asc),\n" +
                "  iterate: true" +
                "}\n" +
                "CaptureOp(outstream) {}\n";
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL(graph);

        DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
        Map<String, Object> operators = CollectionUtil.populateNameValueMap("CaptureOp", capture);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MySelect", options);
        EPDataFlowInstanceCaptive captive = instance.startCaptive();

        Emitter emitter = captive.getEmitters().get("emitterS0");
        emitter.submit(new SupportBean("E3", 4));
        emitter.submit(new SupportBean("E2", 3));
        emitter.submit(new SupportBean("E1", 1));
        emitter.submit(new SupportBean("E2", 2));
        emitter.submit(new SupportBean("E1", 5));
        assertEquals(0, capture.getCurrent().length);

        emitter.submitSignal(new EPDataFlowSignalFinalMarker() {
        });
        EPAssertionUtil.assertPropsPerRow(capture.getCurrent(), "theString,sumInt".split(","), new Object[][]{{"E1", 6}, {"E2", 5}, {"E3", 4}});

        instance.cancel();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOutputRateLimit(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String graph = "create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean>{name: 'emitterS0'}\n" +
                "Select(instream_s0) -> outstream {\n" +
                "  select: (select sum(intPrimitive) as sumInt from instream_s0 output snapshot every 1 minute)\n" +
                "}\n" +
                "CaptureOp(outstream) {}\n";
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL(graph);

        DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
        Map<String, Object> operators = CollectionUtil.populateNameValueMap("CaptureOp", capture);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MySelect", options);
        EPDataFlowInstanceCaptive captive = instance.startCaptive();
        Emitter emitter = captive.getEmitters().get("emitterS0");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000));
        emitter.submit(new SupportBean("E1", 5));
        emitter.submit(new SupportBean("E2", 3));
        emitter.submit(new SupportBean("E3", 6));
        assertEquals(0, capture.getCurrentAndReset().length);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(60000 + 5000));
        EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{14});

        emitter.submit(new SupportBean("E4", 3));
        emitter.submit(new SupportBean("E5", 6));
        assertEquals(0, capture.getCurrentAndReset().length);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(120000 + 5000));
        EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{14 + 9});

        instance.cancel();

        emitter.submit(new SupportBean("E5", 6));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(240000 + 5000));
        assertEquals(0, capture.getCurrentAndReset().length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTimeWindowTriggered(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String graph = "create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean>{name: 'emitterS0'}\n" +
                "Select(instream_s0) -> outstream {\n" +
                "  select: (select sum(intPrimitive) as sumInt from instream_s0#time(1 minute))\n" +
                "}\n" +
                "CaptureOp(outstream) {}\n";
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL(graph);

        DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
        Map<String, Object> operators = CollectionUtil.populateNameValueMap("CaptureOp", capture);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MySelect", options);
        EPDataFlowInstanceCaptive captive = instance.startCaptive();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(5000));
        captive.getEmitters().get("emitterS0").submit(new SupportBean("E1", 2));
        EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{2});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        captive.getEmitters().get("emitterS0").submit(new SupportBean("E2", 5));
        EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{7});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(65000));
        EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{5});

        instance.cancel();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionOuterJoinMultirow(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);

        String graph = "create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean_S0>{name: 'emitterS0'}\n" +
                "Emitter -> instream_s1<SupportBean_S1>{name: 'emitterS1'}\n" +
                "Select(instream_s0 as S0, instream_s1 as S1) -> outstream {\n" +
                "  select: (select p00, p10 from S0#keepall full outer join S1#keepall)\n" +
                "}\n" +
                "CaptureOp(outstream) {}\n";
        epService.getEPAdministrator().createEPL(graph);

        DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
        Map<String, Object> operators = CollectionUtil.populateNameValueMap("CaptureOp", capture);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MySelect", options);

        EPDataFlowInstanceCaptive captive = instance.startCaptive();

        captive.getEmitters().get("emitterS0").submit(new SupportBean_S0(1, "S0_1"));
        EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "p00,p11".split(","), new Object[]{"S0_1", null});

        instance.cancel();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFromClauseJoinOrder(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S1.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S2.class);

        tryAssertionJoinOrder(epService, "from S2#lastevent as s2, S1#lastevent as s1, S0#lastevent as s0");
        tryAssertionJoinOrder(epService, "from S0#lastevent as s0, S1#lastevent as s1, S2#lastevent as s2");
        tryAssertionJoinOrder(epService, "from S1#lastevent as s1, S2#lastevent as s2, S0#lastevent as s0");
    }

    private void tryAssertionJoinOrder(EPServiceProvider epService, String fromClause) {
        String graph = "create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean_S0>{name: 'emitterS0'}\n" +
                "Emitter -> instream_s1<SupportBean_S1>{name: 'emitterS1'}\n" +
                "Emitter -> instream_s2<SupportBean_S2>{name: 'emitterS2'}\n" +
                "Select(instream_s0 as S0, instream_s1 as S1, instream_s2 as S2) -> outstream {\n" +
                "  select: (select s0.id as s0id, s1.id as s1id, s2.id as s2id " + fromClause + ")\n" +
                "}\n" +
                "CaptureOp(outstream) {}\n";
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(graph);

        DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
        Map<String, Object> operators = CollectionUtil.populateNameValueMap("CaptureOp", capture);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MySelect", options);

        EPDataFlowInstanceCaptive captive = instance.startCaptive();
        captive.getEmitters().get("emitterS0").submit(new SupportBean_S0(1));
        captive.getEmitters().get("emitterS1").submit(new SupportBean_S1(10));
        assertEquals(0, capture.getCurrent().length);

        captive.getEmitters().get("emitterS2").submit(new SupportBean_S2(100));
        assertEquals(1, capture.getCurrent().length);
        EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "s0id,s1id,s2id".split(","), new Object[]{1, 10, 100});

        instance.cancel();

        captive.getEmitters().get("emitterS2").submit(new SupportBean_S2(101));
        assertEquals(0, capture.getCurrent().length);

        stmtGraph.destroy();
    }

    private void runAssertionAllTypes(EPServiceProvider epService) throws Exception {
        DefaultSupportGraphEventUtil.addTypeConfiguration(epService);

        runAssertionAllTypes(epService, "MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents());
        runAssertionAllTypes(epService, "MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents());
        runAssertionAllTypes(epService, "MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents());
        runAssertionAllTypes(epService, "MyEvent", DefaultSupportGraphEventUtil.getPOJOEvents());
    }

    private void runAssertionAllTypes(EPServiceProvider epService, String typeName, Object[] events) throws Exception {
        String graph = "create dataflow MySelect\n" +
                "DefaultSupportSourceOp -> instream<" + typeName + ">{}\n" +
                "Select(instream as ME) -> outstream {select: (select myString, sum(myInt) as total from ME)}\n" +
                "DefaultSupportCaptureOp(outstream) {}";
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(graph);

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(events);
        DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>(2);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(source, capture));
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MySelect", options);

        instance.run();

        Object[] result = capture.getAndReset().get(0).toArray();
        EPAssertionUtil.assertPropsPerRow(result, "myString,total".split(","), new Object[][]{{"one", 1}, {"two", 3}});

        instance.cancel();

        stmtGraph.destroy();
    }

    private void runAssertionSelectPerformance(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create objectarray schema MyEventOA(p0 string, p1 long)");

        /*
        EPStatement stmt = epService.getEPAdministrator().createEPL("select p0, sum(p1) from MyEvent");

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            epService.getEPRuntime().sendEvent(new Object[] {"E1", 1L}, "MyEvent");
        }
        long end = System.currentTimeMillis();
        System.out.println("delta=" + (end - start) / 1000d);
        System.out.println(stmt.iterator().next().get("sum(p1)"));
        */

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "Emitter -> instream<MyEventOA> {name: 'E1'}" +
                "Select(instream as ME) -> astream {select: (select p0, sum(p1) from ME)}");
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne");
        Emitter emitter = df.startCaptive().getEmitters().get("E1");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            emitter.submit(new Object[]{"E1", 1L});
        }
        long end = System.currentTimeMillis();
        //System.out.println("delta=" + (end - start) / 1000d);
    }

}
