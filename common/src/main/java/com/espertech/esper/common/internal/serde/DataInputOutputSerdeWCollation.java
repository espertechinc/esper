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
package com.espertech.esper.common.internal.serde;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Implementations read and write objects from/to the stream.
 */
public interface DataInputOutputSerdeWCollation<E> {

    /**
     * Write an object to the stream.
     *
     * @param object  to write
     * @param output  to write to
     * @param unitKey the page key of the page containing the object
     * @param writer  the writer for events
     * @throws IOException for io exceptions
     */
    public void write(E object, DataOutput output, byte[] unitKey, EventBeanCollatedWriter writer) throws IOException;

    /**
     * Read an object from the stream.
     *
     * @param input   input to read
     * @param unitKey the identifying key of the reader
     * @return object read
     * @throws IOException for io exceptions
     */
    public E read(DataInput input, byte[] unitKey) throws IOException;
}
