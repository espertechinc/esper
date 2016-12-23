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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.bean.SupportBean_C;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;

public class TestJoinSingleOp3Stream extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    private SupportBean_A eventsA[];
    private SupportBean_B eventsB[];
    private SupportBean_C eventsC[];

    private String eventA = SupportBean_A.class.getName();
    private String eventB = SupportBean_B.class.getName();
    private String eventC = SupportBean_C.class.getName();

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        eventsA = new SupportBean_A[10];
        eventsB = new SupportBean_B[10];
        eventsC = new SupportBean_C[10];
        for (int i = 0; i < eventsA.length; i++)
        {
            eventsA[i] = new SupportBean_A(Integer.toString(i));
            eventsB[i] = new SupportBean_B(Integer.toString(i));
            eventsC[i] = new SupportBean_C(Integer.toString(i));
        }
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        updateListener = null;
        eventA = null;
        eventB = null;
        eventC = null;
    }

    public void testJoinUniquePerId()
    {
        String joinStatement = "select * from " +
            eventA + "#length(3) as streamA," +
            eventB + "#length(3) as streamB," +
            eventC + "#length(3) as streamC" +
            " where (streamA.id = streamB.id) " +
            "   and (streamB.id = streamC.id)" +
            "   and (streamA.id = streamC.id)";

        EPStatement joinView = epService.getEPAdministrator().createEPL(joinStatement);
        joinView.addListener(updateListener);

        runJoinUniquePerId();
    }

    public void testJoinUniquePerIdOM() throws Exception
    {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        FromClause fromClause = FromClause.create(
                FilterStream.create(eventA, "streamA").addView(View.create("length", Expressions.constant(3))),
                FilterStream.create(eventB, "streamB").addView(View.create("length", Expressions.constant(3))),
                FilterStream.create(eventC, "streamC").addView(View.create("length", Expressions.constant(3))));
        model.setFromClause(fromClause);
        model.setWhereClause(Expressions.and(
                Expressions.eqProperty("streamA.id", "streamB.id"),
                Expressions.eqProperty("streamB.id", "streamC.id"),
                Expressions.eqProperty("streamA.id", "streamC.id")));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String joinStatement = "select * from " +
            eventA + "#length(3) as streamA, " +
            eventB + "#length(3) as streamB, " +
            eventC + "#length(3) as streamC " +
            "where streamA.id=streamB.id " +
            "and streamB.id=streamC.id " +
            "and streamA.id=streamC.id";

        EPStatement joinView = epService.getEPAdministrator().create(model);
        joinView.addListener(updateListener);
        assertEquals(joinStatement, model.toEPL());

        runJoinUniquePerId();
    }

    public void testJoinUniquePerIdCompile() throws Exception
    {
        String joinStatement = "select * from " +
            eventA + "#length(3) as streamA, " +
            eventB + "#length(3) as streamB, " +
            eventC + "#length(3) as streamC " +
            "where streamA.id=streamB.id " +
            "and streamB.id=streamC.id " +
            "and streamA.id=streamC.id";

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(joinStatement);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        EPStatement joinView = epService.getEPAdministrator().create(model);
        joinView.addListener(updateListener);
        assertEquals(joinStatement, model.toEPL());

        runJoinUniquePerId();
    }

    private void runJoinUniquePerId()
    {
        // Test sending a C event
        sendEvent(eventsA[0]);
        sendEvent(eventsB[0]);
        assertNull(updateListener.getLastNewData());
        sendEvent(eventsC[0]);
        assertEventsReceived(eventsA[0], eventsB[0], eventsC[0]);

        // Test sending a B event
        sendEvent(new Object[] {eventsA[1], eventsB[2], eventsC[3] });
        sendEvent(eventsC[1]);
        assertNull(updateListener.getLastNewData());
        sendEvent(eventsB[1]);
        assertEventsReceived(eventsA[1], eventsB[1], eventsC[1]);

        // Test sending a C event
        sendEvent(new Object[] {eventsA[4], eventsA[5], eventsB[4], eventsB[3]});
        assertNull(updateListener.getLastNewData());
        sendEvent(eventsC[4]);
        assertEventsReceived(eventsA[4], eventsB[4], eventsC[4]);
        assertNull(updateListener.getLastNewData());
    }

    private void assertEventsReceived(SupportBean_A event_A, SupportBean_B event_B, SupportBean_C event_C)
    {
        assertEquals(1, updateListener.getLastNewData().length);
        assertSame(event_A, updateListener.getLastNewData()[0].get("streamA"));
        assertSame(event_B, updateListener.getLastNewData()[0].get("streamB"));
        assertSame(event_C, updateListener.getLastNewData()[0].get("streamC"));
        updateListener.reset();
    }

    private void sendEvent(Object theEvent)
    {
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEvent(Object[] events)
    {
        for (int i = 0; i < events.length; i++)
        {
            epService.getEPRuntime().sendEvent(events[i]);
        }
    }
}
