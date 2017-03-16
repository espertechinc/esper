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
package com.espertech.esper.example.stockticker;

import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockTickerSamplePlugin implements PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(StockTickerSamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private StockTickerMain main;
    private Thread simulationThread;

    public void init(PluginLoaderInitContext context) {
        if (context.getProperties().getProperty(ENGINE_URI) != null) {
            engineURI = context.getProperties().getProperty(ENGINE_URI);
        } else {
            engineURI = context.getEpServiceProvider().getURI();
        }
    }

    public void postInitialize() {
        log.info("Starting StockTicker-example for engine URI '" + engineURI + "'.");

        try {
            main = new StockTickerMain(engineURI, true);
            simulationThread = new Thread(main, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
            main.run();
        } catch (Exception e) {
            log.error("Error starting StockTicker example: " + e.getMessage());
        }

        log.info("StockTicker-example started.");
    }

    public void destroy() {
        if (main != null) {
            EPServiceProviderManager.getProvider(engineURI).getEPAdministrator().destroyAllStatements();
        }
        try {
            simulationThread.interrupt();
            simulationThread.join();
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
        }
        main = null;
        log.info("StockTicker-example stopped.");
    }
}
