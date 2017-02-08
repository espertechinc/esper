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
package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestNamedWindowLateStartIndex extends TestCase
{
    private EPServiceProviderSPI epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        epService = (EPServiceProviderSPI) EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(MyCountAccessEvent.class);
        listener = new SupportUpdateListener();
    }
    
    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testLateStartIndex() {
         // prepare
        preloadData(false);

        // test join
        String eplJoin = "select * from SupportBean_S0 as s0 unidirectional, AWindow(p00='x') as aw where aw.id = s0.id";
        epService.getEPAdministrator().createEPL(eplJoin).addListener(listener);
        if (!InstrumentationHelper.ENABLED) {
            assertEquals(2, MyCountAccessEvent.getAndResetCountGetterCalled());
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1, "x"));
        assertTrue(listener.getAndClearIsInvoked());

        // test subquery no-index-share
        String eplSubqueryNoIndexShare = "select (select id from AWindow(p00='x') as aw where aw.id = s0.id) " +
                "from SupportBean_S0 as s0 unidirectional";
        epService.getEPAdministrator().createEPL(eplSubqueryNoIndexShare).addListener(listener);
        if (!InstrumentationHelper.ENABLED) {
            assertEquals(2, MyCountAccessEvent.getAndResetCountGetterCalled());
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1, "x"));

        // test subquery with index share
        epService.getEPAdministrator().destroyAllStatements();
        preloadData(true);

        String eplSubqueryWithIndexShare = "select (select id from AWindow(p00='x') as aw where aw.id = s0.id) " +
                "from SupportBean_S0 as s0 unidirectional";
        epService.getEPAdministrator().createEPL(eplSubqueryWithIndexShare).addListener(listener);
        if (!InstrumentationHelper.ENABLED) {
            assertEquals(2, MyCountAccessEvent.getAndResetCountGetterCalled());
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1, "x"));
        assertTrue(listener.isInvoked());
    }

    private void preloadData(boolean indexShare) {
        String createEpl = "create window AWindow#keepall as MyCountAccessEvent";
        if (indexShare) {
            createEpl = "@Hint('enable_window_subquery_indexshare') " + createEpl;
        }

        epService.getEPAdministrator().createEPL(createEpl);
        epService.getEPAdministrator().createEPL("insert into AWindow select * from MyCountAccessEvent");
        epService.getEPAdministrator().createEPL("create index I1 on AWindow(p00)");
        MyCountAccessEvent.getAndResetCountGetterCalled();
        for (int i = 0; i < 100; i++) {
            epService.getEPRuntime().sendEvent(new MyCountAccessEvent(i, "E" + i));
        }
        epService.getEPRuntime().sendEvent(new MyCountAccessEvent(-1, "x"));
        if (!InstrumentationHelper.ENABLED) {
            assertEquals(101, MyCountAccessEvent.getAndResetCountGetterCalled());
        }
    }

    public static class MyCountAccessEvent {
        private static int countGetterCalled;

        private final int id;
        private final String p00;

        public MyCountAccessEvent(int id, String p00) {
            this.id = id;
            this.p00 = p00;
        }

        public static int getAndResetCountGetterCalled() {
            int value = countGetterCalled;
            countGetterCalled = 0;
            return value;
        }

        public int getId() {
            return id;
        }

        public String getP00() {
            countGetterCalled++;
            return p00;
        }
    }
}
