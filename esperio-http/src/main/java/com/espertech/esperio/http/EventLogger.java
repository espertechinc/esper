package com.espertech.esperio.http;

import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EventLogger implements EventListener {

    private static Logger log = LoggerFactory.getLogger(EventLogger.class);

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
