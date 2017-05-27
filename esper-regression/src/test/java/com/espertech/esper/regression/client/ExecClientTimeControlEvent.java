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
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.ScopeTestHelper;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.CurrentTimeSpanEvent;
import com.espertech.esper.core.service.EPRuntimeIsolatedSPI;
import com.espertech.esper.core.service.EPRuntimeSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecClientTimeControlEvent implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setShareViews(false);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        configuration.addEventType("SupportBean", SupportBean.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSendTimeSpan(epService);
        runAssertionSendTimeSpanIsolated(epService);
        runAssertionNextScheduledTime(epService);
    }

    private void runAssertionSendTimeSpan(EPServiceProvider epService) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy MM dd HH:mm:ss SSS");
        Date d = format.parse("2010 01 01 00:00:00 000");
        d.getTime();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select current_timestamp() as ct from pattern[every timer:interval(1.5 sec)]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(3500));
        assertEquals(2, listener.getNewDataList().size());
        assertEquals(1500L, listener.getNewDataList().get(0)[0].get("ct"));
        assertEquals(3000L, listener.getNewDataList().get(1)[0].get("ct"));
        listener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(4500));
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(4500L, listener.getNewDataList().get(0)[0].get("ct"));
        listener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(9000));
        assertEquals(3, listener.getNewDataList().size());
        assertEquals(6000L, listener.getNewDataList().get(0)[0].get("ct"));
        assertEquals(7500L, listener.getNewDataList().get(1)[0].get("ct"));
        assertEquals(9000L, listener.getNewDataList().get(2)[0].get("ct"));
        listener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(10499));
        assertEquals(0, listener.getNewDataList().size());

        epService.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(10499));
        assertEquals(0, listener.getNewDataList().size());

        epService.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(10500));
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(10500L, listener.getNewDataList().get(0)[0].get("ct"));
        listener.reset();

        epService.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(10500));
        assertEquals(0, listener.getNewDataList().size());

        epService.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(14000, 200));
        assertEquals(14000, epService.getEPRuntime().getCurrentTime());
        assertEquals(2, listener.getNewDataList().size());
        assertEquals(12100L, listener.getNewDataList().get(0)[0].get("ct"));
        assertEquals(13700L, listener.getNewDataList().get(1)[0].get("ct"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSendTimeSpanIsolated(EPServiceProvider epService) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        EPStatement stmtOne = isolated.getEPAdministrator().createEPL("select current_timestamp() as ct from pattern[every timer:interval(1.5 sec)]", null, null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        isolated.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(3500));
        assertEquals(2, listener.getNewDataList().size());
        assertEquals(1500L, listener.getNewDataList().get(0)[0].get("ct"));
        assertEquals(3000L, listener.getNewDataList().get(1)[0].get("ct"));
        listener.reset();

        isolated.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(4500));
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(4500L, listener.getNewDataList().get(0)[0].get("ct"));
        listener.reset();

        isolated.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(9000));
        assertEquals(3, listener.getNewDataList().size());
        assertEquals(6000L, listener.getNewDataList().get(0)[0].get("ct"));
        assertEquals(7500L, listener.getNewDataList().get(1)[0].get("ct"));
        assertEquals(9000L, listener.getNewDataList().get(2)[0].get("ct"));
        listener.reset();

        isolated.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(10499));
        assertEquals(0, listener.getNewDataList().size());

        isolated.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(10499));
        assertEquals(10499, isolated.getEPRuntime().getCurrentTime());
        assertEquals(0, listener.getNewDataList().size());

        isolated.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(10500));
        assertEquals(1, listener.getNewDataList().size());
        assertEquals(10500L, listener.getNewDataList().get(0)[0].get("ct"));
        listener.reset();

        isolated.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(10500));
        assertEquals(0, listener.getNewDataList().size());

        isolated.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(14000, 200));
        assertEquals(14000, isolated.getEPRuntime().getCurrentTime());
        assertEquals(2, listener.getNewDataList().size());
        assertEquals(12100L, listener.getNewDataList().get(0)[0].get("ct"));
        assertEquals(13700L, listener.getNewDataList().get(1)[0].get("ct"));

        isolated.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionNextScheduledTime(EPServiceProvider epService) {

        EPRuntimeSPI runtimeSPI = (EPRuntimeSPI) epService.getEPRuntime();

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        assertNull(epService.getEPRuntime().getNextScheduledTime());
        assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[0][]);

        EPStatement stmtOne = epService.getEPAdministrator().createEPL("select * from pattern[timer:interval(2 sec)]");
        assertEquals(2000L, (long) epService.getEPRuntime().getNextScheduledTime());
        assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{stmtOne.getName(), 2000L}});

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("@Name('s2') select * from pattern[timer:interval(150 msec)]");
        assertEquals(150L, (long) epService.getEPRuntime().getNextScheduledTime());
        assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{"s2", 150L}, {stmtOne.getName(), 2000L}});

        stmtTwo.destroy();
        assertEquals(2000L, (long) epService.getEPRuntime().getNextScheduledTime());
        assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{stmtOne.getName(), 2000L}});

        EPStatement stmtThree = epService.getEPAdministrator().createEPL("select * from pattern[timer:interval(3 sec) and timer:interval(4 sec)]");
        assertEquals(2000L, (long) epService.getEPRuntime().getNextScheduledTime());
        assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{stmtOne.getName(), 2000L}, {stmtThree.getName(), 3000L}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2500));
        assertEquals(3000L, (long) epService.getEPRuntime().getNextScheduledTime());
        assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{stmtThree.getName(), 3000L}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(3500));
        assertEquals(4000L, (long) epService.getEPRuntime().getNextScheduledTime());
        assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[][]{{stmtThree.getName(), 4000L}});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(4500));
        assertEquals(null, epService.getEPRuntime().getNextScheduledTime());
        assertSchedules(runtimeSPI.getStatementNearestSchedules(), new Object[0][]);

        // test isolated service
        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        EPRuntimeIsolatedSPI isolatedSPI = (EPRuntimeIsolatedSPI) isolated.getEPRuntime();

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        assertNull(isolated.getEPRuntime().getNextScheduledTime());
        assertSchedules(isolatedSPI.getStatementNearestSchedules(), new Object[0][]);

        EPStatement stmtFour = isolated.getEPAdministrator().createEPL("select * from pattern[timer:interval(2 sec)]", null, null);
        assertEquals(2000L, (long) isolatedSPI.getNextScheduledTime());
        assertSchedules(isolatedSPI.getStatementNearestSchedules(), new Object[][]{{stmtFour.getName(), 2000L}});

        isolated.destroy();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertSchedules(Map<String, Long> schedules, Object[][] expected) {
        ScopeTestHelper.assertEquals(expected.length, schedules.size());

        Set<Integer> matchNumber = new HashSet<Integer>();
        for (Object entryObj : schedules.entrySet()) {
            Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) entryObj;
            boolean matchFound = false;
            for (int i = 0; i < expected.length; i++) {
                if (matchNumber.contains(i)) {
                    continue;
                }
                if (expected[i][0].equals(entry.getKey())) {
                    matchFound = true;
                    matchNumber.add(i);
                    if (expected[i][1] == null && entry.getValue() == null) {
                        continue;
                    }
                    if (!expected[i][1].equals(entry.getValue())) {
                        ScopeTestHelper.fail("Failed to match value for key '" + entry.getKey() + "' expected '" + expected[i][i] + "' received '" + entry.getValue() + "'");
                    }
                }
            }
            if (!matchFound) {
                ScopeTestHelper.fail("Failed to find key '" + entry.getKey() + "'");
            }
        }
    }
}
