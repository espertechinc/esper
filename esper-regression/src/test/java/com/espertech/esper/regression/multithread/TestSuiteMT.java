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
package com.espertech.esper.regression.multithread;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteMT extends TestCase {
    public void testExecMTContextNestedNonOverlapAtNow() {
        RegressionRunner.run(new ExecMTContextNestedNonOverlapAtNow());
    }

    public void testExecMTContextCountSimple() {
        RegressionRunner.run(new ExecMTContextCountSimple());
    }

    public void testExecMTContextUnique() {
        RegressionRunner.run(new ExecMTContextUnique());
    }

    public void testExecMTContextDBAccess() {
        RegressionRunner.run(new ExecMTContextDBAccess());
    }

    public void testExecMTContextInitiatedTerminatedWithNowParallel() {
        RegressionRunner.run(new ExecMTContextInitiatedTerminatedWithNowParallel());
    }

    public void testExecMTContextPartitionedWTerm() {
        RegressionRunner.run(new ExecMTContextPartitionedWTerm());
    }

    public void testExecMTContextListenerDispatch() {
        RegressionRunner.run(new ExecMTContextListenerDispatch());
    }

    public void testExecMTContextMultiStmtStartEnd() {
        RegressionRunner.run(new ExecMTContextMultiStmtStartEnd());
    }

    public void testExecMTContextOverlapDistinct() {
        RegressionRunner.run(new ExecMTContextOverlapDistinct());
    }

    public void testExecMTContextSegmented() {
        RegressionRunner.run(new ExecMTContextSegmented());
    }

    public void testExecMTContextStartedBySameEvent() {
        RegressionRunner.run(new ExecMTContextStartedBySameEvent());
    }

    public void testExecMTContextTemporalStartStop() {
        RegressionRunner.run(new ExecMTContextTemporalStartStop());
    }

    public void testExecMTContextTerminated() {
        RegressionRunner.run(new ExecMTContextTerminated());
    }

    public void testExecMTDeployAtomic() {
        RegressionRunner.run(new ExecMTDeployAtomic());
    }

    public void testExecMTDeterminismInsertInto() {
        RegressionRunner.run(new ExecMTDeterminismInsertInto());
    }

    public void testExecMTDeterminismListener() {
        RegressionRunner.run(new ExecMTDeterminismListener());
    }

    public void testExecMTInsertIntoTimerConcurrency() {
        RegressionRunner.run(new ExecMTInsertIntoTimerConcurrency());
    }

    public void testExecMTIsolation() {
        RegressionRunner.run(new ExecMTIsolation());
    }

    public void testExecMTStmtDatabaseJoin() {
        RegressionRunner.run(new ExecMTStmtDatabaseJoin());
    }

    public void testExecMTStmtFilter() {
        RegressionRunner.run(new ExecMTStmtFilter());
    }

    public void testExecMTStmtFilterSubquery() {
        RegressionRunner.run(new ExecMTStmtFilterSubquery());
    }

    public void testExecMTStmtInsertInto() {
        RegressionRunner.run(new ExecMTStmtInsertInto());
    }

    public void testExecMTStmtIterate() {
        RegressionRunner.run(new ExecMTStmtIterate());
    }

    public void testExecMTStmtJoin() {
        RegressionRunner.run(new ExecMTStmtJoin());
    }

    public void testExecMTStmtListenerAddRemove() {
        RegressionRunner.run(new ExecMTStmtListenerAddRemove());
    }

    public void testExecMTStmtListenerCreateStmt() {
        RegressionRunner.run(new ExecMTStmtListenerCreateStmt());
    }

    public void testExecMTStmtListenerRoute() {
        RegressionRunner.run(new ExecMTStmtListenerRoute());
    }

    public void testExecMTStmtMgmt() {
        RegressionRunner.run(new ExecMTStmtMgmt());
    }

    public void testExecMTStmtNamedWindowConsume() {
        RegressionRunner.run(new ExecMTStmtNamedWindowConsume());
    }

    public void testExecMTStmtNamedWindowDelete() {
        RegressionRunner.run(new ExecMTStmtNamedWindowDelete());
    }

    public void testExecMTStmtNamedWindowFAF() {
        RegressionRunner.run(new ExecMTStmtNamedWindowFAF());
    }

    public void testExecMTStmtNamedWindowIterate() {
        RegressionRunner.run(new ExecMTStmtNamedWindowIterate());
    }

    public void testExecMTStmtNamedWindowJoinUniqueView() {
        RegressionRunner.run(new ExecMTStmtNamedWindowJoinUniqueView());
    }

    public void testExecMTStmtNamedWindowMerge() {
        RegressionRunner.run(new ExecMTStmtNamedWindowMerge());
    }

    public void testExecMTStmtNamedWindowMultiple() {
        RegressionRunner.run(new ExecMTStmtNamedWindowMultiple());
    }

    public void testExecMTStmtNamedWindowPriority() {
        RegressionRunner.run(new ExecMTStmtNamedWindowPriority());
    }

    public void testExecMTStmtNamedWindowSubqueryAgg() {
        RegressionRunner.run(new ExecMTStmtNamedWindowSubqueryAgg());
    }

    public void testExecMTStmtNamedWindowSubqueryLookup() {
        RegressionRunner.run(new ExecMTStmtNamedWindowSubqueryLookup());
    }

    public void testExecMTStmtNamedWindowUniqueTwoWJoinConsumer() {
        RegressionRunner.run(new ExecMTStmtNamedWindowUniqueTwoWJoinConsumer());
    }

    public void testExecMTStmtNamedWindowUpdate() {
        RegressionRunner.run(new ExecMTStmtNamedWindowUpdate());
    }

    public void testExecMTStmtPattern() {
        RegressionRunner.run(new ExecMTStmtPattern());
    }

    public void testExecMTStmtPatternFollowedBy() {
        RegressionRunner.run(new ExecMTStmtPatternFollowedBy());
    }

    public void testExecMTStmtStateless() {
        RegressionRunner.run(new ExecMTStmtStateless());
    }

    public void testExecMTStmtStatelessEnummethod() {
        RegressionRunner.run(new ExecMTStmtStatelessEnummethod());
    }

    public void testExecMTStmtSubquery() {
        RegressionRunner.run(new ExecMTStmtSubquery());
    }

    public void testExecMTStmtTimeWindow() {
        RegressionRunner.run(new ExecMTStmtTimeWindow());
    }

    public void testExecMTStmtTwoPatterns() {
        RegressionRunner.run(new ExecMTStmtTwoPatterns());
    }

    public void testExecMTStmtTwoPatternsStartStop() {
        RegressionRunner.run(new ExecMTStmtTwoPatternsStartStop());
    }

    public void testExecMTUpdate() {
        RegressionRunner.run(new ExecMTUpdate());
    }

    public void testExecMTUpdateIStreamSubselect() {
        RegressionRunner.run(new ExecMTUpdateIStreamSubselect());
    }

    public void testExecMTVariables() {
        RegressionRunner.run(new ExecMTVariables());
    }

}
