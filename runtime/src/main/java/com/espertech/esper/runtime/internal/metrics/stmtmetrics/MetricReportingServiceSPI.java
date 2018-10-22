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
package com.espertech.esper.runtime.internal.metrics.stmtmetrics;

import com.espertech.esper.common.internal.metrics.stmtmetrics.MetricReportingService;

import java.util.Set;

/**
 * SPI for metrics activity.
 */
public interface MetricReportingServiceSPI extends MetricReportingService {
    /**
     * Add stmt result listener.
     *
     * @param listener to add
     */
    public void addStatementResultListener(MetricsStatementResultListener listener);

    /**
     * Remove stmt result listener.
     *
     * @param listener to remove
     */
    public void removeStatementResultListener(MetricsStatementResultListener listener);

    /**
     * Returns output hooks.
     *
     * @return hooks.
     */
    public Set<MetricsStatementResultListener> getStatementOutputHooks();
}