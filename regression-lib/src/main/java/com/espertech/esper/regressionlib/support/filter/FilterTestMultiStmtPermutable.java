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
package com.espertech.esper.regressionlib.support.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterTestMultiStmtPermutable {
    private final String[] filters;
    private final FilterTestMultiStmtAssertStats[] statsPerPermutation;
    private final List<FilterTestMultiStmtAssertItem> items = new ArrayList<FilterTestMultiStmtAssertItem>();

    public FilterTestMultiStmtPermutable(FilterTestMultiStmtAssertStats[] statsPerPermutation, String... filters) {
        this.statsPerPermutation = statsPerPermutation;
        this.filters = filters;
    }

    public String[] getFilters() {
        return filters;
    }

    public List<FilterTestMultiStmtAssertItem> getItems() {
        return items;
    }

    public FilterTestMultiStmtAssertStats[] getStatsPerPermutation() {
        return statsPerPermutation;
    }

    public static void addCase(List<FilterTestMultiStmtPermutable> cases, FilterTestMultiStmtAssertStats[] statsPerPermutation, String filter, FilterTestMultiStmtAssertItem... items) {
        FilterTestMultiStmtPermutable theCase = new FilterTestMultiStmtPermutable(statsPerPermutation, filter);
        theCase.getItems().addAll(Arrays.asList(items));
        cases.add(theCase);
    }

    public static void addCase(List<FilterTestMultiStmtPermutable> cases, FilterTestMultiStmtAssertStats[] statsPerPermutation, String filterOne, String filterTwo, FilterTestMultiStmtAssertItem... items) {
        FilterTestMultiStmtPermutable theCase = new FilterTestMultiStmtPermutable(statsPerPermutation, filterOne, filterTwo);
        theCase.getItems().addAll(Arrays.asList(items));
        cases.add(theCase);
    }

    public static void addCase(List<FilterTestMultiStmtPermutable> cases, FilterTestMultiStmtAssertStats[] statsPerPermutation, String filterOne, String filterTwo, String filterThree, FilterTestMultiStmtAssertItem... items) {
        FilterTestMultiStmtPermutable theCase = new FilterTestMultiStmtPermutable(statsPerPermutation, filterOne, filterTwo, filterThree);
        theCase.getItems().addAll(Arrays.asList(items));
        cases.add(theCase);
    }
}
