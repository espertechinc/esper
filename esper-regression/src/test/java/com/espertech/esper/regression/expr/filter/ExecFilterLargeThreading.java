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
package com.espertech.esper.regression.expr.filter;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportTradeEvent;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecFilterLargeThreading implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportEvent", SupportTradeEvent.class);
        configuration.getEngineDefaults().getExecution().setThreadingProfile(ConfigurationEngineDefaults.ThreadingProfile.LARGE);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmtOneText = "every event1=SupportEvent(userId like '123%')";
        EPStatement statement = epService.getEPAdministrator().createPattern(stmtOneText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportTradeEvent(1, null, 1001));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportTradeEvent(2, "1234", 1001));
        assertEquals(2, listener.assertOneGetNewAndReset().get("event1.id"));
    }
}
