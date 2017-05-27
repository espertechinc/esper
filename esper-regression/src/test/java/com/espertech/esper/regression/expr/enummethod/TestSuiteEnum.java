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
package com.espertech.esper.regression.expr.enummethod;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteEnum extends TestCase {
    public void testExecEnumAggregate() {
        RegressionRunner.run(new ExecEnumAggregate());
    }

    public void testExecEnumAllOfAnyOf() {
        RegressionRunner.run(new ExecEnumAllOfAnyOf());
    }

    public void testExecEnumAverage() {
        RegressionRunner.run(new ExecEnumAverage());
    }

    public void testExecEnumChained() {
        RegressionRunner.run(new ExecEnumChained());
    }

    public void testExecEnumCountOf() {
        RegressionRunner.run(new ExecEnumCountOf());
    }

    public void testExecEnumDataSources() {
        RegressionRunner.run(new ExecEnumDataSources());
    }

    public void testExecEnumDistinct() {
        RegressionRunner.run(new ExecEnumDistinct());
    }

    public void testExecEnumDocSamples() {
        RegressionRunner.run(new ExecEnumDocSamples());
    }

    public void testExecEnumExceptIntersectUnion() {
        RegressionRunner.run(new ExecEnumExceptIntersectUnion());
    }

    public void testExecEnumFirstLastOf() {
        RegressionRunner.run(new ExecEnumFirstLastOf());
    }

    public void testExecEnumGroupBy() {
        RegressionRunner.run(new ExecEnumGroupBy());
    }

    public void testExecEnumInvalid() {
        RegressionRunner.run(new ExecEnumInvalid());
    }

    public void testExecEnumMinMax() {
        RegressionRunner.run(new ExecEnumMinMax());
    }

    public void testExecEnumMinMaxBy() {
        RegressionRunner.run(new ExecEnumMinMaxBy());
    }

    public void testExecEnumMostLeastFrequent() {
        RegressionRunner.run(new ExecEnumMostLeastFrequent());
    }

    public void testExecEnumNamedWindowPerformance() {
        RegressionRunner.run(new ExecEnumNamedWindowPerformance());
    }

    public void testExecEnumNested() {
        RegressionRunner.run(new ExecEnumNested());
    }

    public void testExecEnumNestedPerformance() {
        RegressionRunner.run(new ExecEnumNestedPerformance());
    }

    public void testExecEnumOrderBy() {
        RegressionRunner.run(new ExecEnumOrderBy());
    }

    public void testExecEnumReverse() {
        RegressionRunner.run(new ExecEnumReverse());
    }

    public void testExecEnumSelectFrom() {
        RegressionRunner.run(new ExecEnumSelectFrom());
    }

    public void testExecEnumSequenceEqual() {
        RegressionRunner.run(new ExecEnumSequenceEqual());
    }

    public void testExecEnumSumOf() {
        RegressionRunner.run(new ExecEnumSumOf());
    }

    public void testExecEnumTakeAndTakeLast() {
        RegressionRunner.run(new ExecEnumTakeAndTakeLast());
    }

    public void testExecEnumTakeWhileAndWhileLast() {
        RegressionRunner.run(new ExecEnumTakeWhileAndWhileLast());
    }

    public void testExecEnumToMap() {
        RegressionRunner.run(new ExecEnumToMap());
    }

    public void testExecEnumWhere() {
        RegressionRunner.run(new ExecEnumWhere());
    }
}