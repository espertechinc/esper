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
package com.espertech.esper.regressionrun.suite.multithread;

import com.espertech.esper.regressionlib.suite.multithread.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

/**
 * When running with a shared/default configuration place test in {@link TestSuiteMultithread}
 * since these tests share the runtimevia session.
 * <p>
 * When running with a configuration derived from the default configuration "SupportConfigFactory", use:
 * <pre>RegressionRunner.runConfigurable</pre>
 * <p>
 * When running with a fully custom configuration, use a separate runtime instance but obtain the base
 * configuration from SupportConfigFactory:
 * <pre>new XXX().run(config)</pre>
 */
public class TestSuiteMultithreadWConfig extends TestCase {

    public void testMultithreadPatternTimer() {
        RegressionRunner.runConfigurable(new MultithreadPatternTimer());
    }

    public void testMultithreadContextDBAccess() {
        RegressionRunner.runConfigurable(new MultithreadContextDBAccess());
    }

    public void testMultithreadContextMultiStmtStartEnd() {
        RegressionRunner.runPreConfigured(new MultithreadContextMultiStmtStartEnd(SupportConfigFactory.getConfiguration()));
    }

    public void testMultithreadContextNestedNonOverlapAtNow() {
        RegressionRunner.runPreConfigured(new MultithreadContextNestedNonOverlapAtNow(SupportConfigFactory.getConfiguration()));
    }

    public void testMultithreadContextTerminated() {
        RegressionRunner.runConfigurable(new MultithreadContextTerminated());
    }

    public void testMultithreadDeterminismInsertIntoLockConfig() {
        RegressionRunner.runPreConfigured(new MultithreadDeterminismInsertIntoLockConfig(SupportConfigFactory.getConfiguration()));
    }

    public void testMultithreadDeterminismListener() {
        RegressionRunner.runPreConfigured(new MultithreadDeterminismListener(SupportConfigFactory.getConfiguration()));
    }

    public void testMultithreadInsertIntoTimerConcurrency() {
        RegressionRunner.runPreConfigured(new MultithreadInsertIntoTimerConcurrency(SupportConfigFactory.getConfiguration()));
    }

    public void testMultithreadStmtListenerAddRemove() {
        RegressionRunner.runConfigurable(new MultithreadStmtListenerAddRemove());
    }

    public void testMultithreadStmtNamedWindowPriority() {
        RegressionRunner.runConfigurable(new MultithreadStmtNamedWindowPriority());
    }

    public void testMultithreadStmtPatternFollowedBy() {
        RegressionRunner.runPreConfigured(new MultithreadStmtPatternFollowedBy(SupportConfigFactory.getConfiguration()));
    }

    public void testMultithreadStmtNamedWindowUniqueTwoWJoinConsumer() {
        RegressionRunner.runPreConfigured(new MultithreadStmtNamedWindowUniqueTwoWJoinConsumer(SupportConfigFactory.getConfiguration()));
    }

    public void testMultithreadContextOverlapDistinct() {
        RegressionRunner.runPreConfigured(new MultithreadContextOverlapDistinct(SupportConfigFactory.getConfiguration()));
    }

    public void testMultithreadContextPartitionedWTerm() {
        RegressionRunner.runConfigurable(new MultithreadContextPartitionedWTerm());
    }

    public void testMultithreadContextStartedBySameEvent() {
        RegressionRunner.runConfigurable(new MultithreadContextStartedBySameEvent());
    }
}
