package com.espertech.esper.example.qos_sla;

import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PluginLoader for added this example as part of an Esper configuration file and therefore execute it during startup.
 */
public class QualityOfServiceSamplePlugin implements PluginLoader
{
    private static final Log log = LogFactory.getLog(QualityOfServiceSamplePlugin.class);

    private static final String ENGINE_URI = "engineURI";

    private String engineURI;
    private QualityOfServiceMain main;
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
        log.info("Starting QualityOfService-example for engine URI '" + engineURI + "'.");

        try {
            main = new QualityOfServiceMain(true);
            simulationThread = new Thread(main, this.getClass().getName() + "-simulator");
            simulationThread.setDaemon(true);
            simulationThread.start();
        }
        catch (Exception e) {
            log.error("Error starting QualityOfService example: " + e.getMessage());
        }

        log.info("QualityOfService-example started.");
    }

    public void destroy()
    {
        if (main != null) {
            EPServiceProviderManager.getDefaultProvider().getEPAdministrator().destroyAllStatements();
        }
        try {
            simulationThread.interrupt();
            simulationThread.join();
        }
        catch (InterruptedException e) {
            log.info("Interrupted", e);
        }
        main = null;
        log.info("QualityOfService-example stopped.");
    }
}