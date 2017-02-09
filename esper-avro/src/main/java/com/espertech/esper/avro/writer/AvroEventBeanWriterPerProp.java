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
package com.espertech.esper.avro.writer;

import com.espertech.esper.avro.core.AvroGenericDataBackedEventBean;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.event.EventBeanWriter;
import org.apache.avro.generic.GenericData;

/**
 * Writer method for writing to Object-array-type events.
 */
public class AvroEventBeanWriterPerProp implements EventBeanWriter {
    private final AvroEventBeanPropertyWriter[] writers;

    /**
     * Ctor.
     *
     * @param writers names of properties to write
     */
    public AvroEventBeanWriterPerProp(AvroEventBeanPropertyWriter[] writers) {
        this.writers = writers;
    }

    /**
     * Write values to an event.
     *
     * @param values   to write
     * @param theEvent to write to
     */
    public void write(Object[] values, EventBean theEvent) {
        AvroGenericDataBackedEventBean arrayEvent = (AvroGenericDataBackedEventBean) theEvent;
        GenericData.Record arr = arrayEvent.getProperties();

        for (int i = 0; i < writers.length; i++) {
            writers[i].write(values[i], arr);
        }
    }
}
