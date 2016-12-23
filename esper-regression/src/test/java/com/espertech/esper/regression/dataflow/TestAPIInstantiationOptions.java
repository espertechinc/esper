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
import com.espertech.esper.client.dataflow.*;
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.interfaces.*;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestAPIInstantiationOptions extends TestCase {

    private EPServiceProvider epService;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().createEPL("create schema SomeType ()");
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testParameterInjectionCallback() throws Exception {
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne MyOp -> outstream<SomeType> {propOne:'abc', propThree:'xyz'}");

        MyOp myOp = new MyOp("myid");
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(myOp));
        MyParameterProvider myParameterProvider = new MyParameterProvider(Collections.<String, Object>singletonMap("propTwo", "def"));
        options.parameterProvider(myParameterProvider);
        assertEquals("myid", myOp.getId());
        assertNull(myOp.getPropOne());
        assertNull(myOp.getPropTwo());

        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        assertEquals("abc", myOp.getPropOne());
        assertEquals("def", myOp.getPropTwo());

        assertEquals(3, myParameterProvider.contextMap.size());
        assertNotNull(myParameterProvider.contextMap.get("propOne"));

        EPDataFlowOperatorParameterProviderContext context = myParameterProvider.contextMap.get("propTwo");
        assertEquals("propTwo", context.getParameterName());
        assertEquals("MyOp", context.getOperatorName());
        assertSame(myOp, context.getOperatorInstance());
        assertEquals(0, context.getOperatorNum());
        assertEquals(null, context.getProvidedValue());
        assertEquals("MyDataFlowOne", context.getDataFlowName());

        context = myParameterProvider.contextMap.get("propThree");
        assertEquals("propThree", context.getParameterName());
        assertEquals("MyOp", context.getOperatorName());
        assertSame(myOp, context.getOperatorInstance());
        assertEquals(0, context.getOperatorNum());
        assertEquals("xyz", context.getProvidedValue());
    }

    public void testOperatorInjectionCallback() {
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne MyOp -> outstream<SomeType> {propOne:'abc', propThree:'xyz'}");

        MyOperatorProvider myOperatorProvider = new MyOperatorProvider();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(myOperatorProvider);

        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);

        assertEquals(1, myOperatorProvider.contextMap.size());
        EPDataFlowOperatorProviderContext context = myOperatorProvider.contextMap.get("MyOp");
        assertEquals("MyOp", context.getOperatorName());
        assertNotNull(context.getSpec());
        assertEquals("MyDataFlowOne", context.getDataFlowName());
    }

    public static class MyOp implements DataFlowSourceOperator {

        private final String id;

        @DataFlowOpParameter
        private String propOne;

        @DataFlowOpParameter
        private String propTwo;

        @DataFlowOpParameter
        private String propThree;

        public MyOp(String id) {
            this.id = id;
        }

        public void next() throws InterruptedException {
        }

        public String getPropOne() {
            return propOne;
        }

        public void setPropOne(String propOne) {
            this.propOne = propOne;
        }

        public String getId() {
            return id;
        }

        public String getPropTwo() {
            return propTwo;
        }

        public String getPropThree() {
            return propThree;
        }

        public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
            return null;
        }

        public void open(DataFlowOpOpenContext openContext) {
        }

        public void close(DataFlowOpCloseContext openContext) {
        }
    }

    public static class MyParameterProvider implements EPDataFlowOperatorParameterProvider {

        private Map<String, EPDataFlowOperatorParameterProviderContext> contextMap = new HashMap<String, EPDataFlowOperatorParameterProviderContext>();
        private final Map<String, Object> values;

        public MyParameterProvider(Map<String, Object> values) {
            this.values = values;
        }

        public Object provide(EPDataFlowOperatorParameterProviderContext context) {
            contextMap.put(context.getParameterName(), context);
            return values.get(context.getParameterName());
        }
    }

    public static class MyOperatorProvider implements EPDataFlowOperatorProvider {
        private Map<String, EPDataFlowOperatorProviderContext> contextMap = new HashMap<String, EPDataFlowOperatorProviderContext>();

        public Object provide(EPDataFlowOperatorProviderContext context) {
            contextMap.put(context.getOperatorName(), context);
            return new MyOp("test");
        }
    }
}
