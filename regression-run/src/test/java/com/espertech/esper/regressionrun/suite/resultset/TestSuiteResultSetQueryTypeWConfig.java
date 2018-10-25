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

import com.espertech.esper.regressionlib.suite.resultset.querytype.ResultSetQueryTypeRowPerGroupReclaimMicrosecondResolution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

public class TestSuiteResultSetQueryTypeWConfig extends TestCase {
    public void testResultSetQueryTypeRowPerGroupReclaimMicrosecondResolution() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCommon().getTimeSource().setTimeUnit(TimeUnit.MICROSECONDS);
        RegressionRunner.run(session, new ResultSetQueryTypeRowPerGroupReclaimMicrosecondResolution(5000000));
        session.destroy();
    }
}
