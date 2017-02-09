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
package com.espertech.esper.epl.metric;

import com.espertech.esper.core.service.StatementResultListener;

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
    public void addStatementResultListener(StatementResultListener listener);

    /**
     * Remove stmt result listener.
     *
     * @param listener to remove
     */
    public void removeStatementResultListener(StatementResultListener listener);

    /**
     * Returns output hooks.
     *
     * @return hooks.
     */
    public Set<StatementResultListener> getStatementOutputHooks();
}