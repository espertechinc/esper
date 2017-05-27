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
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecEPLPatternQueries implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionWhere_OM(epService);
        runAssertionWhere_Compile(epService);
        runAssertionWhere(epService);
        runAssertionAggregation(epService);
        runAssertionFollowedByAndWindow(epService);
    }

    private void runAssertionWhere_OM(EPServiceProvider epService) throws Exception {
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
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        sendEventS0(epService, 1);
        assertEventIds(updateListener, 1, null);

        sendEventS0(epService, 101);
        assertFalse(updateListener.isInvoked());

        sendEventS1(epService, 1);
        assertFalse(updateListener.isInvoked());

        sendEventS1(epService, 100);
        assertEventIds(updateListener, null, 100);

        statement.destroy();
    }

    private void runAssertionWhere_Compile(EPServiceProvider epService) throws Exception {
        String stmtText = "select s0.id as idS0, s1.id as idS1 " +
                "from pattern [every s0=" + SupportBean_S0.class.getName() +
                " or every s1=" + SupportBean_S1.class.getName() + "] " +
                "where s0.id is not null and s0.id<100 or s1.id is not null and s1.id>=100";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String reverse = model.toEPL();
        assertEquals(stmtText, reverse);

        EPStatement statement = epService.getEPAdministrator().create(model);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        sendEventS0(epService, 1);
        assertEventIds(updateListener, 1, null);

        sendEventS0(epService, 101);
        assertFalse(updateListener.isInvoked());

        sendEventS1(epService, 1);
        assertFalse(updateListener.isInvoked());

        sendEventS1(epService, 100);
        assertEventIds(updateListener, null, 100);

        statement.destroy();
    }

    private void runAssertionWhere(EPServiceProvider epService) {
        String stmtText = "select s0.id as idS0, s1.id as idS1 " +
                "from pattern [every s0=" + SupportBean_S0.class.getName() +
                " or every s1=" + SupportBean_S1.class.getName() + "] " +
                "where (s0.id is not null and s0.id < 100) or (s1.id is not null and s1.id >= 100)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        sendEventS0(epService, 1);
        assertEventIds(updateListener, 1, null);

        sendEventS0(epService, 101);
        assertFalse(updateListener.isInvoked());

        sendEventS1(epService, 1);
        assertFalse(updateListener.isInvoked());

        sendEventS1(epService, 100);
        assertEventIds(updateListener, null, 100);

        statement.destroy();
    }

    private void runAssertionAggregation(EPServiceProvider epService) {
        String stmtText = "select sum(s0.id) as sumS0, sum(s1.id) as sumS1, sum(s0.id + s1.id) as sumS0S1 " +
                "from pattern [every s0=" + SupportBean_S0.class.getName() +
                " or every s1=" + SupportBean_S1.class.getName() + "]";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        statement.addListener(updateListener);

        sendEventS0(epService, 1);
        assertEventSums(updateListener, 1, null, null);

        sendEventS1(epService, 2);
        assertEventSums(updateListener, 1, 2, null);

        sendEventS1(epService, 10);
        assertEventSums(updateListener, 1, 12, null);

        sendEventS0(epService, 20);
        assertEventSums(updateListener, 21, 12, null);

        statement.destroy();
    }

    private void runAssertionFollowedByAndWindow(EPServiceProvider epService) {
        String stmtText = "select irstream a.id as idA, b.id as idB, " +
                "a.p00 as p00A, b.p00 as p00B from pattern [every a=" + SupportBean_S0.class.getName() +
                " -> every b=" + SupportBean_S0.class.getName() + "(p00=a.p00)]#time(1)";
        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener updateListener = new SupportUpdateListener();

        statement.addListener(updateListener);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        sendEvent(epService, 1, "e1a");
        assertFalse(updateListener.isInvoked());
        sendEvent(epService, 2, "e1a");
        assertNewEvent(updateListener, 1, 2, "e1a");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(500));
        sendEvent(epService, 10, "e2a");
        sendEvent(epService, 11, "e2b");
        sendEvent(epService, 12, "e2c");
        assertFalse(updateListener.isInvoked());
        sendEvent(epService, 13, "e2b");
        assertNewEvent(updateListener, 11, 13, "e2b");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        assertOldEvent(updateListener, 1, 2, "e1a");

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(1500));
        assertOldEvent(updateListener, 11, 13, "e2b");

        statement.destroy();
    }

    private void assertNewEvent(SupportUpdateListener updateListener, int idA, int idB, String p00) {
        EventBean eventBean = updateListener.assertOneGetNewAndReset();
        compareEvent(eventBean, idA, idB, p00);
    }

    private void assertOldEvent(SupportUpdateListener updateListener, int idA, int idB, String p00) {
        EventBean eventBean = updateListener.assertOneGetOldAndReset();
        compareEvent(eventBean, idA, idB, p00);
    }

    private void compareEvent(EventBean eventBean, int idA, int idB, String p00) {
        assertEquals(idA, eventBean.get("idA"));
        assertEquals(idB, eventBean.get("idB"));
        assertEquals(p00, eventBean.get("p00A"));
        assertEquals(p00, eventBean.get("p00B"));
    }

    private void sendEvent(EPServiceProvider epService, int id, String p00) {
        SupportBean_S0 theEvent = new SupportBean_S0(id, p00);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEventS0(EPServiceProvider epService, int id) {
        SupportBean_S0 theEvent = new SupportBean_S0(id);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendEventS1(EPServiceProvider epService, int id) {
        SupportBean_S1 theEvent = new SupportBean_S1(id);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void assertEventIds(SupportUpdateListener updateListener, Integer idS0, Integer idS1) {
        EventBean eventBean = updateListener.getAndResetLastNewData()[0];
        assertEquals(idS0, eventBean.get("idS0"));
        assertEquals(idS1, eventBean.get("idS1"));
        updateListener.reset();
    }

    private void assertEventSums(SupportUpdateListener updateListener, Integer sumS0, Integer sumS1, Integer sumS0S1) {
        EventBean eventBean = updateListener.getAndResetLastNewData()[0];
        assertEquals(sumS0, eventBean.get("sumS0"));
        assertEquals(sumS1, eventBean.get("sumS1"));
        assertEquals(sumS0S1, eventBean.get("sumS0S1"));
        updateListener.reset();
    }
}
