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
package com.espertech.esper.regression.resultset.querytype;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteQuerytype extends TestCase {
    public void testExecQuerytypeRowForAll() {
        RegressionRunner.run(new ExecQuerytypeRowForAll());
    }

    public void testExecQuerytypeRowForAllHaving() {
        RegressionRunner.run(new ExecQuerytypeRowForAllHaving());
    }

    public void testExecQuerytypeRowPerEvent() {
        RegressionRunner.run(new ExecQuerytypeRowPerEvent());
    }

    public void testExecQuerytypeRowPerEventDistinct() {
        RegressionRunner.run(new ExecQuerytypeRowPerEventDistinct());
    }

    public void testExecQuerytypeRollupDimensionality() {
        RegressionRunner.run(new ExecQuerytypeRollupDimensionality());
    }

    public void testExecQuerytypeRollupGroupingFuncs() {
        RegressionRunner.run(new ExecQuerytypeRollupGroupingFuncs());
    }

    public void testExecQuerytypeRollupHavingAndOrderBy() {
        RegressionRunner.run(new ExecQuerytypeRollupHavingAndOrderBy());
    }

    public void testExecQuerytypeRollupOutputRate() {
        RegressionRunner.run(new ExecQuerytypeRollupOutputRate());
    }

    public void testExecQuerytypeRollupPlanningAndSODA() {
        RegressionRunner.run(new ExecQuerytypeRollupPlanningAndSODA());
    }

    public void testExecQuerytypeGroupByEventPerGroup() {
        RegressionRunner.run(new ExecQuerytypeGroupByEventPerGroup());
    }

    public void testExecQuerytypeGroupByEventPerGroupHaving() {
        RegressionRunner.run(new ExecQuerytypeGroupByEventPerGroupHaving());
    }

    public void testExecQuerytypeGroupByEventPerRow() {
        RegressionRunner.run(new ExecQuerytypeGroupByEventPerRow());
    }

    public void testExecQuerytypeGroupByEventPerRowHaving() {
        RegressionRunner.run(new ExecQuerytypeGroupByEventPerRowHaving());
    }

    public void testExecQuerytypeGroupByReclaimMicrosecondResolution() {
        RegressionRunner.run(new ExecQuerytypeGroupByReclaimMicrosecondResolution());
    }

    public void testExecQuerytypeWTimeBatch() {
        RegressionRunner.run(new ExecQuerytypeWTimeBatch());
    }

    public void testExecQuerytypeRowForAllWGroupedTimeWinUnique() {
        RegressionRunner.run(new ExecQuerytypeRowForAllWGroupedTimeWinUnique());
    }

    public void testExecQuerytypeIterator() {
        RegressionRunner.run(new ExecQuerytypeIterator());
    }

    public void testExecQuerytypeHaving() {
        RegressionRunner.run(new ExecQuerytypeHaving());
    }

    public void testExecQuerytypeRowPerEventPerformance() {
        RegressionRunner.run(new ExecQuerytypeRowPerEventPerformance());
    }
}
