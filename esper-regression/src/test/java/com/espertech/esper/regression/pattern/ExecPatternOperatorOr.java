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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.bean.SupportBean_C;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.*;

import static org.junit.Assert.*;

public class ExecPatternOperatorOr implements RegressionExecution, SupportBeanConstants {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOp(epService);
        runAssertionOrAndNotAndZeroStart(epService);
    }

    private void runAssertionOp(EPServiceProvider epService) throws Exception {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("a=" + EVENT_A_CLASS + " or a=" + EVENT_A_CLASS);
        testCase.add("A1", "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("a=" + EVENT_A_CLASS + " or b=" + EVENT_B_CLASS + " or c=" + EVENT_C_CLASS);
        testCase.add("A1", "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=" + EVENT_B_CLASS + " or every d=" + EVENT_D_CLASS);
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("D1", "d", events.getEvent("D1"));
        testCase.add("D2", "d", events.getEvent("D2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCase.add("D3", "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("a=" + EVENT_A_CLASS + " or b=" + EVENT_B_CLASS);
        testCase.add("A1", "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("a=" + EVENT_A_CLASS + " or every b=" + EVENT_B_CLASS);
        testCase.add("A1", "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every a=" + EVENT_A_CLASS + " or d=" + EVENT_D_CLASS);
        testCase.add("A1", "a", events.getEvent("A1"));
        testCase.add("A2", "a", events.getEvent("A2"));
        testCase.add("D1", "d", events.getEvent("D1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (every b=" + EVENT_B_CLASS + "() or d=" + EVENT_D_CLASS + "())");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B2", "b", events.getEvent("B2"));
        for (int i = 0; i < 4; i++) {
            testCase.add("D1", "d", events.getEvent("D1"));
        }
        for (int i = 0; i < 4; i++) {
            testCase.add("D2", "d", events.getEvent("D2"));
        }
        for (int i = 0; i < 4; i++) {
            testCase.add("B3", "b", events.getEvent("B3"));
        }
        for (int i = 0; i < 8; i++) {
            testCase.add("D3", "d", events.getEvent("D3"));
        }
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=" + EVENT_B_CLASS + "() or every d=" + EVENT_D_CLASS + "())");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("D1", "d", events.getEvent("D1"));
        testCase.add("D2", "d", events.getEvent("D2"));
        testCase.add("D2", "d", events.getEvent("D2"));
        for (int i = 0; i < 4; i++) {
            testCase.add("B3", "b", events.getEvent("B3"));
        }
        for (int i = 0; i < 4; i++) {
            testCase.add("D3", "d", events.getEvent("D3"));
        }
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (every d=" + EVENT_D_CLASS + "() or every b=" + EVENT_B_CLASS + "())");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B2", "b", events.getEvent("B2"));
        for (int i = 0; i < 4; i++) {
            testCase.add("D1", "d", events.getEvent("D1"));
        }
        for (int i = 0; i < 8; i++) {
            testCase.add("D2", "d", events.getEvent("D2"));
        }
        for (int i = 0; i < 16; i++) {
            testCase.add("B3", "b", events.getEvent("B3"));
        }
        for (int i = 0; i < 32; i++) {
            testCase.add("D3", "d", events.getEvent("D3"));
        }
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(epService);
    }

    private void runAssertionOrAndNotAndZeroStart(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportBean_B.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("C", SupportBean_C.class.getName());

        tryOrAndNot(epService, "(a=A -> b=B) or (a=A -> not b=B)");
        tryOrAndNot(epService, "a=A -> (b=B or not B)");

        // try zero-time start
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL("select * from pattern [timer:interval(0) or every timer:interval(1 min)]").addListenerWithReplay(listener);
        assertTrue(listener.isInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryOrAndNot(EPServiceProvider epService, String pattern) {
        String expression =
                "select * " +
                        "from pattern [" + pattern + "]";

        EPStatement statement = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Object eventA1 = new SupportBean_A("A1");
        epService.getEPRuntime().sendEvent(eventA1);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(eventA1, theEvent.get("a"));
        assertNull(theEvent.get("b"));

        Object eventB1 = new SupportBean_B("B1");
        epService.getEPRuntime().sendEvent(eventB1);
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(eventA1, theEvent.get("a"));
        assertEquals(eventB1, theEvent.get("b"));

        statement.destroy();
    }
}