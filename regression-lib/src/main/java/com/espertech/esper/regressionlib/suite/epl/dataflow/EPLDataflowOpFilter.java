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

import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstanceCaptive;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphEventUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.dataflow.DefaultSupportCaptureOpStatic;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class EPLDataflowOpFilter {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowInvalid());
        execs.add(new EPLDataflowAllTypes());
        return execs;
    }

    private static class EPLDataflowInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // invalid: no filter
            tryInvalidCompile(env, "create dataflow DF1 BeaconSource -> instream<SupportBean> {} Filter(instream) -> abc {}",
                "Failed to obtain operator 'Filter': Required parameter 'filter' providing the filter expression is not provided");

            // invalid: too many output streams
            tryInvalidCompile(env, "create dataflow DF1 BeaconSource -> instream<SupportBean> {} Filter(instream) -> abc,def,efg { filter : true }",
                "Failed to obtain operator 'Filter': Filter operator requires one or two output stream(s) but produces 3 streams");

            // invalid: too few output streams
            tryInvalidCompile(env, "create dataflow DF1 BeaconSource -> instream<SupportBean> {} Filter(instream) { filter : true }",
                "Failed to obtain operator 'Filter': Filter operator requires one or two output stream(s) but produces 0 streams");

            // invalid filter expressions
            tryInvalidFilter(env, "theString = 1",
                "Failed to obtain operator 'Filter': Failed to validate filter dataflow operator expression 'theString=1': Implicit conversion from datatype 'Integer' to 'String' is not allowed");

            tryInvalidFilter(env, "prev(theString, 1) = 'abc'",
                "Failed to obtain operator 'Filter': Invalid filter dataflow operator expression 'prev(theString,1)=\"abc\"': Aggregation, sub-select, previous or prior functions are not supported in this context");
        }
    }

    private static class EPLDataflowAllTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            runAssertionAllTypes(env, DefaultSupportGraphEventUtil.EVENTTYPENAME, DefaultSupportGraphEventUtil.getPOJOEvents());
            runAssertionAllTypes(env, "MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents());
            runAssertionAllTypes(env, "MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents());
            runAssertionAllTypes(env, "MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents());

            // test doc sample
            String epl = "@name('flow') create dataflow MyDataFlow\n" +
                "  create schema SampleSchema(tagId string, locX double),\t// sample type\n" +
                "  BeaconSource -> samplestream<SampleSchema> {}\n" +
                "  \n" +
                "  // Filter all events that have a tag id of '001'\n" +
                "  Filter(samplestream) -> tags_001 {\n" +
                "    filter : tagId = '001' \n" +
                "  }\n" +
                "  \n" +
                "  // Filter all events that have a tag id of '001', putting all other tags into the second stream\n" +
                "  Filter(samplestream) -> tags_001, tags_other {\n" +
                "    filter : tagId = '001' \n" +
                "  }";
            env.compileDeploy(epl);
            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow");
            env.undeployAll();

            // test two streams
            DefaultSupportCaptureOpStatic.getInstances().clear();
            String graph = "@name('flow') create dataflow MyFilter\n" +
                "Emitter -> sb<SupportBean> {name : 'e1'}\n" +
                "Filter(sb) -> out.ok, out.fail {filter: theString = 'x'}\n" +
                "DefaultSupportCaptureOpStatic(out.ok) {}" +
                "DefaultSupportCaptureOpStatic(out.fail) {}";
            env.compileDeploy(graph);

            EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyFilter");
            EPDataFlowInstanceCaptive captive = instance.startCaptive();

            captive.getEmitters().get("e1").submit(new SupportBean("x", 10));
            captive.getEmitters().get("e1").submit(new SupportBean("y", 11));
            assertEquals(10, ((SupportBean) DefaultSupportCaptureOpStatic.getInstances().get(0).getCurrent().get(0)).getIntPrimitive());
            assertEquals(11, ((SupportBean) DefaultSupportCaptureOpStatic.getInstances().get(1).getCurrent().get(0)).getIntPrimitive());
            DefaultSupportCaptureOpStatic.getInstances().clear();

            env.undeployAll();
        }
    }

    private static void tryInvalidFilter(RegressionEnvironment env, String filter, String message) {
        String graph = "@name('flow') create dataflow MySelect\n" +
            "DefaultSupportSourceOp -> instream<SupportBean>{}\n" +
            "Filter(instream as ME) -> outstream {filter: " + filter + "}\n" +
            "DefaultSupportCaptureOp(outstream) {}";
        tryInvalidCompile(env, graph, message);
    }

    private static void runAssertionAllTypes(RegressionEnvironment env, String typeName, Object[] events) {
        String graph = "@name('flow') create dataflow MySelect\n" +
            "DefaultSupportSourceOp -> instream.with.dot<" + typeName + ">{}\n" +
            "Filter(instream.with.dot) -> outstream.dot {filter: myString = 'two'}\n" +
            "DefaultSupportCaptureOp(outstream.dot) {}";
        env.compileDeploy(graph);

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(events);
        DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>(2);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.setDataFlowInstanceUserObject("myuserobject");
        options.setDataFlowInstanceId("myinstanceid");
        options.operatorProvider(new DefaultSupportGraphOpProvider(source, capture));
        EPDataFlowInstance instance = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MySelect", options);
        assertEquals("myuserobject", instance.getUserObject());
        assertEquals("myinstanceid", instance.getInstanceId());

        instance.run();

        Object[] result = capture.getAndReset().get(0).toArray();
        assertEquals(1, result.length);
        assertSame(events[1], result[0]);

        instance.cancel();

        env.undeployAll();
    }
}
