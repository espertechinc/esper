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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.patternassert.*;

import static org.junit.Assert.assertEquals;

public class PatternGuardTimerWithinOrMax implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("b=SupportBean_B(id='B1') where timer:withinmax(2 sec,100)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=SupportBean_B(id='B1') where timer:withinmax(2001 msec,1)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=SupportBean_B(id='B1') where timer:withinmax(1999 msec,10)");
        testCaseList.addTest(testCase);

        String text = "select * from pattern [b=SupportBean_B(id='B3') where timer:withinmax(10.001d,1)]";
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        model = SerializableObjectCopier.copyMayFail(model);
        Expression filter = Expressions.eq("id", "B3");
        PatternExpr pattern = Patterns.timerWithinMax(10.001, 1, Patterns.filter(Filter.create("SupportBean_B", filter), "b"));
        model.setFromClause(FromClause.create(PatternStream.create(pattern)));
        assertEquals(text, model.toEPL().replace("\"", "'"));
        testCase = new EventExpressionCase(model);
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=SupportBean_B) where timer:withinmax(4.001, 0)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=SupportBean_B) where timer:withinmax(4.001, 1)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(every b=SupportBean_B) where timer:withinmax(4.001, 2)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=SupportBean_B where timer:withinmax(2.001, 4)");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        // Note how every restarts the max
        testCase = new EventExpressionCase("every (b=SupportBean_B where timer:withinmax(2001 msec, 2))");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=SupportBean_B where timer:withinmax(2001 msec, 3))");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=SupportBean_B where timer:withinmax(2001 msec, 1))");
        testCase.add("B1", "b", events.getEvent("B1"));
        testCase.add("B2", "b", events.getEvent("B2"));
        testCase.add("B3", "b", events.getEvent("B3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every (b=SupportBean_B where timer:withinmax(2001 msec, 0))");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=SupportBean_B -> d=SupportBean_D where timer:withinmax(4000 msec, 1)");
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=SupportBean_B() -> every d=SupportBean_D where timer:withinmax(4000 msec, 1)");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=SupportBean_B() -> (every d=SupportBean_D) where timer:withinmax(1 day, 3)");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B1"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B2"), "d", events.getEvent("D3"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=SupportBean_B() -> (every d=SupportBean_D) where timer:withinmax(1 day, 2)");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D2", "b", events.getEvent("B1"), "d", events.getEvent("D2"));
        testCase.add("D2", "b", events.getEvent("B2"), "d", events.getEvent("D2"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("every b=SupportBean_B() -> (every d=SupportBean_D) where timer:withinmax(1 day, 1)");
        testCase.add("D1", "b", events.getEvent("B1"), "d", events.getEvent("D1"));
        testCase.add("D1", "b", events.getEvent("B2"), "d", events.getEvent("D1"));
        testCase.add("D3", "b", events.getEvent("B3"), "d", events.getEvent("D3"));
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList);
        util.runTest(env);
    }
}