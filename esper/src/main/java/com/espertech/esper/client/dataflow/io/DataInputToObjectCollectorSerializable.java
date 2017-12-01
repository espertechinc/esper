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

import com.espertech.esper.util.EventBeanSummarizer;
import com.espertech.esper.util.SerializerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Reads a {@link java.io.Serializable} from {@link java.io.DataInput} and emits the resulting object.
 * <p>
 * The input must carry an int-typed number of bytes followed by the serialized object.
 * </p>
 */
public class DataInputToObjectCollectorSerializable implements DataInputToObjectCollector {
    private static final Logger log = LoggerFactory.getLogger(DataInputToObjectCollectorSerializable.class);

    public void collect(DataInputToObjectCollectorContext context) throws IOException {
        int size = context.getDataInput().readInt();
        byte[] bytes = new byte[size];
        context.getDataInput().readFully(bytes);
        Object event = SerializerUtil.byteArrToObject(bytes);
        if (log.isDebugEnabled()) {
            log.debug("Submitting event " + EventBeanSummarizer.summarizeUnderlying(event));
        }
        context.getEmitter().submit(event);
    }
}
