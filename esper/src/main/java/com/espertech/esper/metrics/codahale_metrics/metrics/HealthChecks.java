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
package com.espertech.esper.metrics.codahale_metrics.metrics;

import com.espertech.esper.metrics.codahale_metrics.metrics.core.HealthCheck;
import com.espertech.esper.metrics.codahale_metrics.metrics.core.HealthCheckRegistry;

import java.util.Map;

/**
 * A manager class for health checks.
 */
public class HealthChecks {
    private static final HealthCheckRegistry DEFAULT_REGISTRY = new HealthCheckRegistry();

    private HealthChecks() { /* unused */ }

    /**
     * Registers an application {@link HealthCheck} with a given name.
     *
     * @param healthCheck the {@link HealthCheck} instance
     */
    public static void register(HealthCheck healthCheck) {
        DEFAULT_REGISTRY.register(healthCheck);
    }

    /**
     * Runs the registered health checks and returns a map of the results.
     *
     * @return a map of the health check results
     */
    public static Map<String, HealthCheck.Result> runHealthChecks() {
        return DEFAULT_REGISTRY.runHealthChecks();
    }

    /**
     * Returns the (static) default registry.
     *
     * @return the registry
     */
    public static HealthCheckRegistry defaultRegistry() {
        return DEFAULT_REGISTRY;
    }
}
