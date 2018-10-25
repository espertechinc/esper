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

/**
 * Index reuse with different-key
 * - index: {key: [filter, index: {filter}]}
 */
public class ExprFilterPlanTwoFilterIndexReuse extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {
        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        FilterTestMultiStmtAssertStats[] reuseStats = new FilterTestMultiStmtAssertStats[]{
            new FilterTestMultiStmtAssertStats("P0=(fh:1, fi:1),P1=(fh:2, fi:2),P2=(fh:1, fi:2),P3=(fh:0, fi:0, fipar:0)", 0, 1),
            new FilterTestMultiStmtAssertStats("P0=(fh:1, fi:2),P1=(fh:2, fi:2),P2=(fh:1, fi:1),P3=(fh:0, fi:0, fipar:0)", 1, 0)
        };

        // equals
        addCase(cases,
            reuseStats,
            "intPrimitive = 1", "intPrimitive = 0 and longPrimitive = 0",
            makeItem(makeBean("E1", 0, 0), false, true),
            makeItem(makeBean("E2", 1, 0), true, false),
            makeItem(makeBean("E3", 0, 1), false, false));

        // not-equals
        addCase(cases,
            reuseStats,
            "intPrimitive != 5", "intPrimitive != 0 and longPrimitive != 0",
            makeItem(makeBean("E1", 0, 0), true, false),
            makeItem(makeBean("E2", 5, 0), false, false),
            makeItem(makeBean("E3", 5, 5), false, true),
            makeItem(makeBean("E4", -1, -1), true, true));

        // greater
        addCase(cases,
            reuseStats,
            "intPrimitive >= 5", "intPrimitive >= 0 and longPrimitive >= 0",
            makeItem(makeBean("E1", 0, 0), false, true),
            makeItem(makeBean("E2", -1, 0), false, false),
            makeItem(makeBean("E3", 10, -1), true, false),
            makeItem(makeBean("E4", 10, 1), true, true));

        // 'range'
        addCase(cases,
            reuseStats,
            "intPrimitive between 0 and 10", "intPrimitive between 10 and 20 and longPrimitive between 10 and 20",
            makeItem(makeBean("E1", -1, 16), false, false),
            makeItem(makeBean("E2", 10, -1), true, false),
            makeItem(makeBean("E3", 10, 15), true, true),
            makeItem(makeBean("E4", 15, 15), false, true));

        // 'in'
        addCase(cases,
            reuseStats,
            "intPrimitive in (0, 1)", "intPrimitive in (0, 2) and longPrimitive in (0, 2)",
            makeItem(makeBean("E1", 0, 0), true, true),
            makeItem(makeBean("E2", -1, 0), false, false),
            makeItem(makeBean("E3", 1, -1), true, false),
            makeItem(makeBean("E4", 2, 2), false, true));

        // 'not in'
        addCase(cases,
            reuseStats,
            "intPrimitive not in (0, 1)", "intPrimitive not in (0, 2) and longPrimitive not in (0, 2)",
            makeItem(makeBean("E1", 0, 0), false, false),
            makeItem(makeBean("E2", -1, 0), true, false),
            makeItem(makeBean("E3", 1, 1), false, true),
            makeItem(makeBean("E4", 3, 3), true, true));

        // boolean with equals
        addCase(cases,
            reuseStats,
            "intPrimitive = 1", "intPrimitive = 0 and theString like 'B%'",
            makeItem(makeBean("A", 1), true, false),
            makeItem(makeBean("B", 0), false, true),
            makeItem(makeBean("B", 2), false, false));

        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanTwoFilterIndexReuse.class, new PermutationSpec(true), cases);
    }
}
