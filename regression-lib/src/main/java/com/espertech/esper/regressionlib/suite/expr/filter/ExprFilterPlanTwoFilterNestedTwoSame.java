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
package com.espertech.esper.regressionlib.suite.expr.filter;

import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.filter.FilterTestMultiStmtAssertStats;
import com.espertech.esper.regressionlib.support.filter.FilterTestMultiStmtPermutable;
import com.espertech.esper.regressionlib.support.filter.FilterTestMultiStmtRunner;
import com.espertech.esper.regressionlib.support.filter.PermutationSpec;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.espertech.esper.common.internal.support.SupportBean.makeBean;
import static com.espertech.esper.regressionlib.support.filter.FilterTestMultiStmtAssertItem.makeItem;
import static com.espertech.esper.regressionlib.support.filter.FilterTestMultiStmtPermutable.addCase;

public class ExprFilterPlanTwoFilterNestedTwoSame extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {

        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        FilterTestMultiStmtAssertStats[] stats = FilterTestMultiStmtAssertStats.makeTwoSameStat("P0=(fh:1, fi:2),P1=(fh:2, fi:2),P2=(fh:1, fi:2),P3=(fh:0, fi:0, fipar:0)");

        // same equals-indexes
        addCase(cases, stats,
            "intPrimitive = 0 and longPrimitive = 0",
            "intPrimitive = 0 and longPrimitive = 0",
            makeItem(makeBean("E1", 0, 0), true, true),
            makeItem(makeBean("E2", 1, 0), false, false),
            makeItem(makeBean("E3", 0, 1), false, false));

        // same not-equals-index
        addCase(cases, stats,
            "intPrimitive != 1 and longPrimitive != 2",
            "intPrimitive != 1 and longPrimitive != 2",
            makeItem(makeBean("E1", 1, 2), false, false),
            makeItem(makeBean("E2", 2, 3), true, true),
            makeItem(makeBean("E3", 1, -1), false, false),
            makeItem(makeBean("E4", -1, 2), false, false));

        // same greater-indexes
        addCase(cases, stats,
            "intPrimitive > 0 and longPrimitive > 0",
            "intPrimitive > 0 and longPrimitive > 0",
            makeItem(makeBean("E1", 1, 1), true, true),
            makeItem(makeBean("E2", 1, 0), false, false),
            makeItem(makeBean("E3", 0, 1), false, false));

        // same range-index
        addCase(cases, stats,
            "intPrimitive between 0 and 10 and longPrimitive between 0 and 10",
            "intPrimitive between 0 and 10 and longPrimitive between 0 and 10",
            makeItem(makeBean("E1", 1, 1), true, true),
            makeItem(makeBean("E2", 1, -1), false, false),
            makeItem(makeBean("E3", -1, 1), false, false));

        // same in-index
        addCase(cases, stats,
            "intPrimitive in (1, 2) and longPrimitive in (2, 3)",
            "intPrimitive in (1, 2) and longPrimitive in (2, 3)",
            makeItem(makeBean("E1", 1, 2), true, true),
            makeItem(makeBean("E2", 2, 3), true, true),
            makeItem(makeBean("E3", 1, -1), false, false),
            makeItem(makeBean("E4", -1, 1), false, false));

        // same not-in-index
        addCase(cases, stats,
            "intPrimitive not in (1, 2) and longPrimitive not in (2, 3)",
            "intPrimitive not in (1, 2) and longPrimitive not in (2, 3)",
            makeItem(makeBean("E1", 1, 2), false, false),
            makeItem(makeBean("E2", 2, 3), false, false),
            makeItem(makeBean("E3", -1, -1), true, true),
            makeItem(makeBean("E4", -1, 2), false, false));

        // we permute only [0, 1] as all filters are the same
        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanTwoFilterNestedTwoSame.class, new PermutationSpec(0, 1), cases);
    }
}
