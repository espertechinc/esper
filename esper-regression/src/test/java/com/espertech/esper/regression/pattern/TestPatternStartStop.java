/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.core.service.EPStatementSPI;
import junit.framework.TestCase;

import java.util.Set;

public class TestPatternStartStop extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testStartStop()
    {
        String viewExpr = "@IterableUnbound every tag=" + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(viewExpr, "MyPattern");
        assertEquals(StatementType.PATTERN, ((EPStatementSPI) patternStmt).getStatementMetadata().getStatementType());

        // Pattern started when created
        assertFalse(patternStmt.iterator().hasNext());
        SafeIterator<EventBean> safe = patternStmt.safeIterator();
        assertFalse(safe.hasNext());
        safe.close();

        // Stop pattern
        patternStmt.stop();
        sendEvent();
        assertNull(patternStmt.iterator());

        // Start pattern
        patternStmt.start();
        assertFalse(patternStmt.iterator().hasNext());

        // Send event
        SupportBean theEvent = sendEvent();
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

    public void testAddRemoveListener()
    {
        String viewExpr = "@IterableUnbound every tag=" + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(viewExpr, "MyPattern");
        assertEquals(StatementType.PATTERN, ((EPStatementSPI) patternStmt).getStatementMetadata().getStatementType());

        // Pattern started when created

        // Add listener
        patternStmt.addListener(listener);
        assertNull(listener.getLastNewData());
        assertFalse(patternStmt.iterator().hasNext());

        // Send event
        SupportBean theEvent = sendEvent();
        assertEquals(theEvent, listener.getAndResetLastNewData()[0].get("tag"));
        assertSame(theEvent, patternStmt.iterator().next().get("tag"));

        // Remove listener
        patternStmt.removeListener(listener);
        theEvent = sendEvent();
        assertSame(theEvent, patternStmt.iterator().next().get("tag"));
        assertNull(listener.getLastNewData());

        // Add listener back
        patternStmt.addListener(listener);
        theEvent = sendEvent();
        assertSame(theEvent, patternStmt.iterator().next().get("tag"));
        assertEquals(theEvent, listener.getAndResetLastNewData()[0].get("tag"));
    }

    private SupportBean sendEvent()
    {
        SupportBean theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }
}
