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
package com.espertech.esper.regression.resultset.aggregate;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteAggregate extends TestCase {
    public void testExecAggregateFirstLastWindow() {
        RegressionRunner.run(new ExecAggregateFirstLastWindow());
    }

    public void testExecAggregateMinMaxBy() {
        RegressionRunner.run(new ExecAggregateMinMaxBy());
    }

    public void testExecAggregateExtInvalid() {
        RegressionRunner.run(new ExecAggregateExtInvalid());
    }

    public void testExecAggregateLeaving() {
        RegressionRunner.run(new ExecAggregateLeaving());
    }

    public void testExecAggregateNTh() {
        RegressionRunner.run(new ExecAggregateNTh());
    }

    public void testExecAggregateRate() {
        RegressionRunner.run(new ExecAggregateRate());
    }

    public void testExecAggregateFiltered() {
        RegressionRunner.run(new ExecAggregateFiltered());
    }

    public void testExecAggregateFilteredWMathContext() {
        RegressionRunner.run(new ExecAggregateFilteredWMathContext());
    }

    public void testExecAggregateFilterNamedParameter() {
        RegressionRunner.run(new ExecAggregateFilterNamedParameter());
    }

    public void testExecAggregateLocalGroupBy() {
        RegressionRunner.run(new ExecAggregateLocalGroupBy());
    }

    public void testExecAggregateFirstEverLastEver() {
        RegressionRunner.run(new ExecAggregateFirstEverLastEver());
    }

    public void testExecAggregateCount() {
        RegressionRunner.run(new ExecAggregateCount());
    }

    public void testExexAggregateCountWGroupBy() {
        RegressionRunner.run(new ExexAggregateCountWGroupBy());
    }

    public void testExecAggregateMaxMinGroupBy() {
        RegressionRunner.run(new ExecAggregateMaxMinGroupBy());
    }

    public void testExecAggregateMedianAndDeviation() {
        RegressionRunner.run(new ExecAggregateMedianAndDeviation());
    }

    public void testExecAggregateMinMax() {
        RegressionRunner.run(new ExecAggregateMinMax());
    }

}