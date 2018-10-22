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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.metric.EPMetricsService;

public class EPMetricsServiceImpl implements EPMetricsService {
    private final EPServicesContext services;

    public EPMetricsServiceImpl(EPServicesContext services) {
        this.services = services;
    }

    public void setMetricsReportingInterval(String stmtGroupName, long newIntervalMSec) throws ConfigurationException {
        try {
            services.getMetricReportingService().setMetricsReportingInterval(stmtGroupName, newIntervalMSec);
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error updating interval for metric reporting: " + e.getMessage(), e);
        }
    }

    public void setMetricsReportingStmtEnabled(String deploymentId, String statementName) throws ConfigurationException {
        try {
            services.getMetricReportingService().setMetricsReportingStmtEnabled(deploymentId, statementName);
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error enabling metric reporting for statement: " + e.getMessage(), e);
        }
    }

    public void setMetricsReportingStmtDisabled(String deploymentId, String statementName) throws ConfigurationException {
        try {
            services.getMetricReportingService().setMetricsReportingStmtDisabled(deploymentId, statementName);
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error enabling metric reporting for statement: " + e.getMessage(), e);
        }
    }

    public void setMetricsReportingEnabled() throws ConfigurationException {
        try {
            services.getMetricReportingService().setMetricsReportingEnabled();
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error enabling metric reporting: " + e.getMessage(), e);
        }
    }

    public void setMetricsReportingDisabled() throws ConfigurationException {
        try {
            services.getMetricReportingService().setMetricsReportingDisabled();
        } catch (RuntimeException e) {
            throw new ConfigurationException("Error enabling metric reporting: " + e.getMessage(), e);
        }
    }

}
