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

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esperio.http.config.ConfigurationHTTPAdapter;
import com.espertech.esperio.http.config.GetHandler;
import com.espertech.esperio.http.config.Request;
import com.espertech.esperio.http.config.Service;
import com.espertech.esperio.http.core.EsperHttpServiceBase;
import com.espertech.esperio.http.core.EsperHttpServiceClassic;
import com.espertech.esperio.http.core.EsperHttpServiceNIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EsperIOHTTPAdapter {
    private final static Logger log = LoggerFactory.getLogger(EsperIOHTTPAdapter.class);

    private final ConfigurationHTTPAdapter config;
    private final String engineURI;

    private final Map<String, EsperHttpServiceBase> services = new HashMap<String, EsperHttpServiceBase>();

    /**
     * Quickstart constructor.
     *
     * @param config    configuration
     * @param engineURI engine URI
     */
    public EsperIOHTTPAdapter(ConfigurationHTTPAdapter config, String engineURI) {
        this.config = config;
        this.engineURI = engineURI;
    }

    /**
     * Re-initialize.
     */
    public void initialize() {
    }

    /**
     * Start the DDS endpoint.
     */
    public synchronized void start() {
        if (log.isInfoEnabled()) {
            log.info("Starting EsperIO HTTP Adapter for engine URI '" + engineURI + "'");
        }

        EPServiceProviderSPI engineSPI = (EPServiceProviderSPI) EPServiceProviderManager.getProvider(engineURI);

        // Start requests (output adapter)
        for (Request request : config.getRequests()) {

            EventType eventType = engineSPI.getEventAdapterService().getExistsTypeByName(request.getStream());
            if (eventType == null) {
                throw new ConfigurationException("Event type by name '" + request.getStream() + "' not found");
            }

            try {
                EsperIOHTTPSubscription subs = new EsperIOHTTPSubscription(request.getStream(), request.getUri());
                subs.seteventTypeName(request.getStream());
                subs.setSubscriptionName("EsperIOHTTP-" + request.getUri());
                subs.registerAdapter(engineSPI);
            } catch (Throwable t) {
                log.error("Error starting HTTP Request definition for URI " + request.getUri() + "'" + t.getMessage(), t);
            }
        }

        // Configure services (input adapter)
        Set<Integer> ports = new HashSet<Integer>();
        for (Map.Entry<String, Service> entry : config.getServices().entrySet()) {
            if (services.containsKey(entry.getKey())) {
                throw new ConfigurationException("A service by name '" + entry.getKey() + "' has already been configured.");
            }

            int port = entry.getValue().getPort();
            if (ports.contains(port)) {
                throw new ConfigurationException("A service for port '" + port + "' has already been configured.");
            }
            ports.add(port);

            EsperHttpServiceBase httpService;
            if (entry.getValue().isNio()) {
                try {
                    engineSPI.getEngineImportService().getClassForNameProvider().classForName("org.apache.http.nio.NHttpServiceHandler");
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("NIO Handler not found in classpath, please ensure httpcore-nio exists in classpath.");
                }
                httpService = new EsperHttpServiceNIO(entry.getKey(), entry.getValue());
            } else {
                httpService = new EsperHttpServiceClassic(entry.getKey(), entry.getValue());
            }
            services.put(entry.getKey(), httpService);
        }

        // Add handlers (input adapter)
        for (GetHandler handler : config.getGetHandlers()) {
            if (!services.containsKey(handler.getService())) {
                throw new ConfigurationException("A service by name '" + handler.getService() + "' has not been configured.");
            }
            EsperHttpServiceBase httpService = services.get(handler.getService());
            httpService.add(handler);
        }

        // Start services
        for (EsperHttpServiceBase service : services.values()) {
            try {
                service.start(engineSPI);
            } catch (IOException e) {
                log.error("Error starting service '" + service.getServiceName() + "' :" + e.getMessage());
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Completed starting EsperIO HTTP Adapter for engine URI '" + engineURI + "'.");
        }
    }

    /**
     * Destroy the adapter.
     */
    public synchronized void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying Esper HTTP Adapter");
        }

        for (EsperHttpServiceBase service : services.values()) {
            try {
                service.destroy();
            } catch (Throwable t) {
                log.info("Error destroying service '" + service.getServiceName() + "' :" + t.getMessage());
            }
        }
        services.clear();
    }
}
