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
package com.espertech.esper.epl.core.engineimport;

import com.espertech.esper.client.ConfigurationEngineDefaults;

import java.net.URI;

/**
 * Service for engine-level settings around threading and concurrency.
 */
public class EngineSettingsService {
    private ConfigurationEngineDefaults config;
    private URI[] plugInEventTypeResolutionURIs;

    /**
     * Ctor.
     *
     * @param config                        is the configured defaults
     * @param plugInEventTypeResolutionURIs is URIs for resolving the event name against plug-inn event representations, if any
     */
    public EngineSettingsService(ConfigurationEngineDefaults config, URI[] plugInEventTypeResolutionURIs) {
        this.config = config;
        this.plugInEventTypeResolutionURIs = plugInEventTypeResolutionURIs;
    }

    /**
     * Returns the settings.
     *
     * @return engine settings
     */
    public ConfigurationEngineDefaults getEngineSettings() {
        return config;
    }

    /**
     * Returns URIs for resolving the event name against plug-in event representations, if any.
     *
     * @return URIs
     */
    public URI[] getPlugInEventTypeResolutionURIs() {
        return plugInEventTypeResolutionURIs;
    }

    /**
     * Sets URIs for resolving the event name against plug-in event representations, if any.
     *
     * @param plugInEventTypeResolutionURIs URIs
     */
    public void setPlugInEventTypeResolutionURIs(URI[] plugInEventTypeResolutionURIs) {
        this.plugInEventTypeResolutionURIs = plugInEventTypeResolutionURIs;
    }
}
