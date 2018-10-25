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

public class ExprFilterPlanThreeFilterIndexReuse extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {
        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        // Permutations:
        // [0, 1, 2]
        // [0, 2, 1]
        // [1, 0, 2]
        // [1, 2, 0]
        // [2, 0, 1]
        // [2, 1, 0]

        String perm012 = "P0=(fh:1, fi:1),P1=(fh:2, fi:2),P2=(fh:3, fi:3),P3=(fh:2, fi:3),P4=(fh:1, fi:3),P5=(fh:0, fi:0, fipar:0)";
        String perm021 = "P0=(fh:1, fi:1),P1=(fh:2, fi:3),P2=(fh:3, fi:3),P3=(fh:2, fi:3),P4=(fh:1, fi:2),P5=(fh:0, fi:0, fipar:0)";
        String perm102 = "P0=(fh:1, fi:2),P1=(fh:2, fi:2),P2=(fh:3, fi:3),P3=(fh:2, fi:3),P4=(fh:1, fi:3),P5=(fh:0, fi:0, fipar:0)";
        String perm120 = "P0=(fh:1, fi:2),P1=(fh:2, fi:3),P2=(fh:3, fi:3),P3=(fh:2, fi:3),P4=(fh:1, fi:1),P5=(fh:0, fi:0, fipar:0)";
        String perm201 = "P0=(fh:1, fi:3),P1=(fh:2, fi:3),P2=(fh:3, fi:3),P3=(fh:2, fi:2),P4=(fh:1, fi:2),P5=(fh:0, fi:0, fipar:0)";
        String perm210 = "P0=(fh:1, fi:3),P1=(fh:2, fi:3),P2=(fh:3, fi:3),P3=(fh:2, fi:2),P4=(fh:1, fi:1),P5=(fh:0, fi:0, fipar:0)";
        FilterTestMultiStmtAssertStats[] reuseStats = new FilterTestMultiStmtAssertStats[]{
            new FilterTestMultiStmtAssertStats(perm012, 0, 1, 2),
            new FilterTestMultiStmtAssertStats(perm021, 0, 2, 1),
            new FilterTestMultiStmtAssertStats(perm102, 1, 0, 2),
            new FilterTestMultiStmtAssertStats(perm120, 1, 2, 0),
            new FilterTestMultiStmtAssertStats(perm201, 2, 0, 1),
            new FilterTestMultiStmtAssertStats(perm210, 2, 1, 0),
        };

        // equals
        FilterTestMultiStmtPermutable.addCase(cases,
            reuseStats,
            "intPrimitive = 1",
            "intPrimitive = 1 and longPrimitive = 10",
            "intPrimitive = 1 and longPrimitive = 10 and doublePrimitive=100",
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E1", 1, 10, 100), true, true, true),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E2", 0, 10, 100), false, false, false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E2", 1, 0, 100), true, false, false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E3", 1, 10, 0), true, true, false));

        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanThreeFilterIndexReuse.class, new PermutationSpec(2, 1, 0), cases);
    }
}
