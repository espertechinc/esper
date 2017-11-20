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
package com.espertech.esper.regression.context;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteContext extends TestCase {
    public void testExecContextAdminListen() {
        RegressionRunner.run(new ExecContextAdminListen());
    }

    public void testExecContextAdminPartitionSPI() {
        RegressionRunner.run(new ExecContextAdminPartitionSPI());
    }

    public void testExecContextCategory() {
        RegressionRunner.run(new ExecContextCategory());
    }

    public void testExecContextDocExamples() {
        RegressionRunner.run(new ExecContextDocExamples());
    }

    public void testExecContextHashSegmented() {
        RegressionRunner.run(new ExecContextHashSegmented());
    }

    public void testExecContextInitTerm() {
        RegressionRunner.run(new ExecContextInitTerm());
    }

    public void testExecContextInitTermPrioritized() {
        RegressionRunner.run(new ExecContextInitTermPrioritized());
    }

    public void testExecContextInitTermTemporalFixed() {
        RegressionRunner.run(new ExecContextInitTermTemporalFixed());
    }

    public void testExecContextInitTermWithDistinct() {
        RegressionRunner.run(new ExecContextInitTermWithDistinct());
    }

    public void testExecContextInitTermWithNow() {
        RegressionRunner.run(new ExecContextInitTermWithNow());
    }

    public void testExecContextLifecycle() {
        RegressionRunner.run(new ExecContextLifecycle());
    }

    public void testExecContextNested() {
        RegressionRunner.run(new ExecContextNested());
    }

    public void testExecContextPartitioned() {
        RegressionRunner.run(new ExecContextPartitioned());
    }

    public void testExecContextPartitionedAggregate() {
        RegressionRunner.run(new ExecContextPartitionedAggregate());
    }

    public void testExecContextPartitionedInfra() {
        RegressionRunner.run(new ExecContextPartitionedInfra());
    }

    public void testExecContextPartitionedNamedWindow() {
        RegressionRunner.run(new ExecContextPartitionedNamedWindow());
    }

    public void testExecContextPartitionedPrioritized() {
        RegressionRunner.run(new ExecContextPartitionedPrioritized());
    }

    public void testExecContextPartitionedWInitTermPrioritized() {
        RegressionRunner.run(new ExecContextPartitionedWInitTermPrioritized());
    }

    public void testExecContextPartitionedWInitTermNotPrioritized() {
        RegressionRunner.run(new ExecContextPartitionedWInitTermNotPrioritized());
    }

    public void testExecContextSelectionAndFireAndForget() {
        RegressionRunner.run(new ExecContextSelectionAndFireAndForget());
    }

    public void testExecContextWDeclaredExpression() {
        RegressionRunner.run(new ExecContextWDeclaredExpression());
    }

    public void testExecContextVariables() {
        RegressionRunner.run(new ExecContextVariables());
    }
}
