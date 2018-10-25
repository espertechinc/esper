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
package com.espertech.esper.regressionlib.suite.epl.fromclausemethod;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.runtime.client.scopetest.SupportListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

public class EPLFromClauseMethod {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLFromClauseMethod2JoinHistoricalIndependentOuter());
        execs.add(new EPLFromClauseMethod2JoinHistoricalSubordinateOuterMultiField());
        execs.add(new EPLFromClauseMethod2JoinHistoricalSubordinateOuter());
        execs.add(new EPLFromClauseMethod2JoinHistoricalOnlyDependent());
        execs.add(new EPLFromClauseMethod2JoinHistoricalOnlyIndependent());
        execs.add(new EPLFromClauseMethodNoJoinIterateVariables());
        execs.add(new EPLFromClauseMethodOverloaded());
        execs.add(new EPLFromClauseMethod2StreamMaxAggregation());
        execs.add(new EPLFromClauseMethodDifferentReturnTypes());
        execs.add(new EPLFromClauseMethodArrayNoArg());
        execs.add(new EPLFromClauseMethodArrayWithArg());
        execs.add(new EPLFromClauseMethodObjectNoArg());
        execs.add(new EPLFromClauseMethodObjectWithArg());
        execs.add(new EPLFromClauseMethodInvocationTargetEx());
        execs.add(new EPLFromClauseMethodStreamNameWContext());
        execs.add(new EPLFromClauseMethodWithMethodResultParam());
        execs.add(new EPLFromClauseMethodInvalid());
        execs.add(new EPLFromClauseMethodEventBeanArray());
        execs.add(new EPLFromClauseMethodUDFAndScriptReturningEvents());
        return execs;
    }

    private static class EPLFromClauseMethodWithMethodResultParam implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean as e,\n" +
                "method:" + EPLFromClauseMethod.class.getName() + ".getWithMethodResultParam('somevalue', e, "
                + EPLFromClauseMethod.class.getName() + ".getWithMethodResultParamCompute(true)) as s";
            env.compileDeploy(epl).addListener("s0");
            assertStatelessStmt(env, "s0", false);

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "s.p00,s.p01,s.p02".split(","), new Object[]{"somevalue", "E1", "s0"});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodStreamNameWContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportBean as e,\n" +
                "method:" + EPLFromClauseMethod.class.getName() + ".getStreamNameWContext('somevalue', e) as s";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "s.p00,s.p01,s.p02".split(","), new Object[]{"somevalue", "E1", "s0"});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodUDFAndScriptReturningEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType("create schema ItemEvent(id string)", path);

            String script = "@name('script') create expression EventBean[] @type(ItemEvent) js:myItemProducerScript() [\n" +
                "myItemProducerScript();" +
                "function myItemProducerScript() {" +
                "  var EventBeanArray = Java.type(\"com.espertech.esper.common.client.EventBean[]\");\n" +
                "  var events = new EventBeanArray(2);\n" +
                "  events[0] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id1\"), \"ItemEvent\");\n" +
                "  events[1] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id3\"), \"ItemEvent\");\n" +
                "  return events;\n" +
                "}]";
            env.compileDeploy(script, path);

            tryAssertionUDFAndScriptReturningEvents(env, path, "myItemProducerUDF");
            tryAssertionUDFAndScriptReturningEvents(env, path, "myItemProducerScript");

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodEventBeanArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeployWBusPublicType("create schema MyItemEvent(p0 string)", path);

            tryAssertionEventBeanArray(env, path, "eventBeanArrayForString", false);
            tryAssertionEventBeanArray(env, path, "eventBeanArrayForString", true);
            tryAssertionEventBeanArray(env, path, "eventBeanCollectionForString", false);
            tryAssertionEventBeanArray(env, path, "eventBeanIteratorForString", false);

            tryInvalidCompile(env, path, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".fetchResult12(0) @type(ItemEvent)",
                "The @type annotation is only allowed when the invocation target returns EventBean instances");

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodOverloaded implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionOverloaded(env, "", "A", "B");
            tryAssertionOverloaded(env, "10", "10", "B");
            tryAssertionOverloaded(env, "10, 20", "10", "20");
            tryAssertionOverloaded(env, "'x'", "x", "B");
            tryAssertionOverloaded(env, "'x', 50", "x", "50");
        }

        private static void tryAssertionOverloaded(RegressionEnvironment env, String params, String expectedFirst, String expectedSecond) {
            String epl = "@name('s0') select col1, col2 from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".overloadedMethodForJoin(" + params + ")";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean());
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "col1,col2".split(","), new Object[]{expectedFirst, expectedSecond});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod2StreamMaxAggregation implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String className = SupportStaticMethodLib.class.getName();
            String stmtText;
            String[] fields = "maxcol1".split(",");

            // ESPER 556
            stmtText = "@name('s0') select max(col1) as maxcol1 from SupportBean#unique(theString), method:" + className + ".fetchResult100() ";
            env.compileDeploy(stmtText).addListener("s0");
            assertStatelessStmt(env, "s0", false);

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{9}});

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{9}});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod2JoinHistoricalSubordinateOuterMultiField implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String className = SupportStaticMethodLib.class.getName();
            String stmtText;

            // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
            stmtText = "@name('s0') select intPrimitive,intBoxed,col1,col2 from SupportBean#keepall " +
                "left outer join " +
                "method:" + className + ".fetchResult100() " +
                "on intPrimitive = col1 and intBoxed = col2";

            String[] fields = "intPrimitive,intBoxed,col1,col2".split(",");
            env.compileDeploy(stmtText).addListener("s0");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

            sendSupportBeanEvent(env, 2, 4);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{2, 4, 2, 4}});
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{2, 4, 2, 4}});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod2JoinHistoricalSubordinateOuter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String className = SupportStaticMethodLib.class.getName();
            String stmtText;

            // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
            stmtText = "select s0.value as valueOne, s1.value as valueTwo from method:" + className + ".fetchResult12(0) as s0 " +
                "left outer join " +
                "method:" + className + ".fetchResult23(s0.value) as s1 on s0.value = s1.value";
            assertJoinHistoricalSubordinateOuter(env, stmtText);

            stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult23(s0.value) as s1 " +
                "right outer join " +
                "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
            assertJoinHistoricalSubordinateOuter(env, stmtText);

            stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult23(s0.value) as s1 " +
                "full outer join " +
                "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
            assertJoinHistoricalSubordinateOuter(env, stmtText);

            stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult12(0) as s0 " +
                "full outer join " +
                "method:" + className + ".fetchResult23(s0.value) as s1 on s0.value = s1.value";
            assertJoinHistoricalSubordinateOuter(env, stmtText);
        }
    }

    private static class EPLFromClauseMethod2JoinHistoricalIndependentOuter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "valueOne,valueTwo".split(",");
            String className = SupportStaticMethodLib.class.getName();
            String stmtText;

            stmtText = "@name('s0') select s0.value as valueOne, s1.value as valueTwo from method:" + className + ".fetchResult12(0) as s0 " +
                "left outer join " +
                "method:" + className + ".fetchResult23(0) as s1 on s0.value = s1.value";
            env.compileDeploy(stmtText).addListener("s0");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{1, null}, {2, 2}});
            env.undeployAll();

            stmtText = "@name('s0') select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult23(0) as s1 " +
                "right outer join " +
                "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
            env.compileDeploy(stmtText);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{1, null}, {2, 2}});
            env.undeployAll();

            stmtText = "@name('s0') select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult23(0) as s1 " +
                "full outer join " +
                "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
            env.compileDeploy(stmtText);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{1, null}, {2, 2}, {null, 3}});
            env.undeployAll();

            stmtText = "@name('s0') select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult12(0) as s0 " +
                "full outer join " +
                "method:" + className + ".fetchResult23(0) as s1 on s0.value = s1.value";
            env.compileDeploy(stmtText);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{1, null}, {2, 2}, {null, 3}});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod2JoinHistoricalOnlyDependent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable int lower", path);
            env.compileDeploy("create variable int upper", path);
            env.compileDeploy("on SupportBean set lower=intPrimitive,upper=intBoxed", path);

            String className = SupportStaticMethodLib.class.getName();
            String stmtText;

            // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
            stmtText = "select value,result from method:" + className + ".fetchBetween(lower, upper), " +
                "method:" + className + ".fetchIdDelimited(value)";
            assertJoinHistoricalOnlyDependent(env, path, stmtText);

            stmtText = "select value,result from " +
                "method:" + className + ".fetchIdDelimited(value), " +
                "method:" + className + ".fetchBetween(lower, upper)";
            assertJoinHistoricalOnlyDependent(env, path, stmtText);

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethod2JoinHistoricalOnlyIndependent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable int lower", path);
            env.compileDeploy("create variable int upper", path);
            env.compileDeploy("on SupportBean set lower=intPrimitive,upper=intBoxed", path);

            String className = SupportStaticMethodLib.class.getName();
            String stmtText;

            // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
            stmtText = "select s0.value as valueOne, s1.value as valueTwo from method:" + className + ".fetchBetween(lower, upper) as s0, " +
                "method:" + className + ".fetchBetweenString(lower, upper) as s1";
            assertJoinHistoricalOnlyIndependent(env, path, stmtText);

            stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchBetweenString(lower, upper) as s1, " +
                "method:" + className + ".fetchBetween(lower, upper) as s0 ";
            assertJoinHistoricalOnlyIndependent(env, path, stmtText);

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodNoJoinIterateVariables implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create variable int lower", path);
            env.compileDeploy("create variable int upper", path);
            env.compileDeploy("on SupportBean set lower=intPrimitive,upper=intBoxed", path);

            // Test int and singlerow
            String className = SupportStaticMethodLib.class.getName();
            String stmtText = "@name('s0') select value from method:" + className + ".fetchBetween(lower, upper)";
            env.compileDeploy(stmtText, path).addListener("s0");

            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), new String[]{"value"}, null);

            sendSupportBeanEvent(env, 5, 10);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), new String[]{"value"}, new Object[][]{{5}, {6}, {7}, {8}, {9}, {10}});

            env.milestone(0);

            sendSupportBeanEvent(env, 10, 5);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), new String[]{"value"}, null);

            sendSupportBeanEvent(env, 4, 4);
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), new String[]{"value"}, new Object[][]{{4}});

            assertFalse(env.listener("s0").isInvoked());
            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodDifferentReturnTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionSingleRowFetch(env, "fetchMap(theString, intPrimitive)");
            tryAssertionSingleRowFetch(env, "fetchMapEventBean(s1, 'theString', 'intPrimitive')");
            tryAssertionSingleRowFetch(env, "fetchObjectArrayEventBean(theString, intPrimitive)");
            tryAssertionSingleRowFetch(env, "fetchPOJOArray(theString, intPrimitive)");
            tryAssertionSingleRowFetch(env, "fetchPOJOCollection(theString, intPrimitive)");
            tryAssertionSingleRowFetch(env, "fetchPOJOIterator(theString, intPrimitive)");

            tryAssertionReturnTypeMultipleRow(env, "fetchMapArrayMR(theString, intPrimitive)");
            tryAssertionReturnTypeMultipleRow(env, "fetchOAArrayMR(theString, intPrimitive)");
            tryAssertionReturnTypeMultipleRow(env, "fetchPOJOArrayMR(theString, intPrimitive)");
            tryAssertionReturnTypeMultipleRow(env, "fetchMapCollectionMR(theString, intPrimitive)");
            tryAssertionReturnTypeMultipleRow(env, "fetchOACollectionMR(theString, intPrimitive)");
            tryAssertionReturnTypeMultipleRow(env, "fetchPOJOCollectionMR(theString, intPrimitive)");
            tryAssertionReturnTypeMultipleRow(env, "fetchMapIteratorMR(theString, intPrimitive)");
            tryAssertionReturnTypeMultipleRow(env, "fetchOAIteratorMR(theString, intPrimitive)");
            tryAssertionReturnTypeMultipleRow(env, "fetchPOJOIteratorMR(theString, intPrimitive)");
        }
    }

    private static class EPLFromClauseMethodArrayNoArg implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select id, theString from " +
                "SupportBean#length(3) as s1, " +
                "method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayNoArg";
            env.compileDeploy(joinStatement).addListener("s0");
            tryArrayNoArg(env);

            joinStatement = "@name('s0') select id, theString from " +
                "SupportBean#length(3) as s1, " +
                "method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayNoArg()";
            env.compileDeploy(joinStatement).addListener("s0");
            tryArrayNoArg(env);

            env.eplToModelCompileDeploy(joinStatement).addListener("s0");
            tryArrayNoArg(env);

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("id", "theString"));
            model.setFromClause(FromClause.create()
                .add(FilterStream.create("SupportBean", "s1").addView("length", Expressions.constant(3)))
                .add(MethodInvocationStream.create(SupportStaticMethodLib.class.getName(), "fetchArrayNoArg")));
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");
            assertEquals(joinStatement, model.toEPL());

            tryArrayNoArg(env);
        }

        private static void tryArrayNoArg(RegressionEnvironment env) {
            String[] fields = new String[]{"id", "theString"};

            sendBeanEvent(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"1", "E1"});

            sendBeanEvent(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"1", "E2"});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodArrayWithArg implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select irstream id, theString from " +
                "SupportBean()#length(3) as s1, " +
                " method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayGen(intPrimitive)";
            env.compileDeploy(joinStatement).addListener("s0");
            tryArrayWithArg(env);

            joinStatement = "@name('s0') select irstream id, theString from " +
                "method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayGen(intPrimitive) as s0, " +
                "SupportBean#length(3)";
            env.compileDeploy(joinStatement).addListener("s0");
            tryArrayWithArg(env);

            env.eplToModelCompileDeploy(joinStatement).addListener("s0");
            tryArrayWithArg(env);

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create("id", "theString").streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH));
            model.setFromClause(FromClause.create()
                .add(MethodInvocationStream.create(SupportStaticMethodLib.class.getName(), "fetchArrayGen", "s0")
                    .addParameter(Expressions.property("intPrimitive")))
                .add(FilterStream.create("SupportBean").addView("length", Expressions.constant(3)))
            );
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");
            assertEquals(joinStatement, model.toEPL());

            tryArrayWithArg(env);
        }

        private static void tryArrayWithArg(RegressionEnvironment env) {

            String[] fields = new String[]{"id", "theString"};

            sendBeanEvent(env, "E1", -1);
            assertFalse(env.listener("s0").isInvoked());

            sendBeanEvent(env, "E2", 0);
            assertFalse(env.listener("s0").isInvoked());

            sendBeanEvent(env, "E3", 1);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"A", "E3"});

            sendBeanEvent(env, "E4", 2);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"A", "E4"}, {"B", "E4"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendBeanEvent(env, "E5", 3);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"A", "E5"}, {"B", "E5"}, {"C", "E5"}});
            assertNull(env.listener("s0").getLastOldData());
            env.listener("s0").reset();

            sendBeanEvent(env, "E6", 1);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"A", "E6"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields, new Object[][]{{"A", "E3"}});
            env.listener("s0").reset();

            sendBeanEvent(env, "E7", 1);
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"A", "E7"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastOldData(), fields, new Object[][]{{"A", "E4"}, {"B", "E4"}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodObjectNoArg implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select id, theString from " +
                "SupportBean()#length(3) as s1, " +
                " method:" + SupportStaticMethodLib.class.getName() + ".fetchObjectNoArg()";
            env.compileDeploy(joinStatement).addListener("s0");
            String[] fields = new String[]{"id", "theString"};

            sendBeanEvent(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"2", "E1"});

            env.milestone(0);

            sendBeanEvent(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"2", "E2"});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodObjectWithArg implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "@name('s0') select id, theString from " +
                "SupportBean()#length(3) as s1, " +
                " method:" + SupportStaticMethodLib.class.getName() + ".fetchObject(theString)";
            env.compileDeploy(joinStatement).addListener("s0");

            String[] fields = new String[]{"id", "theString"};

            sendBeanEvent(env, "E1");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"|E1|", "E1"});

            sendBeanEvent(env, null);
            assertFalse(env.listener("s0").isInvoked());

            sendBeanEvent(env, "E2");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"|E2|", "E2"});

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodInvocationTargetEx implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String joinStatement = "select s1.theString from " +
                "SupportBean()#length(3) as s1, " +
                " method:" + SupportStaticMethodLib.class.getName() + ".throwExceptionBeanReturn()";

            env.compileDeploy(joinStatement);

            try {
                sendBeanEvent(env, "E1");
                fail(); // default test configuration rethrows this exception
            } catch (EPException ex) {
                // fine
            }

            env.undeployAll();
        }
    }

    private static class EPLFromClauseMethodInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayGen()",
                "Method footprint does not match the number or type of expression parameters, expecting no parameters in method: Could not find static method named 'fetchArrayGen' in class '" + SupportStaticMethodLib.class.getName() + "' taking no parameters (nearest match found was 'fetchArrayGen' taking type(s) 'int') [");

            tryInvalidCompile(env, "select * from SupportBean, method:.abc where 1=2",
                "Incorrect syntax near '.' at line 1 column 34, please check the method invocation join within the from clause [select * from SupportBean, method:.abc where 1=2]");

            tryInvalidCompile(env, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".fetchObjectAndSleep(1)",
                "Method footprint does not match the number or type of expression parameters, expecting a method where parameters are typed 'int': Could not find static method named 'fetchObjectAndSleep' in class '" + SupportStaticMethodLib.class.getName() + "' with matching parameter number and expected parameter type(s) 'int' (nearest match found was 'fetchObjectAndSleep' taking type(s) 'String, int, long') [");

            tryInvalidCompile(env, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".sleep(100) where 1=2",
                "Invalid return type for static method 'sleep' of class '" + SupportStaticMethodLib.class.getName() + "', expecting a Java class [select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".sleep(100) where 1=2]");

            tryInvalidCompile(env, "select * from SupportBean, method:AClass. where 1=2",
                "Incorrect syntax near 'where' (a reserved keyword) expecting an identifier but found 'where' at line 1 column 42, please check the view specifications within the from clause [select * from SupportBean, method:AClass. where 1=2]");

            tryInvalidCompile(env, "select * from SupportBean, method:Dummy.abc where 1=2",
                "Could not load class by name 'Dummy', please check imports [select * from SupportBean, method:Dummy.abc where 1=2]");

            tryInvalidCompile(env, "select * from SupportBean, method:Math where 1=2",
                "A function named 'Math' is not defined");

            tryInvalidCompile(env, "select * from SupportBean, method:Dummy.dummy()#length(100) where 1=2",
                "Method data joins do not allow views onto the data, view 'length' is not valid in this context [select * from SupportBean, method:Dummy.dummy()#length(100) where 1=2]");

            tryInvalidCompile(env, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".dummy where 1=2",
                "Could not find public static method named 'dummy' in class '" + SupportStaticMethodLib.class.getName() + "' [");

            tryInvalidCompile(env, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".minusOne(10) where 1=2",
                "Invalid return type for static method 'minusOne' of class '" + SupportStaticMethodLib.class.getName() + "', expecting a Java class [");

            tryInvalidCompile(env, "select * from SupportBean, xyz:" + SupportStaticMethodLib.class.getName() + ".fetchArrayNoArg() where 1=2",
                "Expecting keyword 'method', found 'xyz' [select * from SupportBean, xyz:" + SupportStaticMethodLib.class.getName() + ".fetchArrayNoArg() where 1=2]");

            tryInvalidCompile(env, "select * from method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s1.value, s1.value) as s0, method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s0.value, s0.value) as s1",
                "Circular dependency detected between historical streams [");

            tryInvalidCompile(env, "select * from method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s0.value, s0.value) as s0, method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s0.value, s0.value) as s1",
                "Parameters for historical stream 0 indicate that the stream is subordinate to itself as stream parameters originate in the same stream [");

            tryInvalidCompile(env, "select * from method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s0.value, s0.value) as s0",
                "Parameters for historical stream 0 indicate that the stream is subordinate to itself as stream parameters originate in the same stream [");

            tryInvalidCompile(env, "select * from method:SupportMethodInvocationJoinInvalid.readRowNoMetadata()",
                "Could not find getter method for method invocation, expected a method by name 'readRowNoMetadataMetadata' accepting no parameters [select * from method:SupportMethodInvocationJoinInvalid.readRowNoMetadata()]");

            tryInvalidCompile(env, "select * from method:SupportMethodInvocationJoinInvalid.readRowWrongMetadata()",
                "Getter method 'readRowWrongMetadataMetadata' does not return java.util.Map [select * from method:SupportMethodInvocationJoinInvalid.readRowWrongMetadata()]");

            tryInvalidCompile(env, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".invalidOverloadForJoin(null)",
                "Method by name 'invalidOverloadForJoin' is overloaded in class '" + SupportStaticMethodLib.class.getName() + "' and overloaded methods do not return the same type");
        }
    }

    private static void tryAssertionUDFAndScriptReturningEvents(RegressionEnvironment env, RegressionPath path, String methodName) {
        env.compileDeploy("@name('s0') select id from SupportBean, method:" + methodName, path).addListener("s0");

        env.sendEventBean(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "id".split(","), new Object[][]{{"id1"}, {"id3"}});

        env.undeployModuleContaining("s0");
    }

    private static void tryAssertionEventBeanArray(RegressionEnvironment env, RegressionPath path, String methodName, boolean soda) {
        String epl = "@name('s0') select p0 from SupportBean, method:" + SupportStaticMethodLib.class.getName() + "." + methodName + "(theString) @type(MyItemEvent)";
        env.compileDeploy(soda, epl, path).addListener("s0");

        env.sendEventBean(new SupportBean("a,b", 0));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), "p0".split(","), new Object[][]{{"a"}, {"b"}});

        env.undeployModuleContaining("s0");
    }

    private static void sendBeanEvent(RegressionEnvironment env, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        env.sendEventBean(bean);
    }

    private static void sendBeanEvent(RegressionEnvironment env, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        env.sendEventBean(bean);
    }

    public static EventBean[] myItemProducerUDF(EPLMethodInvocationContext context) {
        EventBean[] events = new EventBean[2];
        int count = 0;
        for (String id : "id1,id3".split(",")) {
            events[count++] = context.getEventBeanService().adapterForMap(Collections.singletonMap("id", id), "ItemEvent");
        }
        return events;
    }

    private static void assertJoinHistoricalSubordinateOuter(RegressionEnvironment env, String expression) {
        String[] fields = "valueOne,valueTwo".split(",");
        env.compileDeploy("@name('s0') " + expression).addListener("s0");
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{1, null}, {2, 2}});
        env.undeployAll();
    }

    private static void assertJoinHistoricalOnlyDependent(RegressionEnvironment env, RegressionPath path, String expression) {
        env.compileDeploy("@name('s0') " + expression, path).addListener("s0");

        String[] fields = "value,result".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

        sendSupportBeanEvent(env, 5, 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{5, "|5|"}});

        sendSupportBeanEvent(env, 1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{1, "|1|"}, {2, "|2|"}});

        sendSupportBeanEvent(env, 0, -1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

        sendSupportBeanEvent(env, 4, 6);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{4, "|4|"}, {5, "|5|"}, {6, "|6|"}});

        SupportListener listener = env.listener("s0");
        env.undeployModuleContaining("s0");

        sendSupportBeanEvent(env, 0, -1);
        assertFalse(listener.isInvoked());
    }

    private static void assertJoinHistoricalOnlyIndependent(RegressionEnvironment env, RegressionPath path, String expression) {
        env.compileDeploy("@name('s0') " + expression, path).addListener("s0");

        String[] fields = "valueOne,valueTwo".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

        sendSupportBeanEvent(env, 5, 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{5, "5"}});

        sendSupportBeanEvent(env, 1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, new Object[][]{{1, "1"}, {1, "2"}, {2, "1"}, {2, "2"}});

        sendSupportBeanEvent(env, 0, -1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("s0"), fields, null);

        SupportListener listener = env.listener("s0");
        env.undeployModuleContaining("s0");

        sendSupportBeanEvent(env, 0, -1);
        assertFalse(listener.isInvoked());
    }

    private static void tryAssertionSingleRowFetch(RegressionEnvironment env, String method) {
        String epl = "@name('s0') select theString, intPrimitive, mapstring, mapint from " +
            "SupportBean as s1, " +
            "method:" + SupportStaticMethodLib.class.getName() + "." + method;
        env.compileDeploy(epl).addListener("s0");

        String[] fields = new String[]{"theString", "intPrimitive", "mapstring", "mapint"};

        sendBeanEvent(env, "E1", 1);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", 1, "|E1|", 2});

        sendBeanEvent(env, "E2", 3);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", 3, "|E2|", 4});

        sendBeanEvent(env, "E3", 0);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 0, null, null});

        sendBeanEvent(env, "E4", -1);
        assertFalse(env.listener("s0").isInvoked());

        env.undeployAll();
    }

    private static void tryAssertionReturnTypeMultipleRow(RegressionEnvironment env, String method) {
        String epl = "@name('s0') select theString, intPrimitive, mapstring, mapint from " +
            "SupportBean#keepall as s1, " +
            "method:" + SupportStaticMethodLib.class.getName() + "." + method;
        String[] fields = "theString,intPrimitive,mapstring,mapint".split(",");
        env.compileDeploy(epl).addListener("s0");

        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

        sendBeanEvent(env, "E1", 0);
        assertFalse(env.listener("s0").isInvoked());
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

        sendBeanEvent(env, "E2", -1);
        assertFalse(env.listener("s0").isInvoked());
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, null);

        sendBeanEvent(env, "E3", 1);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E3", 1, "|E3_0|", 100});
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E3", 1, "|E3_0|", 100}});

        sendBeanEvent(env, "E4", 2);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"E4", 2, "|E4_0|", 100}, {"E4", 2, "|E4_1|", 101}});
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E3", 1, "|E3_0|", 100}, {"E4", 2, "|E4_0|", 100}, {"E4", 2, "|E4_1|", 101}});

        sendBeanEvent(env, "E5", 3);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields,
            new Object[][]{{"E5", 3, "|E5_0|", 100}, {"E5", 3, "|E5_1|", 101}, {"E5", 3, "|E5_2|", 102}});
        EPAssertionUtil.assertPropsPerRow(env.iterator("s0"), fields, new Object[][]{{"E3", 1, "|E3_0|", 100},
            {"E4", 2, "|E4_0|", 100}, {"E4", 2, "|E4_1|", 101},
            {"E5", 3, "|E5_0|", 100}, {"E5", 3, "|E5_1|", 101}, {"E5", 3, "|E5_2|", 102}});

        env.listener("s0").reset();
        env.undeployAll();
    }

    public static SupportBean_S0 getStreamNameWContext(String a, SupportBean bean, EPLMethodInvocationContext context) {
        return new SupportBean_S0(1, a, bean.getTheString(), context.getStatementName());
    }

    public static SupportBean_S0 getWithMethodResultParam(String a, SupportBean bean, String b) {
        return new SupportBean_S0(1, a, bean.getTheString(), b);
    }

    public static String getWithMethodResultParamCompute(boolean param, EPLMethodInvocationContext context) {
        return context.getStatementName();
    }
}
