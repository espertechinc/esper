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

import com.espertech.esper.regressionlib.suite.resultset.outputlimit.ResultSetOutputLimitChangeSetOpt;
import com.espertech.esper.regressionlib.suite.resultset.outputlimit.ResultSetOutputLimitMicrosecondResolution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

public class TestSuiteResultSetOutputLimitWConfig extends TestCase {
    public void testResultSetOutputLimitChangeSetOpt() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCompiler().getViewResources().setOutputLimitOpt(false);
        RegressionRunner.run(session, new ResultSetOutputLimitChangeSetOpt(false));
        session.destroy();
    }

    public void testResultSetOutputLimitMicrosecondResolution() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCommon().getTimeSource().setTimeUnit(TimeUnit.MICROSECONDS);
        RegressionRunner.run(session,
            new ResultSetOutputLimitMicrosecondResolution(0, "1", 1000000, 1000000));
        RegressionRunner.run(session,
            new ResultSetOutputLimitMicrosecondResolution(789123456789L, "0.1", 789123456789L + 100000, 100000));
        session.destroy();
    }
}
