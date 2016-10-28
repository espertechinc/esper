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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;

public class TestSubselectIn extends TestCase
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

    public void testInSelect()
    {
        String stmtText = "select id in (select id from S1#length(1000)) as value from S0";

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        assertFalse(stmt.getStatementContext().isStatelessSelect());

        runTestInSelect();
    }

    public void testInSelectOM() throws Exception
    {
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
        stmt.addListener(listener);

        runTestInSelect();
    }

    public void testInSelectCompile() throws Exception
    {
        String stmtText = "select id in (select id from S1#length(1000)) as value from S0";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        runTestInSelect();
    }

    private void runTestInSelect()
    {
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

    public void testInSelectWhere()
    {
        String stmtText = "select id in (select id from S1#length(1000) where id > 0) as value from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
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
    }

    public void testInSelectWhereExpressions()
    {
        String stmtText = "select 3*id in (select 2*id from S1#length(1000)) as value from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
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
    }

    public void testInWildcard()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("ArrayBean", SupportBeanArrayCollMap.class);
        String stmtText = "select s0.anyObject in (select * from S1#length(1000)) as value from ArrayBean s0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
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
    }

    public void testInNullable()
    {
        String stmtText = "select id from S0 as s0 where p00 in (select p10 from S1#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
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
    }

    public void testInNullableCoercion()
    {
        String stmtText = "select longBoxed from " + SupportBean.class.getName() + "(theString='A') as s0 " +
                          "where longBoxed in " +
                          "(select intBoxed from " + SupportBean.class.getName() + "(theString='B')#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendBean("A", 0, 0L);
        sendBean("A", null, null);
        assertFalse(listener.isInvoked());

        sendBean("B", null, null);

        sendBean("A", 0, 0L);
        assertFalse(listener.isInvoked());
        sendBean("A", null, null);
        assertFalse(listener.isInvoked());

        sendBean("B", 99, null);

        sendBean("A", null, null);
        assertFalse(listener.isInvoked());
        sendBean("A", null, 99l);
        assertEquals(99L, listener.assertOneGetNewAndReset().get("longBoxed"));

        sendBean("B", 98, null);

        sendBean("A", null, 98l);
        assertEquals(98L, listener.assertOneGetNewAndReset().get("longBoxed"));
    }

    public void testInNullRow()
    {
        String stmtText = "select intBoxed from " + SupportBean.class.getName() + "(theString='A') as s0 " +
                          "where intBoxed in " +
                          "(select longBoxed from " + SupportBean.class.getName() + "(theString='B')#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendBean("B", 1, 1l);

        sendBean("A", null, null);
        assertFalse(listener.isInvoked());

        sendBean("A", 1, 1l);
        assertEquals(1, listener.assertOneGetNewAndReset().get("intBoxed"));

        sendBean("B", null, null);

        sendBean("A", null, null);
        assertFalse(listener.isInvoked());

        sendBean("A", 1, 1l);
        assertEquals(1, listener.assertOneGetNewAndReset().get("intBoxed"));
    }

    public void testNotInNullRow()
    {
        String stmtText = "select intBoxed from " + SupportBean.class.getName() + "(theString='A') as s0 " +
                          "where intBoxed not in " +
                          "(select longBoxed from " + SupportBean.class.getName() + "(theString='B')#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendBean("B", 1, 1l);

        sendBean("A", null, null);
        assertFalse(listener.isInvoked());

        sendBean("A", 1, 1l);
        assertFalse(listener.isInvoked());

        sendBean("B", null, null);

        sendBean("A", null, null);
        assertFalse(listener.isInvoked());

        sendBean("A", 1, 1l);
        assertFalse(listener.isInvoked());
    }

    public void testNotInSelect()
    {
        String stmtText = "select not id in (select id from S1#length(1000)) as value from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
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
    }

    public void testNotInNullableCoercion()
    {
        String stmtText = "select longBoxed from " + SupportBean.class.getName() + "(theString='A') as s0 " +
                          "where longBoxed not in " +
                          "(select intBoxed from " + SupportBean.class.getName() + "(theString='B')#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendBean("A", 0, 0L);
        assertEquals(0L, listener.assertOneGetNewAndReset().get("longBoxed"));

        sendBean("A", null, null);
        assertEquals(null, listener.assertOneGetNewAndReset().get("longBoxed"));

        sendBean("B", null, null);

        sendBean("A", 1, 1L);
        assertFalse(listener.isInvoked());
        sendBean("A", null, null);
        assertFalse(listener.isInvoked());

        sendBean("B", 99, null);

        sendBean("A", null, null);
        assertFalse(listener.isInvoked());
        sendBean("A", null, 99l);
        assertFalse(listener.isInvoked());

        sendBean("B", 98, null);

        sendBean("A", null, 98l);
        assertFalse(listener.isInvoked());

        sendBean("A", null, 97l);
        assertFalse(listener.isInvoked());
    }

    public void testInvalid()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ArrayBean", SupportBeanArrayCollMap.class);
        try
        {
            String stmtText = "select " +
                          "intArr in (select intPrimitive from SupportBean#keepall()) as r1 from ArrayBean";
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals("Error starting statement: Failed to validate select-clause expression subquery number 1 querying SupportBean: Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords [select intArr in (select intPrimitive from SupportBean#keepall()) as r1 from ArrayBean]", ex.getMessage());
        }
    }

    private void sendBean(String theString, Integer intBoxed, Long longBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
