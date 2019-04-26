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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Output on fast byte array output stream.
 */
public class FastByteArrayOutputStream extends OutputStream {
    /**
     * Buffer initial size.
     */
    public static final int DEFAULT_INITIAL_SIZE = 100;

    /**
     * Buffer increase size, zero means double.
     */
    public static final int DEFAULT_INCREASE_SIZE = 0;

    private byte[] bytes;
    private int length;
    private int increaseLength;

    private static final byte[] ZERO_LENGTH_BYTE_ARRAY = new byte[0];

    /**
     * Ctor.
     *
     * @param buffer to write
     */
    public FastByteArrayOutputStream(byte[] buffer) {
        bytes = buffer;
        increaseLength = DEFAULT_INCREASE_SIZE;
    }

    /**
     * Ctor.
     *
     * @param buffer   to write
     * @param increase zero for
     */
    public FastByteArrayOutputStream(byte[] buffer, int increase) {
        bytes = buffer;
        increaseLength = increase;
    }

    /**
     * Ctor.
     */
    public FastByteArrayOutputStream() {
        bytes = new byte[DEFAULT_INITIAL_SIZE];
        increaseLength = DEFAULT_INCREASE_SIZE;
    }

    /**
     * Ctor.
     *
     * @param initialSize initial size
     */
    public FastByteArrayOutputStream(int initialSize) {
        bytes = new byte[initialSize];
        increaseLength = DEFAULT_INCREASE_SIZE;
    }

    /**
     * Ctor.
     *
     * @param initialSize  initial size
     * @param increaseSize increase size
     */
    public FastByteArrayOutputStream(int initialSize, int increaseSize) {
        bytes = new byte[initialSize];
        increaseLength = increaseSize;
    }

    /**
     * Returns buffer size.
     *
     * @return size
     */
    public int size() {
        return length;
    }

    /**
     * Reset buffer.
     */
    public void reset() {
        length = 0;
    }

    /**
     * Write byte.
     *
     * @param b byte to write
     * @throws IOException for io errors
     */
    public void write(int b) throws IOException {
        writeFast(b);
    }

    public void write(byte[] fromBuf) throws IOException {
        writeFast(fromBuf);
    }

    public void write(byte[] fromBuf, int offset, int length)
        throws IOException {
        writeFast(fromBuf, offset, length);
    }

    /**
     * Write bytes to another stream.
     *
     * @param out other stream
     * @throws IOException if a write exception occurs
     */
    public void writeTo(OutputStream out) throws IOException {
        out.write(bytes, 0, length);
    }

    public String toString() {
        return new String(bytes, 0, length);
    }

    /**
     * Outputs contents.
     *
     * @param encoding to use
     * @return contents
     * @throws UnsupportedEncodingException when encoding is not supported
     */
    public String toString(String encoding) throws UnsupportedEncodingException {
        return new String(bytes, 0, length, encoding);
    }

    /**
     * Returns the byte array.
     *
     * @return byte array
     */
    public byte[] getByteArrayWithCopy() {
        if (length == 0) {
            return ZERO_LENGTH_BYTE_ARRAY;
        } else {
            byte[] toBuf = new byte[length];
            System.arraycopy(bytes, 0, toBuf, 0, length);

            return toBuf;
        }
    }

    /**
     * Fast getter checking if the byte array matches the requested length and returnin the buffer itself if it does.
     *
     * @return byte array without offset
     */
    public byte[] getByteArrayFast() {
        if (bytes.length == length) {
            return bytes;
        }
        return getByteArrayWithCopy();
    }

    /**
     * Fast write.
     *
     * @param b byte to write
     */
    public final void writeFast(int b) {
        if (length + 1 > bytes.length) {
            bump(1);
        }

        bytes[length++] = (byte) b;
    }

    /**
     * Fast write.
     *
     * @param fromBuf to write
     */
    public final void writeFast(byte[] fromBuf) {
        int needed = length + fromBuf.length - bytes.length;
        if (needed > 0) {
            bump(needed);
        }

        System.arraycopy(fromBuf, 0, bytes, length, fromBuf.length);
        length += fromBuf.length;
    }

    /**
     * Fast write.
     *
     * @param fromBuf buffer to write
     * @param offset  offset of write from
     * @param length  length to write
     */
    public final void writeFast(byte[] fromBuf, int offset, int length) {
        int needed = this.length + length - bytes.length;
        if (needed > 0)
            bump(needed);

        System.arraycopy(fromBuf, offset, bytes, this.length, length);
        this.length += length;
    }

    /**
     * Returns the buffer itself.
     *
     * @return buffer
     */
    public byte[] getBufferBytes() {
        return bytes;
    }

    /**
     * Returns the offset, always zero.
     *
     * @return offset
     */
    public int getBufferOffset() {
        return 0;
    }

    /**
     * Returns the length.
     *
     * @return length
     */
    public int getBufferLength() {
        return length;
    }

    /**
     * Increase buffer size.
     *
     * @param sizeNeeded bytes needed.
     */
    public void makeSpace(int sizeNeeded) {
        int needed = length + sizeNeeded - bytes.length;
        if (needed > 0) {
            bump(needed);
        }
    }

    /**
     * Add number of bytes to size.
     *
     * @param sizeAdded to be added
     */
    public void addSize(int sizeAdded) {
        length += sizeAdded;
    }

    private void bump(int needed) {
        int bump = (increaseLength > 0) ? increaseLength : bytes.length;

        byte[] toBuf = new byte[bytes.length + needed + bump];

        System.arraycopy(bytes, 0, toBuf, 0, length);

        bytes = toBuf;
    }
}
