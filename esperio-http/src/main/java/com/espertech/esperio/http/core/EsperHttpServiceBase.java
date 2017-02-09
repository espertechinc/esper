package com.espertech.esperio.http.core;

import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esperio.http.EsperHttpRequestHandler;
import com.espertech.esperio.http.config.GetHandler;
import com.espertech.esperio.http.config.Service;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class EsperHttpServiceBase {
    private static Logger log = LoggerFactory.getLogger(EsperHttpServiceBase.class);

    private final String serviceName;
    private final Service serviceConfig;
    private final List<GetHandler> getHandlers = new ArrayList<GetHandler>();

    public abstract void start(EPServiceProviderSPI engine) throws IOException;

    public abstract void destroy();

    protected HttpRequestHandlerRegistry setupRegistry(EPServiceProviderSPI engineSPI) {
        HttpRequestHandlerRegistry registery = new HttpRequestHandlerRegistry();
        for (GetHandler getHandler : getHandlers) {
            log.info("Registering for service '" + serviceName + "' the pattern '" + getHandler.getPattern() + "'");
            registery.register(getHandler.getPattern(), new EsperHttpRequestHandler(engineSPI));
        }
        return registery;
    }

    public EsperHttpServiceBase(String serviceName, Service serviceConfig) {
        this.serviceName = serviceName;
        this.serviceConfig = serviceConfig;
    }

    public void add(GetHandler handler) {
        getHandlers.add(handler);
    }

    public String getServiceName() {
        return serviceName;
    }

    public Service getServiceConfig() {
        return serviceConfig;
    }

    public List<GetHandler> getGetHandlers() {
        return getHandlers;
    }
}
