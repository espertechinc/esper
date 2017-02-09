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
package com.espertech.esper.metrics.codahale_metrics.metrics.core;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry for health checks.
 */
public class HealthCheckRegistry {
    private final ConcurrentMap<String, HealthCheck> healthChecks = new ConcurrentHashMap<String, HealthCheck>();

    /**
     * Registers an application {@link HealthCheck}.
     *
     * @param healthCheck the {@link HealthCheck} instance
     */
    public void register(HealthCheck healthCheck) {
        healthChecks.putIfAbsent(healthCheck.getName(), healthCheck);
    }

    /**
     * Unregisters the application {@link HealthCheck} with the given name.
     *
     * @param name the name of the {@link HealthCheck} instance
     */
    public void unregister(String name) {
        healthChecks.remove(name);
    }

    /**
     * Unregisters the given {@link HealthCheck}.
     *
     * @param healthCheck a {@link HealthCheck}
     */
    public void unregister(HealthCheck healthCheck) {
        unregister(healthCheck.getName());
    }

    /**
     * Runs the registered health checks and returns a map of the results.
     *
     * @return a map of the health check results
     */
    public SortedMap<String, HealthCheck.Result> runHealthChecks() {
        final SortedMap<String, HealthCheck.Result> results = new TreeMap<String, HealthCheck.Result>();
        for (Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
            final HealthCheck.Result result = entry.getValue().execute();
            results.put(entry.getKey(), result);
        }
        return Collections.unmodifiableSortedMap(results);
    }
}
