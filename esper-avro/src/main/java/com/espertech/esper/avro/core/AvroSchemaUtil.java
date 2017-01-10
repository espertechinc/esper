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

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.annotation.AvroField;
import com.espertech.esper.event.BaseNestableEventType;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventTypeUtility;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.util.JavaClassHelper;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.lang.annotation.Annotation;
import java.util.*;

import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.apache.avro.SchemaBuilder.*;

public class AvroSchemaUtil {

    public static String toSchemaStringSafe(Schema schema) {
        try {
            return schema.toString();
        }
        catch (Throwable t) {
            return "[Invalid schema: " + t.getClass().getName() + ": " + t.getMessage() + "]";
        }
    }

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

    public static void assembleField(String propertyName, Object propertyType, SchemaBuilder.FieldAssembler<Schema> assembler, Annotation[] annotations, ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings, EventAdapterService eventAdapterService) {
        if (propertyName.contains(".")) {
            throw new EPException("Invalid property name as Avro does not allow dot '.' in field names (property '" + propertyName + "')");
        }
        Schema schema = getAnnotationSchema(propertyName, annotations);
        if (schema != null) {
            assembler.name(propertyName).type(schema).noDefault();
        }
        else if (propertyType == null) {
            assembler.name(propertyName).type("null");
        }
        else if (propertyType == Boolean.class) {
            assembler.optionalBoolean(propertyName);
        }
        else if (propertyType == boolean.class) {
            assembler.requiredBoolean(propertyName);
        }
        else if (propertyType == Integer.class) {
            assembler.optionalInt(propertyName);
        }
        else if (propertyType == int.class) {
            assembler.requiredInt(propertyName);
        }
        else if (propertyType == Byte.class) {
            assembler.optionalInt(propertyName);
        }
        else if (propertyType == byte.class) {
            assembler.requiredInt(propertyName);
        }
        else if (propertyType == Long.class) {
            assembler.optionalLong(propertyName);
        }
        else if (propertyType == long.class) {
            assembler.requiredLong(propertyName);
        }
        else if (propertyType == Float.class) {
            assembler.optionalFloat(propertyName);
        }
        else if (propertyType == float.class) {
            assembler.requiredFloat(propertyName);
        }
        else if (propertyType == Double.class) {
            assembler.optionalDouble(propertyName);
        }
        else if (propertyType == double.class) {
            assembler.requiredDouble(propertyName);
        }
        else if (propertyType == String.class || propertyType == CharSequence.class) {
            if (avroSettings.isEnableNativeString()) {
                assembler.name(propertyName).type().unionOf()
                        .nullType()
                        .and()
                        .stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString()
                        .endUnion().noDefault();
            }
            else {
                assembler.optionalString(propertyName);
            }
        }
        else if (propertyType instanceof String) {
            String propertyTypeName = propertyType.toString();
            boolean isArray = EventTypeUtility.isPropertyArray(propertyTypeName);
            if (isArray) {
                propertyTypeName = EventTypeUtility.getPropertyRemoveArray(propertyTypeName);
            }

            // Add EventType itself as a property
            EventType eventType = eventAdapterService.getExistsTypeByName(propertyTypeName);
            if (!(eventType instanceof AvroEventType)) {
                throw new EPException("Type definition encountered an unexpected property type name '"
                        + propertyType + "' for property '" + propertyName + "', expected the name of a previously-declared Avro type");
            }
            schema = ((AvroEventType) eventType).getSchemaAvro();

            if (!isArray) {
                assembler.name(propertyName).type(schema).noDefault();
            }
            else {
                assembler.name(propertyName).type(array().items(schema)).noDefault();
            }
        }
        else if (propertyType == Boolean[].class) {
            Schema opt = unionOf().nullType().and().booleanType().endUnion();
            assembler.name(propertyName).type(array().items(opt)).noDefault();
        }
        else if (propertyType == boolean[].class) {
            assembler.name(propertyName).type(array().items().booleanType()).noDefault();
        }
        else if (propertyType == Integer[].class) {
            Schema opt = unionOf().nullType().and().intType().endUnion();
            assembler.name(propertyName).type(array().items(opt)).noDefault();
        }
        else if (propertyType == int[].class) {
            assembler.name(propertyName).type(array().items().intType()).noDefault();
        }
        else if (propertyType == byte[].class) {
            assembler.requiredBytes(propertyName);
        }
        else if (propertyType == Long[].class) {
            Schema opt = unionOf().nullType().and().longType().endUnion();
            assembler.name(propertyName).type(array().items(opt)).noDefault();
        }
        else if (propertyType == long[].class) {
            assembler.name(propertyName).type(array().items().longType()).noDefault();
        }
        else if (propertyType == Float[].class) {
            Schema opt = unionOf().nullType().and().floatType().endUnion();
            assembler.name(propertyName).type(array().items(opt)).noDefault();
        }
        else if (propertyType == float[].class) {
            assembler.name(propertyName).type(array().items().floatType()).noDefault();
        }
        else if (propertyType == Byte[].class) {
            Schema opt = unionOf().nullType().and().intType().endUnion();
            assembler.name(propertyName).type(array().items(opt)).noDefault();
        }
        else if (propertyType == Double[].class) {
            Schema opt = unionOf().nullType().and().doubleType().endUnion();
            assembler.name(propertyName).type(array().items(opt)).noDefault();
        }
        else if (propertyType == double[].class) {
            assembler.name(propertyName).type(array().items().doubleType()).noDefault();
        }
        else if (propertyType == String[].class || propertyType == CharSequence[].class) {
            Schema opt;
            if (avroSettings.isEnableNativeString()) {
                opt = unionOf().nullType().and().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().endUnion();
            }
            else {
                opt = unionOf().nullType().and().stringBuilder().endString().endUnion();
            }
            assembler.name(propertyName).type(array().items(opt)).noDefault();
        }
        else if (propertyType instanceof EventType){
            EventType eventType = (EventType) propertyType;
            checkAvroType(eventType);
            schema = ((AvroEventType) eventType).getSchemaAvro();
            assembler.name(propertyName).type(schema).noDefault();
        }
        else if (propertyType instanceof EventType[]) {
            EventType eventType = ((EventType[]) propertyType)[0];
            checkAvroType(eventType);
            schema = ((AvroEventType) eventType).getSchemaAvro();
            assembler.name(propertyName).type(array().items(schema)).noDefault();
        }
        else if (propertyType instanceof Class && JavaClassHelper.isImplementsInterface((Class)propertyType, Map.class)) {
            assembler.name(propertyName).type(map().values().stringBuilder().endString()).noDefault();
        }
        else {
            throw new EPException("Property '" + propertyName + "' type '" + propertyType + "' does not have a mapping to an Avro type (consider using the AvroField annotation)");
        }
    }

    private static Schema getAnnotationSchema(String propertyName, Annotation[] annotations) {
        if (annotations == null) {
            return null;
        }
        for (Annotation annotation : annotations) {
            if (annotation instanceof AvroField) {
                AvroField avroField = (AvroField) annotation;
                if (avroField.name().equals(propertyName)) {
                    String schema = avroField.schema();
                    try {
                        return new Schema.Parser().parse(schema);
                    }
                    catch (RuntimeException ex) {
                        throw new EPException("Failed to parse Avro schema for property '" + propertyName + "': " + ex.getMessage(), ex);
                    }
                }
            }
        }
        return null;
    }

    private static void checkAvroType(EventType eventType) {
        if (!(eventType instanceof AvroEventType)) {
            throw new EPException("Property type cannot be an event type with an underlying of type '" + eventType.getUnderlyingType().getName() + "'");
        }
    }
}
