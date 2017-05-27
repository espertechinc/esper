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
package com.espertech.esper.regression.epl.fromclausemethod;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteFromClauseMethod extends TestCase {
    public void testExecFromClauseMethod() {
        RegressionRunner.run(new ExecFromClauseMethod());
    }

    public void testExecFromClauseMethodCacheExpiry() {
        RegressionRunner.run(new ExecFromClauseMethodCacheExpiry());
    }

    public void testExecFromClauseMethodCacheLRU() {
        RegressionRunner.run(new ExecFromClauseMethodCacheLRU());
    }

    public void testExecFromClauseMethodNStream() {
        RegressionRunner.run(new ExecFromClauseMethodNStream());
    }

    public void testExecFromClauseMethodOuterNStream() {
        RegressionRunner.run(new ExecFromClauseMethodOuterNStream());
    }

    public void testExecFromClauseMethodVariable() {
        RegressionRunner.run(new ExecFromClauseMethodVariable());
    }

    public void testExecFromClauseMethodJoinPerformance() {
        RegressionRunner.run(new ExecFromClauseMethodJoinPerformance());
    }
}
