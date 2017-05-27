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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.StatementLifecycleEvent;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.client.SupportServiceStateListener;
import com.espertech.esper.supportregression.client.SupportStatementStateListener;
import com.espertech.esper.supportregression.client.SupportStmtLifecycleObserver;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Arrays;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecClientEPServiceProvider implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionObtainEngineWideRWLock(epService);
        runAssertionDefaultEngine(epService);
        runAssertionListenerStateChange();
        runAssertionStatementStateChange();
        runAssertionDestroy();
    }

    private void runAssertionObtainEngineWideRWLock(EPServiceProvider epService) {
        epService.getEngineInstanceWideLock().writeLock().lock();
        try {
            // some action here
        } finally {
            epService.getEngineInstanceWideLock().writeLock().unlock();
        }
    }

    private void runAssertionDefaultEngine(EPServiceProvider epService) {
        assertEquals("default", EPServiceProviderManager.getDefaultProvider().getURI());
        EPServiceProvider engineDefault = EPServiceProviderManager.getDefaultProvider();
        assertTrue(engineDefault.getEPRuntime().isExternalClockingEnabled());

        EPServiceProvider engine = EPServiceProviderManager.getProvider("default");
        assertSame(engineDefault, engine);
        assertSame(engineDefault, epService);

        engine = EPServiceProviderManager.getProvider(null);
        assertSame(engineDefault, engine);

        engine = EPServiceProviderManager.getProvider(null, SupportConfigFactory.getConfiguration());
        assertSame(engineDefault, engine);

        String[] uris = EPServiceProviderManager.getProviderURIs();
        assertTrue(Arrays.asList(uris).contains("default"));
    }

    private void runAssertionDestroy() {

        // test destroy
        Configuration config = SupportConfigFactory.getConfiguration();
        String uriOne = this.getClass().getName() + "_1";
        EPServiceProvider engineOne = EPServiceProviderManager.getProvider(uriOne, config);
        String uriTwo = this.getClass().getName() + "_2";
        EPServiceProvider engineTwo = EPServiceProviderManager.getProvider(uriTwo, config);
        EPAssertionUtil.assertContains(EPServiceProviderManager.getProviderURIs(), uriOne, uriTwo);
        assertNotNull(EPServiceProviderManager.getExistingProvider(uriOne));
        assertNotNull(EPServiceProviderManager.getExistingProvider(uriTwo));

        engineOne.destroy();
        EPAssertionUtil.assertNotContains(EPServiceProviderManager.getProviderURIs(), uriOne);
        EPAssertionUtil.assertContains(EPServiceProviderManager.getProviderURIs(), uriTwo);
        assertNull(EPServiceProviderManager.getExistingProvider(uriOne));

        engineTwo.destroy();
        EPAssertionUtil.assertNotContains(EPServiceProviderManager.getProviderURIs(), uriOne, uriTwo);
        assertNull(EPServiceProviderManager.getExistingProvider(uriTwo));

        try {
            engineTwo.getEPRuntime();
            fail();
        } catch (EPServiceDestroyedException ex) {
            // expected
        }
        try {
            engineTwo.getEPAdministrator();
            fail();
        } catch (EPServiceDestroyedException ex) {
            // expected
        }
        EPAssertionUtil.assertNotContains(EPServiceProviderManager.getProviderURIs(), uriTwo);
    }

    private void runAssertionListenerStateChange() {
        SupportServiceStateListener listener = new SupportServiceStateListener();
        Configuration configuration = SupportConfigFactory.getConfiguration();
        EPServiceProvider epService = EPServiceProviderManager.getProvider(this.getClass().getSimpleName() + "__listenerstatechange", configuration);
        epService.addServiceStateListener(listener);
        epService.destroy();
        assertSame(epService, listener.assertOneGetAndResetDestroyedEvents());

        epService.initialize();
        assertSame(epService, listener.assertOneGetAndResetInitializedEvents());

        epService.removeAllServiceStateListeners();
        epService.initialize();
        assertTrue(listener.getInitializedEvents().isEmpty());

        epService.addServiceStateListener(listener);
        SupportServiceStateListener listenerTwo = new SupportServiceStateListener();
        epService.addServiceStateListener(listenerTwo);
        epService.initialize();
        assertSame(epService, listener.assertOneGetAndResetInitializedEvents());
        assertSame(epService, listenerTwo.assertOneGetAndResetInitializedEvents());

        epService.removeServiceStateListener(listener);
        epService.initialize();
        assertSame(epService, listenerTwo.assertOneGetAndResetInitializedEvents());
        assertTrue(listener.getInitializedEvents().isEmpty());

        epService.destroy();
    }

    private void runAssertionStatementStateChange() {
        EPServiceProvider stateChangeEngine = EPServiceProviderManager.getProvider(this.getClass().getSimpleName() + "_statechange", SupportConfigFactory.getConfiguration());
        EPServiceProviderSPI spi = (EPServiceProviderSPI) stateChangeEngine;

        SupportStmtLifecycleObserver observer = new SupportStmtLifecycleObserver();
        spi.getStatementLifecycleSvc().addObserver(observer);
        SupportStatementStateListener listener = new SupportStatementStateListener();
        stateChangeEngine.addStatementStateListener(listener);

        EPStatement stmt = stateChangeEngine.getEPAdministrator().createEPL("select * from " + SupportBean.class.getName());
        assertEquals("CREATE;STATECHANGE;", observer.getEventsAsString());
        assertEquals(stmt, listener.assertOneGetAndResetCreatedEvents());
        assertEquals(stmt, listener.assertOneGetAndResetStateChangeEvents());

        observer.flush();
        stmt.stop();
        assertEquals("STATECHANGE;", observer.getEventsAsString());
        assertEquals(stmt.getName(), observer.getEvents().get(0).getStatement().getName());
        assertEquals(stmt, listener.assertOneGetAndResetStateChangeEvents());

        observer.flush();
        stmt.addListener(new UpdateListener() {
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
            }
        });
        assertEquals("LISTENER_ADD;", observer.getEventsAsString());
        assertNotNull(observer.getLastContext());
        assertTrue(observer.getLastContext()[0] instanceof UpdateListener);

        observer.flush();
        stmt.removeAllListeners();
        assertEquals(StatementLifecycleEvent.LifecycleEventType.LISTENER_REMOVE_ALL.toString() + ";", observer.getEventsAsString());

        stmt.destroy();
        assertEquals(stmt, listener.assertOneGetAndResetStateChangeEvents());

        stateChangeEngine.destroy();
    }

}
