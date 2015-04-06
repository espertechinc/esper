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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewExternallyBatched extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType(MyEvent.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testExtBatchedNoReference() {
        String[] fields = "id".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select irstream * from MyEvent.win:ext_timed_batch(mytimestamp, 1 minute)");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E1", "8:00:00.000"));
        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E2", "8:00:30.000"));
        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E3", "8:00:59.999"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E4", "8:01:00.000"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{{"E1"}, {"E2"}, {"E3"}}, (Object[][]) null);

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E5", "8:01:02.000"));
        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E6", "8:01:05.000"));
        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E7", "8:02:00.000"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{{"E4"}, {"E5"}, {"E6"}}, new Object[][]{{"E1"}, {"E2"}, {"E3"}});

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E8", "8:03:59.000"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{{"E7"}}, new Object[][]{{"E4"}, {"E5"}, {"E6"}});

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E9", "8:03:59.000"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E10", "8:04:00.000"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{{"E8"}, {"E9"}}, new Object[][]{{"E7"}});

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E11", "8:06:30.000"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{{"E10"}}, new Object[][]{{"E8"}, {"E9"}});

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E12", "8:06:59.999"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E13", "8:07:00.001"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][] {{"E11"}, {"E12"}}, new Object[][] {{"E10"}});
    }

    public void testExtBatchedWithRefTime() {

        String epl = "select irstream * from MyEvent.win:ext_timed_batch(mytimestamp, 1 minute, 5000)";
        runAssertionWithRefTime(epl);

        epl = "select irstream * from MyEvent.win:ext_timed_batch(mytimestamp, 1 minute, 65000)";
        runAssertionWithRefTime(epl);
    }

    private void runAssertionWithRefTime(String epl) {
        String[] fields = "id".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E1", "8:00:00.000"));
        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E2", "8:00:04.999"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E3", "8:00:05.000"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{{"E1"}, {"E2"}}, null);

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E4", "8:00:04.000"));
        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E5", "7:00:00.000"));
        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E6", "8:01:04.999"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E7", "8:01:05.000"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{{"E3"}, {"E4"}, {"E5"}, {"E6"}}, new Object[][]{{"E1"}, {"E2"}});

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E8", "8:03:55.000"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][]{{"E7"}}, new Object[][]{{"E3"}, {"E4"}, {"E5"}, {"E6"}});

        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E9", "0:00:00.000"));
        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E10", "8:04:04.999"));
        epService.getEPRuntime().sendEvent(MyEvent.makeTime("E11", "8:04:05.000"));
        EPAssertionUtil.assertPropsPerRow(listener.assertInvokedAndReset(), fields,
                new Object[][] {{"E8"}, {"E9"}, {"E10"}}, new Object[][] {{"E7"}});

        stmt.destroy();
    }

    public static class MyEvent {
        private String id;
        private long mytimestamp;

        public MyEvent(String id, long mytimestamp) {
            this.id = id;
            this.mytimestamp = mytimestamp;
        }

        public static MyEvent makeTime(String id, String mytime) {
            long msec = DateTime.parseDefaultMSec("2002-05-1T" + mytime);
            return new MyEvent(id, msec);
        }

        public String getId() {
            return id;
        }

        public long getMytimestamp() {
            return mytimestamp;
        }
    }
}
