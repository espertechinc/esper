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
package com.espertech.esper.regressionrun.suite.expr;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerExecution.FilterIndexPlanning;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.common.client.util.ThreadingProfile;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecPlanForge;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamForge;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.suite.expr.filter.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.filter.SupportFilterPlanHook;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TestSuiteExprFilterWConfig extends TestCase {
    private final static String HOOK = "@Hook(type=HookType.INTERNAL_FILTERSPEC, hook='" + SupportFilterPlanHook.class.getName() + "')";

    public void testExprFilterLargeThreading() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCommon().addEventType(SupportTradeEvent.class);
        session.getConfiguration().getCommon().getExecution().setThreadingProfile(ThreadingProfile.LARGE);
        session.getConfiguration().getCompiler().getLogging().setEnableFilterPlan(true);
        RegressionRunner.run(session, new ExprFilterLargeThreading());
        session.destroy();
    }

    public void testExprFilterAdvancedPlanningDisable() {
        Configuration none = makeConfig(FilterIndexPlanning.NONE);
        Configuration basic = makeConfig(FilterIndexPlanning.BASIC);
        Configuration advanced = makeConfig(FilterIndexPlanning.ADVANCED);

        // composite-value-expression planning
        String hintValue = "@Hint('filterindex(valuecomposite)')";
        String eplValue = HOOK + "select * from SupportBean(theString = 'a' || 'b')";
        runAssertionBooleanExpression(none, eplValue, FilterOperator.BOOLEAN_EXPRESSION);
        runAssertionBooleanExpression(basic, eplValue, FilterOperator.BOOLEAN_EXPRESSION);
        runAssertionBooleanExpression(basic, hintValue + eplValue, FilterOperator.EQUAL);
        runAssertionBooleanExpression(advanced, eplValue, FilterOperator.EQUAL);

        // composite-lookup-expression planning
        String hintLookup = "@Hint('filterindex(lkupcomposite)')";
        String eplLookup = HOOK + "select * from SupportBean(theString || 'a' = 'b')";
        runAssertionBooleanExpression(none, eplLookup, FilterOperator.BOOLEAN_EXPRESSION);
        runAssertionBooleanExpression(basic, eplLookup, FilterOperator.BOOLEAN_EXPRESSION);
        runAssertionBooleanExpression(basic, hintLookup + eplLookup, FilterOperator.EQUAL);
        runAssertionBooleanExpression(advanced, eplLookup, FilterOperator.EQUAL);

        // no reusable-boolean planning
        String hintRebool = "@Hint('filterindex(boolcomposite)')";
        String eplRebool = HOOK + "select * from SupportBean(theString regexp 'a')";
        runAssertionBooleanExpression(none, eplRebool, FilterOperator.BOOLEAN_EXPRESSION);
        runAssertionBooleanExpression(basic, eplRebool, FilterOperator.BOOLEAN_EXPRESSION);
        runAssertionBooleanExpression(basic, hintRebool + eplRebool, FilterOperator.REBOOL);
        runAssertionBooleanExpression(advanced, eplRebool, FilterOperator.REBOOL);

        // conditions
        String hintCondition = "@Hint('filterindex(condition)')";
        String eplContext = "create context MyContext start SupportBean_S0 as s0;\n";
        String eplCondition = HOOK + "context MyContext select * from SupportBean(theString = 'a' or context.s0.p00 = 'x');\n";
        runAssertionBooleanExpression(none, eplContext + eplCondition, FilterOperator.BOOLEAN_EXPRESSION);
        assertEquals(2, compileGetPlan(basic, eplContext + eplCondition).getPaths().length);
        FilterSpecPlanForge planBasicWithHint = compileGetPlan(basic, eplContext + hintCondition + eplCondition);
        assertEquals(1, planBasicWithHint.getPaths().length);
        assertNotNull(planBasicWithHint.getFilterConfirm());
        FilterSpecPlanForge planAdvanced = compileGetPlan(advanced, eplContext + eplCondition);
        assertEquals(1, planAdvanced.getPaths().length);
        assertNotNull(planAdvanced.getFilterConfirm());
    }

    public void testExprFilterOptimizableConditionNegateConfirmNone() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterOptimizableConditionNegateConfirm.executions());
    }

    public void testExprFilterOptimizableConditionNegateConfirmBasic() {
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterOptimizableConditionNegateConfirm.executions());
    }

    public void testExprFilterOptimizableConditionNegateConfirmAdvanced() {
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterOptimizableConditionNegateConfirm.executions());
    }

    public void testExprFilterOptimizableLookupableLimitedExprNone() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterOptimizableLookupableLimitedExpr.executions());
    }

    public void testExprFilterOptimizableLookupableLimitedExprBasic() {
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterOptimizableLookupableLimitedExpr.executions());
    }

    public void testExprFilterOptimizableLookupableLimitedExprAdvanced() {
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterOptimizableLookupableLimitedExpr.executions());
    }

    public void testExprFilterOptimizableBooleanLimitedExprNone() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterOptimizableBooleanLimitedExpr.executions());
    }

    public void testExprFilterOptimizableBooleanLimitedExprBasic() {
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterOptimizableBooleanLimitedExpr.executions());
    }

    public void testExprFilterOptimizableBooleanLimitedExprAdvanced() {
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterOptimizableBooleanLimitedExpr.executions());
    }

    public void testExprFilterOptimizableValueLimitedExprNone() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterOptimizableValueLimitedExpr.executions());
    }

    public void testExprFilterOptimizableValueLimitedExprBasic() {
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterOptimizableValueLimitedExpr.executions());
    }

    public void testExprFilterOptimizableValueLimitedExprAdvanced() {
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterOptimizableValueLimitedExpr.executions());
    }

    public void testExprFilterWhereClauseNoDataWindowPerformance() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterWhereClauseNoDataWindowPerformance.executions());
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterWhereClauseNoDataWindowPerformance.executions());
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterWhereClauseNoDataWindowPerformance.executions());
    }

    public void testExprFilterOptimizable() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterOptimizable.executions());
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterOptimizable.executions());
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterOptimizable.executions());
    }

    public void testExprFilterOptimizablePerf() {
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterOptimizablePerf.executions());
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterOptimizablePerf.executions());
    }

    public void testExprFilterOptimizableOrRewrite() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterOptimizableOrRewrite.executions());
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterOptimizableOrRewrite.executions());
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterOptimizableOrRewrite.executions());
    }

    public void testExprFilterOptimizableValueLimitedExpr() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterOptimizableValueLimitedExpr.executions());
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterOptimizableValueLimitedExpr.executions());
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterOptimizableValueLimitedExpr.executions());
    }

    public void testExprFilterExpressions() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterExpressions.executions());
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterExpressions.executions());
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterExpressions.executions());
    }

    public void testExprFilterInAndBetween() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterInAndBetween.executions());
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterInAndBetween.executions());
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterInAndBetween.executions());
    }

    public void testExprFilterPlanInRangeAndBetween() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanInRangeAndBetween.executions());
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanInRangeAndBetween.executions());
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanInRangeAndBetween.executions());
    }

    public void testExprFilterPlanNoFilter() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanNoFilter.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanNoFilter.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanNoFilter.executions(false));
    }

    public void testExprFilterPlanOneFilterNonNested() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanOneFilterNonNested.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanOneFilterNonNested.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanOneFilterNonNested.executions(false));
    }

    public void testExprFilterPlanOneFilterNestedTwoLvl() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanOneFilterNestedTwoLvl.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanOneFilterNestedTwoLvl.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanOneFilterNestedTwoLvl.executions(false));
    }

    public void testExprFilterPlanOneFilterTwoPathNested() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanOneFilterTwoPathNested.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanOneFilterTwoPathNested.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanOneFilterTwoPathNested.executions(false));
    }

    public void testExprFilterPlanOneFilterNestedThreeLvl() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanOneFilterNestedThreeLvl.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanOneFilterNestedThreeLvl.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanOneFilterNestedThreeLvl.executions(false));
    }

    public void testExprFilterPlanOneFilterNestedFourLvl() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanOneFilterNestedFourLvl.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanOneFilterNestedFourLvl.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanOneFilterNestedFourLvl.executions(false));
    }

    public void testExprFilterPlanOneFilterTwoPathNonNested() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanOneFilterTwoPathNonNested.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanOneFilterTwoPathNonNested.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanOneFilterTwoPathNonNested.executions(false));
    }

    public void testExprFilterPlanTwoFilterNestedTwoDiff() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanTwoFilterNestedTwoDiff.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanTwoFilterNestedTwoDiff.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanTwoFilterNestedTwoDiff.executions(false));
    }

    public void testExprFilterPlanTwoFilterSame() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanTwoFilterSame.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanTwoFilterSame.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanTwoFilterSame.executions(false));
    }

    public void testExprFilterPlanTwoFilterIndexWFilterForValueReuse() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanTwoFilterIndexWFilterForValueReuse.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanTwoFilterIndexWFilterForValueReuse.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanTwoFilterIndexWFilterForValueReuse.executions(false));
    }

    public void testExprFilterPlanTwoFilterIndexReuse() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanTwoFilterIndexReuse.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanTwoFilterIndexReuse.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanTwoFilterIndexReuse.executions(false));
    }

    public void testExprFilterPlanTwoFilterDifferent() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanTwoFilterDifferent.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanTwoFilterDifferent.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanTwoFilterDifferent.executions(false));
    }

    public void testExprFilterPlanTwoFilterTwoPathNestedSame() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanTwoFilterTwoPathNestedSame.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanTwoFilterTwoPathNestedSame.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanTwoFilterTwoPathNestedSame.executions(false));
    }

    public void testExprFilterPlanTwoFilterNestedTwoSame() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanTwoFilterNestedTwoSame.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanTwoFilterNestedTwoSame.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanTwoFilterNestedTwoSame.executions(false));
    }

    public void testExprFilterPlanThreeFilterIndexReuse() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterPlanThreeFilterIndexReuse.executions(false));
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterPlanThreeFilterIndexReuse.executions(true));
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterPlanThreeFilterIndexReuse.executions(false));
    }

    public void testExprFilterWhereClause() {
        runAssertionFilter(FilterIndexPlanning.NONE, ExprFilterWhereClause.executions());
        runAssertionFilter(FilterIndexPlanning.BASIC, ExprFilterWhereClause.executions());
        runAssertionFilter(FilterIndexPlanning.ADVANCED, ExprFilterWhereClause.executions());
    }

    private void runAssertionFilter(FilterIndexPlanning config, Collection<? extends RegressionExecution> executions) {
        RegressionSession session = RegressionRunner.session();
        configure(session.getConfiguration());
        session.getConfiguration().getCompiler().getExecution().setFilterIndexPlanning(config);
        RegressionRunner.run(session, executions);
        session.destroy();
    }

    private void runAssertionBooleanExpression(Configuration configuration, String epl, FilterOperator expected) {
        SupportFilterPlanHook.reset();
        try {
            EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments(configuration));
        } catch (EPCompileException e) {
            throw new RuntimeException(e);
        }
        FilterSpecParamForge forge = SupportFilterPlanHook.assertPlanSingleTripletAndReset("SupportBean");
        assertEquals(expected, forge.getFilterOperator());
    }

    private FilterSpecPlanForge compileGetPlan(Configuration configuration, String epl) {
        SupportFilterPlanHook.reset();
        try {
            EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments(configuration));
        } catch (EPCompileException e) {
            throw new RuntimeException(e);
        }
        return SupportFilterPlanHook.assertPlanSingleAndReset().getPlan();
    }

    private Configuration makeConfig(FilterIndexPlanning setting) {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        for (Class bean : new Class[] {SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            configuration.getCommon().addEventType(bean);
        }
        configuration.getCompiler().getExecution().setFilterIndexPlanning(setting);
        configuration.getCompiler().getLogging().setEnableFilterPlan(true);
        return configuration;
    }

    protected static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBeanArrayCollMap.class, SupportTradeEvent.class,
            SupportInstanceMethodBean.class, SupportRuntimeExBean.class, SupportBeanWithEnum.class,
            SupportBeanComplexProps.class, SupportMarketDataBean.class, SupportBeanNumeric.class, SupportBean_S0.class,
            SupportInKeywordBean.class, SupportOverrideBase.class, SupportOverrideOne.class,
            SupportBean_IntAlphabetic.class, SupportBean_StringAlphabetic.class, SupportBean_S1.class, SupportBean_S2.class,
            SupportBeanSimpleNumber.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        Map<String, Object> dict = new HashMap<String, Object>();
        dict.put("criteria", Boolean.class);
        configuration.getCommon().addEventType("MapEventWithCriteriaBool", dict);

        configuration.getCommon().addVariable("myCheckServiceProvider", ExprFilterOptimizable.MyCheckServiceProvider.class, null);
        configuration.getCommon().addVariable("var_optimizable_equals", String.class, "abc", true);
        configuration.getCommon().addVariable("var_optimizable_relop", int.class, 10, true);
        configuration.getCommon().addVariable("var_optimizable_start", int.class, 10, true);
        configuration.getCommon().addVariable("var_optimizable_end", int.class, 11, true);
        configuration.getCommon().addVariable("var_optimizable_array", "int[]", new Integer[]{10, 11}, true);
        configuration.getCommon().addVariable("var_optimizable_start_string", String.class, "c", true);
        configuration.getCommon().addVariable("var_optimizable_end_string", String.class, "d", true);

        configuration.getCommon().addImport(SupportStaticMethodLib.class);
        configuration.getCommon().addImport(DefaultSupportCaptureOp.class.getPackage().getName() + ".*");

        configuration.getCompiler().addPlugInSingleRowFunction("libSplit", SupportStaticMethodLib.class.getName(), "libSplit", ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        configuration.getCompiler().addPlugInSingleRowFunction("funcOne", SupportStaticMethodLib.class.getName(), "libSplit", ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.DISABLED);
        configuration.getCompiler().addPlugInSingleRowFunction("funcOneWDefault", SupportStaticMethodLib.class.getName(), "libSplit");
        configuration.getCompiler().addPlugInSingleRowFunction("funcTwo", SupportStaticMethodLib.class.getName(), "libSplit", ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        configuration.getCompiler().addPlugInSingleRowFunction("libE1True", SupportStaticMethodLib.class.getName(), "libE1True", ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        configuration.getCompiler().addPlugInSingleRowFunction("myCustomBigDecimalEquals", ExprFilterOptimizable.class.getName(), "myCustomBigDecimalEquals");

        ConfigurationCompilerPlugInSingleRowFunction func = new ConfigurationCompilerPlugInSingleRowFunction();
        func.setFunctionClassName(ExprFilterOptimizable.class.getName());
        func.setFunctionMethodName("myCustomOkFunction");
        func.setFilterOptimizable(ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        func.setRethrowExceptions(true);
        func.setName("myCustomOkFunction");
        configuration.getCompiler().getPlugInSingleRowFunctions().add(func);

        configuration.getCompiler().addPlugInSingleRowFunction("getLocalValue", ExprFilterPlanOneFilterNonNested.class.getName(), "getLocalValue");

        configuration.getCompiler().getLogging().setEnableFilterPlan(true);
    }
}
