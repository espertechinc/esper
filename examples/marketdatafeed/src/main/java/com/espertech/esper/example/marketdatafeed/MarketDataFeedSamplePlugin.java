package com.espertech.esper.example.marketdatafeed;

import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import com.espertech.esper.client.EPServiceProviderManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PluginLoader for added this example as part of an Esper configuration file and therefore execute it during startup.
 */
public class MarketDataFeedSamplePlugin implements PluginLoader
{
    private static final Log log = LogFactory.getLog(MarketDataFeedSamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private FeedSimMain feedSimMain;
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
        log.info("Starting MarketDataFeed-example for engine URI '" + engineURI + "'.");

        try {
            feedSimMain = new FeedSimMain(1, 0.01, 10, false, engineURI, true);
            simulationThread = new Thread(feedSimMain, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
        }
        catch (Exception e) {
            log.error("Error starting MarketDataFeed example: " + e.getMessage());
        }

        log.info("MarketDataFeed-example started.");
    }

    public void destroy()
    {
        if (feedSimMain != null) {
            EPServiceProviderManager.getProvider(engineURI).getEPAdministrator().destroyAllStatements();
        }
        try {
            simulationThread.interrupt();
            simulationThread.join();
        }
        catch (InterruptedException e) {
            log.info("Interrupted", e);
        }
        feedSimMain = null;
        log.info("MarketDataFeed-example stopped.");
    }
}
