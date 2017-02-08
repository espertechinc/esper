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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.client.hook.ExceptionHandlerContext;
import com.espertech.esper.client.hook.ExceptionHandlerFactoryContext;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationValidationContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.client.SupportExceptionHandlerFactory;
import junit.framework.TestCase;

import java.util.List;

public class TestExceptionHandler extends TestCase
{
    private EPServiceProvider epService;

    public void testHandler()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        // add same factory twice
        config.getEngineDefaults().getExceptionHandling().getHandlerFactories().clear();
        config.getEngineDefaults().getExceptionHandling().addClass(SupportExceptionHandlerFactory.class);
        config.getEngineDefaults().getExceptionHandling().addClass(SupportExceptionHandlerFactory.class);
        config.addEventType("SupportBean", SupportBean.class);
        config.addPlugInAggregationFunctionFactory("myinvalidagg", InvalidAggTestFactory.class.getName());

        epService = EPServiceProviderManager.getDefaultProvider(config);
        SupportExceptionHandlerFactory.getFactoryContexts().clear();
        SupportExceptionHandlerFactory.getHandlers().clear();
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String epl = "@Name('ABCName') select myinvalidagg() from SupportBean";
        epService.getEPAdministrator().createEPL(epl);

        List<ExceptionHandlerFactoryContext> contexts = SupportExceptionHandlerFactory.getFactoryContexts();
        assertEquals(2, contexts.size());
        assertEquals(epService.getURI(), contexts.get(0).getEngineURI());
        assertEquals(epService.getURI(), contexts.get(1).getEngineURI());

        SupportExceptionHandlerFactory.SupportExceptionHandler handlerOne = SupportExceptionHandlerFactory.getHandlers().get(0);
        SupportExceptionHandlerFactory.SupportExceptionHandler handlerTwo = SupportExceptionHandlerFactory.getHandlers().get(1);
        epService.getEPRuntime().sendEvent(new SupportBean());

        assertEquals(1, handlerOne.getContexts().size());
        assertEquals(1, handlerTwo.getContexts().size());
        ExceptionHandlerContext ehc = handlerOne.getContexts().get(0);
        assertEquals(epService.getURI(), ehc.getEngineURI());
        assertEquals(epl, ehc.getEpl());
        assertEquals("ABCName", ehc.getStatementName());
        assertEquals("Sample exception", ehc.getThrowable().getMessage());
        assertNotNull(ehc.getCurrentEvent());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    /**
     * Ensure the support configuration has an exception handler that rethrows exceptions.
     */
    public void testSupportConfigHandlerRethrow()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.addPlugInAggregationFunctionFactory("myinvalidagg", InvalidAggTestFactory.class.getName());

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String epl = "@Name('ABCName') select myinvalidagg() from SupportBean";
        epService.getEPAdministrator().createEPL(epl);

        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        }
        catch (EPException ex) {
            // expected
        }
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    /**
     * Excercise the configuration that does not have an exception handler.
     */
    public void testNoHandler()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getExceptionHandling().getHandlerFactories().clear();
        config.addPlugInAggregationFunctionFactory("myinvalidagg", InvalidAggTestFactory.class.getName());

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String epl = "@Name('ABCName') select myinvalidagg() from SupportBean";
        epService.getEPAdministrator().createEPL(epl);

        epService.getEPRuntime().sendEvent(new SupportBean());

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public static class InvalidAggTestFactory implements AggregationFunctionFactory {

        @Override
        public void validate(AggregationValidationContext validationContext)
        {
        }

        @Override
        public Class getValueType() {
            return null;
        }

        public void setFunctionName(String functionName) {

        }

        public AggregationMethod newAggregator() {
            return new InvalidAggTest();
        }
    }

    public static class InvalidAggTest implements AggregationMethod {

        @Override
        public void enter(Object value) {
            throw new RuntimeException("Sample exception");
        }

        @Override
        public void leave(Object value) {
        }

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public void clear() {
        }
    }

}
