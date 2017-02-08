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
package com.espertech.esper.regression.epl;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

public class TestComments extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        updateListener = null;
    }

    public void testComment()
    {
        String lineSeparator = System.getProperty("line.separator");
        String statement = "select theString, /* this is my string */\n" +
                "intPrimitive, // same line comment\n" +
                "/* comment taking one line */\n" +
                "// another comment taking a line\n" +
                "intPrimitive as /* rename */ myPrimitive\n" +
                "from " + SupportBean.class.getName() + lineSeparator +
                " where /* inside a where */ intPrimitive /* */ = /* */ 100";

        EPStatement stmt = epService.getEPAdministrator().createEPL(statement);
        stmt.addListener(updateListener);

        epService.getEPRuntime().sendEvent(new SupportBean("e1", 100));

        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("e1", theEvent.get("theString"));
        assertEquals(100, theEvent.get("intPrimitive"));
        assertEquals(100, theEvent.get("myPrimitive"));
        updateListener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("e1", -1));
        assertFalse(updateListener.getAndClearIsInvoked());
    }
}
