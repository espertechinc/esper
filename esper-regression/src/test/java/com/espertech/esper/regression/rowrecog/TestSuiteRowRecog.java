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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteRowRecog extends TestCase {
    public void testExecRowRecogInvalid() {
        RegressionRunner.run(new ExecRowRecogInvalid());
    }

    public void testExecRowRecogMaxStatesEngineWideNoPreventStart() {
        RegressionRunner.run(new ExecRowRecogMaxStatesEngineWideNoPreventStart());
    }

    public void testExecRowRecogMaxStatesEngineWide3Instance() {
        RegressionRunner.run(new ExecRowRecogMaxStatesEngineWide3Instance());
    }

    public void testExecRowRecogMaxStatesEngineWide4Instance() {
        RegressionRunner.run(new ExecRowRecogMaxStatesEngineWide4Instance());
    }

    public void testExecRowRecogAfter() {
        RegressionRunner.run(new ExecRowRecogAfter());
    }

    public void testExecRowRecogAggregation() {
        RegressionRunner.run(new ExecRowRecogAggregation());
    }

    public void testExecRowRecogArrayAccess() {
        RegressionRunner.run(new ExecRowRecogArrayAccess());
    }

    public void testExecRowRecogClausePresence() {
        RegressionRunner.run(new ExecRowRecogClausePresence());
    }

    public void testExecRowRecogDataSet() {
        RegressionRunner.run(new ExecRowRecogDataSet());
    }

    public void testExecRowRecogDataWin() {
        RegressionRunner.run(new ExecRowRecogDataWin());
    }

    public void testExecRowRecogDelete() {
        RegressionRunner.run(new ExecRowRecogDelete());
    }

    public void testExecRowRecogEmptyPartition() {
        RegressionRunner.run(new ExecRowRecogEmptyPartition());
    }

    public void testExecRowRecogEnumMethod() {
        RegressionRunner.run(new ExecRowRecogEnumMethod());
    }

    public void testExecRowRecogGreedyness() {
        RegressionRunner.run(new ExecRowRecogGreedyness());
    }

    public void testExecRowRecogInterval() {
        RegressionRunner.run(new ExecRowRecogInterval());
    }

    public void testExecRowRecogIntervalMicrosecondResolution() {
        RegressionRunner.run(new ExecRowRecogIntervalMicrosecondResolution());
    }

    public void testExecRowRecogIntervalOrTerminated() {
        RegressionRunner.run(new ExecRowRecogIntervalOrTerminated());
    }

    public void testExecRowRecogIterateOnly() {
        RegressionRunner.run(new ExecRowRecogIterateOnly());
    }

    public void testExecRowRecogOps() {
        RegressionRunner.run(new ExecRowRecogOps());
    }

    public void testExecRowRecogPerf() {
        RegressionRunner.run(new ExecRowRecogPerf());
    }

    public void testExecRowRecogPermute() {
        RegressionRunner.run(new ExecRowRecogPermute());
    }

    public void testExecRowRecogPrev() {
        RegressionRunner.run(new ExecRowRecogPrev());
    }

    public void testExecRowRecogRegex() {
        RegressionRunner.run(new ExecRowRecogRegex());
    }

    public void testExecRowRecogRepetition() {
        RegressionRunner.run(new ExecRowRecogRepetition());
    }

    public void testExecRowRecogVariantStream() {
        RegressionRunner.run(new ExecRowRecogVariantStream());
    }
}
