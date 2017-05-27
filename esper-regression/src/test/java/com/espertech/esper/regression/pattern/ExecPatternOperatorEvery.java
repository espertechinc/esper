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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecPatternOperatorEvery implements RegressionExecution, SupportBeanConstants {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOp(epService);
        runAssertionEveryAndNot(epService);
    }

    private void runAssertionOp(EPServiceProvider epService) throws Exception {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=" + EVENT_B_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (every (every b=" + EVENT_B_CLASS + "))");
        testCase.add("B1", "b", events.getEvent("B1"));
        for (int i = 0; i < 3; i++) {
            testCase.add("B2", "b", events.getEvent("B2"));
        }
        for (int i = 0; i < 9; i++) {
            testCase.add("B3", "b", events.getEvent("B3"));
        }
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (every b=" + EVENT_B_CLASS + "())");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B2", "b", events.getEvent("B2"));
        for (int i = 0; i < 4; i++) {
            testCase.add("B3", "b", events.getEvent("B3"));
        }
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every( every (every (every b=" + EVENT_B_CLASS + "())))");
        testCase.add("B1", "b", events.getEvent("B1"));
        for (int i = 0; i < 4; i++) {
            testCase.add("B2", "b", events.getEvent("B2"));
        }
        for (int i = 0; i < 16; i++) {
            testCase.add("B3", "b", events.getEvent("B3"));
        }
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(epService);
    }

    private void runAssertionEveryAndNot(EPServiceProvider epService) {
        sendTimer(epService, 0);
        String expression =
                "select 'No event within 6 seconds' as alert\n" +
                        "from pattern [ every (timer:interval(6) and not " + SupportBean.class.getName() + ") ]";

        EPStatementSPI statement = (EPStatementSPI) epService.getEPAdministrator().createEPL(expression);
        assertFalse(statement.getStatementContext().isStatelessSelect());
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        sendTimer(epService, 2000);
        epService.getEPRuntime().sendEvent(new SupportBean());

        sendTimer(epService, 6000);
        sendTimer(epService, 7000);
        sendTimer(epService, 7999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 8000);
        assertEquals("No event within 6 seconds", listener.assertOneGetNewAndReset().get("alert"));

        sendTimer(epService, 12000);
        epService.getEPRuntime().sendEvent(new SupportBean());
        sendTimer(epService, 13000);
        epService.getEPRuntime().sendEvent(new SupportBean());

        sendTimer(epService, 18999);
        assertFalse(listener.isInvoked());

        sendTimer(epService, 19000);
        assertEquals("No event within 6 seconds", listener.assertOneGetNewAndReset().get("alert"));

        statement.destroy();
    }

    private void sendTimer(EPServiceProvider epService, long timeInMSec) {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}