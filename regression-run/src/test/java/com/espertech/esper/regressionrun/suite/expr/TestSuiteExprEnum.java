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
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompiler;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.expr.enummethod.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.bookexample.BookDesc;
import com.espertech.esper.regressionlib.support.lrreport.*;
import com.espertech.esper.regressionlib.support.sales.PersonSales;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteExprEnum extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testExprEnumAggregate() {
        RegressionRunner.run(session, ExprEnumAggregate.executions());
    }

    public void testExprEnumAllOfAnyOf() {
        RegressionRunner.run(session, ExprEnumAllOfAnyOf.executions());
    }

    public void testExprEnumAverage() {
        RegressionRunner.run(session, ExprEnumAverage.executions());
    }

    public void testExprEnumChained() {
        RegressionRunner.run(session, new ExprEnumChained());
    }

    public void testExprEnumCountOf() {
        RegressionRunner.run(session, ExprEnumCountOf.executions());
    }

    public void testExprEnumDataSources() {
        RegressionRunner.run(session, ExprEnumDataSources.executions());
    }

    public void testExprEnumDistinct() {
        RegressionRunner.run(session, ExprEnumDistinct.executions());
    }

    public void testExprEnumDocSamples() {
        RegressionRunner.run(session, ExprEnumDocSamples.executions());
    }

    public void testExprEnumExceptIntersectUnion() {
        RegressionRunner.run(session, ExprEnumExceptIntersectUnion.executions());
    }

    public void testExprEnumFirstLastOf() {
        RegressionRunner.run(session, ExprEnumFirstLastOf.executions());
    }

    public void testExprEnumGroupBy() {
        RegressionRunner.run(session, ExprEnumGroupBy.executions());
    }

    public void testExprEnumInvalid() {
        RegressionRunner.run(session, new ExprEnumInvalid());
    }

    public void testExprEnumMinMax() {
        RegressionRunner.run(session, ExprEnumMinMax.executions());
    }

    public void testExprEnumMinMaxBy() {
        RegressionRunner.run(session, new ExprEnumMinMaxBy());
    }

    public void testExprEnumMostLeastFrequent() {
        RegressionRunner.run(session, ExprEnumMostLeastFrequent.executions());
    }

    public void testExprEnumNamedWindowPerformance() {
        RegressionRunner.run(session, new ExprEnumNamedWindowPerformance());
    }

    public void testExprEnumNested() {
        RegressionRunner.run(session, ExprEnumNested.executions());
    }

    public void testExprEnumNestedPerformance() {
        RegressionRunner.run(session, new ExprEnumNestedPerformance());
    }

    public void testExprEnumOrderBy() {
        RegressionRunner.run(session, ExprEnumOrderBy.executions());
    }

    public void testExprEnumReverse() {
        RegressionRunner.run(session, ExprEnumReverse.executions());
    }

    public void testExprEnumSelectFrom() {
        RegressionRunner.run(session, ExprEnumSelectFrom.executions());
    }

    public void testExprEnumSequenceEqual() {
        RegressionRunner.run(session, ExprEnumSequenceEqual.executions());
    }

    public void testExprEnumSumOf() {
        RegressionRunner.run(session, ExprEnumSumOf.executions());
    }

    public void testExprEnumTakeAndTakeLast() {
        RegressionRunner.run(session, ExprEnumTakeAndTakeLast.executions());
    }

    public void testExprEnumTakeWhileAndWhileLast() {
        RegressionRunner.run(session, ExprEnumTakeWhileAndWhileLast.executions());
    }

    public void testExprEnumToMap() {
        RegressionRunner.run(session, new ExprEnumToMap());
    }

    public void testExprEnumWhere() {
        RegressionRunner.run(session, ExprEnumWhere.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean_ST0_Container.class, SupportBean.class, SupportBean_ST0_Container.class,
            SupportCollection.class, PersonSales.class, SupportBean_A.class,
            SupportBean_ST0.class, SupportSelectorWithListEvent.class, SupportEnumTwoEvent.class,
            SupportSelectorEvent.class, SupportContainerEvent.class,
            Item.class, LocationReport.class, Zone.class, SupportBeanComplexProps.class,
            SupportEventWithLongArray.class, SupportContainerLevelEvent.class, SupportSelectorWithListEvent.class,
            SupportBean_Container.class, SupportContainerLevel1Event.class, BookDesc.class, SupportEventWithManyArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCommon().addImport(SupportEnumTwo.class);
        configuration.getCommon().addImport(SupportBean_ST0_Container.class);
        configuration.getCommon().addImport(LocationReportFactory.class);
        configuration.getCommon().addImport(SupportCollection.class);
        configuration.getCommon().addImport(ZoneFactory.class);
        configuration.getCompiler().getExpression().setUdfCache(false);

        ConfigurationCompiler configurationCompiler = configuration.getCompiler();
        configurationCompiler.addPlugInSingleRowFunction("makeSampleList", SupportBean_ST0_Container.class.getName(), "makeSampleList");
        configurationCompiler.addPlugInSingleRowFunction("makeSampleArray", SupportBean_ST0_Container.class.getName(), "makeSampleArray");
        configurationCompiler.addPlugInSingleRowFunction("makeSampleListString", SupportCollection.class.getName(), "makeSampleListString");
        configurationCompiler.addPlugInSingleRowFunction("makeSampleArrayString", SupportCollection.class.getName(), "makeSampleArrayString");
        configurationCompiler.addPlugInSingleRowFunction("convertToArray", SupportSelectorWithListEvent.class.getName(), "convertToArray");
        configurationCompiler.addPlugInSingleRowFunction("extractAfterUnderscore", ExprEnumGroupBy.class.getName(), "extractAfterUnderscore");
        configurationCompiler.addPlugInSingleRowFunction("extractNum", ExprEnumMinMax.MyService.class.getName(), "extractNum");
        configurationCompiler.addPlugInSingleRowFunction("extractBigDecimal", ExprEnumMinMax.MyService.class.getName(), "extractBigDecimal");
        configurationCompiler.addPlugInSingleRowFunction("inrect", LRUtil.class.getName(), "inrect");
        configurationCompiler.addPlugInSingleRowFunction("distance", LRUtil.class.getName(), "distance");
        configurationCompiler.addPlugInSingleRowFunction("getZoneNames", Zone.class.getName(), "getZoneNames");
        configurationCompiler.addPlugInSingleRowFunction("makeTest", SupportBean_ST0_Container.class.getName(), "makeTest");
    }
}