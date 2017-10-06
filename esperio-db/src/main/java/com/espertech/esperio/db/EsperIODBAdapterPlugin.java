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
package com.espertech.esperio.db;

import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import com.espertech.esperio.db.config.ConfigurationDBAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class EsperIODBAdapterPlugin implements PluginLoader {
    private final static Logger log = LoggerFactory.getLogger(EsperIODBAdapterPlugin.class);

    private static final String ESPERIO_DB_CONFIG_FILE = "esperio.db.configuration.file";

    private EsperIODBAdapter dbadapter;
    private PluginLoaderInitContext context;

    public void init(PluginLoaderInitContext context) {
        this.context = context;
    }

    public void postInitialize() {
        ConfigurationDBAdapter adapterConfig;
        if (context.getConfigXml() != null) {
            adapterConfig = fromXml(context.getConfigXml());
        } else {
            adapterConfig = fromExternalFile();
        }

        log.info("Starting DB Adapter");
        dbadapter = new EsperIODBAdapter(adapterConfig, context.getEpServiceProvider().getURI());
        dbadapter.start();
    }

    public void destroy() {
        if (dbadapter != null) {
            dbadapter.destroy();
        }
        dbadapter = null;
    }

    private ConfigurationDBAdapter fromXml(String xml) {
        ConfigurationDBAdapter adapter = new ConfigurationDBAdapter();
        return adapter.configureFromString(xml);
    }

    private ConfigurationDBAdapter fromExternalFile() {

        // obtain config file name
        String configFile = context.getProperties().getProperty(ESPERIO_DB_CONFIG_FILE);
        if (configFile == null) {
            String message = "Required initialization property '" + ESPERIO_DB_CONFIG_FILE + "' is not provided";
            log.error(message);
            throw new RuntimeException(message);
        }

        // resolve config file
        URL urlConfigFile;
        try {
            urlConfigFile = resolveURL(configFile);
        } catch (Exception ex) {
            String message = "Error resolving config file from classpath or as a URL: " + ex.getMessage();
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }

        if (urlConfigFile == null) {
            String message = "Configuration file could not be resolved from '" + urlConfigFile + "'";
            log.error(message);
            throw new RuntimeException(message);
        }

        // parse config file
        ConfigurationDBAdapter config = new ConfigurationDBAdapter();
        try {
            config.configure(urlConfigFile);
        } catch (RuntimeException ex) {
            String message = "Configuration file read error for config file '" + urlConfigFile + "': " + ex.getMessage();
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }

        return config;
    }

    private URL resolveURL(String config) throws Exception {
        URL url = this.getClass().getClassLoader().getResource(config);
        if (url != null) {
            return url;
        }

        url = Thread.currentThread().getContextClassLoader().getResource(config);
        if (url != null) {
            return url;
        }

        File file = new File(config);
        if (!file.isAbsolute()) {
            String espereeBase = System.getProperty("esperee.base");
            file = new File(espereeBase, config);
        }
        if (file.exists()) {
            return file.toURI().toURL();
        }

        return new URL(config);
    }
}
