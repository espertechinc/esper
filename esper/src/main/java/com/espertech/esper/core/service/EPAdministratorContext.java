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
package com.espertech.esper.core.service;

import com.espertech.esper.client.ConfigurationOperations;
import com.espertech.esper.epl.spec.SelectClauseStreamSelectorEnum;

/**
 * Context for administrative services.
 */
public class EPAdministratorContext {
    private final EPRuntimeSPI runtimeSPI;
    private final EPServicesContext services;
    private final ConfigurationOperations configurationOperations;
    private final SelectClauseStreamSelectorEnum defaultStreamSelector;

    /**
     * Ctor.
     *
     * @param runtimeSPI              runtime spi
     * @param services                engine services
     * @param configurationOperations configuration snapshot
     * @param defaultStreamSelector   default stream selection
     */
    public EPAdministratorContext(EPRuntimeSPI runtimeSPI, EPServicesContext services, ConfigurationOperations configurationOperations, SelectClauseStreamSelectorEnum defaultStreamSelector) {
        this.runtimeSPI = runtimeSPI;
        this.configurationOperations = configurationOperations;
        this.defaultStreamSelector = defaultStreamSelector;
        this.services = services;
    }

    public EPRuntimeSPI getRuntimeSPI() {
        return runtimeSPI;
    }

    /**
     * Returns configuration.
     *
     * @return configuration
     */
    public ConfigurationOperations getConfigurationOperations() {
        return configurationOperations;
    }

    /**
     * Returns the default stream selector.
     *
     * @return default stream selector
     */
    public SelectClauseStreamSelectorEnum getDefaultStreamSelector() {
        return defaultStreamSelector;
    }

    /**
     * Returns the engine services context.
     *
     * @return engine services
     */
    public EPServicesContext getServices() {
        return services;
    }
}