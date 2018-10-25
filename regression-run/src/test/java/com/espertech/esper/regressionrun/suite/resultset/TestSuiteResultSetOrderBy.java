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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.resultset.orderby.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteResultSetOrderBy extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testResultSetOrderByRowPerEvent() {
        RegressionRunner.run(session, ResultSetOrderByRowPerEvent.executions());
    }

    public void testResultSetOrderByRowPerGroup() {
        RegressionRunner.run(session, ResultSetOrderByRowPerGroup.executions());
    }

    public void testResultSetOrderByAggregateGrouped() {
        RegressionRunner.run(session, ResultSetOrderByAggregateGrouped.executions());
    }

    public void testResultSetOrderByRowForAll() {
        RegressionRunner.run(session, ResultSetOrderByRowForAll.executions());
    }

    public void testResultSetOrderBySelfJoin() {
        RegressionRunner.run(session, ResultSetOrderBySelfJoin.executions());
    }

    public void testResultSetOrderBySimple() {
        RegressionRunner.run(session, ResultSetOrderBySimple.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_A.class, SupportBeanString.class, SupportMarketDataBean.class, SupportHierarchyEvent.class}) {
            configuration.getCommon().addEventType(clazz);
        }
    }
}
