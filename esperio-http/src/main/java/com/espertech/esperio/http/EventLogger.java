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

import org.apache.http.HttpException;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EventLogger implements EventListener {

    private final static Logger log = LoggerFactory.getLogger(EventLogger.class);

    public void connectionOpen(final NHttpConnection conn) {
        if (log.isInfoEnabled()) {
            log.info("Connection open: " + conn);
        }
    }

    public void connectionTimeout(final NHttpConnection conn) {
        if (log.isInfoEnabled()) {
            log.info("Connection timed out: " + conn);
        }
    }

    public void connectionClosed(final NHttpConnection conn) {
        if (log.isInfoEnabled()) {
            log.info("Connection closed: " + conn);
        }
    }

    public void fatalIOException(final IOException ex, final NHttpConnection conn) {
        log.error("I/O error: " + ex.getMessage());
    }

    public void fatalProtocolException(final HttpException ex, final NHttpConnection conn) {
        log.error("HTTP error: " + ex.getMessage());
    }
}
