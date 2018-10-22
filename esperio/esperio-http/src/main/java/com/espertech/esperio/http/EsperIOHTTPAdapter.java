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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esper.runtime.internal.kernel.service.EPRuntimeSPI;
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
import java.util.*;

public class EsperIOHTTPAdapter {
    private final static Logger log = LoggerFactory.getLogger(EsperIOHTTPAdapter.class);

    private final ConfigurationHTTPAdapter config;
    private final String runtimeURI;

    private final Map<String, EsperHttpServiceBase> services = new HashMap<String, EsperHttpServiceBase>();
    private final List<String> deployments = new ArrayList<>();

    /**
     * Quickstart constructor.
     *
     * @param config     configuration
     * @param runtimeURI runtime URI
     */
    public EsperIOHTTPAdapter(ConfigurationHTTPAdapter config, String runtimeURI) {
        this.config = config;
        this.runtimeURI = runtimeURI;
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
            log.info("Starting EsperIO HTTP Adapter for runtime URI '" + runtimeURI + "'");
        }

        EPRuntimeSPI runtime = (EPRuntimeSPI) EPRuntimeProvider.getRuntime(runtimeURI);

        // Start requests (output adapter)
        for (Request request : config.getRequests()) {

            EventType eventType = runtime.getServicesContext().getEventTypeRepositoryBus().getNameToTypeMap().get(request.getStream());
            if (eventType == null) {
                throw new ConfigurationException("Event type by name '" + request.getStream() + "' not found");
            }

            try {
                EsperIOHTTPUpdateListener subs = new EsperIOHTTPUpdateListener(request.getStream(), request.getUri());
                EPDeployment deployment = compileDeploySubscription(runtime, request.getStream(), request.getUri());
                deployments.add(deployment.getDeploymentId());
                deployment.getStatements()[0].addListener(subs);
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
                    runtime.getServicesContext().getClasspathImportServiceRuntime().getClassForNameProvider().classForName("org.apache.http.nio.NHttpServiceHandler");
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
                service.start(runtime);
            } catch (IOException e) {
                log.error("Error starting service '" + service.getServiceName() + "' :" + e.getMessage());
            }
        }

        if (log.isInfoEnabled()) {
            log.info("Completed starting EsperIO HTTP Adapter for runtime URI '" + runtimeURI + "'.");
        }
    }

    /**
     * Destroy the adapter.
     */
    public synchronized void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying Esper HTTP Adapter");
        }

        EPRuntimeSPI runtime = (EPRuntimeSPI) EPRuntimeProvider.getRuntime(runtimeURI);
        for (String deployment : deployments) {
            try {
                runtime.getDeploymentService().undeploy(deployment);
            } catch (EPUndeployException e) {
                throw new EPException("Failed to undeploy: " + e.getMessage(), e);
            }
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

    private EPDeployment compileDeploySubscription(EPRuntimeSPI runtime, String eventTypeName, String name) {
        try {
            String epl = "@name('" + name + "') select * from " + eventTypeName;
            CompilerArguments args = new CompilerArguments(runtime.getConfigurationDeepCopy());
            args.getPath().add(runtime.getRuntimePath());
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            return runtime.getDeploymentService().deploy(compiled);
        } catch (Exception ex) {
            throw new EPException("Failed to compile and deploy subscription: " + ex.getMessage(), ex);
        }
    }
}
