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

package com.espertech.esper.regression.expr;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.util.SerializableObjectCopier;

public class TestTimestampExpr extends TestCase
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

    public void testGetTimestamp()
    {
        sendTimer(0);
        String stmtText = "select current_timestamp(), " +
                          " current_timestamp as t0, " +
                          " current_timestamp() as t1, " +
                          " current_timestamp + 1 as t2 " +
                          " from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(stmtText);
        selectTestCase.addListener(listener);

        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("current_timestamp()"));
        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("t0"));
        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("t1"));
        assertEquals(Long.class, selectTestCase.getEventType().getPropertyType("t2"));

        sendTimer(100);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {100l, 100l, 101l});

        sendTimer(999);
        epService.getEPRuntime().sendEvent(new SupportBean());
        theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[]{999l, 999l, 1000l});
        assertEquals(theEvent.get("current_timestamp()"), theEvent.get("t0"));
    }

    public void testGetTimestamp_OM() throws Exception
    {
        sendTimer(0);
        String stmtText = "select current_timestamp() as t0 from " + SupportBean.class.getName();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.currentTimestamp(), "t0"));
        model.setFromClause(FromClause.create().add(FilterStream.create(SupportBean.class.getName())));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        assertEquals(Long.class, stmt.getEventType().getPropertyType("t0"));

        sendTimer(777);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {777l});
    }

    public void testGetTimestamp_Compile() throws Exception
    {
        sendTimer(0);
        String stmtText = "select current_timestamp() as t0 from " + SupportBean.class.getName();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        assertEquals(Long.class, stmt.getEventType().getPropertyType("t0"));

        sendTimer(777);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertResults(theEvent, new Object[] {777l});
    }

    private void sendTimer(long timeInMSec)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(timeInMSec);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }

    private void assertResults(EventBean theEvent, Object[] result)
    {
        for (int i = 0; i < result.length; i++)
        {
            assertEquals("failed for index " + i, result[i], theEvent.get("t" + i));
        }
    }
}
