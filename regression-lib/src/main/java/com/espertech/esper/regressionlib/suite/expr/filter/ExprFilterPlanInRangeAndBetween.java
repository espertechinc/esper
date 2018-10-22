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
import com.espertech.esper.regressionlib.support.filter.FilterTestCaseSingleField;
import com.espertech.esper.regressionlib.support.filter.FilterTestCaseSingleFieldExecution;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExprFilterPlanInRangeAndBetween extends TestCase {
    public static Collection<? extends RegressionExecution> executions() {

        List<FilterTestCaseSingleField> testCases = new ArrayList<FilterTestCaseSingleField>();
        FilterTestCaseSingleField.addCase(testCases, "(theString > 'b')", "theString", new String[]{"a", "b", "c", "d"}, new boolean[]{false, false, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(theString < 'b')", "theString", new String[]{"a", "b", "c", "d"}, new boolean[]{true, false, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(theString >= 'b')", "theString", new String[]{"a", "b", "c", "d"}, new boolean[]{false, true, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(theString <= 'b')", "theString", new String[]{"a", "b", "c", "d"}, new boolean[]{true, true, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(theString in ['b':'d'])", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{false, true, true, true, false});
        FilterTestCaseSingleField.addCase(testCases, "(theString in ('b':'d'])", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{false, false, true, true, false});
        FilterTestCaseSingleField.addCase(testCases, "(theString in ['b':'d'))", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{false, true, true, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(theString in ('b':'d'))", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{false, false, true, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(boolPrimitive in (false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{false, true});
        FilterTestCaseSingleField.addCase(testCases, "(boolPrimitive in (false, false, false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{false, true});
        FilterTestCaseSingleField.addCase(testCases, "(boolPrimitive in (false, true, false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{true, true});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed in (4, 6, 1))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, true, false, false, true, false, true});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed in (3))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, true, false, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(longBoxed in (3))", "longBoxed", new Object[]{0L, 1L, 2L, 3L, 4L, 5L, 6L}, new boolean[]{false, false, false, true, false, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed between 4 and 6)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, false, true, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed between 2 and 1)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, true, true, false, false, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed between 4 and -1)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, true, true, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed in [2:4])", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, true, true, true, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed in (2:4])", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, true, true, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed in [2:4))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, true, true, false, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed in (2:4))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, true, false, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed not between 4 and 6)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, true, false, false, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed not between 2 and 1)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, false, false, true, true, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed not between 4 and -1)", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{false, false, false, false, false, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed not in [2:4])", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, false, false, false, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed not in (2:4])", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, false, false, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed not in [2:4))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, false, false, true, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed not in (2:4))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, false, true, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(theString not in ['b':'d'])", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{true, false, false, false, true});
        FilterTestCaseSingleField.addCase(testCases, "(theString not in ('b':'d'])", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{true, true, false, false, true});
        FilterTestCaseSingleField.addCase(testCases, "(theString not in ['b':'d'))", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{true, false, false, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(theString not in ('b':'d'))", "theString", new String[]{"a", "b", "c", "d", "e"}, new boolean[]{true, true, false, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(theString not in ('a', 'b'))", "theString", new String[]{"a", "x", "b", "y"}, new boolean[]{false, true, false, true});
        FilterTestCaseSingleField.addCase(testCases, "(boolPrimitive not in (false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{true, false});
        FilterTestCaseSingleField.addCase(testCases, "(boolPrimitive not in (false, false, false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{true, false});
        FilterTestCaseSingleField.addCase(testCases, "(boolPrimitive not in (false, true, false))", "boolPrimitive", new Object[]{true, false}, new boolean[]{false, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed not in (4, 6, 1))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, false, true, true, false, true, false});
        FilterTestCaseSingleField.addCase(testCases, "(intBoxed not in (3))", "intBoxed", new Object[]{0, 1, 2, 3, 4, 5, 6}, new boolean[]{true, true, true, false, true, true, true});
        FilterTestCaseSingleField.addCase(testCases, "(longBoxed not in (3))", "longBoxed", new Object[]{0L, 1L, 2L, 3L, 4L, 5L, 6L}, new boolean[]{true, true, true, false, true, true, true});

        List<FilterTestCaseSingleFieldExecution> executions = new ArrayList<>();
        for (FilterTestCaseSingleField testCase : testCases) {
            executions.add(new FilterTestCaseSingleFieldExecution(ExprFilterPlanInRangeAndBetween.class, testCase, "P0-P1=(fh:1,fi:1),P2=(fh:0,fi:0,fipar:0)"));
        }
        return executions;
    }
}