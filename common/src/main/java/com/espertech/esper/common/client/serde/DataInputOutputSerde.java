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
package com.espertech.esper.common.client.serde;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Implementations read and write objects from/to the stream.
 */
public interface DataInputOutputSerde<E> {

    /**
     * Write an object to the stream.
     *
     * @param object  to write or null if this is a nullable value
     * @param output  to write to
     * @param unitKey the page key of the page containing the object, can be null if not relevant or not provided
     * @param writer  the writer for events, can be null if not relevant or not provided
     * @throws IOException for io exceptions
     */
    public void write(E object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException;

    /**
     * Read an object from the stream.
     *
     * @param input   input to read
     * @param unitKey the identifying key of the reader, can be null if not relevant or not provided
     * @return object read or null if this is a nullable value
     * @throws IOException for io exceptions
     */
    public E read(DataInput input, byte[] unitKey) throws IOException;
}
