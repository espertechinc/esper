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
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.filter.SupportFilterHelper;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPDeployException;
import com.espertech.esper.runtime.client.EPDeploymentService;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterOption;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterItem;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static com.espertech.esper.common.internal.compile.stage2.FilterSpecCompilerPlanner.PROPERTY_NAME_BOOLEAN_EXPRESSION;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class ExprFilterOptimizable {
    private static final Logger log = LoggerFactory.getLogger(ExprFilterOptimizable.class);
    private static EPLMethodInvocationContext methodInvocationContextFilterOptimized;

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprFilterInAndNotInKeywordMultivalue());
        executions.add(new ExprFilterOptimizablePerf());
        executions.add(new ExprFilterOptimizableInspectFilter());
        executions.add(new ExprFilterOrRewrite());
        executions.add(new ExprFilterOrToInRewrite());
        executions.add(new ExprFilterOrPerformance());
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

            tryInvalidCompile(env, "select * from pattern[every a=SupportInKeywordBean -> SupportBean(intPrimitive in (a.longs))]",
                "Implicit conversion from datatype 'long' to 'Integer' for property 'intPrimitive' is not allowed (strict filter type coercion)");
        }
    }

    private static class ExprFilterOptimizablePerf implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            RegressionPath path = new RegressionPath();

            // func(...) = value
            tryOptimizableEquals(env, path, "select * from SupportBean(libSplit(theString) = !NUM!)", 10, milestone);

            // func(...) implied true
            tryOptimizableBoolean(env, path, "select * from SupportBean(libE1True(theString))", milestone);

            // with context
            tryOptimizableMethodInvocationContext(env, milestone);

            // typeof(e)
            tryOptimizableTypeOf(env, milestone);

            // declared expression (...) = value
            env.compileDeploy("@name('create-expr') create expression thesplit {theString => libSplit(theString)}", path).addListener("create-expr");
            tryOptimizableEquals(env, path, "select * from SupportBean(thesplit(*) = !NUM!)", 10, milestone);

            // declared expression (...) implied true
            env.compileDeploy("@name('create-expr') create expression theE1Test {theString => libE1True(theString)}", path).addListener("create-expr");
            tryOptimizableBoolean(env, path, "select * from SupportBean(theE1Test(*))", milestone);

            // with variable and separate thread
            tryOptimizableVariableAndSeparateThread(env, milestone);
        }
    }

    private static class ExprFilterOptimizableInspectFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;
            AtomicInteger milestone = new AtomicInteger();
            RegressionPath path = new RegressionPath();

            epl = "select * from SupportBean(funcOne(theString) = 0)";
            assertFilterSingle(env, path, epl, PROPERTY_NAME_BOOLEAN_EXPRESSION, FilterOperator.BOOLEAN_EXPRESSION, milestone);

            epl = "select * from SupportBean(funcOneWDefault(theString) = 0)";
            assertFilterSingle(env, path, epl, "funcOneWDefault(theString)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(funcTwo(theString) = 0)";
            assertFilterSingle(env, path, epl, "funcTwo(theString)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(libE1True(theString))";
            assertFilterSingle(env, path, epl, "libE1True(theString)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(funcTwo( theString ) > 10)";
            assertFilterSingle(env, path, epl, "funcTwo(theString)", FilterOperator.GREATER, milestone);

            epl = "select * from SupportBean(libE1True(theString))";
            assertFilterSingle(env, path, epl, "libE1True(theString)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(typeof(e) = 'SupportBean') as e";
            assertFilterSingle(env, path, epl, "typeof(e)", FilterOperator.EQUAL, milestone);

            env.compileDeploy("@name('create-expr') create expression thesplit {theString => funcOne(theString)}", path).addListener("create-expr");
            epl = "select * from SupportBean(thesplit(*) = 0)";
            assertFilterSingle(env, path, epl, "thesplit(*)", FilterOperator.EQUAL, milestone);

            epl = "select * from SupportBean(thesplit(*) > 10)";
            assertFilterSingle(env, path, epl, "thesplit(*)", FilterOperator.GREATER, milestone);

            epl = "expression housenumber alias for {10} select * from SupportBean(intPrimitive = housenumber)";
            assertFilterSingle(env, path, epl, "intPrimitive", FilterOperator.EQUAL, milestone);

            epl = "expression housenumber alias for {intPrimitive*10} select * from SupportBean(intPrimitive = housenumber)";
            assertFilterSingle(env, path, epl, ".boolean_expression", FilterOperator.BOOLEAN_EXPRESSION, milestone);

            env.undeployAll();
        }
    }

    private static class ExprFilterOrRewrite implements RegressionExecution {

        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();

            tryOrRewriteTwoOr(env, milestone);

            tryOrRewriteOrRewriteThreeOr(env, milestone);

            tryOrRewriteOrRewriteWithAnd(env, milestone);

            tryOrRewriteOrRewriteThreeWithOverlap(env, milestone);

            tryOrRewriteOrRewriteFourOr(env, milestone);

            tryOrRewriteOrRewriteEightOr(env, milestone);

            tryOrRewriteAndRewriteNotEquals(env, milestone);

            tryOrRewriteAndRewriteInnerOr(env, milestone);

            tryOrRewriteOrRewriteAndOrMulti(env, milestone);

            tryOrRewriteBooleanExprSimple(env, milestone);

            tryOrRewriteBooleanExprAnd(env, milestone);

            tryOrRewriteSubquery(env, milestone);

            tryOrRewriteHint(env, milestone);

            tryOrRewriteContextPartitionedSegmented(env, milestone);

            tryOrRewriteContextPartitionedHash(env, milestone);

            tryOrRewriteContextPartitionedCategory(env, milestone);

            tryOrRewriteContextPartitionedInitiatedSameEvent(env, milestone);

            tryOrRewriteContextPartitionedInitiated(env, milestone);
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

            assertTrue(env.listener("s0").isInvoked());

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
                assertFilterSingle(env.statement("s0"), epl, "theString", FilterOperator.IN_LIST_OF_VALUES);

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
            SupportFilterHelper.assertFilterTwo(env.statement("s0"), epl, "intPrimitive", FilterOperator.EQUAL, "theString", FilterOperator.IN_LIST_OF_VALUES);
            env.undeployAll();
        }
    }

    private static class ExprFilterOrPerformance implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            SupportUpdateListener listener = new SupportUpdateListener();
            for (int i = 0; i < 100; i++) {
                String epl = "@name('s" + i + "') select * from SupportBean(theString = '" + i + "' or intPrimitive=" + i + ")";
                EPCompiled compiled = env.compile(epl);
                env.deploy(compiled).statement("s" + i).addListener(listener);
            }

            long start = System.nanoTime();
            // System.out.println("Starting " + DateTime.print(new Date()));
            for (int i = 0; i < 10000; i++) {
                env.sendEventBean(new SupportBean("100", 1));
                assertTrue(listener.isInvoked());
                listener.reset();
            }
            // System.out.println("Ending " + DateTime.print(new Date()));
            double delta = (System.nanoTime() - start) / 1000d / 1000d;
            // System.out.println("Delta=" + (delta + " msec"));
            assertTrue(delta < 500);

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

            tryInvalidCompile(env, "select * from SupportBean(intPrimitive=?:p0:long)",
                "Implicit conversion from datatype 'Long' to 'Integer' for property 'intPrimitive' is not allowed");

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
        assertFilterSingle(env.statement("s0"), epl, "intPrimitive", FilterOperator.RANGE_CLOSED);
        tryAssertionWSubsFrom9To12(env);
        env.undeployAll();
    }

    private static void runAssertionBetweenWVariableWNumeric(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        assertFilterSingle(env.statement("s0"), epl, "intPrimitive", FilterOperator.RANGE_CLOSED);
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
        assertFilterSingle(env.statement("s0"), epl, "theString", FilterOperator.RANGE_CLOSED);

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
        assertFilterSingle(env.statement("s0"), epl, "intPrimitive", FilterOperator.IN_LIST_OF_VALUES);
        tryAssertionWSubsFrom9To12(env);
        env.undeployAll();
    }

    private static void runAssertionInWVariableWArray(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        assertFilterSingle(env.statement("s0"), epl, "intPrimitive", FilterOperator.IN_LIST_OF_VALUES);
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
        assertFilterSingle(env.statement("s0"), epl, "intPrimitive", FilterOperator.IN_LIST_OF_VALUES);
        tryAssertionWSubsFrom9To12(env);
        env.undeployAll();
    }

    private static void runAssertionInWVariable(RegressionEnvironment env, String epl) {
        env.compileDeploy("@name('s0') " + epl).addListener("s0");
        assertFilterSingle(env.statement("s0"), epl, "intPrimitive", FilterOperator.IN_LIST_OF_VALUES);
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
        assertFilterSingle(env.statement("s0"), epl, "intPrimitive", FilterOperator.GREATER);

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
        assertFilterSingle(env.statement("s0"), epl, "theString", FilterOperator.EQUAL);

        env.sendEventBean(new SupportBean("abc", 0));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("x", 0));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void runAssertionEqualsWSubsWCoercion(RegressionEnvironment env, String epl) {
        compileDeployWSubstitution(env, epl, CollectionUtil.buildMap("p0", 100));
        assertFilterSingle(env.statement("s0"), epl, "longPrimitive", FilterOperator.EQUAL);

        SupportBean sb = new SupportBean();
        sb.setLongPrimitive(100);
        env.sendEventBean(sb);
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void tryOrRewriteHint(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@Hint('MAX_FILTER_WIDTH=0') @name('s0') select * from SupportBean_IntAlphabetic((b=1 or c=1) and (d=1 or e=1))";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
        assertFilterSingle(env.statement("s0"), epl, ".boolean_expression", FilterOperator.BOOLEAN_EXPRESSION);
        env.undeployAll();
    }

    private static void tryOrRewriteSubquery(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@name('s0') select (select * from SupportBean_IntAlphabetic(a=1 or b=1)#keepall) as c0 from SupportBean";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        SupportBean_IntAlphabetic iaOne = intEvent(1, 1);
        env.sendEventBean(iaOne);
        env.sendEventBean(new SupportBean());
        assertEquals(iaOne, env.listener("s0").assertOneGetNewAndReset().get("c0"));

        env.undeployAll();
    }

    private static void tryOrRewriteContextPartitionedCategory(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@name('ctx') create context MyContext \n" +
            "  group a=1 or b=1 as g1,\n" +
            "  group c=1 as g1\n" +
            "  from SupportBean_IntAlphabetic;" +
            "@name('s0') context MyContext select * from SupportBean_IntAlphabetic(d=1 or e=1)";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        sendAssertEvents(env,
            new Object[]{intEvent(1, 0, 0, 0, 1), intEvent(0, 1, 0, 1, 0), intEvent(0, 0, 1, 1, 1)},
            new Object[]{intEvent(0, 0, 0, 1, 0), intEvent(1, 0, 0, 0, 0), intEvent(0, 0, 1, 0, 0)}
        );

        env.undeployAll();
    }

    private static void tryOrRewriteContextPartitionedHash(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "create context MyContext " +
            "coalesce by consistent_hash_crc32(a) from SupportBean_IntAlphabetic(b=1) granularity 16 preallocate;" +
            "@name('s0') context MyContext select * from SupportBean_IntAlphabetic(c=1 or d=1)";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        sendAssertEvents(env,
            new Object[]{intEvent(100, 1, 0, 1), intEvent(100, 1, 1, 0)},
            new Object[]{intEvent(100, 0, 0, 1), intEvent(100, 1, 0, 0)}
        );
        env.undeployAll();
    }

    private static void tryOrRewriteContextPartitionedSegmented(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "create context MyContext partition by a from SupportBean_IntAlphabetic(b=1 or c=1);" +
            "@name('s0') context MyContext select * from SupportBean_IntAlphabetic(d=1)";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        sendAssertEvents(env,
            new Object[]{intEvent(100, 1, 0, 1), intEvent(100, 0, 1, 1)},
            new Object[]{intEvent(100, 0, 0, 1), intEvent(100, 1, 0, 0)}
        );
        env.undeployAll();
    }

    private static void tryOrRewriteBooleanExprAnd(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filters = new String[]{
            "(a='a' or a like 'A%') and (b='b' or b like 'B%')",
        };
        for (String filter : filters) {
            String epl = "@name('s0') select * from SupportBean_StringAlphabetic(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean_StringAlphabetic", new FilterItem[][]{
                {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("b", FilterOperator.EQUAL)},
                {new FilterItem("a", FilterOperator.EQUAL), FilterItem.getBoolExprFilterItem()},
                {new FilterItem("b", FilterOperator.EQUAL), FilterItem.getBoolExprFilterItem()},
                {FilterItem.getBoolExprFilterItem()},
            });

            sendAssertEvents(env,
                new Object[]{stringEvent("a", "b"), stringEvent("A1", "b"), stringEvent("a", "B1"), stringEvent("A1", "B1")},
                new Object[]{stringEvent("x", "b"), stringEvent("a", "x"), stringEvent("A1", "C"), stringEvent("C", "B1")}
            );
            env.undeployAll();
        }
    }

    private static void tryOrRewriteBooleanExprSimple(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filters = new String[]{
            "a like 'a%' and (b='b' or c='c')",
        };
        for (String filter : filters) {
            String epl = "@name('s0') select * from SupportBean_StringAlphabetic(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean_StringAlphabetic", new FilterItem[][]{
                {new FilterItem("b", FilterOperator.EQUAL), FilterItem.getBoolExprFilterItem()},
                {new FilterItem("c", FilterOperator.EQUAL), FilterItem.getBoolExprFilterItem()},
            });

            sendAssertEvents(env,
                new Object[]{stringEvent("a1", "b", null), stringEvent("a1", null, "c")},
                new Object[]{stringEvent("x", "b", null), stringEvent("a1", null, null), stringEvent("a1", null, "x")}
            );
            env.undeployAll();
        }
    }

    private static void tryOrRewriteAndRewriteNotEquals(RegressionEnvironment env, AtomicInteger milestone) {
        tryOrRewriteAndRewriteNotEqualsOr(env, milestone);

        tryOrRewriteAndRewriteNotEqualsConsolidate(env, milestone);

        tryOrRewriteAndRewriteNotEqualsWithOrConsolidateSecond(env, milestone);
    }

    private static void tryOrRewriteAndRewriteNotEqualsWithOrConsolidateSecond(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filters = new String[]{
            "a!=1 and a!=2 and ((a!=3 and a!=4) or (a!=5 and a!=6))",
        };
        for (String filter : filters) {
            String epl = "@name('s0') select * from SupportBean_IntAlphabetic(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean_IntAlphabetic", new FilterItem[][]{
                {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), FilterItem.getBoolExprFilterItem()},
                {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), FilterItem.getBoolExprFilterItem()},
            });

            sendAssertEvents(env,
                new Object[]{intEvent(3), intEvent(4), intEvent(0)},
                new Object[]{intEvent(2), intEvent(1)}
            );
            env.undeployAll();
        }
    }

    private static void tryOrRewriteAndRewriteNotEqualsConsolidate(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filters = new String[]{
            "a!=1 and a!=2 and (a!=3 or a!=4)",
        };
        for (String filter : filters) {
            String epl = "@name('s0') select * from SupportBean_IntAlphabetic(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean_IntAlphabetic", new FilterItem[][]{
                {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("a", FilterOperator.NOT_EQUAL)},
                {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("a", FilterOperator.NOT_EQUAL)},
            });

            sendAssertEvents(env,
                new Object[]{intEvent(3), intEvent(4), intEvent(0)},
                new Object[]{intEvent(2), intEvent(1)}
            );
            env.undeployAll();
        }
    }

    private static void tryOrRewriteAndRewriteNotEqualsOr(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filters = new String[]{
            "a!=1 and a!=2 and (b=1 or c=1)",
        };
        for (String filter : filters) {
            String epl = "@name('s0') select * from SupportBean_IntAlphabetic(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean_IntAlphabetic", new FilterItem[][]{
                {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("b", FilterOperator.EQUAL)},
                {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("c", FilterOperator.EQUAL)},
            });

            sendAssertEvents(env,
                new Object[]{intEvent(3, 1, 0), intEvent(3, 0, 1), intEvent(0, 1, 0)},
                new Object[]{intEvent(2, 0, 0), intEvent(1, 0, 0), intEvent(3, 0, 0)}
            );
            env.undeployAll();
        }
    }

    private static void tryOrRewriteAndRewriteInnerOr(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filtersAB = new String[]{
            "theString='a' and (intPrimitive=1 or longPrimitive=10)",
        };
        for (String filter : filtersAB) {
            String epl = "@name('s0') select * from SupportBean(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("longPrimitive", FilterOperator.EQUAL)},
            });

            sendAssertEvents(env,
                new SupportBean[]{makeEvent("a", 1, 0), makeEvent("a", 0, 10), makeEvent("a", 1, 10)},
                new SupportBean[]{makeEvent("x", 0, 0), makeEvent("a", 2, 20), makeEvent("x", 1, 10)}
            );
            env.undeployAll();
        }
    }

    private static void tryOrRewriteOrRewriteAndOrMulti(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filtersAB = new String[]{
            "a=1 and (b=1 or c=1) and (d=1 or e=1)",
        };
        for (String filter : filtersAB) {
            String epl = "@name('s0') select * from SupportBean_IntAlphabetic(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean_IntAlphabetic", new FilterItem[][]{
                {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("b", FilterOperator.EQUAL), new FilterItem("d", FilterOperator.EQUAL)},
                {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("c", FilterOperator.EQUAL), new FilterItem("d", FilterOperator.EQUAL)},
                {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("c", FilterOperator.EQUAL), new FilterItem("e", FilterOperator.EQUAL)},
                {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("b", FilterOperator.EQUAL), new FilterItem("e", FilterOperator.EQUAL)},
            });

            sendAssertEvents(env,
                new Object[]{intEvent(1, 1, 0, 1, 0), intEvent(1, 0, 1, 0, 1), intEvent(1, 1, 0, 0, 1), intEvent(1, 0, 1, 1, 0)},
                new Object[]{intEvent(1, 0, 0, 1, 0), intEvent(1, 0, 0, 1, 0), intEvent(1, 1, 1, 0, 0), intEvent(0, 1, 1, 1, 1)}
            );
            env.undeployAll();
        }
    }

    private static void tryOrRewriteOrRewriteEightOr(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filtersAB = new String[]{
            "theString = 'a' or intPrimitive=1 or longPrimitive=10 or doublePrimitive=100 or boolPrimitive=true or " +
                "intBoxed=2 or longBoxed=20 or doubleBoxed=200",
            "longBoxed=20 or theString = 'a' or boolPrimitive=true or intBoxed=2 or longPrimitive=10 or doublePrimitive=100 or " +
                "intPrimitive=1 or doubleBoxed=200",
        };
        for (String filter : filtersAB) {
            String epl = "@name('s0') select * from SupportBean(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                {new FilterItem("theString", FilterOperator.EQUAL)},
                {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                {new FilterItem("longPrimitive", FilterOperator.EQUAL)},
                {new FilterItem("doublePrimitive", FilterOperator.EQUAL)},
                {new FilterItem("boolPrimitive", FilterOperator.EQUAL)},
                {new FilterItem("intBoxed", FilterOperator.EQUAL)},
                {new FilterItem("longBoxed", FilterOperator.EQUAL)},
                {new FilterItem("doubleBoxed", FilterOperator.EQUAL)},
            });

            sendAssertEvents(env,
                new SupportBean[]{makeEvent("a", 1, 10, 100, true, 2, 20, 200), makeEvent("a", 0, 0, 0, true, 0, 0, 0),
                    makeEvent("a", 0, 0, 0, true, 0, 20, 0), makeEvent("x", 0, 0, 100, false, 0, 0, 0),
                    makeEvent("x", 1, 0, 0, false, 0, 0, 200), makeEvent("x", 0, 0, 0, false, 0, 0, 200),
                },
                new SupportBean[]{makeEvent("x", 0, 0, 0, false, 0, 0, 0)}
            );
            env.undeployAll();
        }
    }

    private static void tryOrRewriteOrRewriteFourOr(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filtersAB = new String[]{
            "theString = 'a' or intPrimitive=1 or longPrimitive=10 or doublePrimitive=100",
        };
        for (String filter : filtersAB) {
            String epl = "@name('s0') select * from SupportBean(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                {new FilterItem("theString", FilterOperator.EQUAL)},
                {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                {new FilterItem("longPrimitive", FilterOperator.EQUAL)},
                {new FilterItem("doublePrimitive", FilterOperator.EQUAL)},
            });

            sendAssertEvents(env,
                new SupportBean[]{makeEvent("a", 1, 10, 100), makeEvent("x", 0, 0, 100), makeEvent("x", 0, 10, 100), makeEvent("a", 0, 0, 0)},
                new SupportBean[]{makeEvent("x", 0, 0, 0)}
            );
            env.undeployAll();
        }
    }


    private static void assertFilterSingle(EPStatement stmt, String epl, String expression, FilterOperator op) {
        EPStatementSPI statementSPI = (EPStatementSPI) stmt;
        FilterItem param = SupportFilterHelper.getFilterSingle(statementSPI);
        assertEquals("failed for '" + epl + "'", op, param.getOp());
        assertEquals(expression, param.getName());
    }

    private static void tryOrRewriteContextPartitionedInitiated(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@name('ctx') create context MyContext initiated by SupportBean(theString='A' or intPrimitive=1) terminated after 24 hours;\n"
            + "@name('s0') context MyContext select * from SupportBean;\n";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(new SupportBean("A", 1));
        env.listener("s0").assertOneGetNewAndReset();

        env.undeployAll();
    }

    private static void tryOrRewriteContextPartitionedInitiatedSameEvent(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "create context MyContext initiated by SupportBean terminated after 24 hours;" +
            "@name('s0') context MyContext select * from SupportBean(theString='A' or intPrimitive=1)";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(new SupportBean("A", 1));
        env.listener("s0").assertOneGetNewAndReset();

        env.undeployAll();
    }

    private static void tryInKeyword(RegressionEnvironment env, String field, SupportInKeywordBean prototype, AtomicInteger milestone) {
        tryInKeywordPlain(env, field, prototype, milestone);
        tryInKeywordPattern(env, field, prototype, milestone);
    }

    private static void tryOrRewriteOrRewriteThreeOr(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filtersAB = new String[]{
            "theString = 'a' or intPrimitive = 1 or longPrimitive = 2",
            "2 = longPrimitive or 1 = intPrimitive or theString = 'a'"
        };
        for (String filter : filtersAB) {
            String epl = "@name('s0') select * from SupportBean(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                {new FilterItem("theString", FilterOperator.EQUAL)},
                {new FilterItem("longPrimitive", FilterOperator.EQUAL)},
            });

            sendAssertEvents(env,
                new SupportBean[]{makeEvent("a", 0, 0), makeEvent("b", 1, 0), makeEvent("c", 0, 2), makeEvent("c", 0, 2)},
                new SupportBean[]{makeEvent("v", 0, 0), makeEvent("c", 2, 1)}
            );

            env.undeployAll();
        }
    }

    private static void sendAssertEvents(RegressionEnvironment env, Object[] matches, Object[] nonMatches) {
        env.listener("s0").reset();
        for (Object match : matches) {
            env.sendEventBean(match);
            assertSame(match, env.listener("s0").assertOneGetNewAndReset().getUnderlying());
        }
        env.listener("s0").reset();
        for (Object nonMatch : nonMatches) {
            env.sendEventBean(nonMatch);
            assertFalse(env.listener("s0").isInvoked());
        }
    }

    private static void tryInKeywordPattern(RegressionEnvironment env, String field, SupportInKeywordBean prototype, AtomicInteger milestone) {

        String epl = "@name('s0') select * from pattern[every a=SupportInKeywordBean -> SupportBean(intPrimitive in (a." + field + "))]";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        assertInKeywordReceivedPattern(env, SerializableObjectCopier.copyMayFail(prototype), 1, true);
        assertInKeywordReceivedPattern(env, SerializableObjectCopier.copyMayFail(prototype), 2, true);
        assertInKeywordReceivedPattern(env, SerializableObjectCopier.copyMayFail(prototype), 3, false);

        SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
            {new FilterItem("intPrimitive", FilterOperator.IN_LIST_OF_VALUES)},
        });

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
        SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
            {new FilterItem("intPrimitive", FilterOperator.NOT_IN_LIST_OF_VALUES)},
        });

        env.undeployAll();
    }

    private static void tryOrRewriteOrRewriteThreeWithOverlap(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filtersAB = new String[]{
            "theString = 'a' or theString = 'b' or intPrimitive=1",
            "intPrimitive = 1 or theString = 'b' or theString = 'a'",
        };
        for (String filter : filtersAB) {
            String epl = "@name('s0') select * from SupportBean(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                {new FilterItem("theString", FilterOperator.EQUAL)},
                {new FilterItem("theString", FilterOperator.EQUAL)},
                {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
            });

            sendAssertEvents(env,
                new SupportBean[]{makeEvent("a", 1), makeEvent("b", 0), makeEvent("x", 1)},
                new SupportBean[]{makeEvent("x", 0)}
            );
            env.undeployAll();
        }
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

        SupportFilterHelper.assertFilterMulti(env.statement("s2"), "SupportBean", new FilterItem[][]{
            {new FilterItem("intPrimitive", FilterOperator.IN_LIST_OF_VALUES)},
        });

        env.undeployAll();
    }

    private static void tryOptimizableEquals(RegressionEnvironment env, RegressionPath path, String epl, int numStatements, AtomicInteger milestone) {

        // test function returns lookup value and "equals"
        for (int i = 0; i < numStatements; i++) {
            String text = "@name('s" + i + "') " + epl.replace("!NUM!", Integer.toString(i));
            env.compileDeploy(text, path).addListener("s" + i);
        }
        env.milestone(milestone.getAndIncrement());

        long startTime = System.currentTimeMillis();
        SupportStaticMethodLib.resetCountInvoked();
        int loops = 1000;
        for (int i = 0; i < loops; i++) {
            env.sendEventBean(new SupportBean("E_" + i % numStatements, 0));
            SupportListener listener = env.listener("s" + i % numStatements);
            assertTrue(listener.getAndClearIsInvoked());
        }
        long delta = System.currentTimeMillis() - startTime;
        assertEquals(loops, SupportStaticMethodLib.getCountInvoked());

        log.info("Equals delta=" + delta);
        assertTrue("Delta is " + delta, delta < 1000);
        env.undeployAll();
    }

    private static void tryOptimizableBoolean(RegressionEnvironment env, RegressionPath path, String epl, AtomicInteger milestone) {

        // test function returns lookup value and "equals"
        int count = 10;
        for (int i = 0; i < count; i++) {
            EPCompiled compiled = env.compile("@name('s" + i + "')" + epl, path);
            EPDeploymentService admin = env.runtime().getDeploymentService();
            try {
                admin.deploy(compiled);
            } catch (EPDeployException ex) {
                ex.printStackTrace();
                fail();
            }
        }

        env.milestoneInc(milestone);

        SupportUpdateListener listener = new SupportUpdateListener();
        for (int i = 0; i < 10; i++) {
            env.statement("s" + i).addListener(listener);
        }

        long startTime = System.currentTimeMillis();
        SupportStaticMethodLib.resetCountInvoked();
        int loops = 10000;
        for (int i = 0; i < loops; i++) {
            String key = "E_" + i % 100;
            env.sendEventBean(new SupportBean(key, 0));
            if (key.equals("E_1")) {
                assertEquals(count, listener.getNewDataList().size());
                listener.reset();
            } else {
                assertFalse(listener.isInvoked());
            }
        }
        long delta = System.currentTimeMillis() - startTime;
        assertEquals(loops, SupportStaticMethodLib.getCountInvoked());

        log.info("Boolean delta=" + delta);
        assertTrue("Delta is " + delta, delta < 1000);
        env.undeployAll();
    }

    private static void tryOptimizableTypeOf(RegressionEnvironment env, AtomicInteger milestone) {
        String epl = "@name('s0') select * from SupportOverrideBase(typeof(e) = 'SupportOverrideBase') as e";
        env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());

        env.sendEventBean(new SupportOverrideBase(""));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportOverrideOne("a", "b"));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }

    private static void tryOptimizableVariableAndSeparateThread(RegressionEnvironment env, AtomicInteger milestone) {

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

    private static void tryOrRewriteTwoOr(RegressionEnvironment env, AtomicInteger milestone) {
        // test 'or' rewrite
        String[] filtersAB = new String[]{
            "select * from SupportBean(theString = 'a' or intPrimitive = 1)",
            "select * from SupportBean(theString = 'a' or 1 = intPrimitive)",
            "select * from SupportBean('a' = theString or 1 = intPrimitive)",
            "select * from SupportBean('a' = theString or intPrimitive = 1)",
        };

        for (String filter : filtersAB) {
            env.compileDeployAddListenerMile("@name('s0')" + filter, "s0", milestone.getAndIncrement());

            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                {new FilterItem("theString", FilterOperator.EQUAL)},
            });

            env.sendEventBean(new SupportBean("a", 0));
            env.listener("s0").assertOneGetNewAndReset();
            env.sendEventBean(new SupportBean("b", 1));
            env.listener("s0").assertOneGetNewAndReset();
            env.sendEventBean(new SupportBean("c", 0));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static void tryOrRewriteOrRewriteWithAnd(RegressionEnvironment env, AtomicInteger milestone) {
        String[] filtersAB = new String[]{
            "(theString = 'a' and intPrimitive = 1) or (theString = 'b' and intPrimitive = 2)",
            "(intPrimitive = 1 and theString = 'a') or (intPrimitive = 2 and theString = 'b')",
            "(theString = 'b' and intPrimitive = 2) or (theString = 'a' and intPrimitive = 1)",
        };
        for (String filter : filtersAB) {
            String epl = "@name('s0') select * from SupportBean(" + filter + ")";
            env.compileDeployAddListenerMile(epl, "s0", milestone.getAndIncrement());
            SupportFilterHelper.assertFilterMulti(env.statement("s0"), "SupportBean", new FilterItem[][]{
                {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("intPrimitive", FilterOperator.EQUAL)},
            });

            sendAssertEvents(env,
                new SupportBean[]{makeEvent("a", 1), makeEvent("b", 2)},
                new SupportBean[]{makeEvent("x", 0), makeEvent("a", 0), makeEvent("a", 2), makeEvent("b", 1)}
            );
            env.undeployAll();
        }
    }

    private static void tryOptimizableMethodInvocationContext(RegressionEnvironment env, AtomicInteger milestone) {
        methodInvocationContextFilterOptimized = null;
        env.compileDeployAddListenerMile("@name('s0') select * from SupportBean e where myCustomOkFunction(e) = \"OK\"", "s0", milestone.getAndIncrement());
        env.sendEventBean(new SupportBean());
        assertEquals("default", methodInvocationContextFilterOptimized.getRuntimeURI());
        assertEquals("myCustomOkFunction", methodInvocationContextFilterOptimized.getFunctionName());
        assertNull(methodInvocationContextFilterOptimized.getStatementUserObject());
        assertEquals("s0", methodInvocationContextFilterOptimized.getStatementName());
        assertEquals(-1, methodInvocationContextFilterOptimized.getContextPartitionId());
        methodInvocationContextFilterOptimized = null;
        env.undeployAll();
    }

    private static void assertInKeywordReceivedPattern(RegressionEnvironment env, Object event, int intPrimitive, boolean expected) {
        env.sendEventBean(event);
        env.sendEventBean(new SupportBean(null, intPrimitive));
        assertEquals(expected, env.listener("s0").getIsInvokedAndReset());
    }

    private static void assertFilterSingle(RegressionEnvironment env, RegressionPath path, String epl, String expression, FilterOperator op, AtomicInteger milestone) {
        env.compileDeploy("@name('s0')" + epl, path).addListener("s0").milestoneInc(milestone);
        EPStatementSPI statementSPI = (EPStatementSPI) env.statement("s0");
        FilterItem param = SupportFilterHelper.getFilterSingle(statementSPI);
        assertEquals("failed for '" + epl + "'", op, param.getOp());
        assertEquals(expression, param.getName());
        env.undeployModuleContaining("s0");
    }

    private static SupportBean makeEvent(String theString, int intPrimitive) {
        return makeEvent(theString, intPrimitive, 0L);
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        return makeEvent(theString, intPrimitive, longPrimitive, 0d);
    }

    private static SupportBean_IntAlphabetic intEvent(int a) {
        return new SupportBean_IntAlphabetic(a);
    }

    private static SupportBean_IntAlphabetic intEvent(int a, int b) {
        return new SupportBean_IntAlphabetic(a, b);
    }

    private static SupportBean_IntAlphabetic intEvent(int a, int b, int c, int d) {
        return new SupportBean_IntAlphabetic(a, b, c, d);
    }

    private static SupportBean_StringAlphabetic stringEvent(String a, String b) {
        return new SupportBean_StringAlphabetic(a, b);
    }

    private static SupportBean_StringAlphabetic stringEvent(String a, String b, String c) {
        return new SupportBean_StringAlphabetic(a, b, c);
    }

    private static SupportBean_IntAlphabetic intEvent(int a, int b, int c) {
        return new SupportBean_IntAlphabetic(a, b, c);
    }

    private static SupportBean_IntAlphabetic intEvent(int a, int b, int c, int d, int e) {
        return new SupportBean_IntAlphabetic(a, b, c, d, e);
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        SupportBean event = new SupportBean(theString, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        event.setDoublePrimitive(doublePrimitive);
        return event;
    }

    private static SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive,
                                         boolean boolPrimitive, int intBoxed, long longBoxed, double doubleBoxed) {
        SupportBean event = new SupportBean(theString, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        event.setDoublePrimitive(doublePrimitive);
        event.setBoolPrimitive(boolPrimitive);
        event.setLongBoxed(longBoxed);
        event.setDoubleBoxed(doubleBoxed);
        event.setIntBoxed(intBoxed);
        return event;
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
