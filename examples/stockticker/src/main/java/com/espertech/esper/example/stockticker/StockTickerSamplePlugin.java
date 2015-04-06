package com.espertech.esper.example.stockticker;

import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StockTickerSamplePlugin implements PluginLoader
{
    private static final Log log = LogFactory.getLog(StockTickerSamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private StockTickerMain main;
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
        log.info("Starting StockTicker-example for engine URI '" + engineURI + "'.");

        try {
            main = new StockTickerMain(engineURI, true);
            simulationThread = new Thread(main, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
            main.run();
        }
        catch (Exception e) {
            log.error("Error starting StockTicker example: " + e.getMessage());
        }

        log.info("StockTicker-example started.");
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
        log.info("StockTicker-example stopped.");
    }
}
