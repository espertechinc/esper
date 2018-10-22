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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

/**
 * Factory for ObjectArray-underlying events.
 */
public class EventBeanManufacturerAvro implements EventBeanManufacturer {
    private final AvroEventType eventType;
    private final Schema schema;
    private final EventBeanTypedEventFactory eventAdapterService;
    private final int[] indexPerWritable;

    public EventBeanManufacturerAvro(AvroSchemaEventType eventType, EventBeanTypedEventFactory eventAdapterService, int[] indexPerWritable) {
        this.eventAdapterService = eventAdapterService;
        this.eventType = (AvroEventType) eventType;
        this.schema = this.eventType.getSchemaAvro();
        this.indexPerWritable = indexPerWritable;
    }

    public EventBean make(Object[] properties) {
        Object record = makeUnderlying(properties);
        return eventAdapterService.adapterForTypedAvro(record, eventType);
    }

    public Object makeUnderlying(Object[] properties) {
        GenericData.Record record = new GenericData.Record(schema);
        for (int i = 0; i < properties.length; i++) {
            int indexToWrite = indexPerWritable[i];
            record.put(indexToWrite, properties[i]);
        }
        return record;
    }
}
