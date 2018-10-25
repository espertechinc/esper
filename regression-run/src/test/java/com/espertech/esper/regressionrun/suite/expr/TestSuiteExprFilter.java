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
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.suite.expr.filter.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestSuiteExprFilter extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testExprFilterWhereClauseNoDataWindowPerformance() {
        RegressionRunner.run(session, new ExprFilterWhereClauseNoDataWindowPerformance());
    }

    public void testExprFilterOptimizable() {
        RegressionRunner.run(session, ExprFilterOptimizable.executions());
    }

    public void testExprFilterExpressions() {
        RegressionRunner.run(session, ExprFilterExpressions.executions());
    }

    public void testExprFilterInAndBetween() {
        RegressionRunner.run(session, ExprFilterInAndBetween.executions());
    }

    public void testExprFilterPlanInRangeAndBetween() {
        RegressionRunner.run(session, ExprFilterPlanInRangeAndBetween.executions());
    }

    public void testExprFilterPlanNoFilter() {
        RegressionRunner.run(session, ExprFilterPlanNoFilter.executions());
    }

    public void testExprFilterPlanOneFilterNonNested() {
        RegressionRunner.run(session, ExprFilterPlanOneFilterNonNested.executions());
    }

    public void testExprFilterPlanOneFilterNestedTwoLvl() {
        RegressionRunner.run(session, ExprFilterPlanOneFilterNestedTwoLvl.executions());
    }

    public void testExprFilterPlanOneFilterTwoPathNested() {
        RegressionRunner.run(session, ExprFilterPlanOneFilterTwoPathNested.executions());
    }

    public void testExprFilterPlanOneFilterNestedThreeLvl() {
        RegressionRunner.run(session, ExprFilterPlanOneFilterNestedThreeLvl.executions());
    }

    public void testExprFilterPlanOneFilterNestedFourLvl() {
        RegressionRunner.run(session, ExprFilterPlanOneFilterNestedFourLvl.executions());
    }

    public void testExprFilterPlanOneFilterTwoPathNonNested() {
        RegressionRunner.run(session, ExprFilterPlanOneFilterTwoPathNonNested.executions());
    }

    public void testExprFilterPlanTwoFilterNestedTwoDiff() {
        RegressionRunner.run(session, ExprFilterPlanTwoFilterNestedTwoDiff.executions());
    }

    public void testExprFilterPlanTwoFilterSame() {
        RegressionRunner.run(session, ExprFilterPlanTwoFilterSame.executions());
    }

    public void testExprFilterPlanTwoFilterIndexWFilterForValueReuse() {
        RegressionRunner.run(session, ExprFilterPlanTwoFilterIndexWFilterForValueReuse.executions());
    }

    public void testExprFilterPlanTwoFilterIndexReuse() {
        RegressionRunner.run(session, ExprFilterPlanTwoFilterIndexReuse.executions());
    }

    public void testExprFilterPlanTwoFilterDifferent() {
        RegressionRunner.run(session, ExprFilterPlanTwoFilterDifferent.executions());
    }

    public void testExprFilterPlanTwoFilterTwoPathNestedSame() {
        RegressionRunner.run(session, ExprFilterPlanTwoFilterTwoPathNestedSame.executions());
    }

    public void testExprFilterPlanTwoFilterNestedTwoSame() {
        RegressionRunner.run(session, ExprFilterPlanTwoFilterNestedTwoSame.executions());
    }

    public void testExprFilterPlanThreeFilterIndexReuse() {
        RegressionRunner.run(session, ExprFilterPlanThreeFilterIndexReuse.executions());
    }

    public void testExprFilterWhereClause() {
        RegressionRunner.run(session, ExprFilterWhereClause.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBeanArrayCollMap.class, SupportTradeEvent.class,
            SupportInstanceMethodBean.class, SupportRuntimeExBean.class, SupportBeanWithEnum.class,
            SupportBeanComplexProps.class, SupportMarketDataBean.class, SupportBeanNumeric.class, SupportBean_S0.class,
            SupportInKeywordBean.class, SupportOverrideBase.class, SupportOverrideOne.class,
            SupportBean_IntAlphabetic.class, SupportBean_StringAlphabetic.class}) {
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
    }
}
