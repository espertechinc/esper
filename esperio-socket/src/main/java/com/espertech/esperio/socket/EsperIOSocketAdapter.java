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
package com.espertech.esperio.socket;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esperio.socket.config.ConfigurationSocketAdapter;
import com.espertech.esperio.socket.config.SocketConfig;
import com.espertech.esperio.socket.core.EsperSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EsperIOSocketAdapter {
    private final static Logger log = LoggerFactory.getLogger(EsperIOSocketAdapter.class);

    private final ConfigurationSocketAdapter config;
    private final String engineURI;

    private final Map<String, EsperSocketService> sockets = new HashMap<String, EsperSocketService>();

    /**
     * Quickstart constructor.
     *
     * @param config    configuration
     * @param engineURI engine URI
     */
    public EsperIOSocketAdapter(ConfigurationSocketAdapter config, String engineURI) {
        this.config = config;
        this.engineURI = engineURI;
    }

    /**
     * Re-initialize.
     */
    public void initialize() {
    }

    /**
     * Start the socket endpoint.
     */
    public synchronized void start() {
        if (log.isInfoEnabled()) {
            log.info("Starting EsperIO Socket Adapter for engine URI '" + engineURI + "'");
        }

        EPServiceProviderSPI engineSPI = (EPServiceProviderSPI) EPServiceProviderManager.getExistingProvider(engineURI);

        // Configure sockets (input adapter)
        Set<Integer> ports = new HashSet<Integer>();
        for (Map.Entry<String, SocketConfig> entry : config.getSockets().entrySet()) {
            if (sockets.containsKey(entry.getKey())) {
                throw new ConfigurationException("A socket by name '" + entry.getKey() + "' has already been configured.");
            }

            int port = entry.getValue().getPort();
            if (ports.contains(port)) {
                throw new ConfigurationException("A socket for port '" + port + "' has already been configured.");
            }
            ports.add(port);

            EsperSocketService socketService = new EsperSocketService(entry.getKey(), entry.getValue());
            sockets.put(entry.getKey(), socketService);
        }

        // Start sockets
        for (Map.Entry<String, EsperSocketService> entry : sockets.entrySet()) {
            try {
                entry.getValue().start(entry.getKey(), engineSPI);
            } catch (IOException e) {
                String message = "Error starting socket '" + entry.getKey() + "' port " + entry.getValue().getPort() + " :" + e.getMessage();
                log.error(message, e);
                throw new EPException(message, e);
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Completed starting EsperIO Socket Adapter for engine URI '" + engineURI + "'.");
        }
    }

    /**
     * Destroy the adapter.
     */
    public synchronized void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying Esper Socket Adapter");
        }

        for (EsperSocketService service : sockets.values()) {
            try {
                service.destroy();
            } catch (Throwable t) {
                log.info("Error destroying service '" + service.getServiceName() + "' :" + t.getMessage());
            }
        }
        sockets.clear();
    }
}
