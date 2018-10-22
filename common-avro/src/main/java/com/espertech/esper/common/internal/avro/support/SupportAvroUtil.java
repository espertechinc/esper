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
package com.espertech.esper.common.internal.avro.support;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.avro.core.AvroEventType;
import com.espertech.esper.common.internal.avro.core.EventTypeAvroHandlerImpl;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.*;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class SupportAvroUtil {

    public static String avroToJson(EventBean theEvent) {
        Schema schema = (Schema) ((AvroSchemaEventType) theEvent.getEventType()).getSchema();
        GenericData.Record record = (GenericData.Record) theEvent.getUnderlying();
        return avroToJson(schema, record);
    }

    public static <D> String avroToJson(Schema schema, GenericData.Record datum) {
        DatumWriter<Object> writer = new GenericDatumWriter<Object>(schema);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, bos);
            writer.write(datum, encoder);
            encoder.flush();
            return new String(bos.toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static GenericData.Record parseQuoted(Schema schema, String json) {
        return parse(schema, json.replace("'", "\""));
    }

    public static GenericData.Record parse(Schema schema, String json) {
        InputStream input = new ByteArrayInputStream(json.getBytes());
        DataInputStream din = new DataInputStream(input);
        try {
            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, din);
            DatumReader<Object> reader = new GenericDatumReader<>(schema);
            return (GenericData.Record) reader.read(null, decoder);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to parse json: " + ex.getMessage(), ex);
        }
    }

    public static String compareSchemas(Schema schemaOne, Schema schemaTwo) {
        Set<String> names = new HashSet<>();
        addSchemaFieldNames(names, schemaOne);
        addSchemaFieldNames(names, schemaTwo);

        for (String name : names) {
            Schema.Field fieldOne = schemaOne.getField(name);
            Schema.Field fieldTwo = schemaTwo.getField(name);
            if (fieldOne == null) {
                return "Failed to find field '" + name + " in schema-one";
            }
            if (fieldTwo == null) {
                return "Failed to find field '" + name + " in schema-one";
            }
            if (!fieldOne.schema().equals(fieldTwo.schema())) {
                return "\nSchema-One: " + fieldOne.schema() + "\n" +
                        "Schema-Two: " + fieldTwo.schema();
            }
        }
        return null;
    }

    public static AvroEventType makeAvroSupportEventType(Schema schema) {
        EventTypeMetadata metadata = new EventTypeMetadata("typename", null, EventTypeTypeClass.STREAM, EventTypeApplicationType.AVRO, NameAccessModifier.TRANSIENT, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        return new AvroEventType(metadata, schema, null, null, null, null, null, new EventTypeAvroHandlerImpl());
    }

    private static void addSchemaFieldNames(Set<String> names, Schema schema) {
        for (Schema.Field field : schema.getFields()) {
            names.add(field.name());
        }
    }

    public static Schema getAvroSchema(EventBean event) {
        return getAvroSchema(event.getEventType());
    }

    public static Schema getAvroSchema(EventType eventType) {
        return ((AvroEventType) eventType).getSchemaAvro();
    }
}
