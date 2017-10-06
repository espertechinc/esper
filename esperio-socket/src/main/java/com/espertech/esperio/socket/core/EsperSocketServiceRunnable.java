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
package com.espertech.esperio.socket.core;

import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esperio.socket.config.DataType;
import com.espertech.esperio.socket.config.SocketConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EsperSocketServiceRunnable implements Runnable {
    private final static Logger log = LoggerFactory.getLogger(EsperSocketServiceRunnable.class);

    private String serviceName;
    private SocketConfig serviceConfig;
    private ServerSocket serversocket;
    private EPServiceProviderSPI engine;
    private List<WorkerThread> workers = new CopyOnWriteArrayList<WorkerThread>();
    private boolean shutdown;

    public EsperSocketServiceRunnable(String serviceName, SocketConfig serviceConfig, ServerSocket serversocket, EPServiceProviderSPI engine) {
        this.serviceName = serviceName;
        this.serversocket = serversocket;
        this.serviceConfig = serviceConfig;
        this.engine = engine;
    }

    public void run() {
        log.info("For service '" + serviceName + "' listening on port " + this.serversocket.getLocalPort() + " expecting data type " + (serviceConfig.getDataType() == null ? DataType.OBJECT : serviceConfig.getDataType()));
        while (!Thread.interrupted()) {
            try {
                // Set up Socket connection
                Socket socket = this.serversocket.accept();
                log.info("Incoming connection service '" + serviceName + "' from " + socket.getInetAddress());

                // Start worker thread
                WorkerThread t = new WorkerThread(serviceName, engine, this, socket, serviceConfig);
                t.setDaemon(true);
                t.start();
                workers.add(t);
            } catch (InterruptedIOException ex) {
                break;
            } catch (IOException e) {
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
            } catch (InterruptedException e) {
            }
        }
        workers.clear();
    }

    public void remove(WorkerThread workerThread) {
        workers.remove(workerThread);
    }
}
