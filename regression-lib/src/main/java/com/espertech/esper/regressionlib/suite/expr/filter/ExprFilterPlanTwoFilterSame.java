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

public class ExprFilterPlanTwoFilterSame extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {

        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        FilterTestMultiStmtAssertStats[] stats = FilterTestMultiStmtAssertStats.makeTwoSameStat("P0=(fh:1, fi:1),P1=(fh:2, fi:1),P2=(fh:1, fi:1),P3=(fh:0, fi:0, fipar:0)");

        // same equals-index, same value
        addCase(cases, stats, "intPrimitive = 0", "intPrimitive = 0",
            makeItem(makeBean("E1", 0), true, true), makeItem(makeBean("E2", 1), false, false));

        // boolean-index
        addCase(cases, stats, "theString like 'A%'", "theString like 'A%'",
            makeItem(makeBean("A1"), true, true), makeItem(makeBean("B1"), false, false));

        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanTwoFilterSame.class, new PermutationSpec(0, 1), cases);
    }
}
