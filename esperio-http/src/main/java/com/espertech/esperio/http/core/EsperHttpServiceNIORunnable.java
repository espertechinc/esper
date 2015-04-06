package com.espertech.esperio.http.core;

import com.espertech.esperio.http.config.Service;
import com.espertech.esperio.http.config.GetHandler;
import com.espertech.esperio.http.EsperHttpRequestHandler;
import com.espertech.esperio.http.EventLogger;

import java.util.List;
import java.util.ArrayList;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InterruptedIOException;
import java.io.IOException;

import org.apache.http.params.HttpParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.*;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EsperHttpServiceNIORunnable implements Runnable {
    private static Log log = LogFactory.getLog(EsperHttpServiceNIORunnable.class);

    private final String serviceName;
    private final int port;
    private final ListeningIOReactor ioReactor;
    private final IOEventDispatch ioEventDispatch;
    private boolean shutdown;

    public EsperHttpServiceNIORunnable(String serviceName, int port, ListeningIOReactor ioReactor, IOEventDispatch ioEventDispatch) {
        this.serviceName = serviceName;
        this.port = port;
        this.ioReactor = ioReactor;
        this.ioEventDispatch = ioEventDispatch;
    }

    public void run() {
        log.info("For service '" + serviceName + "' listening on port " + port);
        while (!Thread.interrupted()) {
            try {
                ioReactor.execute(ioEventDispatch);
            } catch (InterruptedIOException ex) {
                break;
            } catch (IOException e) {
                if (!shutdown) {
                    log.error("I/O error initialising connection thread for service '" + serviceName + "' : "+ e.getMessage());
                }
                break;
            }
        }
        log.info("For service '" + serviceName + "' listening on port " + port + " ended NIO reactor thread.");
    }

    public void destroy() {
        shutdown = true;
    }

}