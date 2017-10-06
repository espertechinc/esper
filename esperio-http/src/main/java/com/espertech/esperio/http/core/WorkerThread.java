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

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WorkerThread extends Thread {

    private final static Logger log = LoggerFactory.getLogger(WorkerThread.class);

    private final HttpService httpservice;
    private final HttpServerConnection conn;
    private final EsperHttpServiceClassicRunnable runnable;
    private boolean isShutdown;

    public WorkerThread(HttpService httpservice, HttpServerConnection conn, EsperHttpServiceClassicRunnable runnable) {
        this.httpservice = httpservice;
        this.conn = conn;
        this.runnable = runnable;
    }

    public void setShutdown(boolean shutdown) {
        isShutdown = shutdown;
    }

    public void run() {
        HttpContext context = new BasicHttpContext(null);
        try {
            while (!Thread.interrupted() && this.conn.isOpen()) {
                this.httpservice.handleRequest(this.conn, context);
            }
        } catch (ConnectionClosedException ex) {
            if (!isShutdown) {
                log.error("Client closed connection");
            }
        } catch (IOException ex) {
            if (!isShutdown) {
                log.error("I/O error: " + ex.getMessage(), ex);
            }
        } catch (HttpException ex) {
            log.error("Unrecoverable HTTP protocol violation: " + ex.getMessage());
        } finally {
            try {
                this.conn.shutdown();
                runnable.remove(this);
            } catch (IOException ignore) {
            }
        }
    }

}
