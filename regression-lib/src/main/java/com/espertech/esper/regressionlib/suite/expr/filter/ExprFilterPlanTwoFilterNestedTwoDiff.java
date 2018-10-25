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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.filter.*;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExprFilterPlanTwoFilterNestedTwoDiff extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {

        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        FilterTestMultiStmtAssertStats[] stats = FilterTestMultiStmtAssertStats.makeTwoSameStat("P0=(fh:1, fi:2),P1=(fh:2, fi:3),P2=(fh:1, fi:2),P3=(fh:0, fi:0, fipar:0)");

        // same equals-indexes
        FilterTestMultiStmtPermutable.addCase(cases, stats,
            "theString != 'x' and theString != 'y' and doubleBoxed is not null",
            "theString != 'x' and theString != 'y' and longBoxed is not null",
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBeanWBoxed("E1", -1, null, null), false, false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBeanWBoxed("x", -1, 1d, 1L), false, false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBeanWBoxed("x", -1, 1d, null), false, false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBeanWBoxed("y", -1, 1d, 1L), false, false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBeanWBoxed("E2", -1, 1d, 1L), true, true),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBeanWBoxed("E3", -1, 1d, null), true, false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBeanWBoxed("E4", -1, null, 1L), false, true));

        // we permute only [0, 1] as all filters are the same
        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanTwoFilterNestedTwoDiff.class, new PermutationSpec(0, 1), cases);
    }
}
