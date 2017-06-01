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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportEngineFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ExecViewTimeWindowMicrosecondResolution implements RegressionExecution {

    public void run(EPServiceProvider defaultEPService) throws Exception {
        Map<TimeUnit, EPServiceProvider> epServices = SupportEngineFactory.setupEnginesByTimeUnit();

        for (EPServiceProvider epService : epServices.values()) {
            epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        }
        EPServiceProvider engineMillis = epServices.get(TimeUnit.MILLISECONDS);
        EPServiceProvider engineMicros = epServices.get(TimeUnit.MICROSECONDS);

        runAssertionTimeWindow(engineMillis, 0, "1", 1000);
        runAssertionTimeWindow(engineMicros, 0, "1", 1000000);
        runAssertionTimeWindow(engineMicros, 0, "10 milliseconds", 10000);
        runAssertionTimeWindow(engineMicros, 0, "10 microseconds", 10);
        runAssertionTimeWindow(engineMicros, 0, "1 seconds 10 microseconds", 1000010);

        runAssertionTimeWindow(engineMillis, 123456789, "10", 123456789 + 10 * 1000);
        runAssertionTimeWindow(engineMicros, 123456789, "10", 123456789 + 10 * 1000000);

        runAssertionTimeWindow(engineMillis, 0, "1 months 10 milliseconds", timePlusMonth(0, 1) + 10);
        runAssertionTimeWindow(engineMicros, 0, "1 months 10 microseconds", timePlusMonth(0, 1) * 1000 + 10);

        long currentTime = DateTime.parseDefaultMSec("2002-05-1T08:00:01.999");
        runAssertionTimeWindow(engineMillis, currentTime, "1 months 50 milliseconds", timePlusMonth(currentTime, 1) + 50);
        runAssertionTimeWindow(engineMicros, currentTime * 1000 + 33, "3 months 100 microseconds", timePlusMonth(currentTime, 3) * 1000 + 33 + 100);
    }

    private void runAssertionTimeWindow(EPServiceProvider epService, long startTime, String size, long flipTime) {
        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("isolated");
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime));

        String[] fields = "theString".split(",");
        EPStatement stmt = isolated.getEPAdministrator().createEPL("select * from SupportBean#time(" + size + ")", "s0", 0);

        isolated.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime - 1));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1"}});

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(flipTime));
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        isolated.destroy();
    }

    private static long timePlusMonth(long timeInMillis, int monthToAdd) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(timeInMillis);
        cal.add(Calendar.MONTH, monthToAdd);
        return cal.getTimeInMillis();
    }
}
