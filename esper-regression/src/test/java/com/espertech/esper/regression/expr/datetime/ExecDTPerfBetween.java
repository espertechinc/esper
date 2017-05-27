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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportDateTime;
import com.espertech.esper.supportregression.bean.SupportTimeStartEndA;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecDTPerfBetween implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
    }

    public void run(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportTimeStartEndA.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("SupportDateTime", SupportDateTime.class.getName());

        epService.getEPAdministrator().createEPL("create window AWindow#keepall as A");
        epService.getEPAdministrator().createEPL("insert into AWindow select * from A");

        // preload
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("A" + i, "2002-05-30T09:00:00.000", 100));
        }
        epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("AEarlier", "2002-05-30T08:00:00.000", 100));
        epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("ALater", "2002-05-30T10:00:00.000", 100));

        String epl = "select a.key as c0 from SupportDateTime unidirectional, AWindow as a where longdate.between(longdateStart, longdateEnd, false, true)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // query
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(SupportDateTime.make("2002-05-30T08:00:00.050"));
            assertEquals("AEarlier", listener.assertOneGetNewAndReset().get("c0"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("Delta=" + delta / 1000d, delta < 500);

        epService.getEPRuntime().sendEvent(SupportDateTime.make("2002-05-30T10:00:00.050"));
        assertEquals("ALater", listener.assertOneGetNewAndReset().get("c0"));

        stmt.destroy();
    }
}
