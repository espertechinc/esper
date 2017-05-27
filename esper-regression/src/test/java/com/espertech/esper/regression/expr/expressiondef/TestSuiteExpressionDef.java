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
package com.espertech.esper.regression.expr.expressiondef;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteExpressionDef extends TestCase {
    public void testExecExpressionDef() {
        RegressionRunner.run(new ExecExpressionDef());
    }

    public void testExecExpressionDefAliasFor() {
        RegressionRunner.run(new ExecExpressionDefAliasFor());
    }

    public void testExecExpressionDefLambdaLocReport() {
        RegressionRunner.run(new ExecExpressionDefLambdaLocReport());
    }

    public void testExecExpressionDefConfigurations() {
        RegressionRunner.run(new ExecExpressionDefConfigurations(null, 4));
        RegressionRunner.run(new ExecExpressionDefConfigurations(0, 4));
        RegressionRunner.run(new ExecExpressionDefConfigurations(1, 4));
        RegressionRunner.run(new ExecExpressionDefConfigurations(2, 2));
    }
}
