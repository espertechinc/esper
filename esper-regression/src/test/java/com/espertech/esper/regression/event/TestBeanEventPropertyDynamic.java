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

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestBeanEventPropertyDynamic extends TestCase
{
    private SupportUpdateListener listener;
    private EPServiceProvider epService;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testPerformance() throws Exception
    {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();} // exclude test
        String stmtText = "select simpleProperty?, " +
                          "indexed[1]? as indexed, " +
                          "mapped('keyOne')? as mapped " +
                          "from " + SupportBeanComplexProps.class.getName();
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        EventType type = stmt.getEventType();
        assertEquals(Object.class, type.getPropertyType("simpleProperty?"));
        assertEquals(Object.class, type.getPropertyType("indexed"));
        assertEquals(Object.class, type.getPropertyType("mapped"));

        SupportBeanComplexProps inner = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(inner);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(inner.getSimpleProperty(), theEvent.get("simpleProperty?"));
        assertEquals(inner.getIndexed(1), theEvent.get("indexed"));
        assertEquals(inner.getMapped("keyOne"), theEvent.get("mapped"));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++)
        {
            epService.getEPRuntime().sendEvent(inner);
            if (i % 1000 == 0)
            {
                listener.reset();
            }
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("delta=" + delta, delta < 1000);
    }
}
