package com.espertech.esper.example.autoid;

import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PluginLoader for added this example as part of an Esper configuration file and therefore execute it during startup.
 */
public class AutoIdSamplePlugin implements PluginLoader
{
    private static final Log log = LogFactory.getLog(AutoIdSamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private AutoIdSimMain autoIdSimMain;
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
        log.info("Starting AutoID-example for engine URI '" + engineURI + "'.");

        try {
            autoIdSimMain = new AutoIdSimMain(1000, engineURI, true);
            simulationThread = new Thread(autoIdSimMain, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
        }
        catch (Exception e) {
            log.error("Error starting AutoID example: " + e.getMessage());
        }

        log.info("AutoID-example started.");
    }

    public void destroy()
    {
        if (autoIdSimMain != null) {
            autoIdSimMain.destroy();
        }
        try {
            simulationThread.interrupt();
            simulationThread.join();
        }
        catch (InterruptedException e) {
            log.info("Interrupted", e);
        }
        autoIdSimMain = null;
        log.info("AutoID-example stopped.");
    }
}

