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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecNamedWindowLateStartIndex implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_S0.class);
        epService.getEPAdministrator().getConfiguration().addEventType(MyCountAccessEvent.class);
        // prepare
        preloadData(epService, false);

        // test join
        String eplJoin = "select * from SupportBean_S0 as s0 unidirectional, AWindow(p00='x') as aw where aw.id = s0.id";
        SupportUpdateListener listener = new SupportUpdateListener();
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
        preloadData(epService, true);

        String eplSubqueryWithIndexShare = "select (select id from AWindow(p00='x') as aw where aw.id = s0.id) " +
                "from SupportBean_S0 as s0 unidirectional";
        epService.getEPAdministrator().createEPL(eplSubqueryWithIndexShare).addListener(listener);
        if (!InstrumentationHelper.ENABLED) {
            assertEquals(2, MyCountAccessEvent.getAndResetCountGetterCalled());
        }

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1, "x"));
        assertTrue(listener.isInvoked());
    }

    private void preloadData(EPServiceProvider epService, boolean indexShare) {
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
