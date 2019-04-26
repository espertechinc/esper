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
package com.espertech.esper.common.internal.serde.serdeset.multikey;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.collection.MultiKeyArrayObject;
import com.espertech.esper.common.client.serde.EventBeanCollatedWriter;
import com.espertech.esper.common.internal.util.ObjectInputStreamWithTCCL;

import java.io.*;

public class DIOMultiKeyArrayObjectSerde implements DataInputOutputSerde<MultiKeyArrayObject> {
    public final static DIOMultiKeyArrayObjectSerde INSTANCE = new DIOMultiKeyArrayObjectSerde();

    public void write(MultiKeyArrayObject mk, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException {
        writeInternal(mk.getKeys(), output);
    }

    public MultiKeyArrayObject read(DataInput input, byte[] unitKey) throws IOException {
        return new MultiKeyArrayObject(readInternal(input));
    }

    private void writeInternal(Object[] object, DataOutput output) throws IOException {
        if (object == null) {
            output.writeInt(-1);
            return;
        }
        output.writeInt(object.length);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        for (Object i : object) {
            oos.writeObject(i);
        }
        oos.close();

        byte[] result = baos.toByteArray();
        output.writeInt(result.length);
        output.write(result);
        baos.close();
    }

    private Object[] readInternal(DataInput input) throws IOException {
        int len = input.readInt();
        if (len == -1) {
            return null;
        }
        Object[] array = new Object[len];
        int size = input.readInt();
        byte[] buf = new byte[size];
        input.readFully(buf);

        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        try {
            ObjectInputStream ois = new ObjectInputStreamWithTCCL(bais);
            for (int i = 0; i < array.length; i++) {
                array[i] = ois.readObject();
            }
        } catch (IOException e) {
            if (e.getMessage() != null) {
                throw new RuntimeException("IO error de-serializing object: " + e.getMessage(), e);
            } else {
                throw new RuntimeException("IO error de-serializing object", e);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found de-serializing object: " + e.getMessage(), e);
        }
        return array;
    }
}
