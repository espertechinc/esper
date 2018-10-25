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

public class ExprFilterPlanOneFilterNestedTwoLvl extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {
        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        FilterTestMultiStmtAssertStats[] stats = FilterTestMultiStmtAssertStats.makeSingleStat("P0=(fh:1, fi:2),P1=(fh:0, fi:0, fipar:0)");

        // simple two-param equals and not-equals
        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive=1, longPrimitive=10", FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 1, 10), true),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 1, 0), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 0, 10), false));

        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive != 0, longPrimitive != 1", FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E1", 0, 1), false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E2", -1, -1), true), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E3", 0, -1), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E4", -1, 1), false));

        // simple two-param greater
        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive>1, longPrimitive>10", FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 1, 10), false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 2, 11), true), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 2, 10), false));

        // two-param range
        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive between 0 and 2, longPrimitive between 0 and 2",
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E1", 0, 1), true), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E2", 0, 2), true), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E3", 2, 0), true),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E10", 5, 1), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E11", 1, 5), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E12", -1, -1), false));

        // two-param 'in'
        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive in (0,1), longPrimitive in (2, 3)",
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E1", 0, 2), true), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E2", 1, 3), true), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E3", 0, 3), true),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E10", 2, 2), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E11", 1, 4), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E12", -1, -1), false));

        // two-param 'not-in'
        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive not in (0,1), longPrimitive not in (2, 3)",
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E1", 2, 0), true), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E2", -1, -1), true), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E3", 3, 1), true),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E10", 0, 2), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E11", 1, 4), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E12", -1, 2), false));

        // equals with boolean
        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive=1 and theString like 'A%B'",
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A B", 1), true), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 1), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A B", 0), false));

        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanOneFilterNestedTwoLvl.class, new PermutationSpec(true), cases);
    }
}
