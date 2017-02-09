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

import java.io.*;

public class SerializerUtil {
    /**
     * Serialize object to byte array.
     *
     * @param underlying to serialize
     * @return byte array
     */
    public static byte[] objectToByteArr(Object underlying) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(underlying);
            oos.close();
            baos.close();
        } catch (IOException e) {
            throw new RuntimeException("IO error serializing object: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    /**
     * Deserialize byte arry to object.
     *
     * @param bytes to read
     * @return object
     */
    public static Object byteArrToObject(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
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


}
