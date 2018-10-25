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
import com.espertech.esper.regressionlib.suite.infra.namedwindow.InfraNamedWIndowFAFQueryJoinPerformance;
import com.espertech.esper.regressionlib.suite.infra.namedwindow.InfraNamedWindowPerformance;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

// see INFRA suite for additional Named Window tests
public class TestSuiteInfraNamedWindowPerf extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testInfraNamedWindowPerformance() {
        RegressionRunner.run(session, InfraNamedWindowPerformance.executions());
    }

    public void testInfraNamedWIndowFAFQueryJoinPerformance() {
        RegressionRunner.run(session, new InfraNamedWIndowFAFQueryJoinPerformance());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportBeanRange.class, SupportBean_A.class, SupportMarketDataBean.class,
            SupportSimpleBeanTwo.class, SupportSimpleBeanOne.class}) {
            configuration.getCommon().addEventType(clazz);
        }
    }
}
