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


public class ExprFilterPlanOneFilterNestedThreeLvl extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {
        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        FilterTestMultiStmtAssertStats[] stats = FilterTestMultiStmtAssertStats.makeSingleStat("P0=(fh:1, fi:3),P1=(fh:0, fi:0, fipar:0)");

        // simple three-param equals
        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive=1, longPrimitive=10, doublePrimitive=100", FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 1, 10, 100), true),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 1, 10, -1), false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 0, 10, 100), false));

        // two-param equals with boolean
        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive=1, longPrimitive=10, theString like 'A%'",
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 1, 10), true),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("B", 1, 10), false),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("A", 1, 0), false));

        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanOneFilterNestedThreeLvl.class, new PermutationSpec(true), cases);
    }
}
