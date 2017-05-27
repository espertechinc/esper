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
package com.espertech.esper.regression.expr.filter;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteFilter extends TestCase {
    public void testExecFilterExpressionsOptimizable() {
        RegressionRunner.run(new ExecFilterExpressionsOptimizable());
    }

    public void testExecFilterInAndBetween() {
        RegressionRunner.run(new ExecFilterInAndBetween());
    }

    public void testExecFilterExpressions() {
        RegressionRunner.run(new ExecFilterExpressions());
    }

    public void testExecFilterLargeThreading() {
        RegressionRunner.run(new ExecFilterLargeThreading());
    }

    public void testExecFilterWhereClauseNoDataWindowPerformance() {
        RegressionRunner.run(new ExecFilterWhereClauseNoDataWindowPerformance());
    }
}
