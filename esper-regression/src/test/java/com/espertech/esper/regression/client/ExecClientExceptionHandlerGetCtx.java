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
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.hook.ExceptionHandlerContext;
import com.espertech.esper.client.hook.ExceptionHandlerFactoryContext;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportExceptionHandlerFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class ExecClientExceptionHandlerGetCtx implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        // add same factory twice
        SupportExceptionHandlerFactory.getFactoryContexts().clear();
        SupportExceptionHandlerFactory.getHandlers().clear();
        configuration.getEngineDefaults().getExceptionHandling().getHandlerFactories().clear();
        configuration.getEngineDefaults().getExceptionHandling().addClass(SupportExceptionHandlerFactory.class);
        configuration.getEngineDefaults().getExceptionHandling().addClass(SupportExceptionHandlerFactory.class);
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addPlugInAggregationFunctionFactory("myinvalidagg", ExecClientExceptionHandlerNoHandler.InvalidAggTestFactory.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportExceptionHandlerFactory.getFactoryContexts().clear();
        SupportExceptionHandlerFactory.getHandlers().clear();
        epService.initialize();

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
    }
}
