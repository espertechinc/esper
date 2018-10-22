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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.patternassert.*;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class PatternComplexPropertyAccess {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternComplexProperties());
        execs.add(new PatternIndexedFilterProp());
        execs.add(new PatternIndexedValueProp());
        execs.add(new PatternIndexedValuePropOM());
        execs.add(new PatternIndexedValuePropCompile());
        return execs;
    }

    private static class PatternComplexProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventCollection events = EventCollectionFactory.getSetSixComplexProperties();
            CaseList testCaseList = new CaseList();
            EventExpressionCase testCase;

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(mapped('keyOne') = 'valueOne')");
            testCase.add("e1", "s", events.getEvent("e1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(indexed[1] = 2)");
            testCase.add("e1", "s", events.getEvent("e1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(indexed[0] = 2)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(arrayProperty[1] = 20)");
            testCase.add("e1", "s", events.getEvent("e1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(arrayProperty[1] in (10:30))");
            testCase.add("e1", "s", events.getEvent("e1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(arrayProperty[2] = 20)");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(nested.nestedValue = 'nestedValue')");
            testCase.add("e1", "s", events.getEvent("e1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(nested.nestedValue = 'dummy')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(nested.nestedNested.nestedNestedValue = 'nestedNestedValue')");
            testCase.add("e1", "s", events.getEvent("e1"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanComplexProps(nested.nestedNested.nestedNestedValue = 'x')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanCombinedProps(indexed[1].mapped('1mb').value = '1ma1')");
            testCase.add("e2", "s", events.getEvent("e2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanCombinedProps(indexed[0].mapped('1ma').value = 'x')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanCombinedProps(array[0].mapped('0ma').value = '0ma0')");
            testCase.add("e2", "s", events.getEvent("e2"));
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanCombinedProps(array[2].mapped('x').value = 'x')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanCombinedProps(array[879787].mapped('x').value = 'x')");
            testCaseList.addTest(testCase);

            testCase = new EventExpressionCase("s=SupportBeanCombinedProps(array[0].mapped('xxx').value = 'x')");
            testCaseList.addTest(testCase);

            PatternTestHarness util = new PatternTestHarness(events, testCaseList, this.getClass());
            util.runTest(env);
        }
    }

    private static class PatternIndexedFilterProp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String pattern = "@name('s0') select * from pattern[every a=SupportBeanComplexProps(indexed[0]=3)]";
            env.compileDeploy(pattern).addListener("s0");

            Object theEvent = new SupportBeanComplexProps(new int[]{3, 4});
            env.sendEventBean(theEvent);
            assertSame(theEvent, env.listener("s0").assertOneGetNewAndReset().get("a"));

            theEvent = new SupportBeanComplexProps(new int[]{6});
            env.sendEventBean(theEvent);
            assertFalse(env.listener("s0").isInvoked());

            theEvent = new SupportBeanComplexProps(new int[]{3});
            env.sendEventBean(theEvent);
            assertSame(theEvent, env.listener("s0").assertOneGetNewAndReset().get("a"));

            env.undeployAll();
        }
    }

    private static class PatternIndexedValueProp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String pattern = "@name('s0') select * from pattern[every a=SupportBeanComplexProps -> b=SupportBeanComplexProps(indexed[0] = a.indexed[0])]";
            env.compileDeploy(pattern).addListener("s0");
            runIndexedValueProp(env);
            env.undeployAll();
        }
    }

    private static class PatternIndexedValuePropOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String type = SupportBeanComplexProps.class.getSimpleName();

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard());
            PatternExpr pattern = Patterns.followedBy(Patterns.everyFilter(type, "a"),
                Patterns.filter(Filter.create(type, Expressions.eqProperty("indexed[0]", "a.indexed[0]")), "b"));
            model.setFromClause(FromClause.create(PatternStream.create(pattern)));
            model = SerializableObjectCopier.copyMayFail(model);

            String patternText = "select * from pattern [every a=" + type + " -> b=" + type + "(indexed[0]=a.indexed[0])]";
            Assert.assertEquals(patternText, model.toEPL());

            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");
            runIndexedValueProp(env);
            env.undeployAll();
        }
    }

    private static class PatternIndexedValuePropCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String patternText = "@name('s0') select * from pattern [every a=SupportBeanComplexProps -> b=SupportBeanComplexProps(indexed[0]=a.indexed[0])]";
            env.eplToModelCompileDeploy(patternText).addListener("s0");
            runIndexedValueProp(env);
            env.undeployAll();
        }
    }

    private static void runIndexedValueProp(RegressionEnvironment env) {
        Object eventOne = new SupportBeanComplexProps(new int[]{3});
        env.sendEventBean(eventOne);
        assertFalse(env.listener("s0").isInvoked());

        Object theEvent = new SupportBeanComplexProps(new int[]{6});
        env.sendEventBean(theEvent);
        assertFalse(env.listener("s0").isInvoked());

        Object eventTwo = new SupportBeanComplexProps(new int[]{3});
        env.sendEventBean(eventTwo);
        EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
        assertSame(eventOne, eventBean.get("a"));
        assertSame(eventTwo, eventBean.get("b"));
    }
}


