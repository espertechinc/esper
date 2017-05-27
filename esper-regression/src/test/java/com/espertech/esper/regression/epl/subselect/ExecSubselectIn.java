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
package com.espertech.esper.regression.epl.subselect;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.*;

public class ExecSubselectIn implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
        configuration.addEventType("S2", SupportBean_S2.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInSelect(epService);
        runAssertionInSelectOM(epService);
        runAssertionInSelectCompile(epService);
        runAssertionInSelectWhere(epService);
        runAssertionInSelectWhereExpressions(epService);
        runAssertionInWildcard(epService);
        runAssertionInNullable(epService);
        runAssertionInNullableCoercion(epService);
        runAssertionInNullRow(epService);
        runAssertionNotInNullRow(epService);
        runAssertionNotInSelect(epService);
        runAssertionNotInNullableCoercion(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionInSelect(EPServiceProvider epService) {
        String stmtText = "select id in (select id from S1#length(1000)) as value from S0";

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertFalse(stmt.getStatementContext().isStatelessSelect());

        runTestInSelect(epService, listener);

        stmt.destroy();
    }

    private void runAssertionInSelectOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel subquery = new EPStatementObjectModel();
        subquery.setSelectClause(SelectClause.create("id"));
        subquery.setFromClause(FromClause.create(FilterStream.create("S1").addView(View.create("length", Expressions.constant(1000)))));

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setFromClause(FromClause.create(FilterStream.create("S0")));
        model.setSelectClause(SelectClause.create().add(Expressions.subqueryIn("id", subquery), "value"));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String stmtText = "select id in (select id from S1#length(1000)) as value from S0";
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runTestInSelect(epService, listener);

        stmt.destroy();
    }

    private void runAssertionInSelectCompile(EPServiceProvider epService) throws Exception {
        String stmtText = "select id in (select id from S1#length(1000)) as value from S0";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runTestInSelect(epService, listener);

        stmt.destroy();
    }

    private void runTestInSelect(EPServiceProvider epService, SupportUpdateListener listener) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1));
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(5));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(5));
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));
    }

    private void runAssertionInSelectWhere(EPServiceProvider epService) {
        String stmtText = "select id in (select id from S1#length(1000) where id > 0) as value from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(5));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(5));
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void runAssertionInSelectWhereExpressions(EPServiceProvider epService) {
        String stmtText = "select 3*id in (select 2*id from S1#length(1000)) as value from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(6));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4));
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void runAssertionInWildcard(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("ArrayBean", SupportBeanArrayCollMap.class);
        String stmtText = "select s0.anyObject in (select * from S1#length(1000)) as value from ArrayBean s0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean_S1 s1 = new SupportBean_S1(100);
        SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(s1);
        epService.getEPRuntime().sendEvent(s1);
        epService.getEPRuntime().sendEvent(arrayBean);
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));

        SupportBean_S2 s2 = new SupportBean_S2(100);
        arrayBean.setAnyObject(s2);
        epService.getEPRuntime().sendEvent(s2);
        epService.getEPRuntime().sendEvent(arrayBean);
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void runAssertionInNullable(EPServiceProvider epService) {
        String stmtText = "select id from S0 as s0 where p00 in (select p10 from S1#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "a"));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2, null));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3, null));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S0(4, "A"));
        assertEquals(4, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2, null));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, null));
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionInNullableCoercion(EPServiceProvider epService) {
        String stmtText = "select longBoxed from " + SupportBean.class.getName() + "(theString='A') as s0 " +
                "where longBoxed in " +
                "(select intBoxed from " + SupportBean.class.getName() + "(theString='B')#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBean(epService, "A", 0, 0L);
        sendBean(epService, "A", null, null);
        assertFalse(listener.isInvoked());

        sendBean(epService, "B", null, null);

        sendBean(epService, "A", 0, 0L);
        assertFalse(listener.isInvoked());
        sendBean(epService, "A", null, null);
        assertFalse(listener.isInvoked());

        sendBean(epService, "B", 99, null);

        sendBean(epService, "A", null, null);
        assertFalse(listener.isInvoked());
        sendBean(epService, "A", null, 99L);
        assertEquals(99L, listener.assertOneGetNewAndReset().get("longBoxed"));

        sendBean(epService, "B", 98, null);

        sendBean(epService, "A", null, 98L);
        assertEquals(98L, listener.assertOneGetNewAndReset().get("longBoxed"));

        stmt.destroy();
    }

    private void runAssertionInNullRow(EPServiceProvider epService) {
        String stmtText = "select intBoxed from " + SupportBean.class.getName() + "(theString='A') as s0 " +
                "where intBoxed in " +
                "(select longBoxed from " + SupportBean.class.getName() + "(theString='B')#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBean(epService, "B", 1, 1L);

        sendBean(epService, "A", null, null);
        assertFalse(listener.isInvoked());

        sendBean(epService, "A", 1, 1L);
        assertEquals(1, listener.assertOneGetNewAndReset().get("intBoxed"));

        sendBean(epService, "B", null, null);

        sendBean(epService, "A", null, null);
        assertFalse(listener.isInvoked());

        sendBean(epService, "A", 1, 1L);
        assertEquals(1, listener.assertOneGetNewAndReset().get("intBoxed"));

        stmt.destroy();
    }

    private void runAssertionNotInNullRow(EPServiceProvider epService) {
        String stmtText = "select intBoxed from " + SupportBean.class.getName() + "(theString='A') as s0 " +
                "where intBoxed not in " +
                "(select longBoxed from " + SupportBean.class.getName() + "(theString='B')#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBean(epService, "B", 1, 1L);

        sendBean(epService, "A", null, null);
        assertFalse(listener.isInvoked());

        sendBean(epService, "A", 1, 1L);
        assertFalse(listener.isInvoked());

        sendBean(epService, "B", null, null);

        sendBean(epService, "A", null, null);
        assertFalse(listener.isInvoked());

        sendBean(epService, "A", 1, 1L);
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionNotInSelect(EPServiceProvider epService) {
        String stmtText = "select not id in (select id from S1#length(1000)) as value from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(-1));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(5));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(4));
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(5));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void runAssertionNotInNullableCoercion(EPServiceProvider epService) {
        String stmtText = "select longBoxed from " + SupportBean.class.getName() + "(theString='A') as s0 " +
                "where longBoxed not in " +
                "(select intBoxed from " + SupportBean.class.getName() + "(theString='B')#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendBean(epService, "A", 0, 0L);
        assertEquals(0L, listener.assertOneGetNewAndReset().get("longBoxed"));

        sendBean(epService, "A", null, null);
        assertEquals(null, listener.assertOneGetNewAndReset().get("longBoxed"));

        sendBean(epService, "B", null, null);

        sendBean(epService, "A", 1, 1L);
        assertFalse(listener.isInvoked());
        sendBean(epService, "A", null, null);
        assertFalse(listener.isInvoked());

        sendBean(epService, "B", 99, null);

        sendBean(epService, "A", null, null);
        assertFalse(listener.isInvoked());
        sendBean(epService, "A", null, 99L);
        assertFalse(listener.isInvoked());

        sendBean(epService, "B", 98, null);

        sendBean(epService, "A", null, 98L);
        assertFalse(listener.isInvoked());

        sendBean(epService, "A", null, 97L);
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ArrayBean", SupportBeanArrayCollMap.class);
        try {
            String stmtText = "select " +
                    "intArr in (select intPrimitive from SupportBean#keepall) as r1 from ArrayBean";
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression subquery number 1 querying SupportBean: Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords [select intArr in (select intPrimitive from SupportBean#keepall) as r1 from ArrayBean]", ex.getMessage());
        }
    }

    private void sendBean(EPServiceProvider epService, String theString, Integer intBoxed, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
