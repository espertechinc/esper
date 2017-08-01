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

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.ExceptionHandlerContext;
import com.espertech.esper.client.hook.ExceptionHandlerFactoryContext;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportExceptionHandlerFactory;
import com.espertech.esper.supportregression.client.SupportListenerTimerHRes;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecClientThreadedConfigInbound implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        SupportExceptionHandlerFactory.getFactoryContexts().clear();
        SupportExceptionHandlerFactory.getHandlers().clear();
        configuration.getEngineDefaults().getExceptionHandling().getHandlerFactories().clear();
        configuration.getEngineDefaults().getExceptionHandling().addClass(SupportExceptionHandlerFactory.class);

        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        configuration.getEngineDefaults().getThreading().setThreadPoolInbound(true);
        configuration.getEngineDefaults().getThreading().setThreadPoolInboundNumThreads(4);
        configuration.getEngineDefaults().getExpression().setUdfCache(false);
        configuration.addEventType("MyMap", new HashMap<>());
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addImport(SupportStaticMethodLib.class.getName());

        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        configuration.addEventType("XMLType", xmlDOMEventTypeDesc);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionEventsProcessed(epService);
        runAssertionExceptionHandler(epService);
    }

    private void runAssertionExceptionHandler(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("throwException", this.getClass().getName(), "throwException", ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED, true);
        String epl = "@Name('ABCName') select * from SupportBean(throwException())";
        epService.getEPAdministrator().createEPL(epl);

        SupportExceptionHandlerFactory.SupportExceptionHandler handler = SupportExceptionHandlerFactory.getHandlers().get(SupportExceptionHandlerFactory.getHandlers().size() - 1);
        epService.getEPRuntime().sendEvent(new SupportBean());

        int count = 0;
        while(true) {
            if (handler.getInboundPoolContexts().size() == 1) {
                break;
            }
            if (count++ < 100) {
                Thread.sleep(100);
            }
            if (count >= 100) {
                fail();
            }
        }
    }

    private void runAssertionEventsProcessed(EPServiceProvider epService) throws Exception {

        SupportListenerTimerHRes listenerOne = new SupportListenerTimerHRes();
        SupportListenerTimerHRes listenerTwo = new SupportListenerTimerHRes();
        SupportListenerTimerHRes listenerThree = new SupportListenerTimerHRes();
        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select SupportStaticMethodLib.sleep(100) from MyMap");
        stmtOne.addListener(listenerOne);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select SupportStaticMethodLib.sleep(100) from SupportBean");
        stmtTwo.addListener(listenerTwo);
        EPStatement stmtThree = epService.getEPAdministrator().createEPL("select SupportStaticMethodLib.sleep(100) from XMLType");
        stmtThree.addListener(listenerThree);

        EventSender senderOne = epService.getEPRuntime().getEventSender("MyMap");
        EventSender senderTwo = epService.getEPRuntime().getEventSender("SupportBean");
        EventSender senderThree = epService.getEPRuntime().getEventSender("XMLType");

        long start = System.nanoTime();
        for (int i = 0; i < 2; i++) {
            epService.getEPRuntime().sendEvent(new HashMap<String, Object>(), "MyMap");
            senderOne.sendEvent(new HashMap<String, Object>());
            epService.getEPRuntime().sendEvent(new SupportBean());
            senderTwo.sendEvent(new SupportBean());
            epService.getEPRuntime().sendEvent(SupportXML.getDocument("<myevent/>"));
            senderThree.sendEvent(SupportXML.getDocument("<myevent/>"));
        }
        long end = System.nanoTime();
        long delta = (end - start) / 1000000;
        assertTrue(delta < 500);

        Thread.sleep(1000);
        assertEquals(4, listenerOne.getNewEvents().size());
        assertEquals(4, listenerTwo.getNewEvents().size());
        assertEquals(4, listenerThree.getNewEvents().size());

        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertEquals(0, spi.getThreadingService().getInboundQueue().size());
        assertNotNull(spi.getThreadingService().getInboundThreadPool());

        stmtOne.destroy();
        stmtTwo.destroy();
        stmtThree.destroy();
    }

    // Used by test
    public static boolean throwException() {
        throw new RuntimeException("Intended for testing");
    }
}
