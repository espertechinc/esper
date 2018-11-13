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

import com.espertech.esper.common.client.dataflow.core.EPDataFlowEmitterOperator;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstanceCaptive;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.*;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.assertEquals;

public class EPLDataflowOpSelect {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowAllTypes());
        execs.add(new EPLDataflowDocSamples());
        execs.add(new EPLDataflowInvalid());
        execs.add(new EPLDataflowIterateFinalMarker());
        execs.add(new EPLDataflowOutputRateLimit());
        execs.add(new EPLDataflowTimeWindowTriggered());
        execs.add(new EPLDataflowFromClauseJoinOrder());
        execs.add(new EPLDataflowSelectPerformance());
        execs.add(new EPLDataflowOuterJoinMultirow());
        return execs;
    }

    private static class EPLDataflowDocSamples implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            String epl = "@name('flow') create dataflow MyDataFlow\n" +
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
            env.compileDeploy(epl);
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow");
            env.undeployAll();
        }
    }

    private static class EPLDataflowInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            tryInvalidCompileGraph(env, "insert into ABC select theString from ME", false,
                "Failed to obtain operator 'Select': Insert-into clause is not supported");

            tryInvalidCompileGraph(env, "select irstream theString from ME", false,
                "Failed to obtain operator 'Select': Selecting remove-stream is not supported");

            tryInvalidCompileGraph(env, "select theString from pattern[SupportBean]", false,
                "Failed to obtain operator 'Select': From-clause must contain only streams and cannot contain patterns or other constructs");

            tryInvalidCompileGraph(env, "select theString from DUMMY", false,
                "Failed to obtain operator 'Select': Failed to find stream 'DUMMY' among input ports, input ports are [ME]");

            tryInvalidCompileGraph(env, "select theString from ME output every 10 seconds", true,
                "Failed to obtain operator 'Select': Output rate limiting is not supported with 'iterate'");

            tryInvalidCompileGraph(env, "select (select * from SupportBean#lastevent) from ME", false,
                "Failed to obtain operator 'Select': Subselects are not supported");
        }
    }

    private static class EPLDataflowIterateFinalMarker implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            String graph = "@name('flow') create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean>{name: 'emitterS0'}\n" +
                "@Audit Select(instream_s0 as ALIAS) -> outstream {\n" +
                "  select: (select theString, sum(intPrimitive) as sumInt from ALIAS group by theString order by theString asc),\n" +
                "  iterate: true" +
                "}\n" +
                "DefaultSupportCaptureOp(outstream) {}\n";
            env.advanceTime(0);
            env.compileDeploy(graph);

            DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
            Map<String, Object> operators = CollectionUtil.populateNameValueMap("DefaultSupportCaptureOp", capture);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MySelect", options);
            EPDataFlowInstanceCaptive captive = instance.startCaptive();

            EPDataFlowEmitterOperator emitter = captive.getEmitters().get("emitterS0");
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
            env.undeployAll();
        }
    }

    private static class EPLDataflowOutputRateLimit implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            String graph = "@name('flow') create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean>{name: 'emitterS0'}\n" +
                "Select(instream_s0) -> outstream {\n" +
                "  select: (select sum(intPrimitive) as sumInt from instream_s0 output snapshot every 1 minute)\n" +
                "}\n" +
                "DefaultSupportCaptureOp(outstream) {}\n";
            env.advanceTime(0);
            env.compileDeploy(graph);

            DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
            Map<String, Object> operators = CollectionUtil.populateNameValueMap("DefaultSupportCaptureOp", capture);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MySelect", options);
            EPDataFlowInstanceCaptive captive = instance.startCaptive();
            EPDataFlowEmitterOperator emitter = captive.getEmitters().get("emitterS0");

            env.advanceTime(5000);
            emitter.submit(new SupportBean("E1", 5));
            emitter.submit(new SupportBean("E2", 3));
            emitter.submit(new SupportBean("E3", 6));
            assertEquals(0, capture.getCurrentAndReset().length);

            env.advanceTime(60000 + 5000);
            EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{14});

            emitter.submit(new SupportBean("E4", 3));
            emitter.submit(new SupportBean("E5", 6));
            assertEquals(0, capture.getCurrentAndReset().length);

            env.advanceTime(120000 + 5000);
            EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{14 + 9});

            instance.cancel();

            emitter.submit(new SupportBean("E5", 6));
            env.advanceTime(240000 + 5000);
            assertEquals(0, capture.getCurrentAndReset().length);

            env.undeployAll();
        }
    }

    private static class EPLDataflowTimeWindowTriggered implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            String graph = "@name('flow') create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean>{name: 'emitterS0'}\n" +
                "Select(instream_s0) -> outstream {\n" +
                "  select: (select sum(intPrimitive) as sumInt from instream_s0#time(1 minute))\n" +
                "}\n" +
                "DefaultSupportCaptureOp(outstream) {}\n";
            env.advanceTime(0);
            env.compileDeploy(graph);

            DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
            Map<String, Object> operators = CollectionUtil.populateNameValueMap("DefaultSupportCaptureOp", capture);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MySelect", options);
            EPDataFlowInstanceCaptive captive = instance.startCaptive();

            env.advanceTime(5000);
            captive.getEmitters().get("emitterS0").submit(new SupportBean("E1", 2));
            EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{2});

            env.advanceTime(10000);
            captive.getEmitters().get("emitterS0").submit(new SupportBean("E2", 5));
            EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{7});

            env.advanceTime(65000);
            EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "sumInt".split(","), new Object[]{5});

            instance.cancel();
            env.undeployAll();
        }
    }

    private static class EPLDataflowOuterJoinMultirow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            String graph = "@name('flow') create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean_S0>{name: 'emitterS0'}\n" +
                "Emitter -> instream_s1<SupportBean_S1>{name: 'emitterS1'}\n" +
                "Select(instream_s0 as S0, instream_s1 as S1) -> outstream {\n" +
                "  select: (select p00, p10 from S0#keepall full outer join S1#keepall)\n" +
                "}\n" +
                "DefaultSupportCaptureOp(outstream) {}\n";
            env.compileDeploy(graph);

            DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
            Map<String, Object> operators = CollectionUtil.populateNameValueMap("DefaultSupportCaptureOp", capture);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MySelect", options);

            EPDataFlowInstanceCaptive captive = instance.startCaptive();

            captive.getEmitters().get("emitterS0").submit(new SupportBean_S0(1, "S0_1"));
            EPAssertionUtil.assertProps(capture.getCurrentAndReset()[0], "p00,p11".split(","), new Object[]{"S0_1", null});

            instance.cancel();
            env.undeployAll();
        }
    }

    private static class EPLDataflowFromClauseJoinOrder implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            tryAssertionJoinOrder(env, "from S2#lastevent as s2, S1#lastevent as s1, S0#lastevent as s0");
            tryAssertionJoinOrder(env, "from S0#lastevent as s0, S1#lastevent as s1, S2#lastevent as s2");
            tryAssertionJoinOrder(env, "from S1#lastevent as s1, S2#lastevent as s2, S0#lastevent as s0");
        }

        private void tryAssertionJoinOrder(RegressionEnvironment env, String fromClause) {
            String graph = "@name('flow') create dataflow MySelect\n" +
                "Emitter -> instream_s0<SupportBean_S0>{name: 'emitterS0'}\n" +
                "Emitter -> instream_s1<SupportBean_S1>{name: 'emitterS1'}\n" +
                "Emitter -> instream_s2<SupportBean_S2>{name: 'emitterS2'}\n" +
                "Select(instream_s0 as S0, instream_s1 as S1, instream_s2 as S2) -> outstream {\n" +
                "  select: (select s0.id as s0id, s1.id as s1id, s2.id as s2id " + fromClause + ")\n" +
                "}\n" +
                "DefaultSupportCaptureOp(outstream) {}\n";
            env.compileDeploy(graph);

            DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>();
            Map<String, Object> operators = CollectionUtil.populateNameValueMap("DefaultSupportCaptureOp", capture);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));
            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MySelect", options);

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

            env.undeployAll();
        }
    }

    private static class EPLDataflowAllTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }
            runAssertionAllTypes(env, DefaultSupportGraphEventUtil.EVENTTYPENAME, DefaultSupportGraphEventUtil.getPOJOEvents());
            runAssertionAllTypes(env, "MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents());
            runAssertionAllTypes(env, "MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents());
            runAssertionAllTypes(env, "MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents());
        }
    }

    private static void runAssertionAllTypes(RegressionEnvironment env, String typeName, Object[] events) {
        String graph = "@name('flow') create dataflow MySelect\n" +
            "DefaultSupportSourceOp -> instream<" + typeName + ">{}\n" +
            "Select(instream as ME) -> outstream {select: (select myString, sum(myInt) as total from ME)}\n" +
            "DefaultSupportCaptureOp(outstream) {}";
        env.compileDeploy(graph);

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(events);
        DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>(2);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(source, capture));
        EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MySelect", options);

        instance.run();

        Object[] result = capture.getAndReset().get(0).toArray();
        EPAssertionUtil.assertPropsPerRow(result, "myString,total".split(","), new Object[][]{{"one", 1}, {"two", 3}});

        instance.cancel();

        env.undeployAll();
    }

    private static class EPLDataflowSelectPerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }

            env.compileDeploy("create objectarray schema MyEventOA(p0 string, p1 long);\n" +
                "@name('flow') create dataflow MyDataFlowOne " +
                "Emitter -> instream<MyEventOA> {name: 'E1'}" +
                "Select(instream as ME) -> astream {select: (select p0, sum(p1) from ME)}");
            EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne");
            EPDataFlowEmitterOperator emitter = df.startCaptive().getEmitters().get("E1");
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1; i++) {
                emitter.submit(new Object[]{"E1", 1L});
            }
            long end = System.currentTimeMillis();
            //System.out.println("delta=" + (end - start) / 1000d);

            env.undeployAll();
        }
    }

    private static void tryInvalidCompileGraph(RegressionEnvironment env, String select, boolean iterate, String message) {
        String graph = "@name('flow') create dataflow MySelect\n" +
            "DefaultSupportSourceOp -> instream<SupportBean>{}\n" +
            "Select(instream as ME) -> outstream {select: (" + select + "), iterate: " + iterate + "}\n" +
            "DefaultSupportCaptureOp(outstream) {}";
        tryInvalidCompile(env, graph, message);
    }
}
