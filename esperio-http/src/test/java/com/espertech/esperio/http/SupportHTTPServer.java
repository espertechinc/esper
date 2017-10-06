package com.espertech.esperio.http;

import com.espertech.esperio.http.core.EsperHttpServiceClassicRunnable;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public class SupportHTTPServer {
    private final static Logger log = LoggerFactory.getLogger(SupportHTTPServer.class);

    private final int port;
    private ServerSocket serversocket;
    private HttpParams parameters;
    private HttpService httpService;
    private EsperHttpServiceClassicRunnable runnable;
    private Thread socketThread;

    public SupportHTTPServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        if (serversocket != null) {
            throw new RuntimeException("Server socket already initialized");
        }

        this.serversocket = new ServerSocket(port);
        this.parameters = new BasicHttpParams();
        this.parameters
                .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

        // Set up the HTTP protocol processor
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        // Set up request handlers
        HttpRequestHandlerRegistry registery = new HttpRequestHandlerRegistry();
        registery.register("*", new SupportHTTPServerReqestHandler());

        // Set up the HTTP service
        this.httpService = new HttpService(
                httpproc,
                new DefaultConnectionReuseStrategy(),
                new DefaultHttpResponseFactory());
        this.httpService.setParams(this.parameters);
        this.httpService.setHandlerResolver(registery);

        runnable = new EsperHttpServiceClassicRunnable("regressionTestService", serversocket, parameters, httpService);
        socketThread = new Thread(runnable);
        socketThread.setDaemon(true);
        socketThread.start();
    }

    public void stop() {

        log.info("Closing existing workers");
        runnable.destroy();

        log.info("Closing server socket for port " + port);
        try {
            serversocket.close();
        } catch (IOException e) {
            log.debug("Error closing server socket: " + e.getMessage(), e);
        }

        log.info("Stopping socket thread");
        socketThread.interrupt();
        try {
            socketThread.join(10000);
        } catch (InterruptedException e) {
            log.debug("Interrupted", e);
        }
    }

}
