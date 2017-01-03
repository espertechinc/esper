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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.annotation.AvroField;
import com.espertech.esper.util.JavaClassHelper;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.lang.annotation.Annotation;
import java.util.*;

public class AvroSchemaUtil {

    public static Schema findUnionRecordSchemaSingle(Schema schema) {
        if (schema.getType() != Schema.Type.UNION) {
            return null;
        }
        Schema found = null;
        for (Schema member : schema.getTypes()) {
            if (member.getType() == Schema.Type.RECORD) {
                if (found == null) {
                    found = member;
                }
                else {
                    return null;
                }
            }
        }
        return found;
    }

    public static void assembleField(Map.Entry<String, Object> prop, SchemaBuilder.FieldAssembler<Schema> assembler, Annotation[] annotations) {
        Object type = prop.getValue();
        if (type == Object.class) {
            assembleFieldObject(prop.getKey(), assembler, annotations, Object.class);
        }
        else if (type == Boolean.class) { {
            assembler.optionalBoolean(prop.getKey());
        }}
        else {
            // TODO
            throw new UnsupportedOperationException();
        }
    }

    private static void assembleFieldObject(String key, SchemaBuilder.FieldAssembler<Schema> assembler, Annotation[] annotations, Class outputType) {
        Set<Schema.Type> schemaTypes = null;
        for (Annotation annotation : annotations) {
            if (annotation instanceof AvroField) {
                AvroField avroField = (AvroField) annotation;
                if (avroField.name().equals(key)) {
                    String[] types = avroField.types().split(",");
                    for (String type : types) {
                        try {
                            if (schemaTypes == null) {
                                schemaTypes = new LinkedHashSet<>();
                            }
                            Schema.Type schemaType = Schema.Type.valueOf(type.toUpperCase());
                            schemaTypes.add(schemaType);
                        }
                        catch (RuntimeException ex) {
                            throw new EPException("Failed to resolve Avro schema type '" + type + "' to a known Avro type");
                        }
                    }
                }
            }
        }
        if (schemaTypes == null) {
            throw new EPException("Failed to determine Avro field schema type for field '" + key + "' typed '" + outputType.getName() + "', please use the " + AvroField.class.getName() + " annotation to assign type or union");
        }
        if (schemaTypes.size() == 1) {
            assembler.name(key).type(Schema.create(schemaTypes.iterator().next()));
            return;
        }
        SchemaBuilder.UnionFieldTypeBuilder<Schema> unionOf = assembler.name(key).type().unionOf();
        int count = 0;
        SchemaBuilder.UnionAccumulator accumulator = null;
        SchemaBuilder.BaseTypeBuilder<SchemaBuilder.UnionAccumulator<?>> builder = null;
        for (Schema.Type type : schemaTypes) {
            if (type == Schema.Type.STRING) {
                if (accumulator == null) {
                    accumulator = unionOf.stringBuilder().prop("avro.java.string", "String").endString();
                }
                else {
                    accumulator = builder.stringBuilder().prop("avro.java.string", "String").endString();
                }
            }
            else if (type == Schema.Type.INT) {
                if (accumulator == null) {
                    accumulator = unionOf.intBuilder().endInt();
                }
                else {
                    accumulator = builder.intBuilder().endInt();
                }
            }
            else if (type == Schema.Type.NULL) {
                if (accumulator == null) {
                    accumulator = unionOf.nullBuilder().endNull();
                }
                else {
                    accumulator = builder.nullBuilder().endNull();
                }
            }
            // TODO more types
            if (count < schemaTypes.size()) {
                builder = accumulator.and();
            }
            count++;
        }
        ((SchemaBuilder.FieldDefault) accumulator.endUnion()).noDefault();
    }
}
