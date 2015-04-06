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
import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportBean_S1;
import com.espertech.esper.support.bean.SupportBean_S2;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;

public class TestSubselectExists extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("S0", SupportBean_S0.class);
        config.addEventType("S1", SupportBean_S1.class);
        config.addEventType("S2", SupportBean_S2.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testExistsInSelect()
    {
        String stmtText = "select exists (select * from S1.win:length(1000)) as value from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        runTestExistsInSelect();
    }

    public void testExistsInSelectOM() throws Exception
    {
        EPStatementObjectModel subquery = new EPStatementObjectModel();
        subquery.setSelectClause(SelectClause.createWildcard());
        subquery.setFromClause(FromClause.create(FilterStream.create("S1").addView(View.create("win", "length", Expressions.constant(1000)))));

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setFromClause(FromClause.create(FilterStream.create("S0")));
        model.setSelectClause(SelectClause.create().add(Expressions.subqueryExists(subquery), "value"));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String stmtText = "select exists (select * from S1.win:length(1000)) as value from S0";
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        runTestExistsInSelect();
    }

    public void testExistsInSelectCompile() throws Exception
    {
        String stmtText = "select exists (select * from S1.win:length(1000)) as value from S0";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        runTestExistsInSelect();
    }

    private void runTestExistsInSelect()
    {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));
    }

    public void testExists()
    {
        String stmtText = "select id from S0 where exists (select * from S1.win:length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(3, listener.assertOneGetNewAndReset().get("id"));
    }

    public void testExistsFiltered()
    {
        String stmtText = "select id from S0 as s0 where exists (select * from S1.win:length(1000) as s1 where s1.id=s0.id)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
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
    }

    public void testTwoExistsFiltered()
    {
        String stmtText = "select id from S0 as s0 where " +
                "exists (select * from S1.win:length(1000) as s1 where s1.id=s0.id) " +
                "and " +
                "exists (select * from S2.win:length(1000) as s2 where s2.id=s0.id) "
                ;

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
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
    }

    public void testNotExists_OM() throws Exception
    {
        EPStatementObjectModel subquery = new EPStatementObjectModel();
        subquery.setSelectClause(SelectClause.createWildcard());
        subquery.setFromClause(FromClause.create(FilterStream.create("S1").addView("win", "length", Expressions.constant(1000))));

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("id"));
        model.setFromClause(FromClause.create(FilterStream.create("S0")));
        model.setWhereClause(Expressions.not(Expressions.subqueryExists(subquery)));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String stmtText = "select id from S0 where not exists (select * from S1.win:length(1000))";
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertFalse(listener.isInvoked());
    }

    public void testNotExists_Compile() throws Exception
    {
        String stmtText = "select id from S0 where not exists (select * from S1.win:length(1000))";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertFalse(listener.isInvoked());
    }

    public void testNotExists()
    {
        String stmtText = "select id from S0 where not exists (select * from S1.win:length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertFalse(listener.isInvoked());
    }
}
