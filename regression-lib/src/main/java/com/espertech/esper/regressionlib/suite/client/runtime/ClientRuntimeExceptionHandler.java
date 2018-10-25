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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerContext;
import com.espertech.esper.common.client.hook.exception.ExceptionHandlerFactoryContext;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportInvalidAggregationFunctionForge;
import com.espertech.esper.regressionlib.support.util.SupportExceptionHandlerFactory;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ClientRuntimeExceptionHandler {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeExHandlerInvalidAgg());
        return execs;
    }

    private static class ClientRuntimeExHandlerInvalidAgg implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@Name('ABCName') select myinvalidagg() from SupportBean";
            env.compileDeploy(epl);

            try {
                env.sendEventBean(new SupportBean());
                fail();
            } catch (EPException ex) {
                /* expected */
            }

            env.undeployAll();
        }
    }

    public static class ClientRuntimeExHandlerGetContext {
        public void run(Configuration configuration) {
            SupportExceptionHandlerFactory.getFactoryContexts().clear();
            SupportExceptionHandlerFactory.getHandlers().clear();
            configuration.getRuntime().getExceptionHandling().getHandlerFactories().clear();
            configuration.getRuntime().getExceptionHandling().addClass(SupportExceptionHandlerFactory.class);
            configuration.getRuntime().getExceptionHandling().addClass(SupportExceptionHandlerFactory.class);
            configuration.getCommon().addEventType(SupportBean.class);
            configuration.getCompiler().addPlugInAggregationFunctionForge("myinvalidagg", SupportInvalidAggregationFunctionForge.class.getName());

            EPRuntime runtime = EPRuntimeProvider.getRuntime(ClientRuntimeExHandlerGetContext.class.getName(), configuration);

            SupportExceptionHandlerFactory.getFactoryContexts().clear();
            SupportExceptionHandlerFactory.getHandlers().clear();
            runtime.initialize();

            String epl = "@Name('ABCName') select myinvalidagg() from SupportBean";
            EPDeployment deployment;
            try {
                EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments(configuration));
                deployment = runtime.getDeploymentService().deploy(compiled);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            List<ExceptionHandlerFactoryContext> contexts = SupportExceptionHandlerFactory.getFactoryContexts();
            assertEquals(2, contexts.size());
            assertEquals(runtime.getURI(), contexts.get(0).getRuntimeURI());
            assertEquals(runtime.getURI(), contexts.get(1).getRuntimeURI());

            SupportExceptionHandlerFactory.SupportExceptionHandler handlerOne = SupportExceptionHandlerFactory.getHandlers().get(0);
            SupportExceptionHandlerFactory.SupportExceptionHandler handlerTwo = SupportExceptionHandlerFactory.getHandlers().get(1);
            runtime.getEventService().sendEventBean(new SupportBean(), "SupportBean");

            assertEquals(1, handlerOne.getContexts().size());
            assertEquals(1, handlerTwo.getContexts().size());
            ExceptionHandlerContext ehc = handlerOne.getContexts().get(0);
            assertEquals(runtime.getURI(), ehc.getRuntimeURI());
            assertEquals(epl, ehc.getEpl());
            assertEquals(deployment.getDeploymentId(), ehc.getDeploymentId());
            assertEquals("ABCName", ehc.getStatementName());
            assertEquals("Sample exception", ehc.getThrowable().getMessage());
            assertNotNull(ehc.getCurrentEvent());

            runtime.destroy();
        }
    }

    public static class ClientRuntimeExceptionHandlerNoHandler {
        public void run(Configuration configuration) {
            configuration.getRuntime().getExceptionHandling().getHandlerFactories().clear();
            configuration.getCompiler().addPlugInAggregationFunctionForge("myinvalidagg", SupportInvalidAggregationFunctionForge.class.getName());
            configuration.getCommon().addEventType(SupportBean.class);

            EPRuntime runtime = EPRuntimeProvider.getRuntime(ClientRuntimeExceptionHandlerNoHandler.class.getName(), configuration);

            String epl = "@Name('ABCName') select myinvalidagg() from SupportBean";
            EPDeployment deployment;
            try {
                EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments(configuration));
                deployment = runtime.getDeploymentService().deploy(compiled);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            runtime.getEventService().sendEventBean(new SupportBean(), "SupportBean");

            runtime.destroy();
        }
    }
}
