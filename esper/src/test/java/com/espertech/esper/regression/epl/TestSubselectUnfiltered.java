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
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.epl.SupportStaticMethodLib;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;

public class TestSubselectUnfiltered extends TestCase {
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("S0", SupportBean_S0.class);
        config.addEventType("S1", SupportBean_S1.class);
        config.addEventType("S2", SupportBean_S2.class);
        config.addEventType("S3", SupportBean_S3.class);
        config.addEventType("S4", SupportBean_S4.class);
        config.addEventType("S5", SupportBean_S5.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testSelfSubselect()
    {
        String stmtTextOne = "insert into MyCount select count(*) as cnt from S0";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        String stmtTextTwo = "select (select cnt from MyCount.std:lastevent()) as value from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextTwo);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(null, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(1L, listener.assertOneGetNewAndReset().get("value"));
    }

    public void testStartStopStatement()
    {
        String stmtText = "select id from S0 where (select true from S1.win:length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        stmt.stop();
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        stmt.start();
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(3, listener.assertOneGetNewAndReset().get("id"));
    }

    public void testWhereClauseReturningTrue()
    {
        String stmtText = "select id from S0 where (select true from S1.win:length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));
    }

    public void testWhereClauseWithExpression()
    {
        String stmtText = "select id from S0 where (select p10='X' from S1.win:length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "X"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(0, listener.assertOneGetNewAndReset().get("id"));
    }

    public void testJoinUnfiltered()
    {
        String stmtText = "select (select id from S3.win:length(1000)) as idS3, (select id from S4.win:length(1000)) as idS4 from S0.win:keepall() as s0, S1.win:keepall() as s1 where s0.id = s1.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("idS3"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("idS4"));

        // test no event, should return null
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("idS3"));
        assertEquals(null, theEvent.get("idS4"));

        // send one event
        epService.getEPRuntime().sendEvent(new SupportBean_S3(-1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(1));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(-1, theEvent.get("idS3"));
        assertEquals(null, theEvent.get("idS4"));

        // send one event
        epService.getEPRuntime().sendEvent(new SupportBean_S4(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(-1, theEvent.get("idS3"));
        assertEquals(-2, theEvent.get("idS4"));

        // send second event
        epService.getEPRuntime().sendEvent(new SupportBean_S4(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(3));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(-1, theEvent.get("idS3"));
        assertEquals(null, theEvent.get("idS4"));

        epService.getEPRuntime().sendEvent(new SupportBean_S3(-2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        epService.getEPRuntime().sendEvent(new SupportBean_S1(3));
        EventBean[] events = listener.getNewDataListFlattened();
        assertEquals(3, events.length);
        for (int i = 0; i < events.length; i++)
        {
            assertEquals(null, events[i].get("idS3"));
            assertEquals(null, events[i].get("idS4"));
        }
    }

    public void testInvalidSubselect()
    {
        tryInvalid("select (select id from S1) from S0",
                   "Error starting statement: Failed to plan subquery number 1 querying S1: Subqueries require one or more views to limit the stream, consider declaring a length or time window (applies to correlated or non-fully-aggregated subqueries) [");

        tryInvalid("select (select dummy from S1.std:lastevent()) as idS1 from S0",
                   "Error starting statement: Failed to plan subquery number 1 querying S1: Failed to validate select-clause expression 'dummy': Property named 'dummy' is not valid in any stream [select (select dummy from S1.std:lastevent()) as idS1 from S0]");

        tryInvalid("select (select (select id from S1.std:lastevent()) id from S1.std:lastevent()) as idS1 from S0",
                   "Invalid nested subquery, subquery-within-subquery is not supported [select (select (select id from S1.std:lastevent()) id from S1.std:lastevent()) as idS1 from S0]");

        tryInvalid("select (select id from S1.std:lastevent() where (sum(id) = 5)) as idS1 from S0",
                   "Error starting statement: Failed to plan subquery number 1 querying S1: Aggregation functions are not supported within subquery filters, consider using insert-into instead [select (select id from S1.std:lastevent() where (sum(id) = 5)) as idS1 from S0]");

        tryInvalid("select * from S0(id=5 and (select id from S1))",
                   "Failed to validate subquery number 1 querying S1: Subqueries require one or more views to limit the stream, consider declaring a length or time window [select * from S0(id=5 and (select id from S1))]");

        tryInvalid("select * from S0 group by id + (select id from S1)",
                   "Error starting statement: Subselects not allowed within group-by [select * from S0 group by id + (select id from S1)]");

        tryInvalid("select * from S0 order by (select id from S1) asc",
                   "Error starting statement: Subselects not allowed within order-by clause [select * from S0 order by (select id from S1) asc]");

        tryInvalid("select (select id from S1.std:lastevent() where 'a') from S0",
                   "Error starting statement: Failed to plan subquery number 1 querying S1: Subselect filter expression must return a boolean value [select (select id from S1.std:lastevent() where 'a') from S0]");

        tryInvalid("select (select id from S1.std:lastevent() where id = p00) from S0",
                   "Error starting statement: Failed to plan subquery number 1 querying S1: Failed to validate filter expression 'id=p00': Property named 'p00' must be prefixed by a stream name, use the stream name itself or use the as-clause to name the stream with the property in the format \"stream.property\" [select (select id from S1.std:lastevent() where id = p00) from S0]");

        tryInvalid("select id in (select * from S1.win:length(1000)) as value from S0",
                   "Error starting statement: Failed to validate select-clause expression subquery number 1 querying S1: Implicit conversion from datatype 'SupportBean_S1' to 'Integer' is not allowed [select id in (select * from S1.win:length(1000)) as value from S0]");
    }

    public void testUnfilteredStreamPrior_OM() throws Exception
    {
        EPStatementObjectModel subquery = new EPStatementObjectModel();
        subquery.setSelectClause(SelectClause.create().add(Expressions.prior(0, "id")));
        subquery.setFromClause(FromClause.create(FilterStream.create("S1").addView("win", "length", Expressions.constant(1000))));

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.subquery(subquery), "idS1"));
        model.setFromClause(FromClause.create(FilterStream.create("S0")));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String stmtText = "select (select prior(0,id) from S1.win:length(1000)) as idS1 from S0";
        assertEquals(stmtText, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        runUnfilteredStreamPrior(stmt);
    }

    public void testUnfilteredStreamPrior_Compile() throws Exception
    {
        String stmtText = "select (select prior(0,id) from S1.win:length(1000)) as idS1 from S0";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        runUnfilteredStreamPrior(stmt);
    }

    private void runUnfilteredStreamPrior(EPStatement stmt)
    {
        stmt.addListener(listener);

        // check type
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("idS1"));

        // test no event, should return null
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(null, listener.assertOneGetNewAndReset().get("idS1"));

        // test one event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(10, listener.assertOneGetNewAndReset().get("idS1"));

        // resend event
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(10, listener.assertOneGetNewAndReset().get("idS1"));

        // test second event
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(10, listener.assertOneGetNewAndReset().get("idS1"));
    }

    public void testCustomFunction()
    {
        String stmtText = "select (select " + SupportStaticMethodLib.class.getName() + ".minusOne(id) from S1.win:length(1000)) as idS1 from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Double.class, stmt.getEventType().getPropertyType("idS1"));

        // test no event, should return null
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(null, listener.assertOneGetNewAndReset().get("idS1"));

        // test one event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(9d, listener.assertOneGetNewAndReset().get("idS1"));

        // resend event
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(9d, listener.assertOneGetNewAndReset().get("idS1"));
    }

    public void testComputedResult()
    {
        String stmtText = "select 100*(select id from S1.win:length(1000)) as idS1 from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("idS1"));

        // test no event, should return null
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(null, listener.assertOneGetNewAndReset().get("idS1"));

        // test one event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(1000, listener.assertOneGetNewAndReset().get("idS1"));

        // resend event
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(1000, listener.assertOneGetNewAndReset().get("idS1"));
    }

    public void testFilterInside()
    {
        String stmtText = "select (select id from S1(p10='A').win:length(1000)) as idS1 from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "X"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(null, listener.assertOneGetNewAndReset().get("idS1"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(1, listener.assertOneGetNewAndReset().get("idS1"));
    }

    public void testUnfilteredUnlimitedStream()
    {
        String stmtText = "select (select id from S1.win:length(1000)) as idS1 from S0";
        runAssertMultiRowUnfiltered(stmtText, "idS1");
    }

    public void testUnfilteredLengthWindow()
    {
        String stmtText = "select (select id from S1.win:length(2)) as idS1 from S0";
        runAssertMultiRowUnfiltered(stmtText, "idS1");
    }

    public void testUnfilteredAsAfterSubselect()
    {
        String stmtText = "select (select id from S1.std:lastevent()) as idS1 from S0";
        runAssertSingleRowUnfiltered(stmtText, "idS1");
    }

    public void testUnfilteredWithAsWithinSubselect()
    {
        String stmtText = "select (select id as myId from S1.std:lastevent()) from S0";
        runAssertSingleRowUnfiltered(stmtText, "myId");
    }

    public void testUnfilteredNoAs()
    {
        String stmtText = "select (select id from S1.std:lastevent()) from S0";
        runAssertSingleRowUnfiltered(stmtText, "id");
    }

    public void testUnfilteredExpression()
    {
        String stmtText = "select (select p10 || p11 from S1.std:lastevent()) as value from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(String.class, stmt.getEventType().getPropertyType("value"));

        // test no event, should return null
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("value"));

        // test one event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(-1, "a", "b"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals("ab", theEvent.get("value"));
    }

    public void testMultiColumnSelect()
    {
        String stmtText = "select (select id+1 as myId from S1.std:lastevent()) as idS1_0, " +
                "(select id+2 as myId from S1.std:lastevent()) as idS1_1 from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("idS1_0"));
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("idS1_1"));

        // test no event, should return null
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("idS1_0"));
        assertEquals(null, theEvent.get("idS1_1"));

        // test one event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(11, theEvent.get("idS1_0"));
        assertEquals(12, theEvent.get("idS1_1"));

        // resend event
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(11, theEvent.get("idS1_0"));
        assertEquals(12, theEvent.get("idS1_1"));

        // test second event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(999));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        theEvent = listener.assertOneGetNewAndReset();
        assertEquals(1000, theEvent.get("idS1_0"));
        assertEquals(1001, theEvent.get("idS1_1"));
    }

    private void runAssertSingleRowUnfiltered(String stmtText, String columnName)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Integer.class, stmt.getEventType().getPropertyType(columnName));

        // test no event, should return null
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(null, listener.assertOneGetNewAndReset().get(columnName));

        // test one event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(10, listener.assertOneGetNewAndReset().get(columnName));

        // resend event
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(10, listener.assertOneGetNewAndReset().get(columnName));

        // test second event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(999));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(999, listener.assertOneGetNewAndReset().get(columnName));
    }

    private void runAssertMultiRowUnfiltered(String stmtText, String columnName)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        // check type
        assertEquals(Integer.class, stmt.getEventType().getPropertyType(columnName));

        // test no event, should return null
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(null, listener.assertOneGetNewAndReset().get(columnName));

        // test one event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(10, listener.assertOneGetNewAndReset().get(columnName));

        // resend event
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(10, listener.assertOneGetNewAndReset().get(columnName));

        // test second event
        epService.getEPRuntime().sendEvent(new SupportBean_S1(999));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(3));
        assertEquals(null, listener.assertOneGetNewAndReset().get(columnName));
    }

    private void tryInvalid(String stmtText, String expectedMsg)
    {
        try
        {
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        }
        catch (EPStatementException ex)
        {
            SupportMessageAssertUtil.assertMessage(ex, expectedMsg);
        }
    }
}
