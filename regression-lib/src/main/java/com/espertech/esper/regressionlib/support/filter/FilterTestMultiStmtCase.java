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

import java.util.List;

public class FilterTestMultiStmtCase {
    private final String[] filters;
    private final String stats;
    private final List<FilterTestMultiStmtAssertItem> items;

    public FilterTestMultiStmtCase(String[] filters, String stats, List<FilterTestMultiStmtAssertItem> items) {
        this.filters = filters;
        this.stats = stats;
        this.items = items;
    }

    public String[] getFilters() {
        return filters;
    }

    public String getStats() {
        return stats;
    }

    public List<FilterTestMultiStmtAssertItem> getItems() {
        return items;
    }
}
