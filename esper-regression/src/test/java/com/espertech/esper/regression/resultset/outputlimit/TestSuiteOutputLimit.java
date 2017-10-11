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
package com.espertech.esper.regression.resultset.outputlimit;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteOutputLimit extends TestCase {
    public void testExecOutputLimitAfter() {
        RegressionRunner.run(new ExecOutputLimitAfter());
    }

    public void testExecOutputLimitRowPerEvent() {
        RegressionRunner.run(new ExecOutputLimitRowPerEvent());
    }

    public void testExecOutputLimitChangeSetOpt() {
        RegressionRunner.run(new ExecOutputLimitChangeSetOpt(true));
        RegressionRunner.run(new ExecOutputLimitChangeSetOpt(false));
    }

    public void testExecOutputLimitCrontabWhen() {
        RegressionRunner.run(new ExecOutputLimitCrontabWhen());
    }

    public void testExecOutputLimitRowPerGroup() {
        RegressionRunner.run(new ExecOutputLimitRowPerGroup());
    }

    public void testExecOutputLimitAggregateGrouped() {
        RegressionRunner.run(new ExecOutputLimitAggregateGrouped());
    }

    public void testExecOutputLimitFirstHaving() {
        RegressionRunner.run(new ExecOutputLimitFirstHaving());
    }

    public void testExecOutputLimitMicrosecondResolution() {
        RegressionRunner.run(new ExecOutputLimitMicrosecondResolution());
    }

    public void testExecOutputLimitParameterizedByContext() {
        RegressionRunner.run(new ExecOutputLimitParameterizedByContext());
    }

    public void testExecOutputLimitRowLimit() {
        RegressionRunner.run(new ExecOutputLimitRowLimit());
    }

    public void testExecOutputLimitRowForAll() {
        RegressionRunner.run(new ExecOutputLimitRowForAll());
    }

    public void testExecOutputLimitSimple() {
        RegressionRunner.run(new ExecOutputLimitSimple());
    }

    public void testExecOutputLimitRowPerGroupRollup() {
        RegressionRunner.run(new ExecOutputLimitRowPerGroupRollup());
    }
}
