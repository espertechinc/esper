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

public class ExprFilterPlanTwoFilterDifferent extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {

        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        FilterTestMultiStmtAssertStats[] stats = FilterTestMultiStmtAssertStats.makeTwoSameStat("P0=(fh:1, fi:1),P1=(fh:2, fi:1),P2=(fh:1, fi:1),P3=(fh:0, fi:0, fipar:0)");

        // same equals-index, different value
        FilterTestMultiStmtPermutable.addCase(cases, stats, "intPrimitive = 0", "intPrimitive = 1",
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E1", 0), true, false), FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E2", 1), false, true),
            FilterTestMultiStmtAssertItem.makeItem(SupportBean.makeBean("E3", -1), false, false));

        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanTwoFilterDifferent.class, new PermutationSpec(true), cases);
    }
}
