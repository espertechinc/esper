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
package com.espertech.esper.regressionrun.suite.epl;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.suite.epl.subselect.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLSubselect extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLSubselectUnfiltered() {
        RegressionRunner.run(session, EPLSubselectUnfiltered.executions());
    }

    public void testEPLSubselectExists() {
        RegressionRunner.run(session, EPLSubselectExists.executions());
    }

    public void testEPLSubselectAllAnySomeExpr() {
        RegressionRunner.run(session, EPLSubselectAllAnySomeExpr.executions());
    }

    public void testEPLSubselectIn() {
        RegressionRunner.run(session, EPLSubselectIn.executions());
    }

    public void testEPLSubselectFiltered() {
        RegressionRunner.run(session, EPLSubselectFiltered.executions());
    }

    public void testEPLSubselectOrderOfEval() {
        RegressionRunner.run(session, EPLSubselectOrderOfEval.executions());
    }

    public void testEPLSubselectFilteredPerformance() {
        RegressionRunner.run(session, EPLSubselectFilteredPerformance.executions());
    }

    public void testEPLSubselectIndex() {
        RegressionRunner.run(session, EPLSubselectIndex.executions());
    }

    public void testEPLSubselectInKeywordPerformance() {
        RegressionRunner.run(session, EPLSubselectInKeywordPerformance.executions());
    }

    public void testEPLSubselectAggregatedSingleValue() {
        RegressionRunner.run(session, EPLSubselectAggregatedSingleValue.executions());
    }

    public void testEPLSubselectAggregatedInExistsAnyAll() {
        RegressionRunner.run(session, EPLSubselectAggregatedInExistsAnyAll.executions());
    }

    public void testEPLSubselectMulticolumn() {
        RegressionRunner.run(session, EPLSubselectMulticolumn.executions());
    }

    public void testEPLSubselectMultirow() {
        RegressionRunner.run(session, EPLSubselectMultirow.executions());
    }

    public void testEPLSubselectAggregatedMultirowAndColumn() {
        RegressionRunner.run(session, EPLSubselectAggregatedMultirowAndColumn.executions());
    }

    public void testEPLSubselectCorrelatedAggregationPerformance() {
        RegressionRunner.run(session, new EPLSubselectCorrelatedAggregationPerformance());
    }

    public void testEPLSubselectNamedWindowPerformance() {
        RegressionRunner.run(session, EPLSubselectNamedWindowPerformance.executions());
    }

    public void testEPLSubselectWithinHaving() {
        RegressionRunner.run(session, EPLSubselectWithinHaving.executions());
    }

    public void testEPLSubselectWithinPattern() {
        RegressionRunner.run(session, EPLSubselectWithinPattern.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportBean_S2.class, SupportBean_S3.class, SupportBean_S4.class,
            SupportValueEvent.class, SupportIdAndValueEvent.class, SupportBeanArrayCollMap.class,
            SupportSensorEvent.class, SupportBeanRange.class, SupportSimpleBeanOne.class, SupportSimpleBeanTwo.class,
            SupportBean_ST0.class, SupportBean_ST1.class, SupportBean_ST2.class, SupportTradeEventTwo.class,
            SupportMaxAmountEvent.class, SupportMarketDataBean.class, SupportEventWithIntArray.class,
            SupportEventWithManyArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCommon().getLogging().setEnableQueryPlan(true);

        configuration.getCompiler().addPlugInSingleRowFunction("supportSingleRowFunction", EPLSubselectWithinPattern.class.getName(), "supportSingleRowFunction");
    }
}