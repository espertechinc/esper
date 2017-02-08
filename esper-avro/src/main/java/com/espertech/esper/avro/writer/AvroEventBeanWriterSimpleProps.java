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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.event.EventBeanWriter;
import org.apache.avro.generic.GenericData;

public class AvroEventBeanWriterSimpleProps implements EventBeanWriter {
    private final int[] positions;

    public AvroEventBeanWriterSimpleProps(int[] positions) {
        this.positions = positions;
    }

    public void write(Object[] values, EventBean theEvent) {
        GenericData.Record row = (GenericData.Record) theEvent.getUnderlying();
        for (int i = 0; i < values.length; i++) {
            row.put(positions[i], values[i]);
        }
    }
}
