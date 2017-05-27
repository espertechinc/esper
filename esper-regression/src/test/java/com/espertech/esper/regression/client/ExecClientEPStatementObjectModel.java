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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecClientEPStatementObjectModel implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCreateFromOM(epService);
        runAssertionCreateFromOMComplete();
        runAssertionCompileToOM(epService);
        runAssertionEPLtoOMtoStmt(epService);
        runAssertionPrecedenceExpressions(epService);
        runAssertionPrecedencePatterns(epService);
    }

    // This is a simple EPL only.
    // Each OM/SODA Api is tested in it's respective unit test (i.e. TestInsertInto), including toEPL()
    // 
    private void runAssertionCreateFromOM(EPServiceProvider epService) throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));
        SerializableObjectCopier.copy(model);

        EPStatement stmt = epService.getEPAdministrator().create(model, "s1");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals(theEvent, listener.assertOneGetNewAndReset().getUnderlying());

        stmt.destroy();
    }

    // This is a simple EPL only.
    // Each OM/SODA Api is tested in it's respective unit test (i.e. TestInsertInto), including toEPL()
    //
    private void runAssertionCreateFromOMComplete() throws Exception {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setInsertInto(InsertIntoClause.create("ReadyStreamAvg", "line", "avgAge"));
        model.setSelectClause(SelectClause.create()
                .add("line")
                .add(Expressions.avg("age"), "avgAge"));
        Filter filter = Filter.create(SupportBean.class.getName(), Expressions.in("line", 1, 8, 10));
        model.setFromClause(FromClause.create(FilterStream.create(filter, "RS").addView("time", Expressions.constant(10))));
        model.setWhereClause(Expressions.isNotNull("waverId"));
        model.setGroupByClause(GroupByClause.create("line"));
        model.setHavingClause(Expressions.lt(Expressions.avg("age"), Expressions.constant(0)));
        model.setOutputLimitClause(OutputLimitClause.create(Expressions.timePeriod(null, null, null, 10, null)));
        model.setOrderByClause(OrderByClause.create("line"));

        assertEquals("insert into ReadyStreamAvg(line, avgAge) select line, avg(age) as avgAge from " + SupportBean.class.getName() + "(line in (1,8,10))#time(10) as RS where waverId is not null group by line having avg(age)<0 output every 10 seconds order by line", model.toEPL());
        SerializableObjectCopier.copy(model);
    }

    private void runAssertionCompileToOM(EPServiceProvider epService) throws Exception {
        String stmtText = "select * from " + SupportBean.class.getName();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        SerializableObjectCopier.copy(model);
        assertNotNull(model);
    }

    private void runAssertionEPLtoOMtoStmt(EPServiceProvider epService) throws Exception {
        String stmtText = "select * from " + SupportBean.class.getName();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        SerializableObjectCopier.copy(model);

        EPStatement stmt = epService.getEPAdministrator().create(model, "s1");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object theEvent = new SupportBean();
        epService.getEPRuntime().sendEvent(theEvent);
        assertEquals(theEvent, listener.assertOneGetNewAndReset().getUnderlying());
        assertEquals(stmtText, stmt.getText());
        assertEquals("s1", stmt.getName());

        stmt.destroy();
    }

    private void runAssertionPrecedenceExpressions(EPServiceProvider epService) throws Exception {
        String[][] testdata = {
                {"1+2*3", null, "ArithmaticExpression"},
                {"1+(2*3)", "1+2*3", "ArithmaticExpression"},
                {"2-2/3-4", null, "ArithmaticExpression"},
                {"2-(2/3)-4", "2-2/3-4", "ArithmaticExpression"},
                {"1+2 in (4,5)", null, "InExpression"},
                {"(1+2) in (4,5)", "1+2 in (4,5)", "InExpression"},
                {"true and false or true", null, "Disjunction"},
                {"(true and false) or true", "true and false or true", "Disjunction"},
                {"true and (false or true)", null, "Conjunction"},
                {"true and (((false or true)))", "true and (false or true)", "Conjunction"},
                {"true and (((false or true)))", "true and (false or true)", "Conjunction"},
                {"false or false and true or false", null, "Disjunction"},
                {"false or (false and true) or false", "false or false and true or false", "Disjunction"},
                {"\"a\"||\"b\"=\"ab\"", null, "RelationalOpExpression"},
                {"(\"a\"||\"b\")=\"ab\"", "\"a\"||\"b\"=\"ab\"", "RelationalOpExpression"},
        };

        for (String[] aTestdata : testdata) {

            String epl = "select * from java.lang.Object where " + aTestdata[0];
            String expected = aTestdata[1];
            String expressionLowestPrecedenceClass = aTestdata[2];

            EPStatementObjectModel modelBefore = epService.getEPAdministrator().compileEPL(epl);
            String eplAfter = modelBefore.toEPL();

            if (expected == null) {
                assertEquals(epl, eplAfter);
            } else {
                String expectedEPL = "select * from java.lang.Object where " + expected;
                assertEquals(expectedEPL, eplAfter);
            }

            // get where clause root expression of both models
            EPStatementObjectModel modelAfter = epService.getEPAdministrator().compileEPL(eplAfter);
            assertEquals(modelAfter.getWhereClause().getClass(), modelBefore.getWhereClause().getClass());
            assertEquals(expressionLowestPrecedenceClass, modelAfter.getWhereClause().getClass().getSimpleName());
        }
    }

    private void runAssertionPrecedencePatterns(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportBean_B.class);
        epService.getEPAdministrator().getConfiguration().addEventType("C", SupportBean_C.class);
        epService.getEPAdministrator().getConfiguration().addEventType("D", SupportBean_D.class);

        String[][] testdata = {
                {"A or B and C", null, "PatternOrExpr"},
                {"(A or B) and C", null, "PatternAndExpr"},
                {"(A or B) and C", null, "PatternAndExpr"},
                {"every A or every B", null, "PatternOrExpr"},
                {"B -> D or A", null, "PatternFollowedByExpr"},
                {"every A and not B", null, "PatternAndExpr"},
                {"every A and not B", null, "PatternAndExpr"},
                {"every A -> B", null, "PatternFollowedByExpr"},
                {"A where timer:within(10)", null, "PatternGuardExpr"},
                {"every (A and B)", null, "PatternEveryExpr"},
                {"every A where timer:within(10)", null, "PatternEveryExpr"},
                {"A or B until C", null, "PatternOrExpr"},
                {"A or (B until C)", "A or B until C", "PatternOrExpr"},
                {"every (every A)", null, "PatternEveryExpr"},
                {"(A until B) until C", null, "PatternMatchUntilExpr"},
        };

        for (String[] aTestdata : testdata) {

            String epl = "select * from pattern [" + aTestdata[0] + "]";
            String expected = aTestdata[1];
            String expressionLowestPrecedenceClass = aTestdata[2];
            String failText = "Failed for [" + aTestdata[0] + "]";

            EPStatementObjectModel modelBefore = epService.getEPAdministrator().compileEPL(epl);
            String eplAfter = modelBefore.toEPL();

            if (expected == null) {
                assertEquals(failText, epl, eplAfter);
            } else {
                String expectedEPL = "select * from pattern [" + expected + "]";
                assertEquals(failText, expectedEPL, eplAfter);
            }

            // get where clause root expression of both models
            EPStatementObjectModel modelAfter = epService.getEPAdministrator().compileEPL(eplAfter);
            assertEquals(failText, getPatternRootExpr(modelAfter).getClass(), getPatternRootExpr(modelBefore).getClass());
            assertEquals(failText, expressionLowestPrecedenceClass, getPatternRootExpr(modelAfter).getClass().getSimpleName());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private PatternExpr getPatternRootExpr(EPStatementObjectModel model) {
        PatternStream patternStrema = (PatternStream) model.getFromClause().getStreams().get(0);
        return patternStrema.getExpression();
    }
}
