package com.espertech.esper.example.ohlc;

import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PluginLoader for added this example as part of an Esper configuration file and therefore execute it during startup.
 */
public class OHLCSamplePlugin implements PluginLoader
{
    private static final Log log = LogFactory.getLog(OHLCSamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private OHLCMain main;

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
        log.info("Starting OHLCSample-example for engine URI '" + engineURI + "'.");

        try {
            main = new OHLCMain();
            main.run(engineURI);
        }
        catch (Exception e) {
            log.error("Error starting OHLCSample example: " + e.getMessage());
        }

        log.info("OHLCSample-example started.");
    }

    public void destroy()
    {
        if (main != null) {
            EPServiceProviderManager.getProvider(engineURI).getEPAdministrator().destroyAllStatements();
        }
        log.info("OHLCSample-example stopped.");
    }
}