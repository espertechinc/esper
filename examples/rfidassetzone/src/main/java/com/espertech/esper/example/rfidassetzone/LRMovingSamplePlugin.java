package com.espertech.esper.example.rfidassetzone;

import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LRMovingSamplePlugin implements PluginLoader
{
    private static final Log log = LogFactory.getLog(LRMovingSamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private LRMovingSimMain main;
    private Thread simulationThread;

    public void init(PluginLoaderInitContext context)
    {
        if (context.getProperties().getProperty(ENGINE_URI) != null)
        {
            engineURI = context.getProperties().getProperty(ENGINE_URI);
        }
        else
        {
            engineURI = context.getEpServiceProvider().getURI();
        }
    }

    public void postInitialize()
    {
        log.info("Starting RFIDAssetZone-example for engine URI '" + engineURI + "'.");

        try {
            main = new LRMovingSimMain(1, 100, 1, false, engineURI, false);
            simulationThread = new Thread(main, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
        }
        catch (Exception e) {
            log.error("Error starting RFIDAssetZone example: " + e.getMessage());
        }

        log.info("RFIDAssetZone-example started.");
    }

    public void destroy()
    {
        if (main != null) {
            EPServiceProviderManager.getProvider(engineURI).getEPAdministrator().destroyAllStatements();
        }
        try {
            simulationThread.interrupt();
            simulationThread.join();
        }
        catch (InterruptedException e) {
            log.info("Interrupted", e);
        }
        main = null;
        log.info("RFIDAssetZone-example stopped.");
    }
}
