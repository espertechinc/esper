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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.*;

public class ExecEPLPatternEventProperties implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionWildcardSimplePattern(epService);
        runAssertionWildcardOrPattern(epService);
        runAssertionPropertiesSimplePattern(epService);
        runAssertionPropertiesOrPattern(epService);
    }

    private void runAssertionWildcardSimplePattern(EPServiceProvider epService) {
        SupportUpdateListener updateListener = setupSimplePattern(epService, "*");

        Object theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);

        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("a"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWildcardOrPattern(EPServiceProvider epService) {
        SupportUpdateListener updateListener = setupOrPattern(epService, "*");

        Object theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("a"));
        assertNull(eventBean.get("b"));

        theEvent = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(theEvent);
        eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("b"));
        assertNull(eventBean.get("a"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPropertiesSimplePattern(EPServiceProvider epService) {
        SupportUpdateListener updateListener = setupSimplePattern(epService, "a, a as myEvent, a.intPrimitive as myInt, a.theString");

        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(1);
        theEvent.setTheString("test");
        epService.getEPRuntime().sendEvent(theEvent);

        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("a"));
        assertSame(theEvent, eventBean.get("myEvent"));
        assertEquals(1, eventBean.get("myInt"));
        assertEquals("test", eventBean.get("a.theString"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPropertiesOrPattern(EPServiceProvider epService) {
        SupportUpdateListener updateListener = setupOrPattern(epService, "a, a as myAEvent, b, b as myBEvent, a.intPrimitive as myInt, " +
                "a.theString, b.simpleProperty as simple, b.indexed[0] as indexed, b.nested.nestedValue as nestedVal");

        Object theEvent = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.get("b"));
        assertEquals("simple", eventBean.get("simple"));
        assertEquals(1, eventBean.get("indexed"));
        assertEquals("nestedValue", eventBean.get("nestedVal"));
        assertNull(eventBean.get("a"));
        assertNull(eventBean.get("myAEvent"));
        assertNull(eventBean.get("myInt"));
        assertNull(eventBean.get("a.theString"));

        SupportBean eventTwo = new SupportBean();
        eventTwo.setIntPrimitive(2);
        eventTwo.setTheString("test2");
        epService.getEPRuntime().sendEvent(eventTwo);
        eventBean = updateListener.assertOneGetNewAndReset();
        assertEquals(2, eventBean.get("myInt"));
        assertEquals("test2", eventBean.get("a.theString"));
        assertNull(eventBean.get("b"));
        assertNull(eventBean.get("myBEvent"));
        assertNull(eventBean.get("simple"));
        assertNull(eventBean.get("indexed"));
        assertNull(eventBean.get("nestedVal"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportUpdateListener setupSimplePattern(EPServiceProvider epService, String selectCriteria) {
        String stmtText = "select " + selectCriteria + " from pattern [a=" + SupportBean.class.getName() + "]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        return listener;
    }

    private SupportUpdateListener setupOrPattern(EPServiceProvider epService, String selectCriteria) {
        String stmtText = "select " + selectCriteria + " from pattern [every(a=" + SupportBean.class.getName() +
                " or b=" + SupportBeanComplexProps.class.getName() + ")]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);
        return updateListener;
    }
}
