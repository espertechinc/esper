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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;

public class TestPatternQueries extends TestCase
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

    public void testWhere_OM() throws Exception
    {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().addWithAsProvidedName("s0.id", "idS0").addWithAsProvidedName("s1.id", "idS1"));
        PatternExpr pattern = Patterns.or()
                .add(Patterns.everyFilter(SupportBean_S0.class.getName(), "s0"))
                .add(Patterns.everyFilter(SupportBean_S1.class.getName(), "s1")
                );
        model.setFromClause(FromClause.create(PatternStream.create(pattern)));
        model.setWhereClause(Expressions.or()
                .add(Expressions.and()
                      .add(Expressions.isNotNull("s0.id"))
                      .add(Expressions.lt("s0.id", 100))
                    )
                .add(Expressions.and()
                      .add(Expressions.isNotNull("s1.id"))
                      .add(Expressions.ge("s1.id", 100))
                    ));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String reverse = model.toEPL();
        String stmtText = "select s0.id as idS0, s1.id as idS1 " +
                "from pattern [every s0=" + SupportBean_S0.class.getName() +
                " or every s1=" + SupportBean_S1.class.getName() + "] " +
                "where s0.id is not null and s0.id<100 or s1.id is not null and s1.id>=100";
        assertEquals(stmtText, reverse);

        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(updateListener);

        sendEventS0(1);
        assertEventIds(1, null);

        sendEventS0(101);
        assertFalse(updateListener.isInvoked());

        sendEventS1(1);
        assertFalse(updateListener.isInvoked());

        sendEventS1(100);
        assertEventIds(null, 100);
    }

    public void testWhere_Compile() throws Exception
    {
        String stmtText = "select s0.id as idS0, s1.id as idS1 " +
                "from pattern [every s0=" + SupportBean_S0.class.getName() +
                " or every s1=" + SupportBean_S1.class.getName() + "] " +
                "where s0.id is not null and s0.id<100 or s1.id is not null and s1.id>=100";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String reverse = model.toEPL();
        assertEquals(stmtText, reverse);

        EPStatement statement = epService.getEPAdministrator().create(model);
        statement.addListener(updateListener);

        sendEventS0(1);
        assertEventIds(1, null);

        sendEventS0(101);
        assertFalse(updateListener.isInvoked());

        sendEventS1(1);
        assertFalse(updateListener.isInvoked());

        sendEventS1(100);
        assertEventIds(null, 100);
    }

    public void testWhere()
    {
        String stmtText = "select s0.id as idS0, s1.id as idS1 " +
                "from pattern [every s0=" + SupportBean_S0.class.getName() +
                " or every s1=" + SupportBean_S1.class.getName() + "] " +
                "where (s0.id is not null and s0.id < 100) or (s1.id is not null and s1.id >= 100)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(updateListener);

        sendEventS0(1);
        assertEventIds(1, null);

        sendEventS0(101);
        assertFalse(updateListener.isInvoked());

        sendEventS1(1);
        assertFalse(updateListener.isInvoked());

        sendEventS1(100);
        assertEventIds(null, 100);
    }

    public void testAggregation()
    {
        String stmtText = "select sum(s0.id) as sumS0, sum(s1.id) as sumS1, sum(s0.id + s1.id) as sumS0S1 " +
                "from pattern [every s0=" + SupportBean_S0.class.getName() +
                " or every s1=" + SupportBean_S1.class.getName() + "]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        statement.addListener(updateListener);

        sendEventS0(1);
        assertEventSums(1, null, null);

        sendEventS1(2);
        assertEventSums(1, 2, null);

        sendEventS1(10);
        assertEventSums(1, 12, null);

        sendEventS0(20);
        assertEventSums(21, 12, null);
    }

    public void testFollowedByAndWindow()
    {
        String stmtText = "select irstream a.id as idA, b.id as idB, " +
                "a.p00 as p00A, b.p00 as p00B from pattern [every a=" + SupportBean_S0.class.getName() +
                " -> every b=" + SupportBean_S0.class.getName() + "(p00=a.p00)].win:time(1)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);

        statement.addListener(updateListener);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        sendEvent(1, "e1a");
        assertFalse(updateListener.isInvoked());
        sendEvent(2, "e1a");
        assertNewEvent(1, 2, "e1a");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(500));
        sendEvent(10, "e2a");
        sendEvent(11, "e2b");
        sendEvent(12, "e2c");
        assertFalse(updateListener.isInvoked());
        sendEvent(13, "e2b");
        assertNewEvent(11, 13, "e2b");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        assertOldEvent(1, 2, "e1a");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1500));
        assertOldEvent(11, 13, "e2b");
    }

    private void assertNewEvent(int idA, int idB, String p00)
    {
        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        compareEvent(eventBean, idA, idB, p00);
    }

    private void assertOldEvent(int idA, int idB, String p00)
    {
        EventBean eventBean = updateListener.assertOneGetOldAndReset();
        compareEvent(eventBean, idA, idB, p00);
    }

    private void compareEvent(EventBean eventBean, int idA, int idB, String p00)
    {
        assertEquals(idA, eventBean.get("idA"));
        assertEquals(idB, eventBean.get("idB"));
        assertEquals(p00, eventBean.get("p00A"));
        assertEquals(p00, eventBean.get("p00B"));
    }

    private void sendEvent(int id, String p00)
    {
        SupportBean_S0 theEvent = new SupportBean_S0(id, p00);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private SupportBean_S0 sendEventS0(int id)
    {
        SupportBean_S0 theEvent = new SupportBean_S0(id);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }

    private SupportBean_S1 sendEventS1(int id)
    {
        SupportBean_S1 theEvent = new SupportBean_S1(id);
        epService.getEPRuntime().sendEvent(theEvent);
        return theEvent;
    }

    private void assertEventIds(Integer idS0, Integer idS1)
    {
        EventBean eventBean = updateListener.getAndResetLastNewData()[0];
        assertEquals(idS0, eventBean.get("idS0"));
        assertEquals(idS1, eventBean.get("idS1"));
        updateListener.reset();
    }

    private void assertEventSums(Integer sumS0, Integer sumS1, Integer sumS0S1)
    {
        EventBean eventBean = updateListener.getAndResetLastNewData()[0];
        assertEquals(sumS0, eventBean.get("sumS0"));
        assertEquals(sumS1, eventBean.get("sumS1"));
        assertEquals(sumS0S1, eventBean.get("sumS0S1"));
        updateListener.reset();
    }
}
