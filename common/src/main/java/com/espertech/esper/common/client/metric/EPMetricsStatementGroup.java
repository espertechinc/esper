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
package com.espertech.esper.common.client.metric;

import com.espertech.esper.common.internal.metrics.stmtmetrics.StatementMetricArray;

import java.util.function.Consumer;

/**
 * Represents a statement group in the query API for statement metrics
 */
public class EPMetricsStatementGroup {
    private final StatementMetricArray array;

    /**
     * Ctor.
     * @param array statements
     */
    public EPMetricsStatementGroup(StatementMetricArray array) {
        this.array = array;
    }

    /**
     * Returns the group name
     * @return group name
     */
    public String getName() {
        return array.getName();
    }

    /**
     * Returns an indicator whether to report inactive statements
     * @return indicator
     */
    public boolean isReportInactive() {
        return array.isReportInactive();
    }

    /**
     * Iterate statements of the group.
     * <p>
     *     This obtains a read-lock on the list of statements belonging to that group, for the duration of the call.
     * </p>
     * @param consumer receives the statement metrics
     */
    public void iterateStatements(Consumer<EPMetricsStatement> consumer) {
        array.iterate(consumer);
    }
}
