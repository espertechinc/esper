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

import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportTimeStartEndA;
import com.espertech.esper.supportregression.bean.SupportTimeStartEndB;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExecDTDocSamples implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        Map<String, Object> meta = new HashMap<>();
        meta.put("timeTaken", Date.class);
        epService.getEPAdministrator().getConfiguration().addEventType("RFIDEvent", meta);

        epService.getEPAdministrator().createEPL("select timeTaken.format() as timeTakenStr from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.get('month') as timeTakenMonth from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.getMonthOfYear() as timeTakenMonth from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.minus(2 minutes) as timeTakenMinus2Min from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.minus(2*60*1000) as timeTakenMinus2Min from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.plus(2 minutes) as timeTakenMinus2Min from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.plus(2*60*1000) as timeTakenMinus2Min from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.roundCeiling('min') as timeTakenRounded from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.roundFloor('min') as timeTakenRounded from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.set('month', 3) as timeTakenMonth from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.withDate(2002, 4, 30) as timeTakenDated from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.withMax('sec') as timeTakenMaxSec from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.toCalendar() as timeTakenCal from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.toDate() as timeTakenDate from RFIDEvent");
        epService.getEPAdministrator().createEPL("select timeTaken.toMillisec() as timeTakenLong from RFIDEvent");

        // test pattern use
        ConfigurationEventTypeLegacy leg = new ConfigurationEventTypeLegacy();
        leg.setStartTimestampPropertyName("longdateStart");
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportTimeStartEndA.class.getName(), leg);
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportTimeStartEndB.class.getName(), leg);

        tryRun(epService, "a.longdateStart.after(b)", "2002-05-30T09:00:00.000", "2002-05-30T08:59:59.999", true);
        tryRun(epService, "a.after(b.longdateStart)", "2002-05-30T09:00:00.000", "2002-05-30T08:59:59.999", true);
        tryRun(epService, "a.after(b)", "2002-05-30T09:00:00.000", "2002-05-30T08:59:59.999", true);
        tryRun(epService, "a.after(b)", "2002-05-30T08:59:59.999", "2002-05-30T09:00:00.000", false);
    }

    private void tryRun(EPServiceProvider epService, String condition, String tsa, String tsb, boolean isInvoked) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from pattern [a=A -> b=B] as abc where " + condition);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("E1", tsa, 0));
        epService.getEPRuntime().sendEvent(SupportTimeStartEndB.make("E2", tsb, 0));
        assertEquals(isInvoked, listener.getAndClearIsInvoked());

        stmt.destroy();
    }

    public static class MyEvent {

        public String get() {
            return "abc";
        }
    }
}
