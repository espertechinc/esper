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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;

public class TestViewInheritAndInterface extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener testListener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }
    
    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        testListener = null;
    }

    public void testOverridingSubclass()
    {
        String viewExpr = "select val as value from " +
                SupportOverrideOne.class.getName() + "#length(10)";

        EPStatement testView = epService.getEPAdministrator().createEPL(viewExpr);
        testListener = new SupportUpdateListener();
        testView.addListener(testListener);

        epService.getEPRuntime().sendEvent(new SupportOverrideOneA("valA", "valOne", "valBase"));
        EventBean theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("valA", theEvent.get("value"));

        epService.getEPRuntime().sendEvent(new SupportOverrideBase("x"));
        assertFalse(testListener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportOverrideOneB("valB", "valTwo", "valBase2"));
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("valB", theEvent.get("value"));

        epService.getEPRuntime().sendEvent(new SupportOverrideOne("valThree", "valBase3"));
        theEvent = testListener.getAndResetLastNewData()[0];
        assertEquals("valThree", theEvent.get("value"));
    }

    public void testImplementationClass()
    {
        String[] viewExpr = {
            "select baseAB from " + ISupportBaseAB.class.getName() + "#length(10)",
            "select baseAB, a from " + ISupportA.class.getName() + "#length(10)",
            "select baseAB, b from " + ISupportB.class.getName() + "#length(10)",
            "select c from " + ISupportC.class.getName() + "#length(10)",
            "select baseAB, a, g from " + ISupportAImplSuperG.class.getName() + "#length(10)",
            "select baseAB, a, b, g, c from " + ISupportAImplSuperGImplPlus.class.getName() + "#length(10)",
        };

        String[][] expected = {
                             {"baseAB"},
                             {"baseAB", "a"},
                             {"baseAB", "b"},
                             {"c"},
                             {"baseAB", "a", "g"},
                             {"baseAB", "a", "b", "g", "c"}
                            };

        EPStatement testViews[] = new EPStatement[viewExpr.length];
        SupportUpdateListener listeners[] = new SupportUpdateListener[viewExpr.length];
        for (int i = 0; i < viewExpr.length; i++)
        {
            testViews[i] = epService.getEPAdministrator().createEPL(viewExpr[i]);
            listeners[i] = new SupportUpdateListener();
            testViews[i].addListener(listeners[i]);
        }

        epService.getEPRuntime().sendEvent(new ISupportAImplSuperGImplPlus("g", "a", "baseAB", "b", "c"));
        for (int i = 0; i < listeners.length; i++)
        {
            assertTrue(listeners[i].isInvoked());
            EventBean theEvent = listeners[i].getAndResetLastNewData()[0];

            for (int j = 0; j < expected[i].length; j++)
            {
                assertTrue("failed property valid check for stmt=" + viewExpr[i], theEvent.getEventType().isProperty(expected[i][j]));
                assertEquals("failed property check for stmt=" + viewExpr[i], expected[i][j], theEvent.get(expected[i][j]));
            }
        }
    }
}
