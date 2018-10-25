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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecutionWithConfigure;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.util.SupportExceptionHandlerFactory;
import com.espertech.esper.regressionlib.support.util.SupportListenerTimerHRes;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;

import java.util.HashMap;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class ClientRuntimeThreadedConfigInbound implements RegressionExecutionWithConfigure {
    public void configure(Configuration configuration) {
        SupportExceptionHandlerFactory.getFactoryContexts().clear();
        SupportExceptionHandlerFactory.getHandlers().clear();
        configuration.getRuntime().getExceptionHandling().getHandlerFactories().clear();
        configuration.getRuntime().getExceptionHandling().addClass(SupportExceptionHandlerFactory.class);

        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getRuntime().getThreading().setThreadPoolInbound(true);
        configuration.getRuntime().getThreading().setThreadPoolInboundNumThreads(4);
        configuration.getCompiler().getExpression().setUdfCache(false);
        configuration.getCommon().addEventType("MyMap", new HashMap<>());
        configuration.getCommon().addEventType("SupportBean", SupportBean.class);
        configuration.getCommon().addImport(SupportStaticMethodLib.class.getName());

        ConfigurationCommonEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationCommonEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        configuration.getCommon().addEventType("XMLType", xmlDOMEventTypeDesc);

        configuration.getCompiler().addPlugInSingleRowFunction("throwException", this.getClass().getName(), "throwException", ConfigurationCompilerPlugInSingleRowFunction.ValueCache.DISABLED, ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED, true);
    }

    public void run(RegressionEnvironment env) {
        runAssertionEventsProcessed(env);
        runAssertionExceptionHandler(env);
    }

    private void runAssertionExceptionHandler(RegressionEnvironment env) {
        String epl = "@Name('ABCName') select * from SupportBean(throwException())";
        env.compileDeploy(epl);

        SupportExceptionHandlerFactory.SupportExceptionHandler handler = SupportExceptionHandlerFactory.getHandlers().get(SupportExceptionHandlerFactory.getHandlers().size() - 1);
        env.sendEventBean(new SupportBean());

        int count = 0;
        while (true) {
            if (handler.getInboundPoolContexts().size() == 1) {
                break;
            }
            if (count++ < 100) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (count >= 100) {
                fail();
            }
        }

        env.undeployAll();
    }

    private void runAssertionEventsProcessed(RegressionEnvironment env) {

        SupportListenerTimerHRes listenerOne = new SupportListenerTimerHRes();
        SupportListenerTimerHRes listenerTwo = new SupportListenerTimerHRes();
        SupportListenerTimerHRes listenerThree = new SupportListenerTimerHRes();
        env.compileDeploy("@name('s0') select SupportStaticMethodLib.sleep(100) from MyMap").statement("s0").addListener(listenerOne);
        env.compileDeploy("@name('s1') select SupportStaticMethodLib.sleep(100) from SupportBean").statement("s1").addListener(listenerTwo);
        env.compileDeploy("@name('s2') select SupportStaticMethodLib.sleep(100) from XMLType").statement("s2").addListener(listenerThree);

        EventSender senderOne = env.eventService().getEventSender("MyMap");
        EventSender senderTwo = env.eventService().getEventSender("SupportBean");
        EventSender senderThree = env.eventService().getEventSender("XMLType");

        long start = System.nanoTime();
        for (int i = 0; i < 2; i++) {
            env.sendEventMap(new HashMap<String, Object>(), "MyMap");
            senderOne.sendEvent(new HashMap<String, Object>());
            env.sendEventBean(new SupportBean());
            senderTwo.sendEvent(new SupportBean());
            env.sendEventXMLDOM(SupportXML.getDocument("<myevent/>"), "XMLType");
            senderThree.sendEvent(SupportXML.getDocument("<myevent/>"));
        }
        long end = System.nanoTime();
        long delta = (end - start) / 1000000;
        assertTrue(delta < 500);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(4, listenerOne.getNewEvents().size());
        assertEquals(4, listenerTwo.getNewEvents().size());
        assertEquals(4, listenerThree.getNewEvents().size());

        EPRuntimeSPI spi = (EPRuntimeSPI) env.runtime();
        assertEquals(0, spi.getServicesContext().getThreadingService().getInboundQueue().size());
        assertNotNull(spi.getServicesContext().getThreadingService().getInboundThreadPool());

        env.undeployAll();
    }

    // Used by test
    public static boolean throwException() {
        throw new RuntimeException("Intended for testing");
    }
}
