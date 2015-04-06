package com.espertech.esper.example.matchmaker;

import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PluginLoader for added this example as part of an Esper configuration file and therefore execute it during startup.
 */
public class MatchMakerSamplePlugin implements PluginLoader
{
    private static final Log log = LogFactory.getLog(MatchMakerSamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private MatchMakerMain matchMakerMain;
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
        log.info("Starting MatchMaker-example for engine URI '" + engineURI + "'.");

        try {
            matchMakerMain = new MatchMakerMain(engineURI, true);
            simulationThread = new Thread(matchMakerMain, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
        }
        catch (Exception e) {
            log.error("Error starting MatchMaker example: " + e.getMessage());
        }

        log.info("MatchMaker-example started.");
    }

    public void destroy()
    {
        if (matchMakerMain != null) {
            EPServiceProviderManager.getProvider(engineURI).getEPAdministrator().destroyAllStatements();
        }
        try {
            simulationThread.interrupt();
            simulationThread.join();
        }
        catch (InterruptedException e) {
            log.info("Interrupted", e);
        }
        matchMakerMain = null;
        log.info("MatchMaker-example stopped.");
    }
}
