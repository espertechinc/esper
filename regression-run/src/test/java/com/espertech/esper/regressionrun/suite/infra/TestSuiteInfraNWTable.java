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
package com.espertech.esper.regressionrun.suite.infra;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.infra.nwtable.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.bookexample.OrderBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteInfraNWTable extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testInfraNWTableFAF() {
        RegressionRunner.run(session, InfraNWTableFAF.executions());
    }

    public void testInfraNWTableFAFSubstitutionParams() {
        RegressionRunner.run(session, InfraNWTableFAFSubstitutionParams.executions());
    }

    public void testInfraNWTableFAFResolve() {
        RegressionRunner.run(session, InfraNWTableFAFResolve.executions());
    }

    public void testInfraNWTableInfraOnDelete() {
        RegressionRunner.run(session, InfraNWTableOnDelete.executions());
    }

    public void testInfraNWTableInfraOnSelect() {
        RegressionRunner.run(session, InfraNWTableOnSelect.executions());
    }

    public void testInfraNWTableInfraOnMerge() {
        RegressionRunner.run(session, InfraNWTableOnMerge.executions());
    }

    public void testInfraNWTableInfraOnUpdate() {
        RegressionRunner.run(session, InfraNWTableOnUpdate.executions());
    }

    public void testInfraNWTableOnSelectWDelete() {
        RegressionRunner.run(session, InfraNWTableOnSelectWDelete.executions());
    }

    public void testInfraNWTableInfraComparative() {
        RegressionRunner.run(session, InfraNWTableComparative.executions());
    }

    public void testInfraNWTableInfraContext() {
        RegressionRunner.run(session, InfraNWTableContext.executions());
    }

    public void testInfraNWTableInfraSubquery() {
        RegressionRunner.run(session, InfraNWTableSubquery.executions());
    }

    public void testInfraNWTableInfraSubqCorrelJoin() {
        RegressionRunner.run(session, InfraNWTableSubqCorrelJoin.executions());
    }

    public void testInfraNWTableInfraSubqUncorrel() {
        RegressionRunner.run(session, InfraNWTableSubqUncorrel.executions());
    }

    public void testInfraNWTableInfraSubqueryAtEventBean() {
        RegressionRunner.run(session, InfraNWTableSubqueryAtEventBean.executions());
    }

    public void testInfraNWTableInfraSubqCorrelIndex() {
        RegressionRunner.run(session, InfraNWTableSubqCorrelIndex.executions());
    }

    public void testInfraNWTableInfraSubqCorrelCoerce() {
        RegressionRunner.run(session, InfraNWTableSubqCorrelCoerce.executions());
    }

    public void testInfraNWTableInfraCreateIndex() {
        RegressionRunner.run(session, InfraNWTableCreateIndex.executions());
    }

    public void testInfraNWTableInfraEventType() {
        RegressionRunner.run(session, InfraNWTableEventType.executions());
    }

    public void testInfraNWTableInfraFAFIndex() {
        RegressionRunner.run(session, InfraNWTableFAFIndex.executions());
    }

    public void testInfraNWTableInfraCreateIndexAdvancedSyntax() {
        RegressionRunner.run(session, new InfraNWTableCreateIndexAdvancedSyntax());
    }

    public void testInfraNWTableInfraOnMergePerf() {
        RegressionRunner.run(session, InfraNWTableOnMergePerf.executions());
    }

    public void testInfraNWTableInfraStartStop() {
        RegressionRunner.run(session, InfraNWTableStartStop.executions());
    }

    public void testInfraNWTableJoin() {
        RegressionRunner.run(session, InfraNWTableJoin.executions());
    }

    public void testInfraNWTableFAFSubquery() {
        RegressionRunner.run(session, InfraNWTableFAFSubquery.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class, SupportBean_A.class,
            SupportBean_B.class, SupportBeanRange.class, SupportSimpleBeanOne.class, SupportSimpleBeanTwo.class,
            SupportBean_ST0.class, SupportSpatialPoint.class, SupportMarketDataBean.class, SupportBean_Container.class,
            OrderBean.class, SupportEventWithIntArray.class, SupportEventWithManyArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCommon().getLogging().setEnableQueryPlan(true);

        configuration.getCompiler().addPlugInSingleRowFunction("doubleInt", InfraNWTableFAF.class.getName(), "doubleInt");
        configuration.getCompiler().addPlugInSingleRowFunction("justCount", InfraNWTableFAFIndexPerfWNoQueryPlanLog.InvocationCounter.class.getName(), "justCount");
        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
    }
}
