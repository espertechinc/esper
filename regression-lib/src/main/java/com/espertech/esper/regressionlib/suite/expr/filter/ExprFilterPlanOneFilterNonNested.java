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

public class ExprFilterPlanOneFilterNonNested extends TestCase {

    public static Collection<? extends RegressionExecution> executions() {

        List<FilterTestMultiStmtPermutable> cases = new ArrayList<FilterTestMultiStmtPermutable>();

        FilterTestMultiStmtAssertStats[] stats = FilterTestMultiStmtAssertStats.makeSingleStat("P0=(fh:1,fi:1),P1=(fh:0,fi:0,fipar:0)");

        // simple equals
        addCase(cases, stats, "intPrimitive = 0", makeItem(makeBean("E1", 0), true), makeItem(makeBean("E2", 1), false));

        // simple "not-equals"
        addCase(cases, stats, "theString != 'A'", makeItem(makeBean("B"), true), makeItem(makeBean("A"), false));

        // simple greater
        addCase(cases, stats, "intPrimitive >= 0", makeItem(makeBean("E1", 0), true), makeItem(makeBean("E1", 1), true), makeItem(makeBean("E2", -1), false));

        // simple "is"
        addCase(cases, stats, "theString is null", makeItem(makeBean(null), true), makeItem(makeBean("A"), false));

        // simple "is-not"
        addCase(cases, stats, "theString is not null", makeItem(makeBean(null), false), makeItem(makeBean("A"), true));

        // simple boolean expression
        addCase(cases, stats, "theString like 'A%'", makeItem(makeBean("A"), true), makeItem(makeBean("B"), false));
        addCase(cases, stats, "getLocalValue(theString) = 'A'", makeItem(makeBean("A"), true), makeItem(makeBean("B"), false));

        // nullable-endpoint range handled as boolean
        addCase(cases, stats, "theString between null and 'Z'", makeItem(makeBean("A"), false), makeItem(makeBean("B"), false));

        return FilterTestMultiStmtRunner.computePermutations(ExprFilterPlanOneFilterNonNested.class, new PermutationSpec(true), cases);
    }

    public static String getLocalValue(String value) {
        return value;
    }
}
