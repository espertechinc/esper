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
package com.espertech.esper.supportregression.util;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SupportEngineFactory {

    public static Map<TimeUnit, EPServiceProvider> setupEnginesByTimeUnit() {
        Map<TimeUnit, EPServiceProvider> engines = new HashMap<>();
        engines.put(TimeUnit.MILLISECONDS, setupEngine("default_millis", TimeUnit.MILLISECONDS));
        engines.put(TimeUnit.MICROSECONDS, setupEngine("default_micros", TimeUnit.MICROSECONDS));
        return engines;
    }

    public static EPServiceProvider setupEngineDefault(TimeUnit timeUnit, long startTime) {
        EPServiceProvider epService = setupEngine(EPServiceProviderSPI.DEFAULT_ENGINE_URI, timeUnit);
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(startTime));
        return epService;
    }

    private static EPServiceProvider setupEngine(String engineURI, TimeUnit timeUnit) {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.getEngineDefaults().getTimeSource().setTimeUnit(timeUnit);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        configuration.getEngineDefaults().getViewResources().setShareViews(false);
        EPServiceProvider epService = EPServiceProviderManager.getProvider(engineURI, configuration);
        epService.initialize();
        return epService;
    }
}
