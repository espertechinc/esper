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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBeanCombinedProps;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.patternassert.*;
import com.espertech.esper.util.SerializableObjectCopier;

import static org.junit.Assert.*;

public class ExecPatternComplexPropertyAccess implements RegressionExecution {
    private final static String EVENT_COMPLEX = SupportBeanComplexProps.class.getName();
    private final static String EVENT_NESTED = SupportBeanCombinedProps.class.getName();

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionComplexProperties(epService);
        runAssertionIndexedFilterProp(epService);
        runAssertionIndexedValueProp(epService);
        runAssertionIndexedValuePropOM(epService);
        runAssertionIndexedValuePropCompile(epService);
    }

    private void runAssertionComplexProperties(EPServiceProvider epService) throws Exception {
        EventCollection events = EventCollectionFactory.getSetSixComplexProperties();
        CaseList testCaseList = new CaseList();
        EventExpressionCase testCase;

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(mapped('keyOne') = 'valueOne')");
        testCase.add("e1", "s", events.getEvent("e1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(indexed[1] = 2)");
        testCase.add("e1", "s", events.getEvent("e1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(indexed[0] = 2)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(arrayProperty[1] = 20)");
        testCase.add("e1", "s", events.getEvent("e1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(arrayProperty[1] in (10:30))");
        testCase.add("e1", "s", events.getEvent("e1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(arrayProperty[2] = 20)");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(nested.nestedValue = 'nestedValue')");
        testCase.add("e1", "s", events.getEvent("e1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(nested.nestedValue = 'dummy')");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(nested.nestedNested.nestedNestedValue = 'nestedNestedValue')");
        testCase.add("e1", "s", events.getEvent("e1"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_COMPLEX + "(nested.nestedNested.nestedNestedValue = 'x')");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_NESTED + "(indexed[1].mapped('1mb').value = '1ma1')");
        testCase.add("e2", "s", events.getEvent("e2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_NESTED + "(indexed[0].mapped('1ma').value = 'x')");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_NESTED + "(array[0].mapped('0ma').value = '0ma0')");
        testCase.add("e2", "s", events.getEvent("e2"));
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_NESTED + "(array[2].mapped('x').value = 'x')");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_NESTED + "(array[879787].mapped('x').value = 'x')");
        testCaseList.addTest(testCase);

        testCase = new EventExpressionCase("s=" + EVENT_NESTED + "(array[0].mapped('xxx').value = 'x')");
        testCaseList.addTest(testCase);

        PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
        util.runTest(epService);
    }

    private void runAssertionIndexedFilterProp(EPServiceProvider epService) throws Exception {
        SupportUpdateListener testListener = new SupportUpdateListener();
        String type = SupportBeanComplexProps.class.getName();
        String pattern = "every a=" + type + "(indexed[0]=3)";

        EPStatement stmt = epService.getEPAdministrator().createPattern(pattern);
        stmt.addListener(testListener);

        Object theEvent = new SupportBeanComplexProps(new int[]{3, 4});
        epService.getEPRuntime().sendEvent(theEvent);
        assertSame(theEvent, testListener.assertOneGetNewAndReset().get("a"));

        theEvent = new SupportBeanComplexProps(new int[]{6});
        epService.getEPRuntime().sendEvent(theEvent);
        assertFalse(testListener.isInvoked());

        theEvent = new SupportBeanComplexProps(new int[]{3});
        epService.getEPRuntime().sendEvent(theEvent);
        assertSame(theEvent, testListener.assertOneGetNewAndReset().get("a"));

        stmt.destroy();
    }

    private void runAssertionIndexedValueProp(EPServiceProvider epService) throws Exception {
        String type = SupportBeanComplexProps.class.getName();
        String pattern = "every a=" + type + " -> b=" + type + "(indexed[0] = a.indexed[0])";

        EPStatement stmt = epService.getEPAdministrator().createPattern(pattern);
        runIndexedValueProp(epService, stmt);
        stmt.destroy();
    }

    private void runAssertionIndexedValuePropOM(EPServiceProvider epService) throws Exception {
        String type = SupportBeanComplexProps.class.getName();

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.createWildcard());
        PatternExpr pattern = Patterns.followedBy(Patterns.everyFilter(type, "a"),
                Patterns.filter(Filter.create(type, Expressions.eqProperty("indexed[0]", "a.indexed[0]")), "b"));
        model.setFromClause(FromClause.create(PatternStream.create(pattern)));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        String patternText = "select * from pattern [every a=" + type + " -> b=" + type + "(indexed[0]=a.indexed[0])]";
        assertEquals(patternText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        runIndexedValueProp(epService, stmt);
        stmt.destroy();
    }

    private void runAssertionIndexedValuePropCompile(EPServiceProvider epService) throws Exception {
        String type = SupportBeanComplexProps.class.getName();

        String patternText = "select * from pattern [every a=" + type + " -> b=" + type + "(indexed[0]=a.indexed[0])]";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(patternText);
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(patternText, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().create(model);
        runIndexedValueProp(epService, stmt);
        stmt.destroy();
    }

    private void runIndexedValueProp(EPServiceProvider epService, EPStatement stmt) {
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        Object eventOne = new SupportBeanComplexProps(new int[]{3});
        epService.getEPRuntime().sendEvent(eventOne);
        assertFalse(testListener.isInvoked());

        Object theEvent = new SupportBeanComplexProps(new int[]{6});
        epService.getEPRuntime().sendEvent(theEvent);
        assertFalse(testListener.isInvoked());

        Object eventTwo = new SupportBeanComplexProps(new int[]{3});
        epService.getEPRuntime().sendEvent(eventTwo);
        EventBean eventBean = testListener.assertOneGetNewAndReset();
        assertSame(eventOne, eventBean.get("a"));
        assertSame(eventTwo, eventBean.get("b"));
    }
}


