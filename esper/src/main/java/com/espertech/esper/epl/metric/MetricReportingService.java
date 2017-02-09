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

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.core.service.EPServicesContext;

/**
 * Metrics reporting service for instrumentation data publishing, if enabled.
 */
public interface MetricReportingService {
    /**
     * Sets runtime and services.
     *
     * @param runtime         runtime
     * @param servicesContext services
     */
    public void setContext(EPRuntime runtime, EPServicesContext servicesContext);

    /**
     * Indicates current engine time.
     *
     * @param currentTime engine time
     */
    public void processTimeEvent(long currentTime);

    /**
     * Destroy the service.
     */
    public void destroy();

    /**
     * Account for statement CPU and wall time.
     *
     * @param metricsHandle statement handle
     * @param deltaCPU      cpu time nsec
     * @param deltaWall     wall time nsec
     * @param numInput      number of input rows
     */
    public void accountTime(StatementMetricHandle metricsHandle, long deltaCPU, long deltaWall, int numInput);

    /**
     * Account for statement output row counting.
     *
     * @param handle     statement handle
     * @param numIStream number of insert stream rows
     * @param numRStream number of remove stream rows
     */
    public void accountOutput(StatementMetricHandle handle, int numIStream, int numRStream);

    /**
     * Returns for a new statement a handle for later accounting.
     *
     * @param statementId   statement id
     * @param statementName statement name
     * @return handle
     */
    public StatementMetricHandle getStatementHandle(int statementId, String statementName);

    /**
     * Change the reporting interval for the given statement group name.
     *
     * @param stmtGroupName group name
     * @param newInterval   new interval, or zero or negative value to disable reporting
     */
    public void setMetricsReportingInterval(String stmtGroupName, long newInterval);

    /**
     * Disable metrics reporting for statement.
     *
     * @param statementName statement name
     */
    public void setMetricsReportingStmtDisabled(String statementName);

    /**
     * Enable metrics reporting for statement.
     *
     * @param statementName statement name
     */
    public void setMetricsReportingStmtEnabled(String statementName);

    /**
     * Enables metrics reporting globally.
     */
    public void setMetricsReportingEnabled();

    /**
     * Disables metrics reporting globally.
     */
    public void setMetricsReportingDisabled();
}
