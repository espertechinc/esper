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
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecSubselectUnfiltered implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("S0", SupportBean_S0.class);
        configuration.addEventType("S1", SupportBean_S1.class);
        configuration.addEventType("S2", SupportBean_S2.class);
        configuration.addEventType("S3", SupportBean_S3.class);
        configuration.addEventType("S4", SupportBean_S4.class);
        configuration.addEventType("S5", SupportBean_S5.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSelfSubselect(epService);
        runAssertionStartStopStatement(epService);
        runAssertionWhereClauseReturningTrue(epService);
        runAssertionWhereClauseWithExpression(epService);
        runAssertionJoinUnfiltered(epService);
        runAssertionInvalidSubselect(epService);
        runAssertionUnfilteredStreamPrior_OM(epService);
        runAssertionUnfilteredStreamPrior_Compile(epService);
        runAssertionCustomFunction(epService);
        runAssertionComputedResult(epService);
        runAssertionFilterInside(epService);
        runAssertionUnfilteredUnlimitedStream(epService);
        runAssertionUnfilteredLengthWindow(epService);
        runAssertionUnfilteredAsAfterSubselect(epService);
        runAssertionUnfilteredWithAsWithinSubselect(epService);
        runAssertionUnfilteredNoAs(epService);
        runAssertionUnfilteredExpression(epService);
        runAssertionMultiColumnSelect(epService);
    }

    private void runAssertionSelfSubselect(EPServiceProvider epService) {
        String stmtTextOne = "insert into MyCount select count(*) as cnt from S0";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        String stmtTextTwo = "select (select cnt from MyCount#lastevent) as value from S0";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(null, listener.assertOneGetNewAndReset().get("value"));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(1L, listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void runAssertionStartStopStatement(EPServiceProvider epService) {
        String stmtText = "select id from S0 where (select true from S1#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void runAssertionWhereClauseReturningTrue(EPServiceProvider epService) {
        String stmtText = "select id from S0 where (select true from S1#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(2));
        assertEquals(2, listener.assertOneGetNewAndReset().get("id"));

        stmt.destroy();
    }

    private void runAssertionWhereClauseWithExpression(EPServiceProvider epService) {
        String stmtText = "select id from S0 where (select p10='X' from S1#length(1000))";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean_S1(10, "X"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        assertEquals(0, listener.assertOneGetNewAndReset().get("id"));

        stmt.destroy();
    }

    private void runAssertionJoinUnfiltered(EPServiceProvider epService) {
        String stmtText = "select (select id from S3#length(1000)) as idS3, (select id from S4#length(1000)) as idS4 from S0#keepall as s0, S1#keepall as s1 where s0.id = s1.id";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
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
        for (int i = 0; i < events.length; i++) {
            assertEquals(null, events[i].get("idS3"));
            assertEquals(null, events[i].get("idS4"));
        }

        stmt.destroy();
    }

    private void runAssertionInvalidSubselect(EPServiceProvider epService) {
        tryInvalid(epService, "select (select id from S1) from S0",
                "Error starting statement: Failed to plan subquery number 1 querying S1: Subqueries require one or more views to limit the stream, consider declaring a length or time window (applies to correlated or non-fully-aggregated subqueries) [");

        tryInvalid(epService, "select (select dummy from S1#lastevent) as idS1 from S0",
                "Error starting statement: Failed to plan subquery number 1 querying S1: Failed to validate select-clause expression 'dummy': Property named 'dummy' is not valid in any stream [select (select dummy from S1#lastevent) as idS1 from S0]");

        tryInvalid(epService, "select (select (select id from S1#lastevent) id from S1#lastevent) as idS1 from S0",
                "Invalid nested subquery, subquery-within-subquery is not supported [select (select (select id from S1#lastevent) id from S1#lastevent) as idS1 from S0]");

        tryInvalid(epService, "select (select id from S1#lastevent where (sum(id) = 5)) as idS1 from S0",
                "Error starting statement: Failed to plan subquery number 1 querying S1: Aggregation functions are not supported within subquery filters, consider using a having-clause or insert-into instead [select (select id from S1#lastevent where (sum(id) = 5)) as idS1 from S0]");

        tryInvalid(epService, "select * from S0(id=5 and (select id from S1))",
                "Failed to validate subquery number 1 querying S1: Subqueries require one or more views to limit the stream, consider declaring a length or time window [select * from S0(id=5 and (select id from S1))]");

        tryInvalid(epService, "select * from S0 group by id + (select id from S1)",
                "Error starting statement: Subselects not allowed within group-by [select * from S0 group by id + (select id from S1)]");

        tryInvalid(epService, "select * from S0 order by (select id from S1) asc",
                "Error starting statement: Subselects not allowed within order-by clause [select * from S0 order by (select id from S1) asc]");

        tryInvalid(epService, "select (select id from S1#lastevent where 'a') from S0",
                "Error starting statement: Failed to plan subquery number 1 querying S1: Subselect filter expression must return a boolean value [select (select id from S1#lastevent where 'a') from S0]");

        tryInvalid(epService, "select (select id from S1#lastevent where id = p00) from S0",
                "Error starting statement: Failed to plan subquery number 1 querying S1: Failed to validate filter expression 'id=p00': Property named 'p00' must be prefixed by a stream name, use the stream name itself or use the as-clause to name the stream with the property in the format \"stream.property\" [select (select id from S1#lastevent where id = p00) from S0]");

        tryInvalid(epService, "select id in (select * from S1#length(1000)) as value from S0",
                "Error starting statement: Failed to validate select-clause expression subquery number 1 querying S1: Implicit conversion from datatype 'SupportBean_S1' to 'Integer' is not allowed [select id in (select * from S1#length(1000)) as value from S0]");
    }

    private void runAssertionUnfilteredStreamPrior_OM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel subquery = new EPStatementObjectModel();
        subquery.setSelectClause(SelectClause.create().add(Expressions.prior(0, "id")));
        subquery.setFromClause(FromClause.create(FilterStream.create("S1").addView("length", Expressions.constant(1000))));

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.subquery(subquery), "idS1"));
        model.setFromClause(FromClause.create(FilterStream.create("S0")));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String stmtText = "select (select prior(0,id) from S1#length(1000)) as idS1 from S0";
        assertEquals(stmtText, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        runUnfilteredStreamPrior(epService, stmt);
        stmt.destroy();
    }

    private void runAssertionUnfilteredStreamPrior_Compile(EPServiceProvider epService) throws Exception {
        String stmtText = "select (select prior(0,id) from S1#length(1000)) as idS1 from S0";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        runUnfilteredStreamPrior(epService, stmt);
        stmt.destroy();
    }

    private void runUnfilteredStreamPrior(EPServiceProvider epService, EPStatement stmt) {
        SupportUpdateListener listener = new SupportUpdateListener();
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

    private void runAssertionCustomFunction(EPServiceProvider epService) {
        String stmtText = "select (select " + SupportStaticMethodLib.class.getName() + ".minusOne(id) from S1#length(1000)) as idS1 from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void runAssertionComputedResult(EPServiceProvider epService) {
        String stmtText = "select 100*(select id from S1#length(1000)) as idS1 from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void runAssertionFilterInside(EPServiceProvider epService) {
        String stmtText = "select (select id from S1(p10='A')#length(1000)) as idS1 from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "X"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(null, listener.assertOneGetNewAndReset().get("idS1"));

        epService.getEPRuntime().sendEvent(new SupportBean_S1(1, "A"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        assertEquals(1, listener.assertOneGetNewAndReset().get("idS1"));

        stmt.destroy();
    }

    private void runAssertionUnfilteredUnlimitedStream(EPServiceProvider epService) {
        String stmtText = "select (select id from S1#length(1000)) as idS1 from S0";
        tryAssertMultiRowUnfiltered(epService, stmtText, "idS1");
    }

    private void runAssertionUnfilteredLengthWindow(EPServiceProvider epService) {
        String stmtText = "select (select id from S1#length(2)) as idS1 from S0";
        tryAssertMultiRowUnfiltered(epService, stmtText, "idS1");
    }

    private void runAssertionUnfilteredAsAfterSubselect(EPServiceProvider epService) {
        String stmtText = "select (select id from S1#lastevent) as idS1 from S0";
        tryAssertSingleRowUnfiltered(epService, stmtText, "idS1");
    }

    private void runAssertionUnfilteredWithAsWithinSubselect(EPServiceProvider epService) {
        String stmtText = "select (select id as myId from S1#lastevent) from S0";
        tryAssertSingleRowUnfiltered(epService, stmtText, "myId");
    }

    private void runAssertionUnfilteredNoAs(EPServiceProvider epService) {
        String stmtText = "select (select id from S1#lastevent) from S0";
        tryAssertSingleRowUnfiltered(epService, stmtText, "id");
    }

    private void runAssertionUnfilteredExpression(EPServiceProvider epService) {
        String stmtText = "select (select p10 || p11 from S1#lastevent) as value from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void runAssertionMultiColumnSelect(EPServiceProvider epService) {
        String stmtText = "select (select id+1 as myId from S1#lastevent) as idS1_0, " +
                "(select id+2 as myId from S1#lastevent) as idS1_1 from S0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void tryAssertSingleRowUnfiltered(EPServiceProvider epService, String stmtText, String columnName) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }

    private void tryAssertMultiRowUnfiltered(EPServiceProvider epService, String stmtText, String columnName) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
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

        stmt.destroy();
    }
}
