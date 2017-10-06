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

import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;

public class EsperHttpServiceNIORunnable implements Runnable {
    private final static Logger log = LoggerFactory.getLogger(EsperHttpServiceNIORunnable.class);

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
                    log.error("I/O error initialising connection thread for service '" + serviceName + "' : " + e.getMessage());
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
