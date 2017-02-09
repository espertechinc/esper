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
package com.espertech.esper.avro.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventBeanManufacturer;
import com.espertech.esper.event.WriteablePropertyDescriptor;
import com.espertech.esper.event.avro.AvroSchemaEventType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

/**
 * Factory for ObjectArray-underlying events.
 */
public class EventBeanManufacturerAvro implements EventBeanManufacturer {
    private final AvroEventType eventType;
    private final Schema schema;
    private final EventAdapterService eventAdapterService;
    private final int[] indexPerWritable;

    /**
     * Ctor.
     *
     * @param eventType           type to create
     * @param eventAdapterService event factory
     * @param properties          written properties
     */
    public EventBeanManufacturerAvro(AvroSchemaEventType eventType, EventAdapterService eventAdapterService, WriteablePropertyDescriptor[] properties) {
        this.eventAdapterService = eventAdapterService;
        this.eventType = (AvroEventType) eventType;
        this.schema = this.eventType.getSchemaAvro();

        indexPerWritable = new int[properties.length];
        for (int i = 0; i < properties.length; i++) {
            String propertyName = properties[i].getPropertyName();

            Schema.Field field = schema.getField(propertyName);
            if (field == null) {
                throw new IllegalStateException("Failed to find property '" + propertyName + "' among the array indexes");
            }
            indexPerWritable[i] = field.pos();
        }
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
