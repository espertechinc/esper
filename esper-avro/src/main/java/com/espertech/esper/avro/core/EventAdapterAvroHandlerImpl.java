/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.avro.core;

import com.espertech.esper.avro.selectexprrep.SelectExprProcessorRepresentationFactoryAvro;
import com.espertech.esper.client.*;
import com.espertech.esper.event.EventAdapterServiceImpl;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.avro.EventAdapterAvroHandler;
import com.espertech.esper.core.SelectExprProcessorRepresentationFactory;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.lang.annotation.Annotation;
import java.util.Map;

import static org.apache.avro.SchemaBuilder.record;

public class EventAdapterAvroHandlerImpl implements EventAdapterAvroHandler {
    private final static SelectExprProcessorRepresentationFactoryAvro factory = new SelectExprProcessorRepresentationFactoryAvro();

    public EventType newEventTypeFromSchema(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterServiceImpl eventAdapterService, ConfigurationEventTypeAvro avro) {
        Object avroSchemaObj = avro.getAvroSchema();
        String avroSchemaText = avro.getAvroSchemaText();

        if (avroSchemaObj == null && avroSchemaText == null) {
            throw new IllegalArgumentException("Null value for schema and schema text");
        }
        if (avroSchemaObj != null && avroSchemaText != null) {
            throw new IllegalArgumentException("Both avro schema and avro schema text are supplied and one can be provided");
        }
        if (avroSchemaObj != null && !(avroSchemaObj instanceof Schema)) {
            throw new IllegalArgumentException("Schema expected of type " + Schema.class.getName() + " but received " + avroSchemaObj.getClass().getName());
        }

        Schema schema;
        if (avroSchemaObj != null) {
            schema = (Schema) avroSchemaObj;
        }
        else {
            try {
                schema = new Schema.Parser().parse(avroSchemaText);
            }
            catch (Throwable t) {
                throw new EPException("Failed for parse avro schema: " + t.getMessage(), t);
            }
        }
        return new AvroEventType(metadata, eventTypeName, typeId, eventAdapterService, schema);
    }

    public EventType newEventTypeFromNormalized(EventTypeMetadata metadata, String eventTypeName, int typeId, EventAdapterServiceImpl eventAdapterService, Map<String, Object> properties, Annotation[] annotations) {
        SchemaBuilder.FieldAssembler<Schema> assembler = record(eventTypeName).fields();
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            AvroSchemaUtil.assembleField(prop, assembler, annotations);
        }
        Schema schema = assembler.endRecord();
        return new AvroEventType(metadata, eventTypeName, typeId, eventAdapterService, schema);
    }

    public EventBean adapterForTypeAvro(Object avroGenericDataDotRecord, EventType existingType) {
        if (!(avroGenericDataDotRecord instanceof GenericData.Record)) {
            throw new EPException("Unexpected event object type '" + (avroGenericDataDotRecord == null ? "null" : avroGenericDataDotRecord.getClass().getName()) + "' encountered, please supply a GenericData.Record");
        }

        GenericData.Record record = (GenericData.Record) avroGenericDataDotRecord;
        return new AvroEventBean(record, existingType);
    }

    public SelectExprProcessorRepresentationFactory getOutputFactory() {
        return factory;
    }
}
