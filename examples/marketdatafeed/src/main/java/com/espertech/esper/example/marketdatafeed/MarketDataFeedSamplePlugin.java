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
package com.espertech.esper.example.marketdatafeed;

import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esper.runtime.client.plugin.PluginLoader;
import com.espertech.esper.runtime.client.plugin.PluginLoaderInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PluginLoader for added this example as part of an Esper configuration file and therefore execute it during startup.
 */
public class MarketDataFeedSamplePlugin implements PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(MarketDataFeedSamplePlugin.class);

    private static final String RUNTIME_URI = "runtimeURI";

    private String runtimeURI;
    private FeedSimMain feedSimMain;
    private Thread simulationThread;

    public void init(PluginLoaderInitContext context) {
        if (context.getProperties().getProperty(RUNTIME_URI) != null) {
            runtimeURI = context.getProperties().getProperty(RUNTIME_URI);
        } else {
            runtimeURI = context.getRuntime().getURI();
        }
    }

    public void postInitialize() {
        log.info("Starting MarketDataFeed-example for runtime URI '" + runtimeURI + "'.");

        try {
            feedSimMain = new FeedSimMain(1, 0.01, 10, false, runtimeURI, true);
            simulationThread = new Thread(feedSimMain, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
        } catch (Exception e) {
            log.error("Error starting MarketDataFeed example: " + e.getMessage());
        }

        log.info("MarketDataFeed-example started.");
    }

    public void destroy() {
        if (feedSimMain != null) {
            try {
                EPRuntimeProvider.getRuntime(runtimeURI).getDeploymentService().undeployAll();
            } catch (EPUndeployException e) {
                log.warn("Failed to undeploy: " + e.getMessage(), e);
            }
        }
        try {
            simulationThread.interrupt();
            simulationThread.join();
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
        }
        feedSimMain = null;
        log.info("MarketDataFeed-example stopped.");
    }
}
