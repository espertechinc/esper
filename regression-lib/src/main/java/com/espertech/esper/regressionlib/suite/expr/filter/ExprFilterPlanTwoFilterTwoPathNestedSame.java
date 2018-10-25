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

public class ExprFilterPlanTwoFilterTwoPathNestedSame extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {
        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        FilterTestMultiStmtAssertStats[] stats = FilterTestMultiStmtAssertStats.makeTwoSameStat("P0=(fh:1, fi:3),P1=(fh:2, fi:3),P2=(fh:1, fi:3),P3=(fh:0, fi:0, fipar:0)");

        addCase(cases, stats,
            "theString = 'A' and (intPrimitive = 0 or longPrimitive = 0)",
            "theString = 'A' and (intPrimitive = 0 or longPrimitive = 0)",
            makeItem(makeBean("A", 0, 1), true, true),
            makeItem(makeBean("A", 1, 0), true, true),
            makeItem(makeBean("A", 1, 1), false, false),
            makeItem(makeBean("B", 0, 0), false, false));

        // we permute only [0, 1] as all filters are the same
        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanTwoFilterTwoPathNestedSame.class, new PermutationSpec(0, 1), cases);
    }
}
