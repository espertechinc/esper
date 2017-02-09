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
package com.espertech.esper.client.dataflow.io;

import com.espertech.esper.util.SerializerUtil;

import java.io.IOException;

/**
 * Writes a {@link java.io.Serializable} object to {@link java.io.DataOutput}.
 * <p>
 * The output contains the byte array length integer followed by the byte array of the serialized object.
 * </p>
 */
public class ObjectToDataOutputCollectorSerializable implements ObjectToDataOutputCollector {

    public void collect(ObjectToDataOutputCollectorContext context) throws IOException {
        byte[] bytes = SerializerUtil.objectToByteArr(context.getEvent());
        context.getDataOutput().writeInt(bytes.length);
        context.getDataOutput().write(bytes);
    }
}
