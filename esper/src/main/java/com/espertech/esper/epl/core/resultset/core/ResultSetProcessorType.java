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
package com.espertech.esper.epl.core.resultset.core;

public enum ResultSetProcessorType {
    HANDTHROUGH(false, false),
    UNAGGREGATED_UNGROUPED(false, false),
    FULLYAGGREGATED_UNGROUPED(true, false),
    AGGREGATED_UNGROUPED(true, false),
    FULLYAGGREGATED_GROUPED(true, true),
    FULLYAGGREGATED_GROUPED_ROLLUP(true, true),
    AGGREGATED_GROUPED(true, true);

    private final boolean aggregated;
    private final boolean grouped;

    ResultSetProcessorType(boolean aggregated, boolean grouped) {
        this.aggregated = aggregated;
        this.grouped = grouped;
    }

    public boolean isAggregated() {
        return aggregated;
    }

    public boolean isGrouped() {
        return grouped;
    }

    public boolean isUnaggregatedUngrouped() {
        return !isAggregated() && !isGrouped();
    }
}
