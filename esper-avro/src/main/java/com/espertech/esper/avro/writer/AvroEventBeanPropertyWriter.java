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
import com.espertech.esper.event.EventPropertyWriter;
import org.apache.avro.generic.GenericData;

public class AvroEventBeanPropertyWriter implements EventPropertyWriter {

    protected final int index;

    public AvroEventBeanPropertyWriter(int index) {
        this.index = index;
    }

    public void write(Object value, EventBean target) {
        AvroGenericDataBackedEventBean avroEvent = (AvroGenericDataBackedEventBean) target;
        write(value, avroEvent.getProperties());
    }

    public void write(Object value, GenericData.Record record) {
        record.put(index, value);
    }
}
