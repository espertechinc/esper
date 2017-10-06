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
package com.espertech.esperio.http.core;

import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esperio.http.EventLogger;
import com.espertech.esperio.http.config.Service;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class EsperHttpServiceNIO extends EsperHttpServiceBase {
    private final static Logger log = LoggerFactory.getLogger(EsperHttpServiceNIO.class);

    private EsperHttpServiceNIORunnable runnable;
    private Thread reactorThread;
    private ListeningIOReactor ioReactor;

    public EsperHttpServiceNIO(String serviceName, Service value) {
        super(serviceName, value);
    }

    public void start(EPServiceProviderSPI engine) {
        HttpParams parameters = new BasicHttpParams();
        parameters
                .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        BufferingHttpServiceHandler handler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                parameters);

        // Set up request handlers
        HttpRequestHandlerRegistry reqistry = setupRegistry(engine);

        handler.setHandlerResolver(reqistry);

        // Provide an event logger
        handler.setEventListener(new EventLogger());

        IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(handler, parameters);
        try {
            ioReactor = new DefaultListeningIOReactor(2, parameters);
            ioReactor.listen(new InetSocketAddress(this.getServiceConfig().getPort()));
        } catch (IOException e) {
            log.error("I/O for service '" + this.getServiceName() + "' error: " + e.getMessage());
            return;
        }

        runnable = new EsperHttpServiceNIORunnable(this.getServiceName(), this.getServiceConfig().getPort(), ioReactor, ioEventDispatch);
        reactorThread = new Thread(runnable);
        reactorThread.setDaemon(true);
        reactorThread.start();
    }

    public void destroy() {
        log.info("Closing service '" + this.getServiceName() + "'");
        runnable.destroy();

        log.info("Pausing reactor for service '" + this.getServiceName() + "' and port " + this.getServiceConfig().getPort());
        try {
            ioReactor.pause();
        } catch (IOException e) {
            log.debug("Error closing server socket: " + e.getMessage(), e);
        }

        log.info("Stopping reactor thread for service '" + this.getServiceName() + "'");
        reactorThread.interrupt();
        try {
            reactorThread.join(10000);
        } catch (InterruptedException e) {
            log.debug("Interrupted", e);
        }
    }
}
