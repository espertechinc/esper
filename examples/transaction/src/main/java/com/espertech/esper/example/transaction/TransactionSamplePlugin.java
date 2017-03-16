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
package com.espertech.esper.example.transaction;

import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.example.transaction.sim.TxnGenMain;
import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionSamplePlugin implements PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(TransactionSamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private TxnGenMain main;
    private Thread simulationThread;

    public void init(PluginLoaderInitContext context) {
        if (context.getProperties().getProperty(ENGINE_URI) != null) {
            engineURI = context.getProperties().getProperty(ENGINE_URI);
        } else {
            engineURI = context.getEpServiceProvider().getURI();
        }
    }

    public void postInitialize() {
        log.info("Starting Transaction-example for engine URI '" + engineURI + "'.");

        try {
            main = new TxnGenMain(20, 200, engineURI, true);
            simulationThread = new Thread(main, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
        } catch (Exception e) {
            log.error("Error starting Transaction example: " + e.getMessage());
        }

        log.info("Transaction-example started.");
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
        log.info("Transaction-example stopped.");
    }
}
