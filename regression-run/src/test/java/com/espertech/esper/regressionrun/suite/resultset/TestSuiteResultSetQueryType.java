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
package com.espertech.esper.regressionrun.suite.resultset;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.resultset.querytype.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportConcatWManagedAggregationFunctionForge;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFMultiRTForge;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteResultSetQueryType extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testResultSetQueryTypeRowForAll() {
        RegressionRunner.run(session, ResultSetQueryTypeRowForAll.executions());
    }

    public void testResultSetQueryTypeRowForAllHaving() {
        RegressionRunner.run(session, ResultSetQueryTypeRowForAllHaving.executions());
    }

    public void testResultSetQueryTypeRowPerEvent() {
        RegressionRunner.run(session, ResultSetQueryTypeRowPerEvent.executions());
    }

    public void testResultSetQueryTypeRowPerEventPerformance() {
        RegressionRunner.run(session, new ResultSetQueryTypeRowPerEventPerformance());
    }

    public void testResultSetQueryTypeRowPerGroup() {
        RegressionRunner.run(session, ResultSetQueryTypeRowPerGroup.executions());
    }

    public void testResultSetQueryTypeRowPerGroupHaving() {
        RegressionRunner.run(session, ResultSetQueryTypeRowPerGroupHaving.executions());
    }

    public void testResultSetQueryTypeAggregateGrouped() {
        RegressionRunner.run(session, ResultSetQueryTypeAggregateGrouped.executions());
    }

    public void testResultSetQueryTypeAggregateGroupedHaving() {
        RegressionRunner.run(session, ResultSetQueryTypeAggregateGroupedHaving.executions());
    }

    public void testResultSetQueryTypeGroupByReclaimMicrosecondResolution() {
        RegressionRunner.run(session, new ResultSetQueryTypeRowPerGroupReclaimMicrosecondResolution(5000));
    }

    public void testResultSetQueryTypeWTimeBatch() {
        RegressionRunner.run(session, ResultSetQueryTypeWTimeBatch.executions());
    }

    public void testResultSetQueryTypeIterator() {
        RegressionRunner.run(session, ResultSetQueryTypeIterator.executions());
    }

    public void testResultSetQueryTypeHaving() {
        RegressionRunner.run(session, ResultSetQueryTypeHaving.executions());
    }

    public void testResultSetQueryTypeRollupDimensionality() {
        RegressionRunner.run(session, ResultSetQueryTypeRollupDimensionality.executions());
    }

    public void testResultSetQueryTypeRollupGroupingFuncs() {
        RegressionRunner.run(session, ResultSetQueryTypeRollupGroupingFuncs.executions());
    }

    public void testResultSetQueryTypeRollupHavingAndOrderBy() {
        RegressionRunner.run(session, ResultSetQueryTypeRollupHavingAndOrderBy.executions());
    }

    public void testResultSetQueryTypeRollupPlanningAndSODA() {
        RegressionRunner.run(session, new ResultSetQueryTypeRollupPlanningAndSODA());
    }

    public void testResultSetQueryTypeLocalGroupBy() {
        RegressionRunner.run(session, ResultSetQueryTypeLocalGroupBy.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class, SupportMarketDataBean.class,
            SupportCarEvent.class, SupportCarInfoEvent.class, SupportEventABCProp.class, SupportBeanString.class,
            SupportPriceEvent.class, SupportMarketDataIDBean.class, SupportBean_A.class, SupportBean_B.class,
            SupportEventWithIntArray.class, SupportThreeArrayEvent.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCompiler().addPlugInSingleRowFunction("myfunc", ResultSetQueryTypeRollupGroupingFuncs.GroupingSupportFunc.class.getName(), "myfunc");

        configuration.getCompiler().addPlugInAggregationFunctionForge("concatstring", SupportConcatWManagedAggregationFunctionForge.class.getName());

        ConfigurationCompilerPlugInAggregationMultiFunction mfAggConfig = new ConfigurationCompilerPlugInAggregationMultiFunction("sc".split(","), SupportAggMFMultiRTForge.class.getName());
        configuration.getCompiler().addPlugInAggregationMultiFunction(mfAggConfig);

        configuration.getCommon().addVariable("MyVar", String.class, "");
    }
}
