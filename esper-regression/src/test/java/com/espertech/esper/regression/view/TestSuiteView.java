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
package com.espertech.esper.regression.view;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteView extends TestCase {

    public void testExecViewExpiryIntersect() {
        RegressionRunner.run(new ExecViewExpiryIntersect());
    }

    public void testExecViewExpiryUnion() {
        RegressionRunner.run(new ExecViewExpiryUnion());
    }

    public void testExecViewExpressionBatch() {
        RegressionRunner.run(new ExecViewExpressionBatch());
    }

    public void testExecViewExpressionWindow() {
        RegressionRunner.run(new ExecViewExpressionWindow());
    }

    public void testExecViewExternallyBatched() {
        RegressionRunner.run(new ExecViewExternallyBatched());
    }

    public void testExecViewGroupLengthWinWeightAvg() {
        RegressionRunner.run(new ExecViewGroupLengthWinWeightAvg());
    }

    public void testExecViewGroupWin() {
        RegressionRunner.run(new ExecViewGroupWin());
    }

    public void testExecViewGroupWinReclaimMicrosecondResolution() {
        RegressionRunner.run(new ExecViewGroupWinReclaimMicrosecondResolution());
    }

    public void testExecViewGroupWinSharedViewStartStop() {
        RegressionRunner.run(new ExecViewGroupWinSharedViewStartStop());
    }

    public void testExecViewInheritAndInterface() {
        RegressionRunner.run(new ExecViewInheritAndInterface());
    }

    public void testExecViewInvalid() {
        RegressionRunner.run(new ExecViewInvalid());
    }

    public void testExecViewKeepAllWindow() {
        RegressionRunner.run(new ExecViewKeepAllWindow());
    }

    public void testExecViewLengthBatch() {
        RegressionRunner.run(new ExecViewLengthBatch());
    }

    public void testExecViewLengthWindowStats() {
        RegressionRunner.run(new ExecViewLengthWindowStats());
    }

    public void testExecViewMultipleExpiry() {
        RegressionRunner.run(new ExecViewMultipleExpiry());
    }

    public void testExecViewParameterizedByContext() {
        RegressionRunner.run(new ExecViewParameterizedByContext());
    }

    public void testExecViewPropertyAccess() {
        RegressionRunner.run(new ExecViewPropertyAccess());
    }

    public void testExecViewRank() {
        RegressionRunner.run(new ExecViewRank());
    }

    public void testExecViewSimpleFilter() {
        RegressionRunner.run(new ExecViewSimpleFilter());
    }

    public void testExecViewSize() {
        RegressionRunner.run(new ExecViewSize());
    }

    public void testExecViewSort() {
        RegressionRunner.run(new ExecViewSort());
    }

    public void testExecViewStartStop() {
        RegressionRunner.run(new ExecViewStartStop());
    }

    public void testExecViewTimeAccum() {
        RegressionRunner.run(new ExecViewTimeAccum());
    }

    public void testExecViewTimeBatch() {
        RegressionRunner.run(new ExecViewTimeBatch());
    }

    public void testExecViewTimeBatchMean() {
        RegressionRunner.run(new ExecViewTimeBatchMean());
    }

    public void testExecViewTimeFirst() {
        RegressionRunner.run(new ExecViewTimeFirst());
    }

    public void testExecViewTimeInterval() {
        RegressionRunner.run(new ExecViewTimeInterval());
    }

    public void testExecViewTimeLengthBatch() {
        RegressionRunner.run(new ExecViewTimeLengthBatch());
    }

    public void testExecViewTimeOrderAndTimeToLive() {
        RegressionRunner.run(new ExecViewTimeOrderAndTimeToLive());
    }

    public void testExecViewTimeWin() {
        RegressionRunner.run(new ExecViewTimeWin());
    }

    public void testExecViewTimeWindowMicrosecondResolution() {
        RegressionRunner.run(new ExecViewTimeWindowMicrosecondResolution());
    }

    public void testExecViewTimeWindowUnique() {
        RegressionRunner.run(new ExecViewTimeWindowUnique());
    }

    public void testExecViewWhereClause() {
        RegressionRunner.run(new ExecViewWhereClause());
    }

    public void testExecViewUniqueSorted() {
        RegressionRunner.run(new ExecViewUniqueSorted());
    }

    public void testExecViewTimeWindowWeightedAvg() {
        RegressionRunner.run(new ExecViewTimeWindowWeightedAvg());
    }

    public void testExecViewTimeWinMultithreaded() {
        RegressionRunner.run(new ExecViewTimeWinMultithreaded());
    }
}
