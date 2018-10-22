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
package com.espertech.esper.common.internal.settings;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntime;

/**
 * Service for runtime-level settings around threading and concurrency.
 */
public class RuntimeSettingsService {
    private final ConfigurationCommon configurationCommon;
    private final ConfigurationRuntime configurationRuntime;

    public RuntimeSettingsService(ConfigurationCommon configurationCommon, ConfigurationRuntime configurationRuntime) {
        this.configurationCommon = configurationCommon;
        this.configurationRuntime = configurationRuntime;
    }

    public ConfigurationRuntime getConfigurationRuntime() {
        return configurationRuntime;
    }

    public ConfigurationCommon getConfigurationCommon() {
        return configurationCommon;
    }
}
