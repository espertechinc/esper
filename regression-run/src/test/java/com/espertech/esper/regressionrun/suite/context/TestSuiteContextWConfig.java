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
package com.espertech.esper.regressionrun.suite.context;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.suite.context.ContextInitTermPrioritized;
import com.espertech.esper.regressionlib.suite.context.ContextKeySegmentedPrioritized;
import com.espertech.esper.regressionlib.suite.context.ContextKeySegmentedWInitTermPrioritized;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteContextWConfig extends TestCase {
    public void testContextKeySegmentedPrioritized() {
        RegressionSession session = RegressionRunner.session();
        configurePrioritized(session.getConfiguration());
        RegressionRunner.run(session, new ContextKeySegmentedPrioritized());
        session.destroy();
    }

    public void testContextKeySegmentedWInitTermPrioritized() {
        RegressionSession session = RegressionRunner.session();
        configurePrioritized(session.getConfiguration());
        RegressionRunner.run(session, ContextKeySegmentedWInitTermPrioritized.executions());
        session.destroy();
    }

    public void testContextInitTermPrioritized() {
        RegressionSession session = RegressionRunner.session();
        configurePrioritized(session.getConfiguration());
        RegressionRunner.run(session, ContextInitTermPrioritized.executions());
        session.destroy();
    }

    private static void configurePrioritized(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class, SupportBean_S2.class, ISupportA.class, ISupportB.class,
            ISupportABCImpl.class, ISupportAImpl.class, ISupportBImpl.class, SupportProductIdEvent.class}) {
            configuration.getCommon().addEventType(clazz.getSimpleName(), clazz);
        }

        configuration.getRuntime().getExecution().setPrioritized(true);
    }
}
