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
package com.espertech.esper.regression.script;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteScript extends TestCase {
    public void testExecScriptExpression() {
        RegressionRunner.run(new ExecScriptExpression());
    }

    public void testExecScriptExpressionConfiguration() {
        RegressionRunner.run(new ExecScriptExpressionConfiguration());
    }

    public void testExecScriptSandboxJSR223() {
        RegressionRunner.run(new ExecScriptSandboxJSR223());
    }

    public void testExecScriptSandboxMVEL() {
        RegressionRunner.run(new ExecScriptSandboxMVEL());
    }

}
