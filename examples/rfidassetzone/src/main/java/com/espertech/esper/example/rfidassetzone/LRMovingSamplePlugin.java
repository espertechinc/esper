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
package com.espertech.esper.example.rfidassetzone;

import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esper.runtime.client.plugin.PluginLoader;
import com.espertech.esper.runtime.client.plugin.PluginLoaderInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LRMovingSamplePlugin implements PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(LRMovingSamplePlugin.class);

    private static final String RUNTIME_URI = "runtimeURI";

    private String runtimeURI;
    private LRMovingSimMain main;
    private Thread simulationThread;

    public void init(PluginLoaderInitContext context) {
        if (context.getProperties().getProperty(RUNTIME_URI) != null) {
            runtimeURI = context.getProperties().getProperty(RUNTIME_URI);
        } else {
            runtimeURI = context.getRuntime().getURI();
        }
    }

    public void postInitialize() {
        log.info("Starting RFIDAssetZone-example for runtime URI '" + runtimeURI + "'.");

        try {
            main = new LRMovingSimMain(1, 100, 1, false, runtimeURI, false);
            simulationThread = new Thread(main, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
        } catch (Exception e) {
            log.error("Error starting RFIDAssetZone example: " + e.getMessage());
        }

        log.info("RFIDAssetZone-example started.");
    }

    public void destroy() {
        if (main != null) {
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
        main = null;
        log.info("RFIDAssetZone-example stopped.");
    }
}
