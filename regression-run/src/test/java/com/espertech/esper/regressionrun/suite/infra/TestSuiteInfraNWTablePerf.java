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
import com.espertech.esper.regressionlib.suite.infra.nwtable.InfraNWTableFAFIndexPerfWNoQueryPlanLog;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteInfraNWTablePerf extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testInfraNWTableFAFIndexPerfWNoQueryPlanLog() {
        RegressionRunner.run(session, InfraNWTableFAFIndexPerfWNoQueryPlanLog.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class}) {
            configuration.getCommon().addEventType(clazz);
        }
        configuration.getCompiler().addPlugInSingleRowFunction("justCount", InfraNWTableFAFIndexPerfWNoQueryPlanLog.InvocationCounter.class.getName(), "justCount");
    }
}
