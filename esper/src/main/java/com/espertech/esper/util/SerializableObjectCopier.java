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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utility class for copying serializable objects via object input and output streams.
 */
public class SerializableObjectCopier {
    /**
     * Deep copies the input object.
     *
     * @param orig is the object to be copied, must be serializable
     * @return copied object
     * @throws IOException            if the streams returned an exception
     * @throws ClassNotFoundException if the de-serialize fails
     */
    public static Object copy(Object orig) throws IOException, ClassNotFoundException {
        SimpleByteArrayOutputStream fbos = new SimpleByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(fbos);
        try {
            outStream.writeObject(orig);
            outStream.flush();
        } finally {
            outStream.close();
        }

        ObjectInputStream inStream = new ObjectInputStreamWithTCCL(fbos.getInputStream());
        try {
            return inStream.readObject();
        } finally {
            inStream.close();
        }
    }
}
