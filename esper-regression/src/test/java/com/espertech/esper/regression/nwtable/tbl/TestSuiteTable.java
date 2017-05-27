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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

// see INFRA suite for additional Table tests
public class TestSuiteTable extends TestCase {
    public void testExecTableAccessAggregationState() {
        RegressionRunner.run(new ExecTableAccessAggregationState());
    }

    public void testExecTableAccessCore() {
        RegressionRunner.run(new ExecTableAccessCore());
    }

    public void testExecTableNonAccessDotSubqueryAndJoin() {
        RegressionRunner.run(new ExecTableNonAccessDotSubqueryAndJoin());
    }

    public void testExecTableContext() {
        RegressionRunner.run(new ExecTableContext());
    }

    public void testExecTableCountMinSketch() {
        RegressionRunner.run(new ExecTableCountMinSketch());
    }

    public void testExecTableAccessDotMethod() {
        RegressionRunner.run(new ExecTableAccessDotMethod());
    }

    public void testExecTableDocSamples() {
        RegressionRunner.run(new ExecTableDocSamples());
    }

    public void testExecTableFAFExecuteQuery() {
        RegressionRunner.run(new ExecTableFAFExecuteQuery());
    }

    public void testExecTableFilters() {
        RegressionRunner.run(new ExecTableFilters());
    }

    public void testExecTableInsertInto() {
        RegressionRunner.run(new ExecTableInsertInto());
    }

    public void testExecTableIntoTable() {
        RegressionRunner.run(new ExecTableIntoTable());
    }

    public void testExecTableInvalid() {
        RegressionRunner.run(new ExecTableInvalid());
    }

    public void testExecTableIterate() {
        RegressionRunner.run(new ExecTableIterate());
    }

    public void testExecTableJoin() {
        RegressionRunner.run(new ExecTableJoin());
    }

    public void testExecTableLifecycle() {
        RegressionRunner.run(new ExecTableLifecycle());
    }

    public void testExecTableOnDelete() {
        RegressionRunner.run(new ExecTableOnDelete());
    }

    public void testExecTableOnMerge() {
        RegressionRunner.run(new ExecTableOnMerge());
    }

    public void testExecTableOnSelect() {
        RegressionRunner.run(new ExecTableOnSelect());
    }

    public void testExecTableOnUpdate() {
        RegressionRunner.run(new ExecTableOnUpdate());
    }

    public void testExecTableOutputRateLimiting() {
        RegressionRunner.run(new ExecTableOutputRateLimiting());
    }

    public void testExecTablePlugInAggregation() {
        RegressionRunner.run(new ExecTablePlugInAggregation());
    }

    public void testExecTableRollup() {
        RegressionRunner.run(new ExecTableRollup());
    }

    public void testExecTableSubquery() {
        RegressionRunner.run(new ExecTableSubquery());
    }

    public void testExecTableUpdateAndIndex() {
        RegressionRunner.run(new ExecTableUpdateAndIndex());
    }

    public void testExecTableWNamedWindow() {
        RegressionRunner.run(new ExecTableWNamedWindow());
    }

    public void testExecTableSelectStarPublicTypeVisibility() {
        RegressionRunner.run(new ExecTableSelectStarPublicTypeVisibility());
    }

    public void testExecTableMTAccessReadMergeWriteInsertDeleteRowVisible() {
        RegressionRunner.run(new ExecTableMTAccessReadMergeWriteInsertDeleteRowVisible());
    }

    public void testExecTableMTGroupedAccessReadIntoTableWriteAggColConsistency() {
        RegressionRunner.run(new ExecTableMTGroupedAccessReadIntoTableWriteAggColConsistency());
    }

    public void testExecTableMTGroupedAccessReadIntoTableWriteNewRowCreation() {
        RegressionRunner.run(new ExecTableMTGroupedAccessReadIntoTableWriteNewRowCreation());
    }

    public void testExecTableMTGroupedFAFReadFAFWriteChain() {
        RegressionRunner.run(new ExecTableMTGroupedFAFReadFAFWriteChain());
    }

    public void testExecTableMTGroupedJoinReadMergeWriteSecondaryIndexUpd() {
        RegressionRunner.run(new ExecTableMTGroupedJoinReadMergeWriteSecondaryIndexUpd());
    }

    public void testExecTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd() {
        RegressionRunner.run(new ExecTableMTGroupedMergeReadMergeWriteSecondaryIndexUpd());
    }

    public void testExecTableMTGroupedSubqueryReadInsertIntoWriteConcurr() {
        RegressionRunner.run(new ExecTableMTGroupedSubqueryReadInsertIntoWriteConcurr());
    }

    public void testExecTableMTGroupedSubqueryReadMergeWriteSecondaryIndexUpd() {
        RegressionRunner.run(new ExecTableMTGroupedSubqueryReadMergeWriteSecondaryIndexUpd());
    }

    public void testExecTableMTGroupedWContextIntoTableWriteAsContextTable() {
        RegressionRunner.run(new ExecTableMTGroupedWContextIntoTableWriteAsContextTable());
    }

    public void testExecTableMTGroupedWContextIntoTableWriteAsSharedTable() {
        RegressionRunner.run(new ExecTableMTGroupedWContextIntoTableWriteAsSharedTable());
    }

    public void testExecTableMTUngroupedAccessReadInotTableWriteIterate() {
        RegressionRunner.run(new ExecTableMTUngroupedAccessReadInotTableWriteIterate());
    }

    public void testExecTableMTUngroupedAccessReadIntoTableWriteFilterUse() {
        RegressionRunner.run(new ExecTableMTUngroupedAccessReadIntoTableWriteFilterUse());
    }

    public void testExecTableMTUngroupedAccessReadMergeWrite() {
        RegressionRunner.run(new ExecTableMTUngroupedAccessReadMergeWrite());
    }

    public void testExecTableMTUngroupedAccessWithinRowFAFConsistency() {
        RegressionRunner.run(new ExecTableMTUngroupedAccessWithinRowFAFConsistency());
    }

    public void testExecTableMTUngroupedIntoTableWriteMultiWriterAgg() {
        RegressionRunner.run(new ExecTableMTUngroupedIntoTableWriteMultiWriterAgg());
    }

    public void testExecTableMTUngroupedJoinColumnConsistency() {
        RegressionRunner.run(new ExecTableMTUngroupedJoinColumnConsistency());
    }

    public void testExecTableMTUngroupedSubqueryReadMergeWriteColumnUpd() {
        RegressionRunner.run(new ExecTableMTUngroupedSubqueryReadMergeWriteColumnUpd());
    }
}
