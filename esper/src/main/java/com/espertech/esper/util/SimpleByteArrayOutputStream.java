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
package com.espertech.esper.util;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Output stream that relies on a simple byte array, unchecked.
 */
public class SimpleByteArrayOutputStream extends OutputStream {
    private byte[] buf = null;
    private int size = 0;

    /**
     * Ctor.
     */
    public SimpleByteArrayOutputStream() {
        this(5 * 1024);
    }

    /**
     * Ctor.
     *
     * @param initSize initial size
     */
    public SimpleByteArrayOutputStream(int initSize) {
        this.size = 0;
        this.buf = new byte[initSize];
    }

    private void verifyBufferSize(int sz) {
        if (sz > buf.length) {
            byte[] old = buf;
            buf = new byte[Math.max(sz, 2 * buf.length)];
            System.arraycopy(old, 0, buf, 0, old.length);
        }
    }

    public final void write(byte[] b) {
        verifyBufferSize(size + b.length);
        System.arraycopy(b, 0, buf, size, b.length);
        size += b.length;
    }

    public final void write(byte[] b, int off, int len) {
        verifyBufferSize(size + len);
        System.arraycopy(b, off, buf, size, len);
        size += len;
    }

    public final void write(int b) {
        verifyBufferSize(size + 1);
        buf[size++] = (byte) b;
    }

    /**
     * Return the input stream for the output buffer.
     *
     * @return input stream for existing buffer
     */
    public InputStream getInputStream() {
        return new SimpleByteArrayInputStream(buf, size);
    }

}
