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
package com.espertech.esper.regression.expr.datetime;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportDateTime;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportEngineFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecDTMicrosecondResolution implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        Map<TimeUnit, EPServiceProvider> epServices = SupportEngineFactory.setupEnginesByTimeUnit();
        runAssertionEventTime(epServices);
        runAssertionLongProperty(epServices);
    }

    public void runAssertionEventTime(Map<TimeUnit, EPServiceProvider> epServices) {
        for (EPServiceProvider epService : epServices.values()) {
            epService.getEPAdministrator().createEPL("create objectarray schema MyEvent(id string, sts long, ets long) starttimestamp sts endtimestamp ets");
        }

        long time = DateTime.parseDefaultMSec("2002-05-30T09:00:00.000");
        runAssertionEventTime(epServices.get(TimeUnit.MILLISECONDS), time, time);
        runAssertionEventTime(epServices.get(TimeUnit.MICROSECONDS), time * 1000, time * 1000);
    }

    private void runAssertionLongProperty(Map<TimeUnit, EPServiceProvider> epServices) {
        for (EPServiceProvider epService : epServices.values()) {
            epService.getEPAdministrator().getConfiguration().addEventType(SupportDateTime.class);
        }

        long time = DateTime.parseDefaultMSec("2002-05-30T09:05:06.007");
        Calendar calTime = GregorianCalendar.getInstance();
        calTime.setTimeInMillis(time);

        Calendar calMod = GregorianCalendar.getInstance();
        calMod.setTimeInMillis(time);
        calMod.set(Calendar.HOUR_OF_DAY, 1);
        calMod.set(Calendar.MINUTE, 2);
        calMod.set(Calendar.SECOND, 3);
        calMod.set(Calendar.MILLISECOND, 4);

        String select =
                "longdate.withTime(1, 2, 3, 4) as c0," +
                        "longdate.set('hour', 1).set('minute', 2).set('second', 3).set('millisecond', 4).toCalendar() as c1," +
                        "longdate.get('month') as c2," +
                        "current_timestamp.get('month') as c3," +
                        "current_timestamp.getMinuteOfHour() as c4," +
                        "current_timestamp.toDate() as c5," +
                        "current_timestamp.toCalendar() as c6," +
                        "current_timestamp.minus(1) as c7";
        String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7".split(",");

        runAssertionLongProperty(epServices.get(TimeUnit.MILLISECONDS), time, new SupportDateTime(time, null, null, null, null), select, fields,
                new Object[]{calMod.getTimeInMillis(), calMod, 4, 4, 5, calTime.getTime(), calTime, time - 1});
        runAssertionLongProperty(epServices.get(TimeUnit.MICROSECONDS), time * 1000, new SupportDateTime(time * 1000 + 123, null, null, null, null), select, fields,
                new Object[]{calMod.getTimeInMillis() * 1000 + 123, calMod, 4, 4, 5, calTime.getTime(), calTime, time * 1000 - 1000});
    }

    private static void runAssertionEventTime(EPServiceProvider epService, long tsB, long flipTimeEndtsA) {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("isolated");
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        EPStatement stmt = isolated.getEPAdministrator().createEPL("select * from MyEvent(id='A') as a unidirectional, MyEvent(id='B')#lastevent as b where a.withDate(2002, 4, 30).before(b)", "s0", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(new Object[]{"B", tsB, tsB}, "MyEvent");

        isolated.getEPRuntime().sendEvent(new Object[]{"A", flipTimeEndtsA - 1, flipTimeEndtsA - 1}, "MyEvent");
        assertTrue(listener.getIsInvokedAndReset());

        isolated.getEPRuntime().sendEvent(new Object[]{"A", flipTimeEndtsA, flipTimeEndtsA}, "MyEvent");
        assertFalse(listener.getIsInvokedAndReset());

        isolated.destroy();
    }

    private void runAssertionLongProperty(EPServiceProvider epService, long startTime, SupportDateTime event, String select, String[] fields, Object[] expected) {
        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("isolated");
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime));

        EPStatement stmt = isolated.getEPAdministrator().createEPL("select " + select + " from SupportDateTime", "s0", null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        isolated.getEPRuntime().sendEvent(event);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);

        isolated.destroy();
    }
}
