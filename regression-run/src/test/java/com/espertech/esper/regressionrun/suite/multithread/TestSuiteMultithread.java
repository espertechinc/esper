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
package com.espertech.esper.regressionrun.suite.multithread;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.multithread.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportIntListAggregationForge;
import com.espertech.esper.regressionlib.support.util.SupportDatabaseService;
import com.espertech.esper.regressionlib.support.wordexample.SentenceEvent;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.Properties;

public class TestSuiteMultithread extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testMultithreadViewTimeWindow() {
        RegressionRunner.run(session, new MultithreadViewTimeWindow());
    }

    public void testMultithreadedViewTimeWindowSceneTwo() {
        RegressionRunner.run(session, new MultithreadViewTimeWindowSceneTwo());
    }

    public void testMultithreadNamedWindowDelete() {
        RegressionRunner.run(session, new MultithreadNamedWindowDelete());
    }

    public void testMultithreadContextCountSimple() {
        RegressionRunner.run(session, new MultithreadContextCountSimple());
    }

    public void testMultithreadContextInitiatedTerminatedWithNowParallel() {
        RegressionRunner.run(session, new MultithreadContextInitiatedTerminatedWithNowParallel());
    }

    public void testMultithreadContextPartitioned() {
        RegressionRunner.run(session, new MultithreadContextPartitioned());
    }

    public void testMultithreadMultithreadContextPartitionedWCount() {
        RegressionRunner.run(session, new MultithreadContextPartitionedWCount());
    }

    public void testMultithreadContextTemporalStartStop() {
        RegressionRunner.run(session, new MultithreadContextTemporalStartStop());
    }

    public void testMultithreadContextUnique() {
        RegressionRunner.run(session, new MultithreadContextUnique());
    }

    public void testMultithreadDeployAtomic() {
        RegressionRunner.run(session, new MultithreadDeployAtomic());
    }

    public void testMultithreadDeterminismInsertInto() {
        RegressionRunner.run(session, new MultithreadDeterminismInsertInto());
    }

    public void testMultithreadStmtDatabaseJoin() {
        RegressionRunner.run(session, new MultithreadStmtDatabaseJoin());
    }

    public void testMultithreadStmtFilter() {
        RegressionRunner.run(session, new MultithreadStmtFilter());
    }

    public void testMultithreadStmtFilterSubquery() {
        RegressionRunner.run(session, new MultithreadStmtFilterSubquery());
    }

    public void testMultithreadStmtInsertInto() {
        RegressionRunner.run(session, new MultithreadStmtInsertInto());
    }

    public void testMultithreadStmtIterate() {
        RegressionRunner.run(session, new MultithreadStmtIterate());
    }

    public void testMultithreadStmtJoin() {
        RegressionRunner.run(session, new MultithreadStmtJoin());
    }

    public void testMultithreadStmtListenerCreateStmt() {
        RegressionRunner.run(session, new MultithreadStmtListenerCreateStmt());
    }

    public void testMultithreadStmtListenerRoute() {
        RegressionRunner.run(session, new MultithreadStmtListenerRoute());
    }

    public void testMultithreadStmtMgmt() {
        RegressionRunner.run(session, new MultithreadStmtMgmt());
    }

    public void testMultithreadStmtNamedWindowConsume() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowConsume());
    }

    public void testMultithreadStmtNamedWindowDelete() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowDelete());
    }

    public void testMultithreadStmtNamedWindowFAF() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowFAF());
    }

    public void testMultithreadStmtNamedWindowIterate() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowIterate());
    }

    public void testMultithreadStmtNamedWindowJoinUniqueView() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowJoinUniqueView());
    }

    public void testMultithreadStmtNamedWindowMerge() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowMerge());
    }

    public void testMultithreadStmtNamedWindowMultiple() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowMultiple());
    }

    public void testMultithreadStmtNamedWindowSubqueryAgg() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowSubqueryAgg());
    }

    public void testMultithreadStmtNamedWindowSubqueryLookup() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowSubqueryLookup());
    }

    public void testMultithreadStmtNamedWindowUpdate() {
        RegressionRunner.run(session, new MultithreadStmtNamedWindowUpdate());
    }

    public void testMultithreadStmtPattern() {
        RegressionRunner.run(session, new MultithreadStmtPattern());
    }

    public void testMultithreadStmtStateless() {
        RegressionRunner.run(session, new MultithreadStmtStateless());
    }

    public void testMultithreadStmtStatelessEnummethod() {
        RegressionRunner.run(session, new MultithreadStmtStatelessEnummethod());
    }

    public void testMultithreadStmtSubquery() {
        RegressionRunner.run(session, new MultithreadStmtSubquery());
    }

    public void testMultithreadStmtTimeWindow() {
        RegressionRunner.run(session, new MultithreadStmtTimeWindow());
    }

    public void testMultithreadStmtTwoPatterns() {
        RegressionRunner.run(session, new MultithreadStmtTwoPatterns());
    }

    public void testMultithreadUpdate() {
        RegressionRunner.run(session, new MultithreadUpdate());
    }

    public void testMultithreadUpdateIStreamSubselect() {
        RegressionRunner.run(session, new MultithreadUpdateIStreamSubselect());
    }

    public void testMultithreadVariables() {
        RegressionRunner.run(session, new MultithreadVariables());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportMarketDataBean.class, SupportByteArrEventLongId.class,
            SupportBean_A.class, SupportBean_S0.class, SupportBean_S1.class, SupportCollection.class,
            MultithreadStmtNamedWindowJoinUniqueView.MyEventA.class,
            MultithreadStmtNamedWindowJoinUniqueView.MyEventB.class,
            MultithreadStmtNamedWindowMultiple.OrderEvent.class,
            MultithreadStmtNamedWindowMultiple.OrderCancelEvent.class,
            SentenceEvent.class, SupportTradeEvent.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        ConfigurationCommonDBRef configDB = new ConfigurationCommonDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionTransactionIsolation(1);
        configDB.setConnectionAutoCommit(true);
        configuration.getCommon().addDatabaseReference("MyDB", configDB);

        ConfigurationCommon common = configuration.getCommon();
        common.addVariable("var1", Long.class, 0);
        common.addVariable("var2", Long.class, 0);
        common.addVariable("var3", Long.class, 0);

        configuration.getCompiler().addPlugInAggregationFunctionForge("intListAgg", SupportIntListAggregationForge.class.getName());
    }
}
