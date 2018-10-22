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
        new MultithreadContextMultiStmtStartEnd().run(SupportConfigFactory.getConfiguration());
    }

    public void testMultithreadContextNestedNonOverlapAtNow() {
        new MultithreadContextNestedNonOverlapAtNow().run(SupportConfigFactory.getConfiguration());
    }

    public void testMultithreadContextTerminated() {
        RegressionRunner.runConfigurable(new MultithreadContextTerminated());
    }

    public void testMultithreadDeterminismInsertIntoLockConfig() {
        new MultithreadDeterminismInsertIntoLockConfig().run(SupportConfigFactory.getConfiguration());
    }

    public void testMultithreadDeterminismListener() {
        new MultithreadDeterminismListener().run(SupportConfigFactory.getConfiguration());
    }

    public void testMultithreadInsertIntoTimerConcurrency() {
        new MultithreadInsertIntoTimerConcurrency().run(SupportConfigFactory.getConfiguration());
    }

    public void testMultithreadStmtListenerAddRemove() {
        RegressionRunner.runConfigurable(new MultithreadStmtListenerAddRemove());
    }

    public void testMultithreadStmtNamedWindowPriority() {
        RegressionRunner.runConfigurable(new MultithreadStmtNamedWindowPriority());
    }

    public void testMultithreadStmtPatternFollowedBy() {
        new MultithreadStmtPatternFollowedBy().run(SupportConfigFactory.getConfiguration());
    }

    public void testMultithreadStmtNamedWindowUniqueTwoWJoinConsumer() {
        new MultithreadStmtNamedWindowUniqueTwoWJoinConsumer().run(SupportConfigFactory.getConfiguration());
    }

    public void testMultithreadContextOverlapDistinct() {
        new MultithreadContextOverlapDistinct().run(SupportConfigFactory.getConfiguration());
    }

    public void testMultithreadContextPartitionedWTerm() {
        RegressionRunner.runConfigurable(new MultithreadContextPartitionedWTerm());
    }

    public void testMultithreadContextStartedBySameEvent() {
        RegressionRunner.runConfigurable(new MultithreadContextStartedBySameEvent());
    }
}
