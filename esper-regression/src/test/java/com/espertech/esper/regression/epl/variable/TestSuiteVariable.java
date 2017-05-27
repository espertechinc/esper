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
package com.espertech.esper.regression.epl.variable;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteVariable extends TestCase {
    public void testExecVariables() {
        RegressionRunner.run(new ExecVariables());
    }

    public void testExecVariablesCreate() {
        RegressionRunner.run(new ExecVariablesCreate());
    }

    public void testExecVariablesDestroy() {
        RegressionRunner.run(new ExecVariablesDestroy());
    }

    public void testExecVariablesEventTyped() {
        RegressionRunner.run(new ExecVariablesEventTyped());
    }

    public void testExecVariablesOutputRate() {
        RegressionRunner.run(new ExecVariablesOutputRate());
    }

    public void testExecVariablesPerf() {
        RegressionRunner.run(new ExecVariablesPerf());
    }

    public void testExecVariablesTimer() {
        RegressionRunner.run(new ExecVariablesTimer());
    }
}
