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
import com.espertech.esper.client.annotation.Audit;
import com.espertech.esper.client.annotation.Name;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.dataflow.EPDataFlowSignalFinalMarker;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.*;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.dataflow.MyLineFeedSource;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecDataflowAPIOpLifecycle implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionTypeEvent(epService);
        runAssertionFlowGraphSource(epService);
        runAssertionFlowGraphOperator(epService);
    }

    private void runAssertionTypeEvent(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addImport(MyCaptureOutputPortOp.class);
        epService.getEPAdministrator().createEPL("create schema MySchema(key string, value int)");
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne MyCaptureOutputPortOp -> outstream<EventBean<MySchema>> {}");

        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne");
        assertEquals("MySchema", MyCaptureOutputPortOp.getPort().getOptionalDeclaredType().getEventType().getName());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFlowGraphSource(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addImport(SupportGraphSource.class);
        SupportGraphSource.getAndResetLifecycle();

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlow @Name('Goodie') @Audit SupportGraphSource -> outstream<SupportBean> {propOne:'abc'}");
        assertEquals(0, SupportGraphSource.getAndResetLifecycle().size());

        // instantiate
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().dataFlowInstanceId("id1").dataFlowInstanceUserObject("myobject");
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow", options);

        List<Object> events = SupportGraphSource.getAndResetLifecycle();
        assertEquals(3, events.size());
        assertEquals("instantiated", events.get(0));    // instantiated
        assertEquals("setPropOne=abc", events.get(1));  // injected properties

        assertTrue(events.get(2) instanceof DataFlowOpInitializateContext); // called initialize
        DataFlowOpInitializateContext initContext = (DataFlowOpInitializateContext) events.get(2);
        assertNotNull(initContext.getAgentInstanceContext());
        assertNotNull(initContext.getRuntimeEventSender());
        assertNotNull(initContext.getServicesContext());
        assertNotNull(initContext.getStatementContext());
        assertEquals("id1", initContext.getDataflowInstanceId());
        assertEquals("myobject", initContext.getDataflowInstanceUserObject());
        assertEquals(0, initContext.getInputPorts().size());
        assertEquals(1, initContext.getOutputPorts().size());
        assertEquals("outstream", initContext.getOutputPorts().get(0).getStreamName());
        assertEquals("SupportBean", initContext.getOutputPorts().get(0).getOptionalDeclaredType().getEventType().getName());
        assertEquals(2, initContext.getOperatorAnnotations().length);
        assertEquals("Goodie", ((Name) initContext.getOperatorAnnotations()[0]).value());
        assertNotNull((Audit) initContext.getOperatorAnnotations()[1]);

        // run
        df.run();

        events = SupportGraphSource.getAndResetLifecycle();
        assertEquals(5, events.size());
        assertTrue(events.get(0) instanceof DataFlowOpOpenContext); // called open (GraphSource only)
        assertEquals("next(numrows=0)", events.get(1));
        assertEquals("next(numrows=1)", events.get(2));
        assertEquals("next(numrows=2)", events.get(3));
        assertTrue(events.get(4) instanceof DataFlowOpCloseContext); // called close (GraphSource only)

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFlowGraphOperator(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addImport(MyLineFeedSource.class);
        epService.getEPAdministrator().getConfiguration().addImport(SupportOperator.class);
        SupportGraphSource.getAndResetLifecycle();

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlow MyLineFeedSource -> outstream {} SupportOperator(outstream) {propOne:'abc'}");
        assertEquals(0, SupportOperator.getAndResetLifecycle().size());

        // instantiate
        MyLineFeedSource src = new MyLineFeedSource(Arrays.asList("abc", "def").iterator());
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src));
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow", options);

        List<Object> events = SupportOperator.getAndResetLifecycle();
        assertEquals(3, events.size());
        assertEquals("instantiated", events.get(0));    // instantiated
        assertEquals("setPropOne=abc", events.get(1));  // injected properties

        assertTrue(events.get(2) instanceof DataFlowOpInitializateContext); // called initialize
        DataFlowOpInitializateContext initContext = (DataFlowOpInitializateContext) events.get(2);
        assertNotNull(initContext.getAgentInstanceContext());
        assertNotNull(initContext.getRuntimeEventSender());
        assertNotNull(initContext.getServicesContext());
        assertNotNull(initContext.getStatementContext());
        assertNull(initContext.getDataflowInstanceId());
        assertNull(initContext.getDataflowInstanceUserObject());
        assertEquals(1, initContext.getInputPorts().size());
        assertEquals("[line]", Arrays.toString(initContext.getInputPorts().get(0).getTypeDesc().getEventType().getPropertyNames()));
        assertEquals("[outstream]", Arrays.toString(initContext.getInputPorts().get(0).getStreamNames().toArray()));
        assertEquals(0, initContext.getOutputPorts().size());

        // run
        df.run();

        events = SupportOperator.getAndResetLifecycle();
        assertEquals(4, events.size());
        assertTrue(events.get(0) instanceof DataFlowOpOpenContext); // called open (GraphSource only)
        assertEquals("abc", ((Object[]) events.get(1))[0]);
        assertEquals("def", ((Object[]) events.get(2))[0]);
        assertTrue(events.get(3) instanceof DataFlowOpCloseContext); // called close (GraphSource only)

        epService.getEPAdministrator().destroyAllStatements();
    }

    public static class SupportGraphSource implements DataFlowSourceOperator {

        private String propOne;

        private int numrows;

        @DataFlowContext
        private EPDataFlowEmitter graphContext;

        private static List<Object> lifecycle = new ArrayList<Object>();

        public SupportGraphSource() {
            lifecycle.add("instantiated");
        }

        public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
            lifecycle.add(context);
            return null;
        }

        public void open(DataFlowOpOpenContext openContext) {
            lifecycle.add(openContext);
        }

        public void close(DataFlowOpCloseContext closeContext) {
            lifecycle.add(closeContext);
        }

        public void next() throws InterruptedException {
            lifecycle.add("next(numrows=" + numrows + ")");
            if (numrows < 2) {
                numrows++;
                graphContext.submit("E" + numrows);
            } else {
                graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {
                });
            }
        }

        public static List<Object> getAndResetLifecycle() {
            List<Object> copy = new ArrayList<Object>(lifecycle);
            lifecycle = new ArrayList<Object>();
            return copy;
        }

        public String getPropOne() {
            return propOne;
        }

        public void setPropOne(String propOne) {
            lifecycle.add("setPropOne=" + propOne);
            this.propOne = propOne;
        }

        public void setGraphContext(EPDataFlowEmitter graphContext) {
            lifecycle.add(graphContext);
            this.graphContext = graphContext;
        }
    }

    @DataFlowOperator
    public static class SupportOperator implements DataFlowOpLifecycle {

        private String propOne;

        @DataFlowContext
        private EPDataFlowEmitter graphContext;

        private static List<Object> lifecycle = new ArrayList<Object>();

        public static List<Object> getAndResetLifecycle() {
            List<Object> copy = new ArrayList<Object>(lifecycle);
            lifecycle = new ArrayList<Object>();
            return copy;
        }

        public SupportOperator() {
            lifecycle.add("instantiated");
        }

        public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
            lifecycle.add(context);
            return null;
        }

        public String getPropOne() {
            return propOne;
        }

        public void setPropOne(String propOne) {
            lifecycle.add("setPropOne=" + propOne);
            this.propOne = propOne;
        }

        public void setGraphContext(EPDataFlowEmitter graphContext) {
            lifecycle.add(graphContext);
            this.graphContext = graphContext;
        }

        public void onInput(Object abc) {
            lifecycle.add(abc);
        }

        public void open(DataFlowOpOpenContext openContext) {
            lifecycle.add(openContext);
        }

        public void close(DataFlowOpCloseContext closeContext) {
            lifecycle.add(closeContext);
        }
    }

    @DataFlowOperator
    public static class MyCaptureOutputPortOp implements DataFlowOpLifecycle {
        private static DataFlowOpOutputPort port;

        public static DataFlowOpOutputPort getPort() {
            return port;
        }

        public static void setPort(DataFlowOpOutputPort port) {
            MyCaptureOutputPortOp.port = port;
        }

        public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
            port = context.getOutputPorts().get(0);
            return null;
        }

        public void open(DataFlowOpOpenContext openContext) {

        }

        public void close(DataFlowOpCloseContext openContext) {

        }
    }
}
