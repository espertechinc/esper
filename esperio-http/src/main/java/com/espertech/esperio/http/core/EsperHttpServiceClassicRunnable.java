package com.espertech.esperio.http.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpService;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EsperHttpServiceClassicRunnable implements Runnable {
    private static Log log = LogFactory.getLog(EsperHttpServiceClassicRunnable.class);

    private String serviceName;
    private ServerSocket serversocket;
    private HttpParams parameters;
    private HttpService httpService;
    private List<WorkerThread> workers = new CopyOnWriteArrayList<WorkerThread>();
    private boolean shutdown;

    public EsperHttpServiceClassicRunnable(String serviceName, ServerSocket serversocket, HttpParams parameters, HttpService httpService) {
        this.serviceName = serviceName;
        this.serversocket = serversocket;
        this.parameters = parameters;
        this.httpService = httpService;
    }

    public void run() {
        log.info("For service '" + serviceName + "' listening on port " + this.serversocket.getLocalPort());
        while (!Thread.interrupted()) {
            try {
                // Set up HTTP connection
                Socket socket = this.serversocket.accept();
                DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                log.info("Incoming connection service '" + serviceName + "' from " + socket.getInetAddress());
                conn.bind(socket, this.parameters);

                // Start worker thread
                WorkerThread t = new WorkerThread(this.httpService, conn, this);
                t.setDaemon(true);
                t.start();
                workers.add(t);
            }
            catch (InterruptedIOException ex) {
                break;
            }
            catch (IOException e) {
                if (!shutdown) {
                    log.error("I/O error initialising connection thread for service '" + serviceName + "' : " + e.getMessage());
                }
                break;
            }
        }
        log.info("For service '" + serviceName + "' listening on port " + this.serversocket.getLocalPort() + " ended socket thread.");
    }

    public void destroy() {
        log.info("Stopping worker threads for service '" + serviceName + "'");
        shutdown = true;
        for (WorkerThread worker : workers) {
            worker.setShutdown(true);

            if (!worker.isAlive()) {
                worker.interrupt();
            }
            try {
                worker.join(1000);
            }
            catch (InterruptedException e) {               
            }
        }
        workers.clear();
    }

    public void remove(WorkerThread workerThread) {
        workers.remove(workerThread);
    }
}