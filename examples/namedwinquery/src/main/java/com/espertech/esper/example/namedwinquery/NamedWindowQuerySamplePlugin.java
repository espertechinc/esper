package com.espertech.esper.example.namedwinquery;

import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import com.espertech.esper.client.EPServiceProviderManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PluginLoader for added this example as part of an Esper configuration file and therefore execute it during startup.
 */
public class NamedWindowQuerySamplePlugin implements PluginLoader
{
    private static final Log log = LogFactory.getLog(NamedWindowQuerySamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private NamedWindowQueryMain main;

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
        log.info("Starting NamedWindowQuery-example for engine URI '" + engineURI + "'.");

        try {
            main = new NamedWindowQueryMain();
            main.runExample(true, engineURI);
        }
        catch (Exception e) {
            log.error("Error starting NamedWindowQuery example: " + e.getMessage());
        }

        log.info("NamedWindowQuery-example started.");
    }

    public void destroy()
    {
        if (main != null) {
            EPServiceProviderManager.getProvider(engineURI).getEPAdministrator().destroyAllStatements();
        }
        log.info("NamedWindowQuery-example stopped.");
    }
}

