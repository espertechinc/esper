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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global boolean for enabling and disable metrics reporting.
 */
public class MetricReportingPath {
    private static final Logger log = LoggerFactory.getLogger(MetricReportingPath.class);

    /**
     * Public access.
     */
    public static boolean isMetricsEnabled = false;

    /**
     * Sets execution path debug logging.
     *
     * @param metricsEnabled true if metric reporting should be enabled
     */
    public static void setMetricsEnabled(boolean metricsEnabled) {
        if (metricsEnabled) {
            log.info("Metrics reporting has been enabled, this setting takes affect for all engine instances at engine initialization time.");
        } else {
            log.debug("Metrics reporting has been disabled, this setting takes affect for all engine instances at engine initialization time.");
        }
        isMetricsEnabled = metricsEnabled;
    }
}
