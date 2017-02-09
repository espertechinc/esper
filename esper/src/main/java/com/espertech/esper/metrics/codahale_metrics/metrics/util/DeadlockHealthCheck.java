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
package com.espertech.esper.metrics.codahale_metrics.metrics.util;

import com.espertech.esper.metrics.codahale_metrics.metrics.core.HealthCheck;
import com.espertech.esper.metrics.codahale_metrics.metrics.core.VirtualMachineMetrics;

import java.util.Set;

/**
 * A {@link HealthCheck} implementation which returns a list of deadlocked threads, if any.
 */
public class DeadlockHealthCheck extends HealthCheck {
    private final VirtualMachineMetrics vm;

    /**
     * Creates a new {@link DeadlockHealthCheck} with the given {@link VirtualMachineMetrics}
     * instance.
     *
     * @param vm a {@link VirtualMachineMetrics} instance
     */
    public DeadlockHealthCheck(VirtualMachineMetrics vm) {
        super("deadlocks");
        this.vm = vm;
    }

    /**
     * Creates a new {@link DeadlockHealthCheck}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public DeadlockHealthCheck() {
        this(VirtualMachineMetrics.getInstance());
    }

    @Override
    protected Result check() throws Exception {
        final Set<String> threads = vm.deadlockedThreads();
        if (threads.isEmpty()) {
            return Result.healthy();
        }

        final StringBuilder builder = new StringBuilder("Deadlocked threads detected:\n");
        for (String thread : threads) {
            builder.append(thread).append('\n');
        }
        return Result.unhealthy(builder.toString());
    }
}
