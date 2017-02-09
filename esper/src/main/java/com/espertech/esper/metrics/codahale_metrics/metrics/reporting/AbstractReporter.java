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
/***************************************************************************************
 * Attribution Notice
 *
 * This file is imported from Metrics (https://github.com/codahale/metrics subproject metrics-core).
 * Metrics is Copyright (c) 2010-2012 Coda Hale, Yammer.com
 * Metrics is Published under Apache Software License 2.0, see LICENSE in root folder.
 *
 * Thank you for the Metrics developers efforts in making their library available under an Apache license.
 * EsperTech incorporates Metrics version 0.2.2 in source code form since Metrics depends on SLF4J
 * and this dependency is not possible to introduce for Esper.
 * *************************************************************************************
 */

package com.espertech.esper.metrics.codahale_metrics.metrics.reporting;

import com.espertech.esper.metrics.codahale_metrics.metrics.core.MetricsRegistry;

/**
 * The base class for all metric reporters.
 */
public abstract class AbstractReporter {
    private final MetricsRegistry metricsRegistry;

    /**
     * Creates a new {@link AbstractReporter} instance.
     *
     * @param registry the {@link MetricsRegistry} containing the metrics this reporter will
     *                 report
     */
    protected AbstractReporter(MetricsRegistry registry) {
        this.metricsRegistry = registry;
    }

    /**
     * Stops the reporter and closes any internal resources.
     */
    public void shutdown() {
        // nothing to do here
    }

    /**
     * Returns the reporter's {@link MetricsRegistry}.
     *
     * @return the reporter's {@link MetricsRegistry}
     */
    protected MetricsRegistry getMetricsRegistry() {
        return metricsRegistry;
    }
}
