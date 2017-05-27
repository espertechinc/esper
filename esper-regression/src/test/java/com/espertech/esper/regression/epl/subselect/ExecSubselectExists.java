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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.bean.SupportBean_S2;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecSubselectExists implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
        configuration.addEventType("S2", SupportBean_S2.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionExistsInSelect(epService);
        runAssertionExistsInSelectOM(epService);
        runAssertionExistsInSelectCompile(epService);
        runAssertionExists(epService);
        runAssertionExistsFiltered(epService);
        runAssertionTwoExistsFiltered(epService);
        runAssertionNotExists_OM(epService);
        runAssertionNotExists_Compile(epService);
        runAssertionNotExists(epService);
    }

    private void runAssertionExistsInSelect(EPServiceProvider epService) {
        String stmtText = "select exists (select * from S1#length(1000)) as value from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runTestExistsInSelect(epService, listener);

        stmt.destroy();
    }

    private void runAssertionExistsInSelectOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel subquery = new EPStatementObjectModel();
        subquery.setSelectClause(SelectClause.createWildcard());
        subquery.setFromClause(FromClause.create(FilterStream.create("S1").addView(View.create("length", Expressions.constant(1000)))));

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setFromClause(FromClause.create(FilterStream.create("S0")));
        model.setSelectClause(SelectClause.create().add(Expressions.subqueryExists(subquery), "value"));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String stmtText = "select exists (select * from S1#length(1000)) as value from S0";
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runTestExistsInSelect(epService, listener);

        stmt.destroy();
    }

    private void runAssertionExistsInSelectCompile(EPServiceProvider epService) throws Exception {
        String stmtText = "select exists (select * from S1#length(1000)) as value from S0";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runTestExistsInSelect(epService, listener);

        stmt.destroy();
    }

    private void runTestExistsInSelect(EPServiceProvider epService, SupportUpdateListener listener) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));
    }

    private void runAssertionExists(EPServiceProvider epService) {
        String stmtText = "select id from S0 where exists (select * from S1#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(3, listener.assertOneGetNewAndReset().get("id"));

        stmt.destroy();
    }

    private void runAssertionExistsFiltered(EPServiceProvider epService) {
        String stmtText = "select id from S0 as s0 where exists (select * from S1#length(1000) as s1 where s1.id=s0.id)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(-2));
        assertEquals(-2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(3));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(3, listener.assertOneGetNewAndReset().get("id"));

        stmt.destroy();
    }

    private void runAssertionTwoExistsFiltered(EPServiceProvider epService) {
        String stmtText = "select id from S0 as s0 where " +
                "exists (select * from S1#length(1000) as s1 where s1.id=s0.id) " +
                "and " +
                "exists (select * from S2#length(1000) as s2 where s2.id=s0.id) ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S2(3));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(3));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(3, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));
        epService.getEPRuntime().sendEvent(new SupportBean_S2(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(1, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionNotExists_OM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel subquery = new EPStatementObjectModel();
        subquery.setSelectClause(SelectClause.createWildcard());
        subquery.setFromClause(FromClause.create(FilterStream.create("S1").addView("length", Expressions.constant(1000))));

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("id"));
        model.setFromClause(FromClause.create(FilterStream.create("S0")));
        model.setWhereClause(Expressions.not(Expressions.subqueryExists(subquery)));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String stmtText = "select id from S0 where not exists (select * from S1#length(1000))";
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionNotExists_Compile(EPServiceProvider epService) throws Exception {
        String stmtText = "select id from S0 where not exists (select * from S1#length(1000))";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionNotExists(EPServiceProvider epService) {
        String stmtText = "select id from S0 where not exists (select * from S1#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }
}
