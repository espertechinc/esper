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
package com.espertech.esper.regression.epl.subselect;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteSubselect extends TestCase {
    public void testExecSubselectAggregatedInExistsAnyAll() {
        RegressionRunner.run(new ExecSubselectAggregatedInExistsAnyAll());
    }

    public void testExecSubselectAggregatedMultirowAndColumn() {
        RegressionRunner.run(new ExecSubselectAggregatedMultirowAndColumn());
    }

    public void testExecSubselectAggregatedSingleValue() {
        RegressionRunner.run(new ExecSubselectAggregatedSingleValue());
    }

    public void testExecSubselectAllAnySomeExpr() {
        RegressionRunner.run(new ExecSubselectAllAnySomeExpr());
    }

    public void testExecSubselectExists() {
        RegressionRunner.run(new ExecSubselectExists());
    }

    public void testExecSubselectFiltered() {
        RegressionRunner.run(new ExecSubselectFiltered());
    }

    public void testExecSubselectIn() {
        RegressionRunner.run(new ExecSubselectIn());
    }

    public void testExecSubselectIndex() {
        RegressionRunner.run(new ExecSubselectIndex());
    }

    public void testExecSubselectMulticolumn() {
        RegressionRunner.run(new ExecSubselectMulticolumn());
    }

    public void testExecSubselectMultirow() {
        RegressionRunner.run(new ExecSubselectMultirow());
    }

    public void testExecSubselectOrderOfEvalNoPreeval() {
        RegressionRunner.run(new ExecSubselectOrderOfEvalNoPreeval());
    }

    public void testExecSubselectOrderOfEval() {
        RegressionRunner.run(new ExecSubselectOrderOfEval());
    }

    public void testExecSubselectUnfiltered() {
        RegressionRunner.run(new ExecSubselectUnfiltered());
    }

    public void testExecSubselectWithinHaving() {
        RegressionRunner.run(new ExecSubselectWithinHaving());
    }

    public void testExecSubselectWithinPattern() {
        RegressionRunner.run(new ExecSubselectWithinPattern());
    }

    public void testExecSubselectNamedWindowPerformance() {
        RegressionRunner.run(new ExecSubselectNamedWindowPerformance());
    }

    public void testExecSubselectInKeywordPerformance() {
        RegressionRunner.run(new ExecSubselectInKeywordPerformance());
    }

    public void testExecSubselectCorrelatedAggregationPerformance() {
        RegressionRunner.run(new ExecSubselectCorrelatedAggregationPerformance());
    }

    public void testExecSubselectFilteredPerformance() {
        RegressionRunner.run(new ExecSubselectFilteredPerformance());
    }
}