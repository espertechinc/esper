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
import org.junit.Assert;

public class PatternOperatorOperatorMix implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        EventCollection events = EventCollectionFactory.getEventSetOne(0, 1000);
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("(b=SupportBean_B -> d=SupportBean_D) " +
            " and " +
            "(a=SupportBean_A -> e=SupportBean_E)"
        );
        testCase.add("E1", "b", events.getEvent("B1"), "d", events.getEvent("D1"), "a", events.getEvent("A1"), "e", events.getEvent("E1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=SupportBean_B -> (d=SupportBean_D() or a=SupportBean_A)");
        testCase.add("A2", "b", events.getEvent("B1"), "a", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        PatternExpr pattern = Patterns.followedBy(
            Patterns.filter("SupportBean_B", "b"),
            Patterns.or(Patterns.filter("SupportBean_D", "d"), Patterns.filter("SupportBean_A", "a")));
        model.setFromClause(FromClause.create(PatternStream.create(pattern)));
        model = SerializableObjectCopier.copyMayFail(model);
        String text = "select * from pattern [b=SupportBean_B -> d=SupportBean_D or a=SupportBean_A]";
        Assert.assertEquals(text, model.toEPL());
        testCase = new EventExpressionCase(model);
        testCase.add("A2", "b", events.getEvent("B1"), "a", events.getEvent("A2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=SupportBean_B() -> (" +
            "(d=SupportBean_D() -> a=SupportBean_A())" +
            " or " +
            "(a=SupportBean_A() -> e=SupportBean_E()))"
        );
        testCase.add("E1", "b", events.getEvent("B1"), "a", events.getEvent("A2"), "e", events.getEvent("E1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("b=SupportBean_B() and d=SupportBean_D or a=SupportBean_A");
        testCase.add("A1", "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(b=SupportBean_B() -> d=SupportBean_D()) or a=SupportBean_A");
        testCase.add("A1", "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("(b=SupportBean_B() and " +
            "d=SupportBean_D()) or " +
            "a=SupportBean_A");
        testCase.add("A1", "a", events.getEvent("A1"));
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(env);
    }
}