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
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportRuntimeStateListener;
import com.espertech.esper.runtime.client.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class ClientRuntimeRuntimeProvider {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeObtainEngineWideRWLock());
        return execs;
    }

    private static class ClientRuntimeObtainEngineWideRWLock implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.runtime().getRuntimeInstanceWideLock().writeLock().lock();
            try {
                // some action here
            } finally {
                env.runtime().getRuntimeInstanceWideLock().writeLock().unlock();
            }
        }
    }

    public static class ClientRuntimeRuntimeStateChange {
        public void run(Configuration config) {
            SupportRuntimeStateListener listener = new SupportRuntimeStateListener();
            EPRuntime runtime = EPRuntimeProvider.getRuntime(this.getClass().getSimpleName() + "__listenerstatechange", config);
            runtime.addRuntimeStateListener(listener);
            runtime.destroy();
            assertSame(runtime, listener.assertOneGetAndResetDestroyedEvents());

            runtime.initialize();
            assertSame(runtime, listener.assertOneGetAndResetInitializedEvents());

            runtime.removeAllRuntimeStateListeners();
            runtime.initialize();
            assertTrue(listener.getInitializedEvents().isEmpty());

            runtime.addRuntimeStateListener(listener);
            SupportRuntimeStateListener listenerTwo = new SupportRuntimeStateListener();
            runtime.addRuntimeStateListener(listenerTwo);
            runtime.initialize();
            assertSame(runtime, listener.assertOneGetAndResetInitializedEvents());
            assertSame(runtime, listenerTwo.assertOneGetAndResetInitializedEvents());

            assertTrue(runtime.removeRuntimeStateListener(listener));
            runtime.initialize();
            assertSame(runtime, listenerTwo.assertOneGetAndResetInitializedEvents());
            assertTrue(listener.getInitializedEvents().isEmpty());

            runtime.destroy();
        }
    }

    public static class ClientRuntimeRuntimeDestroy {
        public void run(Configuration config) {
            String uriOne = this.getClass().getName() + "_1";
            EPRuntime runtimeOne = EPRuntimeProvider.getRuntime(uriOne, config);
            String uriTwo = this.getClass().getName() + "_2";
            EPRuntime runtimeTwo = EPRuntimeProvider.getRuntime(uriTwo, config);
            EPAssertionUtil.assertContains(EPRuntimeProvider.getRuntimeURIs(), uriOne, uriTwo);
            assertNotNull(EPRuntimeProvider.getExistingRuntime(uriOne));
            assertNotNull(EPRuntimeProvider.getExistingRuntime(uriTwo));

            config.getCommon().addEventType(SupportBean.class);
            EPCompiled compiled;
            try {
                compiled = EPCompilerProvider.getCompiler().compile("select * from SupportBean", new CompilerArguments(config));
            } catch (EPCompileException e) {
                throw new RuntimeException(e);
            }

            EPDeploymentService adminOne = runtimeOne.getDeploymentService();
            runtimeOne.destroy();
            EPAssertionUtil.assertNotContains(EPRuntimeProvider.getRuntimeURIs(), uriOne);
            EPAssertionUtil.assertContains(EPRuntimeProvider.getRuntimeURIs(), uriTwo);
            assertNull(EPRuntimeProvider.getExistingRuntime(uriOne));
            assertTrue(runtimeOne.isDestroyed());
            assertFalse(runtimeTwo.isDestroyed());

            runtimeTwo.destroy();
            EPAssertionUtil.assertNotContains(EPRuntimeProvider.getRuntimeURIs(), uriOne, uriTwo);
            assertNull(EPRuntimeProvider.getExistingRuntime(uriTwo));
            assertTrue(runtimeOne.isDestroyed());
            assertTrue(runtimeTwo.isDestroyed());

            try {
                runtimeTwo.getEventService();
                fail();
            } catch (EPRuntimeDestroyedException ex) {
                // expected
            }

            try {
                runtimeTwo.getDeploymentService();
                fail();
            } catch (EPRuntimeDestroyedException ex) {
                // expected
            }

            try {
                adminOne.deploy(compiled);
                fail();
            } catch (EPDeployException ex) {
                fail();
            } catch (EPRuntimeDestroyedException ex) {
                // expected
            }
            EPAssertionUtil.assertNotContains(EPRuntimeProvider.getRuntimeURIs(), uriTwo);
        }
    }

    public static class ClientRuntimeMicrosecondInvalid {
        public void run(Configuration config) {
            config.getRuntime().getThreading().setInternalTimerEnabled(true);
            config.getCommon().getTimeSource().setTimeUnit(TimeUnit.MICROSECONDS);

            try {
                EPRuntimeProvider.getRuntime(this.getClass().getSimpleName(), config).initialize();
                fail();
            } catch (ConfigurationException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Internal timer requires millisecond time resolution");
            }

            config.getRuntime().getThreading().setInternalTimerEnabled(false);
            EPRuntime runtime = EPRuntimeProvider.getRuntime(this.getClass().getSimpleName(), config);

            try {
                runtime.getEventService().clockInternal();
                fail();
            } catch (EPException ex) {
                SupportMessageAssertUtil.assertMessage(ex, "Internal timer requires millisecond time resolution");
            }
            runtime.destroy();
        }
    }
}
