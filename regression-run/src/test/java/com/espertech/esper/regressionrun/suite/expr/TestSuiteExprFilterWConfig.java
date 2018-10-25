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

import com.espertech.esper.common.client.util.ThreadingProfile;
import com.espertech.esper.regressionlib.suite.expr.filter.ExprFilterLargeThreading;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportTradeEvent;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteExprFilterWConfig extends TestCase {

    public void testExprFilterLargeThreading() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCommon().addEventType(SupportTradeEvent.class);
        session.getConfiguration().getCommon().getExecution().setThreadingProfile(ThreadingProfile.LARGE);
        RegressionRunner.run(session, new ExprFilterLargeThreading());
        session.destroy();
    }
}
