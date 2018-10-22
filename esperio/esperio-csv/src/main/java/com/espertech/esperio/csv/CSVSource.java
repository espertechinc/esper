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
package com.espertech.esperio.csv;

import java.io.*;


/**
 * A wrapper for a Reader or an InputStream.
 */
public class CSVSource {
    private final AdapterInputSource source;
    private Reader reader;
    private InputStream stream;

    /**
     * Ctor.
     *
     * @param source - the AdapterInputSource from which to obtain the uderlying resource
     */
    public CSVSource(AdapterInputSource source) {
        if (source.getAsStream() != null) {
            stream = new BufferedInputStream(source.getAsStream());
        } else {
            reader = new BufferedReader(source.getAsReader());
        }
        this.source = source;
    }

    /**
     * Close the underlying resource.
     *
     * @throws IOException to indicate an io error
     */
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
        } else {
            reader.close();
        }
    }

    /**
     * Read from the underlying resource.
     *
     * @return the result of the read
     * @throws IOException for io errors
     */
    public int read() throws IOException {
        if (stream != null) {
            return stream.read();
        } else {
            return reader.read();
        }
    }

    /**
     * Return true if the underlying resource is resettable.
     *
     * @return true if resettable, false otherwise
     */
    public boolean isResettable() {
        return source.isResettable();
    }

    /**
     * Reset to the last mark position.
     *
     * @throws IOException for io errors
     */
    public void resetToMark() throws IOException {
        if (stream != null) {
            stream.reset();
        } else {
            reader.reset();
        }
    }

    /**
     * Set the mark position.
     *
     * @param readAheadLimit is the maximum number of read-ahead events
     * @throws IOException when an io error occurs
     */
    public void mark(int readAheadLimit) throws IOException {
        if (stream != null) {
            stream.mark(readAheadLimit);
        } else {
            reader.mark(readAheadLimit);
        }
    }

    /**
     * Reset to the beginning of the resource.
     */
    public void reset() {
        if (!isResettable()) {
            throw new UnsupportedOperationException("Reset not supported: underlying source cannot be reset");
        }
        if (stream != null) {
            stream = new BufferedInputStream(source.getAsStream());
        } else {
            reader = new BufferedReader(source.getAsReader());
        }
    }
}
