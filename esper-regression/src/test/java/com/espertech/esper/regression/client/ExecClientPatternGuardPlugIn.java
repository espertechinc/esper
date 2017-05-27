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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.MyCountToPatternGuardFactory;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import static org.junit.Assert.*;

public class ExecClientPatternGuardPlugIn implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addPlugInPatternGuard("myplugin", "count_to", MyCountToPatternGuardFactory.class.getName());
        configuration.addEventType("Bean", SupportBean.class.getName());
        configuration.addPlugInPatternGuard("namespace", "name", String.class.getName());
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionGuard(epService);
        runAssertionGuardVariable(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionGuard(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientPatternGuardPlugIn.class)) {
            return;
        }

        String stmtText = "select * from pattern [(every Bean) where myplugin:count_to(10)]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        for (int i = 0; i < 10; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean());
            assertTrue(listener.isInvoked());
            listener.reset();
        }

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionGuardVariable(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientPatternGuardPlugIn.class)) {
            return;
        }

        epService.getEPAdministrator().createEPL("create variable int COUNT_TO = 3");
        String stmtText = "select * from pattern [(every Bean) where myplugin:count_to(COUNT_TO)]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        for (int i = 0; i < 3; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean());
            assertTrue(listener.isInvoked());
            listener.reset();
        }

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertFalse(listener.isInvoked());

        statement.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        if (SupportConfigFactory.skipTest(ExecClientPatternGuardPlugIn.class)) {
            return;
        }

        try {
            String stmtText = "select * from pattern [every " + SupportBean.class.getName() +
                    " where namespace:name(10)]";
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Failed to resolve pattern guard '" + SupportBean.class.getName() + " where namespace:name(10)': Error casting guard factory instance to com.espertech.esper.pattern.guard.GuardFactory interface for guard 'name'");
        }
    }
}
