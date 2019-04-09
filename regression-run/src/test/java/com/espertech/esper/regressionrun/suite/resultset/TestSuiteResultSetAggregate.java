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
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.resultset.aggregate.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportConcatWManagedAggregationFunctionForge;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFEventsAsListForge;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteResultSetAggregate extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testResultSetAggregateCountSum() {
        RegressionRunner.run(session, ResultSetAggregateCountSum.executions());
    }

    public void testResultSetAggregateLeaving() {
        RegressionRunner.run(session, new ResultSetAggregateLeaving());
    }

    public void testResultSetAggregateNTh() {
        RegressionRunner.run(session, new ResultSetAggregateNTh());
    }

    public void testResultSetAggregateRate() {
        RegressionRunner.run(session, ResultSetAggregateRate.executions());
    }

    public void testResultSetAggregateMedianAndDeviation() {
        RegressionRunner.run(session, ResultSetAggregateMedianAndDeviation.executions());
    }

    public void testResultSetAggregateMinMax() {
        RegressionRunner.run(session, ResultSetAggregateMinMax.executions());
    }

    public void testResultSetAggregateFirstEverLastEver() {
        RegressionRunner.run(session, ResultSetAggregateFirstEverLastEver.executions());
    }

    public void testResultSetAggregateMaxMinGroupBy() {
        RegressionRunner.run(session, ResultSetAggregateMaxMinGroupBy.executions());
    }

    public void testResultSetAggregateFiltered() {
        RegressionRunner.run(session, ResultSetAggregateFiltered.executions());
    }

    public void testResultSetAggregateFirstLastWindow() {
        RegressionRunner.run(session, ResultSetAggregateFirstLastWindow.executions());
    }

    public void testResultSetAggregateSortedMinMaxBy() {
        RegressionRunner.run(session, ResultSetAggregateSortedMinMaxBy.executions());
    }

    public void testResultSetAggregateFilterNamedParameter() {
        RegressionRunner.run(session, ResultSetAggregateFilterNamedParameter.executions());
    }

    public void testResultSetAggregationMethodSorted() {
        RegressionRunner.run(session, ResultSetAggregationMethodSorted.executions());
    }

    public void testResultSetAggregationMethodWindow() {
        RegressionRunner.run(session, ResultSetAggregationMethodWindow.executions());
    }

    private void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBeanString.class, SupportMarketDataBean.class,
            SupportBeanNumeric.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportBean_A.class, SupportBean_B.class, SupportEventPropertyWithMethod.class,
            SupportEventWithManyArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }
        configuration.getCommon().addImport(SupportStaticMethodLib.class.getName());
        configuration.getCommon().addImport(HashableMultiKey.class.getName());
        configuration.getCompiler().getByteCode().setIncludeDebugSymbols(true);

        configuration.getCompiler().addPlugInAggregationFunctionForge("concatMethodAgg", SupportConcatWManagedAggregationFunctionForge.class.getName());

        ConfigurationCompilerPlugInAggregationMultiFunction eventsAsList = new ConfigurationCompilerPlugInAggregationMultiFunction("eventsAsList".split(","), SupportAggMFEventsAsListForge.class.getName());
        configuration.getCompiler().addPlugInAggregationMultiFunction(eventsAsList);
    }
}