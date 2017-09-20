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
package com.espertech.esper.regression.resultset.orderby;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteOrderBy extends TestCase {
    public void testExecOrderByRowPerEvent() {
        RegressionRunner.run(new ExecOrderByRowPerEvent());
    }

    public void testExecOrderByRowPerGroup() {
        RegressionRunner.run(new ExecOrderByRowPerGroup());
    }

    public void testExecOrderByAggregateGrouped() {
        RegressionRunner.run(new ExecOrderByAggregateGrouped());
    }

    public void testExecOrderByRowForAll() {
        RegressionRunner.run(new ExecOrderByRowForAll());
    }

    public void testExecOrderBySelfJoin() {
        RegressionRunner.run(new ExecOrderBySelfJoin());
    }

    public void testExecOrderBySimpleSortCollator() {
        RegressionRunner.run(new ExecOrderBySimpleSortCollator());
    }

    public void testExecOrderBySimple() {
        RegressionRunner.run(new ExecOrderBySimple());
    }

    public void testExecOrderByAggregationResult() {
        RegressionRunner.run(new ExecOrderByAggregationResult());
    }
}
