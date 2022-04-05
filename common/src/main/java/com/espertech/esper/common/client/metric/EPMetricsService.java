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

import com.espertech.esper.common.client.configuration.ConfigurationException;

import java.util.function.Consumer;

/**
 * Service for metrics reporting.
 */
public interface EPMetricsService {
    /**
     * Sets a new interval for metrics reporting for a pre-configured statement group, or changes
     * the default statement reporting interval if supplying a null value for the statement group name.
     *
     * @param stmtGroupName   name of statement group, provide a null value for the default statement interval (default group)
     * @param newIntervalMSec millisecond interval, use zero or negative value to disable
     * @throws ConfigurationException if the statement group cannot be found
     */
    void setMetricsReportingInterval(String stmtGroupName, long newIntervalMSec) throws ConfigurationException;

    /**
     * Enable metrics reporting for the given statement.
     * <p>
     * This operation can only be performed at runtime and is not available at runtime initialization time.
     * <p>
     * Statement metric reporting follows the configured default or statement group interval.
     * <p>
     * Only if metrics reporting (on the runtimelevel) has been enabled at initialization time
     * can statement-level metrics reporting be enabled through this method.
     *
     * @param deploymentId  for which to enable metrics reporting
     * @param statementName for which to enable metrics reporting
     * @throws ConfigurationException if the statement cannot be found
     */
    void setMetricsReportingStmtEnabled(String deploymentId, String statementName) throws ConfigurationException;

    /**
     * Disable metrics reporting for a given statement.
     *
     * @param deploymentId  for which to enable metrics reporting
     * @param statementName for which to disable metrics reporting
     * @throws ConfigurationException if the statement cannot be found
     */
    void setMetricsReportingStmtDisabled(String deploymentId, String statementName) throws ConfigurationException;

    /**
     * Enable runtime-level metrics reporting.
     * <p>
     * Use this operation to control, at runtime, metrics reporting globally.
     * <p>
     * Only if metrics reporting (on the runtimelevel) has been enabled at initialization time
     * can metrics reporting be re-enabled at runtime through this method.
     *
     * @throws ConfigurationException if use at runtime and metrics reporting had not been enabled at initialization time
     */
    void setMetricsReportingEnabled() throws ConfigurationException;

    /**
     * Disable runtime-level metrics reporting.
     * <p>
     * Use this operation to control, at runtime, metrics reporting globally. Setting metrics reporting
     * to disabled removes all performance cost for metrics reporting.
     *
     * @throws ConfigurationException if use at runtime and metrics reporting had not been enabled at initialization time
     */
    void setMetricsReportingDisabled() throws ConfigurationException;

    /**
     * Iterate statement groups.
     * <p>
     *     This by itself does not hold a lock but iterating each group may hold locks.
     * </p>
     * @param consumer of groups
     */
    void iterateStatementGroups(Consumer<EPMetricsStatementGroup> consumer);

    /**
     * Returns the current runtime metric.
     * <p>
     *     The delta-value(s) will be zero. The result is without delta and without resetting count.
     * </p>
     * @return runtime metrio
     */
    RuntimeMetric getRuntimeMetric();
}
