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

import com.espertech.esper.common.client.configuration.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface for a factory class to provide services in a services context for an runtime instance.
 */
public interface EPServicesContextFactory {
    /**
     * Factory method for a new set of runtime services.
     *
     * @param epRuntime             is the runtime instance
     * @param configurationSnapshot is a snapshot of configs at the time of runtime creation
     * @return services context
     */
    EPServicesContext createServicesContext(EPRuntimeSPI epRuntime, Configuration configurationSnapshot);

    EPEventServiceImpl createEPRuntime(EPServicesContext services, AtomicBoolean serviceStatusProvider);
}
