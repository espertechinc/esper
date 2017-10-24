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
package com.espertech.esper.regression.expr.datetime;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteDatetime extends TestCase {
    public void testExecDTBetween() {
        RegressionRunner.run(new ExecDTBetween());
    }

    public void testExecDTDocSamples() {
        RegressionRunner.run(new ExecDTDocSamples());
    }

    public void testExecDTFormat() {
        RegressionRunner.run(new ExecDTFormat());
    }

    public void testExecDTGet() {
        RegressionRunner.run(new ExecDTGet());
    }

    public void testExecDTIntervalOps() {
        RegressionRunner.run(new ExecDTIntervalOps());
    }

    public void testExecDTIntervalOpsCreateSchema() {
        RegressionRunner.run(new ExecDTIntervalOpsCreateSchema());
    }

    public void testExecDTIntervalOpsInvalidConfig() {
        RegressionRunner.run(new ExecDTIntervalOpsInvalidConfig());
    }

    public void testExecDTInvalid() {
        RegressionRunner.run(new ExecDTInvalid());
    }

    public void testExecDTMicrosecondResolution() {
        RegressionRunner.run(new ExecDTMicrosecondResolution());
    }

    public void testExecDTNested() {
        RegressionRunner.run(new ExecDTNested());
    }

    public void testExecDTPerfBetween() {
        RegressionRunner.run(new ExecDTPerfBetween());
    }

    public void testExecDTPerfIntervalOps() {
        RegressionRunner.run(new ExecDTPerfIntervalOps());
    }

    public void testExecDTPlusMinus() {
        RegressionRunner.run(new ExecDTPlusMinus());
    }

    public void testExecDTDataSources() {
        RegressionRunner.run(new ExecDTDataSources());
    }

    public void testExecDTRound() {
        RegressionRunner.run(new ExecDTRound());
    }

    public void testExecDTSet() {
        RegressionRunner.run(new ExecDTSet());
    }

    public void testExecDTToDateCalMSec() {
        RegressionRunner.run(new ExecDTToDateCalMSec());
    }

    public void testExecDTWithDate() {
        RegressionRunner.run(new ExecDTWithDate());
    }

    public void testExecDTWithMax() {
        RegressionRunner.run(new ExecDTWithMax());
    }

    public void testExecDTWithMin() {
        RegressionRunner.run(new ExecDTWithMin());
    }

    public void testExecDTWithTime() {
        RegressionRunner.run(new ExecDTWithTime());
    }
}
