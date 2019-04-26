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
package com.espertech.esper.common.internal.serde.serdeset.builtin;

import java.io.IOException;
import java.io.InputStream;

/**
 * Fast byte array input stream, does not synchronize or check buffer overflow.
 */
public class FastByteArrayInputStream extends InputStream {
    private byte[] bytes;
    private int length;
    private int offset;
    private int currentMark;

    /**
     * Ctor.
     *
     * @param buffer to use
     */
    public FastByteArrayInputStream(byte[] buffer) {
        bytes = buffer;
        length = buffer.length;
    }

    /**
     * Ctor.
     *
     * @param buffer buffer to use
     * @param offset offset to start at
     * @param length length of buffer to use
     */
    public FastByteArrayInputStream(byte[] buffer, int offset, int length) {
        bytes = buffer;
        this.offset = offset;
        this.length = offset + length;
    }

    /**
     * Returns the buffer.
     *
     * @return buffer
     */
    public final byte[] getBytes() {
        return bytes;
    }

    /**
     * Returns buffer length.
     *
     * @return length
     */
    public final int getLength() {
        return length;
    }

    /**
     * Returns buffer offset.
     *
     * @return offset
     */
    public final int getOffset() {
        return offset;
    }

    /**
     * Sets buffer.
     *
     * @param bytes to set
     */
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Set length of buffer.
     *
     * @param length buffer length
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Sets buffer offset.
     *
     * @param offset to set
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Read bytes to buffer.
     *
     * @param target target buffer
     * @param offset buffer offset
     * @param length buffer length
     * @return number of bytes read
     * @throws IOException indicates error
     */
    public int read(byte[] target, int offset, int length) throws IOException {
        return readFast(target, offset, length);
    }

    public int available() {
        return length - offset;
    }

    public int read() throws IOException {
        return readFast();
    }

    public int read(byte[] toBuf) throws IOException {
        return readFast(toBuf, 0, toBuf.length);
    }

    public void reset() {
        offset = currentMark;
    }

    public long skip(long count) {
        int now = (int) count;
        if (now + offset > length) {
            now = length - offset;
        }
        skipFast(now);
        return now;
    }

    public boolean markSupported() {
        return true;
    }

    public void mark(int readLimit) {
        currentMark = offset;
    }

    /**
     * Fast skip.
     *
     * @param count bytes to skip
     */
    public final void skipFast(int count) {
        offset += count;
    }

    /**
     * Fast read without sync.
     *
     * @return read byte
     */
    public final int readFast() {
        return (offset < length) ? (bytes[offset++] & 0xff) : (-1);
    }

    /**
     * Read bytes.
     *
     * @param target to fill
     * @return num bytes
     */
    public final int readFast(byte[] target) {
        return readFast(target, 0, target.length);
    }

    /**
     * Read bytes.
     *
     * @param target to fill
     * @param offset to fill
     * @param length length to use
     * @return num bytes read
     */
    public final int readFast(byte[] target, int offset, int length) {
        int available = this.length - this.offset;
        if (available <= 0) {
            return -1;
        }
        if (length > available) {
            length = available;
        }
        System.arraycopy(bytes, this.offset, target, offset, length);
        this.offset += length;
        return length;
    }
}
