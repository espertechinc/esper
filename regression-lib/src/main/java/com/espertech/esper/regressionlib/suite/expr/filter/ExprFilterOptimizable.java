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
package com.espertech.esper.regressionlib.suite.expr.filter;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportInKeywordBean;
import com.espertech.esper.regressionlib.support.bean.SupportOverrideBase;
import com.espertech.esper.regressionlib.support.bean.SupportOverrideOne;
import com.espertech.esper.regressionlib.support.filter.SupportFilterServiceHelper;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterOption;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerIndexPlanner.PROPERTY_NAME_BOOLEAN_EXPRESSION;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.filter.SupportFilterOptimizableHelper.hasFilterIndexPlanBasicOrMore;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class ExprFilterOptimizable {
    private static EPLMethodInvocationContext methodInvocationContextFilterOptimized;

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterInAndNotInKeywordMultivalue());
        executions.add(new ExprFilterOptimizableMethodInvocationContext());
        executions.add(new ExprFilterOptimizableTypeOf());
        executions.add(new ExprFilterOptimizableVariableAndSeparateThread());
        executions.add(new ExprFilterOptimizableInspectFilter());
        executions.add(new ExprFilterOrToInRewrite());
        executions.add(new ExprFilterOrContext());
        executions.add(new ExprFilterPatternUDFFilterOptimizable());
        executions.add(new ExprFilterDeployTimeConstant());  // substitution and variables are here
        return executions;
    }

    public static class ExprFilterOrContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('ctx') create context MyContext initiated by SupportBean terminated after 24 hours;\n" +
                "@name('select') context MyContext select * from SupportBean(theString='A' or intPrimitive=1)";
            env.compileDeployAddListenerMileZero(epl, "select");

            env.sendEventBean(new SupportBean("A", 1), SupportBean.class.getSimpleName());
            env.listener("select").assertOneGetNewAndReset();

            env.undeployAll();
        }
    }

    private static class ExprFilterInAndNotInKeywordMultivalue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            tryInKeyword(env, "ints", new SupportInKeywordBean(new int[]{1, 2}), milestone);
            tryInKeyword(env, "mapOfIntKey", new SupportInKeywordBean(CollectionUtil.twoEntryMap(1, "x", 2, "y")), milestone);
            tryInKeyword(env, "collOfInt", new SupportInKeywordBean(Arrays.asList(1, 2)), milestone);

            tryNotInKeyword(env, "ints", new SupportInKeywordBean(new int[]{1, 2}), milestone);
            tryNotInKeyword(env, "mapOfIntKey", new SupportInKeywordBean(CollectionUtil.twoEntryMap(1, "x", 2, "y")), milestone);
            tryNotInKeyword(env, "collOfInt", new SupportInKeywordBean(Arrays.asList(1, 2)), milestone);

            tryInArrayContextProvided(env, milestone);

            if (hasFilterIndexPlanBasicOrMore(env)) {
                tryInvalidCompile(env, "select * from pattern[every a=SupportInKeywordBean -> SupportBean(intPrimitive in (a.longs))]",
                    "Implicit conversion from datatype 'long' to 'Integer' for property 'intPrimitive' is not allowed (strict filter type coercion)");
            }
        }
    }

    private static class ExprFilterOptimizableInspectFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;
            AtomicInteger milestone = new AtomicInteger();
            RegressionPath path = new RegressionPath();

            epl = "select * from SupportBean(funcOne(theString) = 0)";
            assertFilterDeploySingle(env, path, epl, PROPERTY_NAME_BOOLEAN_EXPRESSION, FilterOperator.BOOLEAN_EXPRESSION, milestone);

            epl = "select * from SupportBean(funcOneWDefault(theString) = 0)";
            assertFilterDeploySingle(env, path, epl, "funcOneWDefault(theString)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(funcTwo(theString) = 0)";
            assertFilterDeploySingle(env, path, epl, "funcTwo(theString)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(libE1True(theString))";
            assertFilterDeploySingle(env, path, epl, "libE1True(theString)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(funcTwo( theString ) > 10)";
            assertFilterDeploySingle(env, path, epl, "funcTwo(theString)", FilterOperator.GREATER, milestone);

            epl = "select * from SupportBean(libE1True(theString))";
            assertFilterDeploySingle(env, path, epl, "libE1True(theString)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(typeof(e) = 'SupportBean') as e";
            assertFilterDeploySingle(env, path, epl, "typeof(e)", FilterOperator.EQUAL, milestone);

            env.compileDeploy("@name('create-expr') create expression thesplit {theString => funcOne(theString)}", path).addListener("create-expr");
            epl = "select * from SupportBean(thesplit(*) = 0)";
            assertFilterDeploySingle(env, path, epl, "thesplit(*)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(thesplit(*) > 10)";
            assertFilterDeploySingle(env, path, epl, "thesplit(*)", FilterOperator.GREATER, milestone);

            epl = "expression housenumber alias for {10} select * from SupportBean(intPrimitive = housenumber)";
            assertFilterDeploySingle(env, path, epl, "intPrimitive", FilterOperator.EQUAL, milestone);

            epl = "expression housenumber alias for {intPrimitive*10} select * from SupportBean(intPrimitive = housenumber)";
            assertFilterDeploySingle(env, path, epl, ".boolean_expression", FilterOperator.BOOLEAN_EXPRESSION, milestone);

            env.undeployAll();
        }
    }

    private static class ExprFilterPatternUDFFilterOptimizable implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('s0') select * from pattern[a=SupportBean() -> b=SupportBean(myCustomBigDecimalEquals(a.bigDecimal, b.bigDecimal))]";
            env.compileDeploy(epl).addListener("s0");

            SupportBean beanOne = new SupportBean("E1", 0);
            beanOne.setBigDecimal(BigDecimal.valueOf(13));
            env.sendEventBean(beanOne);

            SupportBean beanTwo = new SupportBean("E2", 0);
            beanTwo.setBigDecimal(BigDecimal.valueOf(13));
            env.sendEventBean(beanTwo);

            env.assertListenerInvoked("s0");

            env.undeployAll();
        }
    }

    private static class ExprFilterOrToInRewrite implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            // test 'or' rewrite
            String[] filtersAB = new String[]{
                "theString = 'a' or theString = 'b'",
                "theString = 'a' or 'b' = theString",
                "'a' = theString or 'b' = theString",
                "'a' = theString or theString = 'b'",
            };
            for (String filter : filtersAB) {
                String epl = "@name('s0') select * from SupportBean(" + filter + ")";
                env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
                if (hasFilterIndexPlanBasicOrMore(env)) {
                    SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "theString", FilterOperator.IN_LIST_OF_VALUES);
                }

                env.sendEventBean(new SupportBean("a", 0));
                assertTrue(env.listener("s0").getAndClearIsInvoked());
                env.sendEventBean(new SupportBean("b", 0));
                assertTrue(env.listener("s0").getAndClearIsInvoked());
                env.sendEventBean(new SupportBean("c", 0));
                assertFalse(env.listener("s0").getAndClearIsInvoked());

                env.undeployAll();
            }

            String epl = "@name('s0') select * from SupportBean(intPrimitive = 1 and (theString='a' or theString='b'))";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            if (hasFilterIndexPlanBasicOrMore(env)) {
                SupportFilterServiceHelper.assertFilterSvcTwo(env.statement("s0"), "intPrimitive", FilterOperator.EQUAL, "theString", FilterOperator.IN_LIST_OF_VALUES);
            }
            env.undeployAll();
        }
    }

    private static class ExprFilterDeployTimeConstant implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionEqualsWSubs(env, "select * from SupportBean(theString=?:p0:string)");
            runAssertionEqualsWSubs(env, "select * from SupportBean(?:p0:string=theString)");
            runAssertionEqualsWVariable(env, "select * from SupportBean(theString=var_optimizable_equals)");
            runAssertionEqualsWVariable(env, "select * from SupportBean(var_optimizable_equals=theString)");
            runAssertionEqualsWSubsWCoercion(env, "select * from SupportBean(longPrimitive=?:p0:int)");
            runAssertionEqualsWSubsWCoercion(env, "select * from SupportBean(?:p0:int=longPrimitive)");

            if (hasFilterIndexPlanBasicOrMore(env)) {
                tryInvalidCompile(env, "select * from SupportBean(intPrimitive=?:p0:long)",
                    "Implicit conversion from datatype 'Long' to 'Integer' for property 'intPrimitive' is not allowed");
            }

            runAssertionRelOpWSubs(env, "select * from SupportBean(intPrimitive>?:p0:int)");
            runAssertionRelOpWSubs(env, "select * from SupportBean(?:p0:int<intPrimitive)");
            runAssertionRelOpWVariable(env, "select * from SupportBean(intPrimitive>var_optimizable_relop)");
            runAssertionRelOpWVariable(env, "select * from SupportBean(var_optimizable_relop<intPrimitive)");

            runAssertionInWSubs(env, "select * from SupportBean(intPrimitive in (?:p0:int, ?:p1:int))");
            runAssertionInWVariable(env, "select * from SupportBean(intPrimitive in (var_optimizable_start, var_optimizable_end))");

            runAssertionInWSubsWArray(env, "select * from SupportBean(intPrimitive in (?:p0:int[primitive]))");
            runAssertionInWVariableWArray(env, "select * from SupportBean(intPrimitive in (var_optimizable_array))");

            runAssertionBetweenWSubsWNumeric(env, "select * from SupportBean(intPrimitive between ?:p0:int and ?:p1:int)");
            runAssertionBetweenWVariableWNumeric(env, "select * from SupportBean(intPrimitive between var_optimizable_start and var_optimizable_end)");

            runAssertionBetweenWSubsWString(env, "select * from SupportBean(theString between ?:p0:string and ?:p1:string)");
            runAssertionBetweenWVariableWString(env, "select * from SupportBean(theString between var_optimizable_start_string and var_optimizable_end_string)");
        }
    }

    private static void runAssertionBetweenWSubsWNumeric(RegressionEnvironment env, String epl) {
        compileDeployWSubstitution(env, epl, CollectionUtil.buildMap("p0", 10, "p1", 11));
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "intPrimitive", FilterOperator.RANGE_CLOSED);
        }
        tryAssertionWSubsFrom9To12(env);
        env.undeployAll();
    }

    private static void runAssertionBetweenWVariableWNumeric(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "intPrimitive", FilterOperator.RANGE_CLOSED);
        }
        tryAssertionWSubsFrom9To12(env);
        env.undeployAll();
    }

    private static void runAssertionBetweenWSubsWString(RegressionEnvironment env, String epl) {
        compileDeployWSubstitution(env, epl, CollectionUtil.buildMap("p0", "c", "p1", "d"));
        tryAssertionBetweenDeplotTimeConst(env, epl);
    }

    private static void runAssertionBetweenWVariableWString(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        tryAssertionBetweenDeplotTimeConst(env, epl);
    }

    private static void tryAssertionBetweenDeplotTimeConst(RegressionEnvironment env, String epl) {
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "theString", FilterOperator.RANGE_CLOSED);
        }

        env.sendEventBean(new SupportBean("b", 0));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("c", 0));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("d", 0));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("e", 0));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void runAssertionInWSubsWArray(RegressionEnvironment env, String epl) {
        compileDeployWSubstitution(env, epl, CollectionUtil.buildMap("p0", new int[]{10, 11}));
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "intPrimitive", FilterOperator.IN_LIST_OF_VALUES);
        }
        tryAssertionWSubsFrom9To12(env);
        env.undeployAll();
    }

    private static void runAssertionInWVariableWArray(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "intPrimitive", FilterOperator.IN_LIST_OF_VALUES);
        }
        tryAssertionWSubsFrom9To12(env);
        env.undeployAll();
    }

    private static void tryAssertionWSubsFrom9To12(RegressionEnvironment env) {
        env.sendEventBean(new SupportBean("E1", 9));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("E2", 10));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("E3", 11));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("E1", 12));
        assertFalse(env.listener("s0").getAndClearIsInvoked());
    }

    private static void runAssertionInWSubs(RegressionEnvironment env, String epl) {
        compileDeployWSubstitution(env, epl, CollectionUtil.buildMap("p0", 10, "p1", 11));
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "intPrimitive", FilterOperator.IN_LIST_OF_VALUES);
        }
        tryAssertionWSubsFrom9To12(env);
        env.undeployAll();
    }

    private static void runAssertionInWVariable(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "intPrimitive", FilterOperator.IN_LIST_OF_VALUES);
        }
        tryAssertionWSubsFrom9To12(env);
        env.undeployAll();
    }

    private static void runAssertionRelOpWSubs(RegressionEnvironment env, String epl) {
        compileDeployWSubstitution(env, epl, CollectionUtil.buildMap("p0", 10));
        tryAssertionRelOpWDeployTimeConst(env, epl);
    }

    private static void runAssertionRelOpWVariable(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        tryAssertionRelOpWDeployTimeConst(env, epl);
    }

    private static void tryAssertionRelOpWDeployTimeConst(RegressionEnvironment env, String epl) {
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "intPrimitive", FilterOperator.GREATER);
        }

        env.sendEventBean(new SupportBean("E1", 10));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("E2", 11));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void runAssertionEqualsWSubs(RegressionEnvironment env, String epl) {
        compileDeployWSubstitution(env, epl, CollectionUtil.buildMap("p0", "abc"));
        tryAssertionEqualsWDeployTimeConst(env, epl);
    }

    private static void runAssertionEqualsWVariable(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        tryAssertionEqualsWDeployTimeConst(env, epl);
    }

    private static void tryAssertionEqualsWDeployTimeConst(RegressionEnvironment env, String epl) {
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "theString", FilterOperator.EQUAL);
        }

        env.sendEventBean(new SupportBean("abc", 0));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("x", 0));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void runAssertionEqualsWSubsWCoercion(RegressionEnvironment env, String epl) {
        compileDeployWSubstitution(env, epl, CollectionUtil.buildMap("p0", 100));
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcSingle(env.statement("s0"), "longPrimitive", FilterOperator.EQUAL);
        }

        SupportBean sb = new SupportBean();
        sb.setLongPrimitive(100);
        env.sendEventBean(sb);
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void tryInKeyword(RegressionEnvironment env, String field, SupportInKeywordBean prototype, AtomicInteger milestone) {
        tryInKeywordPlain(env, field, prototype, milestone);
        tryInKeywordPattern(env, field, prototype, milestone);
    }

    private static void tryInKeywordPattern(RegressionEnvironment env, String field, SupportInKeywordBean prototype, AtomicInteger milestone) {

        String epl = "@name('s0') select * from pattern[every a=SupportInKeywordBean -> SupportBean(intPrimitive in (a." + field + "))]";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        assertInKeywordReceivedPattern(env, SerializableObjectCopier.copyMayFail(prototype), 1, true);
        assertInKeywordReceivedPattern(env, SerializableObjectCopier.copyMayFail(prototype), 2, true);
        assertInKeywordReceivedPattern(env, SerializableObjectCopier.copyMayFail(prototype), 3, false);

        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                {new FilterItem("intPrimitive", FilterOperator.IN_LIST_OF_VALUES)},
            });
        }

        env.undeployAll();
    }

    private static void tryInKeywordPlain(RegressionEnvironment env, String field, SupportInKeywordBean prototype, AtomicInteger milestone) {
        String epl = "@name('s0') select * from SupportInKeywordBean#length(2) where 1 in (" + field + ")";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(SerializableObjectCopier.copyMayFail(prototype));
        assertTrue(env.listener("s0").getIsInvokedAndReset());

        env.undeployAll();
    }

    private static void tryNotInKeyword(RegressionEnvironment env, String field, SupportInKeywordBean prototype, AtomicInteger milestone) {
        tryNotInKeywordPlain(env, field, prototype, milestone);
        tryNotInKeywordPattern(env, field, prototype, milestone);
    }

    private static void tryNotInKeywordPlain(RegressionEnvironment env, String field, SupportInKeywordBean prototype, AtomicInteger milestone) {
        String epl = "@name('s0') select * from SupportInKeywordBean#length(2) where 1 not in (" + field + ")";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(SerializableObjectCopier.copyMayFail(prototype));
        assertFalse(env.listener("s0").getIsInvokedAndReset());

        env.undeployAll();
    }

    private static void tryNotInKeywordPattern(RegressionEnvironment env, String field, SupportInKeywordBean prototype, AtomicInteger milestone) {
        String epl = "@name('s0') select * from pattern[every a=SupportInKeywordBean -> SupportBean(intPrimitive not in (a." + field + "))]";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        assertInKeywordReceivedPattern(env, SerializableObjectCopier.copyMayFail(prototype), 0, true);
        assertInKeywordReceivedPattern(env, SerializableObjectCopier.copyMayFail(prototype), 3, true);

        assertInKeywordReceivedPattern(env, SerializableObjectCopier.copyMayFail(prototype), 1, false);
        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcByTypeMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                {new FilterItem("intPrimitive", FilterOperator.NOT_IN_LIST_OF_VALUES)},
            });
        }

        env.undeployAll();
    }

    private static void tryInArrayContextProvided(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "create context MyContext initiated by SupportInKeywordBean as mie terminated after 24 hours;\n" +
            "@name('s1') context MyContext select * from SupportBean#keepall where intPrimitive in (context.mie.ints);\n" +
            "@name('s2') context MyContext select * from SupportBean(intPrimitive in (context.mie.ints));\n";
        env.compileDeploy(epl).addListener("s1").addListener("s2");

        env.sendEventBean(new SupportInKeywordBean(new int[]{1, 2}));

        env.sendEventBean(new SupportBean("E1", 1));
        assertTrue(env.listener("s1").getIsInvokedAndReset() && env.listener("s2").getIsInvokedAndReset());

        env.sendEventBean(new SupportBean("E2", 2));
        assertTrue(env.listener("s1").getIsInvokedAndReset() && env.listener("s2").getIsInvokedAndReset());

        env.sendEventBean(new SupportBean("E3", 3));
        assertFalse(env.listener("s1").getIsInvokedAndReset() || env.listener("s2").getIsInvokedAndReset());

        if (hasFilterIndexPlanBasicOrMore(env)) {
            SupportFilterServiceHelper.assertFilterSvcByTypeMulti(env.statement("s2"), "SupportBean", new FilterItem[][]{
                {new FilterItem("intPrimitive", FilterOperator.IN_LIST_OF_VALUES)},
            });
        }

        env.undeployAll();
    }

    public static class ExprFilterOptimizableTypeOf implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from SupportOverrideBase(typeof(e) = 'SupportOverrideBase') as e";
            env.compileDeployAddListenerMile(epl, "s0", 0);

            env.sendEventBean(new SupportOverrideBase(""));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportOverrideOne("a", "b"));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    public static class ExprFilterOptimizableVariableAndSeparateThread implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.runtime().getVariableService().setVariableValue(null, "myCheckServiceProvider", new MyCheckServiceProvider());

            env.compileDeploy("@name('s0') select * from SupportBean(myCheckServiceProvider.check())").addListener("s0");
            CountDownLatch latch = new CountDownLatch(1);

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(new Runnable() {
                public void run() {
                    env.sendEventBean(new SupportBean());
                    assertTrue(env.listener("s0").getIsInvokedAndReset());
                    latch.countDown();
                }
            });

            try {
                assertTrue(latch.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                fail();
            }

            env.undeployAll();
        }
    }

    public static class ExprFilterOptimizableMethodInvocationContext implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            methodInvocationContextFilterOptimized = null;
            env.compileDeployAddListenerMile("@name('s0') select * from SupportBean e where myCustomOkFunction(e) = \"OK\"", "s0", 0);
            env.sendEventBean(new SupportBean());
            assertEquals("default", methodInvocationContextFilterOptimized.getRuntimeURI());
            assertEquals("myCustomOkFunction", methodInvocationContextFilterOptimized.getFunctionName());
            assertNull(methodInvocationContextFilterOptimized.getStatementUserObject());
            assertEquals(-1, methodInvocationContextFilterOptimized.getContextPartitionId());
            methodInvocationContextFilterOptimized = null;
            env.undeployAll();
        }
    }

    private static void assertInKeywordReceivedPattern(RegressionEnvironment env, Object event, int intPrimitive, boolean expected) {
        env.sendEventBean(event);
        env.sendEventBean(new SupportBean(null, intPrimitive));
        assertEquals(expected, env.listener("s0").getIsInvokedAndReset());
    }

    private static void assertFilterDeploySingle(RegressionEnvironment env, RegressionPath path, String epl, String expression, FilterOperator op, AtomicInteger milestone) {
        env.compileDeploy("@name('s0')" + epl, path).addListener("s0").milestoneInc(milestone);
        EPStatementSPI statementSPI = (EPStatementSPI) env.statement("s0");
        if (hasFilterIndexPlanBasicOrMore(env)) {
            FilterItem param = SupportFilterServiceHelper.getFilterSvcSingle(statementSPI);
            assertEquals("failed for '" + epl + "'", op, param.getOp());
            assertEquals(expression, param.getName());
        }
        env.undeployModuleContaining("s0");
    }

    private static void compileDeployWSubstitution(RegressionEnvironment env, String epl, Map<String, Object> params) {
        EPCompiled compiled = env.compile("@name('s0') " + epl);
        StatementSubstitutionParameterOption resolver = ctx -> {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                ctx.setObject(entry.getKey(), entry.getValue());
            }
        };
        env.deploy(compiled, new DeploymentOptions().setStatementSubstitutionParameter(resolver));
        env.addListener("s0");
    }

    public static String myCustomOkFunction(Object e, EPLMethodInvocationContext ctx) {
        methodInvocationContextFilterOptimized = ctx;
        return "OK";
    }

    public static boolean myCustomBigDecimalEquals(final BigDecimal first, final BigDecimal second) {
        return first.compareTo(second) == 0;
    }

    public static class MyCheckServiceProvider implements Serializable {
        public boolean check() {
            return true;
        }
    }
}
