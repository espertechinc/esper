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
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstanceCaptive;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationException;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.dataflow.util.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.dataflow.SupportDataFlowAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecDataflowOpFilter implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportSourceOp.class.getPackage().getName() + ".*");
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionInvalid(epService);
        runAssertionAllTypes(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {

        // invalid: no filter
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "DF1", "create dataflow DF1 BeaconSource -> instream<SupportBean> {} Filter(instream) -> abc {}",
                "Failed to instantiate data flow 'DF1': Failed validation for operator 'Filter': Required parameter 'filter' providing the filter expression is not provided");

        // invalid: too many output streams
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "DF1", "create dataflow DF1 BeaconSource -> instream<SupportBean> {} Filter(instream) -> abc,def,efg { filter : true }",
                "Failed to instantiate data flow 'DF1': Failed initialization for operator 'Filter': Filter operator requires one or two output stream(s) but produces 3 streams");

        // invalid: too few output streams
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "DF1", "create dataflow DF1 BeaconSource -> instream<SupportBean> {} Filter(instream) { filter : true }",
                "Failed to instantiate data flow 'DF1': Failed initialization for operator 'Filter': Filter operator requires one or two output stream(s) but produces 0 streams");

        // invalid filter expressions
        tryInvalidInstantiate(epService, "theString = 1",
                "Failed to instantiate data flow 'MySelect': Failed validation for operator 'Filter': Failed to validate filter dataflow operator expression 'theString=1': Implicit conversion from datatype 'Integer' to 'String' is not allowed");

        tryInvalidInstantiate(epService, "prev(theString, 1) = 'abc'",
                "Failed to instantiate data flow 'MySelect': Failed validation for operator 'Filter': Invalid filter dataflow operator expression 'prev(theString,1)=\"abc\"': Aggregation, sub-select, previous or prior functions are not supported in this context");
    }

    private void runAssertionAllTypes(EPServiceProvider epService) throws Exception {
        DefaultSupportGraphEventUtil.addTypeConfiguration(epService);

        runAssertionAllTypes(epService, "MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents());
        runAssertionAllTypes(epService, "MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents());
        runAssertionAllTypes(epService, "MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents());
        runAssertionAllTypes(epService, "MyEvent", DefaultSupportGraphEventUtil.getPOJOEvents());

        // test doc sample
        String epl = "create dataflow MyDataFlow\n" +
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
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow");

        // test two streams
        DefaultSupportCaptureOpStatic.getInstances().clear();
        String graph = "create dataflow MyFilter\n" +
                "Emitter -> sb<SupportBean> {name : 'e1'}\n" +
                "Filter(sb) -> out.ok, out.fail {filter: theString = 'x'}\n" +
                "DefaultSupportCaptureOpStatic(out.ok) {}" +
                "DefaultSupportCaptureOpStatic(out.fail) {}";
        epService.getEPAdministrator().createEPL(graph);

        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyFilter");
        EPDataFlowInstanceCaptive captive = instance.startCaptive();

        captive.getEmitters().get("e1").submit(new SupportBean("x", 10));
        captive.getEmitters().get("e1").submit(new SupportBean("y", 11));
        assertEquals(10, ((SupportBean) DefaultSupportCaptureOpStatic.getInstances().get(0).getCurrent().get(0)).getIntPrimitive());
        assertEquals(11, ((SupportBean) DefaultSupportCaptureOpStatic.getInstances().get(1).getCurrent().get(0)).getIntPrimitive());
        DefaultSupportCaptureOpStatic.getInstances().clear();
    }

    private void tryInvalidInstantiate(EPServiceProvider epService, String filter, String message) {
        String graph = "create dataflow MySelect\n" +
                "DefaultSupportSourceOp -> instream<SupportBean>{}\n" +
                "Filter(instream as ME) -> outstream {filter: " + filter + "}\n" +
                "DefaultSupportCaptureOp(outstream) {}";
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(graph);

        try {
            epService.getEPRuntime().getDataFlowRuntime().instantiate("MySelect");
            fail();
        } catch (EPDataFlowInstantiationException ex) {
            assertEquals(message, ex.getMessage());
        }

        stmtGraph.destroy();
    }

    private void runAssertionAllTypes(EPServiceProvider epService, String typeName, Object[] events) throws Exception {
        String graph = "create dataflow MySelect\n" +
                "DefaultSupportSourceOp -> instream.with.dot<" + typeName + ">{}\n" +
                "Filter(instream.with.dot) -> outstream.dot {filter: myString = 'two'}\n" +
                "DefaultSupportCaptureOp(outstream.dot) {}";
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(graph);

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(events);
        DefaultSupportCaptureOp<Object> capture = new DefaultSupportCaptureOp<>(2);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.setDataFlowInstanceUserObject("myuserobject");
        options.setDataFlowInstanceId("myinstanceid");
        options.operatorProvider(new DefaultSupportGraphOpProvider(source, capture));
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MySelect", options);
        assertEquals("myuserobject", instance.getUserObject());
        assertEquals("myinstanceid", instance.getInstanceId());

        instance.run();

        Object[] result = capture.getAndReset().get(0).toArray();
        assertEquals(1, result.length);
        assertSame(events[1], result[0]);

        instance.cancel();

        stmtGraph.destroy();
    }
}
