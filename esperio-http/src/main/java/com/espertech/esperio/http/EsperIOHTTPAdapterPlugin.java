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
package com.espertech.esperio.http;

import com.espertech.esper.plugin.PluginLoader;
import com.espertech.esper.plugin.PluginLoaderInitContext;
import com.espertech.esperio.http.config.ConfigurationHTTPAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class EsperIOHTTPAdapterPlugin implements PluginLoader {
    private final static Logger log = LoggerFactory.getLogger(EsperIOHTTPAdapterPlugin.class);

    private static final String ESPERIO_HTTP_CONFIG_FILE = "esperio.http.configuration.file";

    private EsperIOHTTPAdapter httpadapter;
    private PluginLoaderInitContext context;

    public void init(PluginLoaderInitContext context) {
        this.context = context;
    }

    public void postInitialize() {
        ConfigurationHTTPAdapter adapterConfig;
        if (context.getConfigXml() != null) {
            adapterConfig = fromXml(context.getConfigXml());
        } else {
            adapterConfig = fromExternalFile();
        }

        log.info("Starting HTTP Adapter");
        httpadapter = new EsperIOHTTPAdapter(adapterConfig, context.getEpServiceProvider().getURI());
        httpadapter.start();
    }

    public void destroy() {
        if (httpadapter != null) {
            httpadapter.destroy();
        }
        httpadapter = null;
    }

    private ConfigurationHTTPAdapter fromXml(String xml) {
        ConfigurationHTTPAdapter adapter = new ConfigurationHTTPAdapter();
        return adapter.configureFromString(xml);
    }

    private ConfigurationHTTPAdapter fromExternalFile() {

        // obtain config file name
        String configFile = context.getProperties().getProperty(ESPERIO_HTTP_CONFIG_FILE);
        if (configFile == null) {
            String message = "Required initialization property '" + ESPERIO_HTTP_CONFIG_FILE + "' is not provided";
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
        ConfigurationHTTPAdapter config = new ConfigurationHTTPAdapter();
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
