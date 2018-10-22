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
import com.espertech.esper.common.client.annotation.Audit;
import com.espertech.esper.common.client.annotation.Name;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.dataflow.MyLineFeedSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class EPLDataflowAPIOpLifecycle {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowTypeEvent());
        execs.add(new EPLDataflowFlowGraphSource());
        execs.add(new EPLDataflowFlowGraphOperator());
        return execs;
    }

    private static class EPLDataflowTypeEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compile("create schema MySchema(key string, value int);\n" +
                "@name('flow') create dataflow MyDataFlowOne MyCaptureOutputPortOp -> outstream<EventBean<MySchema>> {}");
            assertEquals("MySchema", MyCaptureOutputPortOpForge.getPort().getOptionalDeclaredType().getEventType().getName());

            env.undeployAll();
        }
    }

    private static class EPLDataflowFlowGraphSource implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportGraphSource.getAndResetLifecycle();

            EPCompiled compiled = env.compile("@name('flow') create dataflow MyDataFlow @Name('Goodie') @Audit SupportGraphSource -> outstream<SupportBean> {propOne:'abc'}");
            List<Object> events = SupportGraphSourceForge.getAndResetLifecycle();
            assertEquals(3, events.size());
            assertEquals("instantiated", events.get(0));
            assertEquals("setPropOne=abc", events.get(1));
            DataFlowOpForgeInitializeContext forgeCtx = (DataFlowOpForgeInitializeContext) events.get(2);
            assertEquals(0, forgeCtx.getInputPorts().size());
            assertEquals(1, forgeCtx.getOutputPorts().size());
            assertEquals("outstream", forgeCtx.getOutputPorts().get(0).getStreamName());
            assertEquals("SupportBean", forgeCtx.getOutputPorts().get(0).getOptionalDeclaredType().getEventType().getName());
            assertEquals(2, forgeCtx.getOperatorAnnotations().length);
            assertEquals("Goodie", ((Name) forgeCtx.getOperatorAnnotations()[0]).value());
            assertNotNull((Audit) forgeCtx.getOperatorAnnotations()[1]);
            assertEquals("MyDataFlow", forgeCtx.getDataflowName());
            assertEquals(0, forgeCtx.getOperatorNumber());

            env.deploy(compiled);
            events = SupportGraphSourceFactory.getAndResetLifecycle();
            assertEquals(3, events.size());
            assertEquals("instantiated", events.get(0));
            assertEquals("setPropOne=abc", events.get(1));
            DataFlowOpFactoryInitializeContext factoryCtx = (DataFlowOpFactoryInitializeContext) events.get(2);
            assertEquals("MyDataFlow", factoryCtx.getDataFlowName());
            assertEquals(0, factoryCtx.getOperatorNumber());
            assertNotNull(factoryCtx.getStatementContext());

            // instantiate
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().dataFlowInstanceId("id1").dataFlowInstanceUserObject("myobject");
            EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow", options);
            events = SupportGraphSourceFactory.getAndResetLifecycle();
            assertEquals(1, events.size());
            DataFlowOpInitializeContext opCtx = (DataFlowOpInitializeContext) events.get(0);
            assertEquals("MyDataFlow", opCtx.getDataFlowName());
            assertEquals("id1", opCtx.getDataFlowInstanceId());
            assertNotNull(opCtx.getAgentInstanceContext());
            assertEquals("myobject", opCtx.getDataflowInstanceUserObject());
            assertEquals(0, opCtx.getOperatorNumber());
            assertEquals("SupportGraphSource", opCtx.getOperatorName());

            events = SupportGraphSource.getAndResetLifecycle();
            assertEquals(1, events.size());
            assertEquals("instantiated", events.get(0));    // instantiated

            // run
            df.run();

            events = SupportGraphSource.getAndResetLifecycle();
            assertEquals(5, events.size());
            assertTrue(events.get(0) instanceof DataFlowOpOpenContext); // called open (GraphSource only)
            assertEquals("next(numrows=0)", events.get(1));
            assertEquals("next(numrows=1)", events.get(2));
            assertEquals("next(numrows=2)", events.get(3));
            assertTrue(events.get(4) instanceof DataFlowOpCloseContext); // called close (GraphSource only)

            env.undeployAll();
        }
    }

    private static class EPLDataflowFlowGraphOperator implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportGraphSource.getAndResetLifecycle();

            env.compileDeploy("@name('flow') create dataflow MyDataFlow MyLineFeedSource -> outstream {} SupportOperator(outstream) {propOne:'abc'}");
            assertEquals(0, SupportOperator.getAndResetLifecycle().size());

            // instantiate
            MyLineFeedSource src = new MyLineFeedSource(Arrays.asList("abc", "def").iterator());
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(src));
            EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow", options);

            List<Object> events = SupportOperator.getAndResetLifecycle();
            assertEquals(1, events.size());
            assertEquals("instantiated", events.get(0));    // instantiated

            // run
            df.run();

            events = SupportOperator.getAndResetLifecycle();
            assertEquals(4, events.size());
            assertTrue(events.get(0) instanceof DataFlowOpOpenContext); // called open (GraphSource only)
            assertEquals("abc", ((Object[]) events.get(1))[0]);
            assertEquals("def", ((Object[]) events.get(2))[0]);
            assertTrue(events.get(3) instanceof DataFlowOpCloseContext); // called close (GraphSource only)

            env.undeployAll();
        }
    }

    public static class SupportGraphSourceForge implements DataFlowOperatorForge {

        private String propOne;
        private static List<Object> lifecycle = new ArrayList<Object>();

        public SupportGraphSourceForge() {
            lifecycle.add("instantiated");
        }

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            lifecycle.add(context);
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return new SAIFFInitializeBuilder(SupportGraphSourceFactory.class, this.getClass(), "s", parent, symbols, classScope)
                .constant("propOne", propOne)
                .build();
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
    }

    public static class SupportGraphSourceFactory implements DataFlowOperatorFactory {

        private String propOne;
        private static List<Object> lifecycle = new ArrayList<Object>();

        public SupportGraphSourceFactory() {
            lifecycle.add("instantiated");
        }

        public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
            lifecycle.add(context);
        }

        public DataFlowOperator operator(DataFlowOpInitializeContext context) {
            lifecycle.add(context);
            return new SupportGraphSource();
        }

        public static List<Object> getAndResetLifecycle() {
            List<Object> copy = new ArrayList<Object>(lifecycle);
            lifecycle = new ArrayList<>();
            return copy;
        }

        public String getPropOne() {
            return propOne;
        }

        public void setPropOne(String propOne) {
            lifecycle.add("setPropOne=" + propOne);
            this.propOne = propOne;
        }
    }

    public static class SupportGraphSource implements DataFlowSourceOperator {

        private int numrows;

        @DataFlowContext
        private EPDataFlowEmitter graphContext;

        private static List<Object> lifecycle = new ArrayList<Object>();

        public SupportGraphSource() {
            lifecycle.add("instantiated");
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

        public void setGraphContext(EPDataFlowEmitter graphContext) {
            lifecycle.add(graphContext);
            this.graphContext = graphContext;
        }
    }

    public static class SupportOperator implements DataFlowOperatorLifecycle {

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

    public static class SupportOperatorForge implements DataFlowOperatorForge {
        @DataFlowOpParameter
        private String propOne;

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return newInstance(SupportOperatorFactory.class);
        }
    }

    public static class SupportOperatorFactory implements DataFlowOperatorFactory {

        public void initializeFactory(DataFlowOpFactoryInitializeContext context) {

        }

        public DataFlowOperator operator(DataFlowOpInitializeContext context) {
            return new SupportOperator();
        }
    }

    public static class MyCaptureOutputPortOpForge implements DataFlowOperatorForge {
        private static DataFlowOpOutputPort port;

        public static DataFlowOpOutputPort getPort() {
            return port;
        }

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            port = context.getOutputPorts().get(0);
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return newInstance("java.lang.Object");
        }
    }
}
