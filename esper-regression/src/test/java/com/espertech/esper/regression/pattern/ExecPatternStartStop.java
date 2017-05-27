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
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Set;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecPatternStartStop implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionStartStop(epService);
        runAssertionAddRemoveListener(epService);
        runAssertionStartStopTwo(epService);
    }

    private void runAssertionStartStopTwo(EPServiceProvider epService) {
        String stmtText = "select * from pattern [every(a=" + SupportBean.class.getName() +
                " or b=" + SupportBeanComplexProps.class.getName() + ")]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        for (int i = 0; i < 100; i++) {
            sendAndAssert(epService, updateListener);

            statement.stop();

            epService.getEPRuntime().sendEvent(new SupportBean());
            epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
            assertFalse(updateListener.isInvoked());

            statement.start();
        }

        statement.destroy();
    }

    private void runAssertionStartStop(EPServiceProvider epService) {
        String epl = "@IterableUnbound every tag=" + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(epl, "MyPattern");
        assertEquals(StatementType.PATTERN, ((EPStatementSPI) patternStmt).getStatementMetadata().getStatementType());

        // Pattern started when created
        assertFalse(patternStmt.iterator().hasNext());
        SafeIterator<EventBean> safe = patternStmt.safeIterator();
        assertFalse(safe.hasNext());
        safe.close();

        // Stop pattern
        patternStmt.stop();
        sendEvent(epService);
        assertNull(patternStmt.iterator());

        // Start pattern
        patternStmt.start();
        assertFalse(patternStmt.iterator().hasNext());

        // Send event
        SupportBean theEvent = sendEvent(epService);
        assertSame(theEvent, patternStmt.iterator().next().get("tag"));
        safe = patternStmt.safeIterator();
        assertSame(theEvent, safe.next().get("tag"));
        safe.close();

        // Stop pattern
        patternStmt.stop();
        assertNull(patternStmt.iterator());

        // Start again, iterator is zero
        patternStmt.start();
        assertFalse(patternStmt.iterator().hasNext());

        // assert statement-eventtype reference info
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        assertTrue(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));
        Set<String> stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        assertTrue(stmtNames.contains("MyPattern"));

        patternStmt.destroy();

        assertFalse(spi.getStatementEventTypeRef().isInUse(SupportBean.class.getName()));
        stmtNames = spi.getStatementEventTypeRef().getStatementNamesForType(SupportBean.class.getName());
        assertFalse(stmtNames.contains("MyPattern"));
    }

    private void runAssertionAddRemoveListener(EPServiceProvider epService) {
        String epl = "@IterableUnbound every tag=" + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(epl, "MyPattern");
        assertEquals(StatementType.PATTERN, ((EPStatementSPI) patternStmt).getStatementMetadata().getStatementType());
        SupportUpdateListener listener = new SupportUpdateListener();

        // Pattern started when created

        // Add listener
        patternStmt.addListener(listener);
        assertNull(listener.getLastNewData());
        assertFalse(patternStmt.iterator().hasNext());

        // Send event
        SupportBean theEvent = sendEvent(epService);
        assertEquals(theEvent, listener.getAndResetLastNewData()[0].get("tag"));
        assertSame(theEvent, patternStmt.iterator().next().get("tag"));

        // Remove listener
        patternStmt.removeListener(listener);
        theEvent = sendEvent(epService);
        assertSame(theEvent, patternStmt.iterator().next().get("tag"));
        assertNull(listener.getLastNewData());

        // Add listener back
        patternStmt.addListener(listener);
        theEvent = sendEvent(epService);
        assertSame(theEvent, patternStmt.iterator().next().get("tag"));
        assertEquals(theEvent, listener.getAndResetLastNewData()[0].get("tag"));
    }

    private void sendAndAssert(EPServiceProvider epService, SupportUpdateListener updateListener) {
        for (int i = 0; i < 1000; i++) {
            Object theEvent = null;
            if (i % 3 == 0) {
                theEvent = new SupportBean();
            } else {
                theEvent = SupportBeanComplexProps.makeDefaultBean();
            }

            epService.getEPRuntime().sendEvent(theEvent);

            EventBean eventBean = updateListener.assertOneGetNewAndReset();
            if (theEvent instanceof SupportBean) {
                assertSame(theEvent, eventBean.get("a"));
                assertNull(eventBean.get("b"));
            } else {
                assertSame(theEvent, eventBean.get("b"));
                assertNull(eventBean.get("a"));
            }
        }
    }

    private SupportBean sendEvent(EPServiceProvider epService) {
        SupportBean theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }
}
