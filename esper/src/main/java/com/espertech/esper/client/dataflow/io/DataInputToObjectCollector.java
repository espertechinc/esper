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

import java.io.IOException;

/**
 * Collects an object from {@link java.io.DataInput} and emits the object to an emitter.
 */
public interface DataInputToObjectCollector {
    /**
     * Reads provided {@link java.io.DataInput} and emits an object using the provided emitter.
     *
     * @param context contains input and emitter
     * @throws IOException when the read operation cannot be completed
     */
    public void collect(DataInputToObjectCollectorContext context) throws IOException;
}
