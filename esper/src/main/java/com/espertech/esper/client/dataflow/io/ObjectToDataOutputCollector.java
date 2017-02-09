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
 * Receives an object and writes to {@link java.io.DataOutput}.
 */
public interface ObjectToDataOutputCollector {
    /**
     * Write the received object to {@link java.io.DataOutput}.
     *
     * @param context the object and output
     * @throws IOException when the write operation failed
     */
    public void collect(ObjectToDataOutputCollectorContext context) throws IOException;
}
