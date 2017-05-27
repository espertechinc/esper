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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;

public class ExecEventBeanPropertyResolutionCaseInsensitiveEngineDefault implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getEventMeta().setClassPropertyResolutionStyle(Configuration.PropertyResolutionStyle.CASE_INSENSITIVE);
        configuration.addEventType("Bean", SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryCaseInsensitive(epService, "select THESTRING, INTPRIMITIVE from Bean where THESTRING='A'", "THESTRING", "INTPRIMITIVE");
        tryCaseInsensitive(epService, "select ThEsTrInG, INTprimitIVE from Bean where THESTRing='A'", "ThEsTrInG", "INTprimitIVE");
    }

    protected static void tryCaseInsensitive(EPServiceProvider epService, String stmtText, String propOneName, String propTwoName) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 10));
        EventBean result = listener.assertOneGetNewAndReset();
        assertEquals("A", result.get(propOneName));
        assertEquals(10, result.get(propTwoName));
    }

}
