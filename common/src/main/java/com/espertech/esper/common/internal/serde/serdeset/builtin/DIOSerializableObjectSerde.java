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

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.internal.util.ObjectInputStreamWithTCCL;

import java.io.*;

/**
 * Serde that serializes and de-serializes using {@link ObjectInputStream} and {@link ObjectOutputStream}.
 */
public class DIOSerializableObjectSerde implements DataInputOutputSerde {

    /**
     * Instance.
     */
    public final static DIOSerializableObjectSerde INSTANCE = new DIOSerializableObjectSerde();

    private DIOSerializableObjectSerde() {
    }

    public void write(Object object, DataOutput output, byte[] pageFullKey, EventBeanCollatedWriter writer) throws IOException {
        byte[] objectBytes = objectToByteArr(object);
        output.writeInt(objectBytes.length);
        output.write(objectBytes);
    }

    public Object read(DataInput input, byte[] resourceKey) throws IOException {
        int size = input.readInt();
        byte[] buf = new byte[size];
        input.readFully(buf);
        return byteArrToObject(buf);
    }

    /**
     * Serialize object to byte array.
     *
     * @param underlying to serialize
     * @return byte array
     */
    public static byte[] objectToByteArr(Object underlying) {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(underlying);
            oos.close();
            baos.close();
        } catch (IOException e) {
            throw new RuntimeException("IO error serializing object: " + e.getMessage(), e);
        }

        return baos.getByteArrayFast();
    }

    /**
     * Deserialize byte arry to object.
     *
     * @param bytes to read
     * @return object
     */
    public static Object byteArrToObject(byte[] bytes) {
        FastByteArrayInputStream bais = new FastByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois = new ObjectInputStreamWithTCCL(bais);
            return ois.readObject();
        } catch (IOException e) {
            if (e.getMessage() != null) {
                throw new RuntimeException("IO error de-serializing object: " + e.getMessage(), e);
            }
            throw new RuntimeException("IO error de-serializing object", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found de-serializing object: " + e.getMessage(), e);
        }
    }

    /**
     * Serialize object
     *
     * @param value  value to serialize
     * @param output output stream
     * @throws IOException when a problem occurs
     */
    public static void serializeTo(Object value, DataOutput output) throws IOException {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(value);
        oos.close();

        byte[] result = baos.getByteArrayWithCopy();
        output.writeInt(result.length);
        output.write(result);
        baos.close();
    }

    /**
     * Deserialize object
     *
     * @param input input stream
     * @return value
     * @throws IOException when a problem occurs
     */
    public static Object deserializeFrom(DataInput input) throws IOException {
        int size = input.readInt();
        byte[] buf = new byte[size];
        input.readFully(buf);

        FastByteArrayInputStream bais = new FastByteArrayInputStream(buf);
        try {
            ObjectInputStream ois = new ObjectInputStreamWithTCCL(bais);
            return ois.readObject();
        } catch (IOException e) {
            if (e.getMessage() != null) {
                throw new RuntimeException("IO error de-serializing object: " + e.getMessage(), e);
            } else {
                throw new RuntimeException("IO error de-serializing object", e);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found de-serializing object: " + e.getMessage(), e);
        }
    }
}
