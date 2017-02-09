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

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestAvroSchemaUtil extends TestCase {
    private final static EventAdapterService EVENT_ADAPTER_SERVICE = SupportEventAdapterService.getService();

    public void testAssemble() {
        ConfigurationEngineDefaults.EventMeta.AvroSettings defaults = new ConfigurationEngineDefaults.EventMeta.AvroSettings();
        ConfigurationEngineDefaults.EventMeta.AvroSettings disableNativeString = new ConfigurationEngineDefaults.EventMeta.AvroSettings();
        disableNativeString.setEnableNativeString(false);
        ConfigurationEngineDefaults.EventMeta.AvroSettings disableRequired = new ConfigurationEngineDefaults.EventMeta.AvroSettings();
        disableRequired.setEnableSchemaDefaultNonNull(false);

        assertType(boolean.class, Schema.Type.BOOLEAN, false, defaults);
        assertType(Boolean.class, Schema.Type.BOOLEAN, false, defaults);
        assertType(int.class, Schema.Type.INT, false, defaults);
        assertType(Integer.class, Schema.Type.INT, false, defaults);
        assertType(byte.class, Schema.Type.INT, false, defaults);
        assertType(Byte.class, Schema.Type.INT, false, defaults);
        assertType(long.class, Schema.Type.LONG, false, defaults);
        assertType(Long.class, Schema.Type.LONG, false, defaults);
        assertType(float.class, Schema.Type.FLOAT, false, defaults);
        assertType(Float.class, Schema.Type.FLOAT, false, defaults);
        assertType(double.class, Schema.Type.DOUBLE, false, defaults);
        assertType(Double.class, Schema.Type.DOUBLE, false, defaults);
        assertType(String.class, Schema.Type.STRING, false, defaults);
        assertType(String.class, Schema.Type.STRING, false, disableNativeString);
        assertType(CharSequence.class, Schema.Type.STRING, false, defaults);

        assertType(boolean.class, Schema.Type.BOOLEAN, false, disableRequired);
        assertType(Boolean.class, Schema.Type.BOOLEAN, true, disableRequired);
        assertType(int.class, Schema.Type.INT, false, disableRequired);
        assertType(Integer.class, Schema.Type.INT, true, disableRequired);
        assertType(byte.class, Schema.Type.INT, false, disableRequired);
        assertType(Byte.class, Schema.Type.INT, true, disableRequired);
        assertType(long.class, Schema.Type.LONG, false, disableRequired);
        assertType(Long.class, Schema.Type.LONG, true, disableRequired);
        assertType(float.class, Schema.Type.FLOAT, false, disableRequired);
        assertType(Float.class, Schema.Type.FLOAT, true, disableRequired);
        assertType(double.class, Schema.Type.DOUBLE, false, disableRequired);
        assertType(Double.class, Schema.Type.DOUBLE, true, disableRequired);
        assertType(String.class, Schema.Type.STRING, true, disableRequired);

        // Array rules:
        // - Array-of-primitive: default non-null and non-null elements
        // - Array-of-boxed: default nullable and nullable elements
        // - Array-of-String: default non-nullable and non-null elements
        assertTypeArray(boolean.class, Schema.Type.BOOLEAN, false, false, defaults);
        assertTypeArray(Boolean.class, Schema.Type.BOOLEAN, false, true, defaults);
        assertTypeArray(int.class, Schema.Type.INT, false, false, defaults);
        assertTypeArray(Integer.class, Schema.Type.INT, false, true, defaults);
        assertTypeArray(Byte.class, Schema.Type.INT, false, true, defaults);
        assertTypeArray(long.class, Schema.Type.LONG, false, false, defaults);
        assertTypeArray(Long.class, Schema.Type.LONG, false, true, defaults);
        assertTypeArray(float.class, Schema.Type.FLOAT, false, false, defaults);
        assertTypeArray(Float.class, Schema.Type.FLOAT, false, true, defaults);
        assertTypeArray(double.class, Schema.Type.DOUBLE, false, false, defaults);
        assertTypeArray(Double.class, Schema.Type.DOUBLE, false, true, defaults);
        assertTypeArray(String.class, Schema.Type.STRING, false, false, defaults);
        assertTypeArray(String.class, Schema.Type.STRING, false, false, disableNativeString);
        assertTypeArray(CharSequence.class, Schema.Type.STRING, false, false, defaults);

        assertTypeArray(boolean.class, Schema.Type.BOOLEAN, true, false, disableRequired);
        assertTypeArray(Boolean.class, Schema.Type.BOOLEAN, true, true, disableRequired);
        assertTypeArray(int.class, Schema.Type.INT, true, false, disableRequired);
        assertTypeArray(Integer.class, Schema.Type.INT, true, true, disableRequired);
        assertTypeArray(Byte.class, Schema.Type.INT, true, true, disableRequired);
        assertTypeArray(long.class, Schema.Type.LONG, true, false, disableRequired);
        assertTypeArray(Long.class, Schema.Type.LONG, true, true, disableRequired);
        assertTypeArray(float.class, Schema.Type.FLOAT, true, false, disableRequired);
        assertTypeArray(Float.class, Schema.Type.FLOAT, true, true, disableRequired);
        assertTypeArray(double.class, Schema.Type.DOUBLE, true, false, disableRequired);
        assertTypeArray(Double.class, Schema.Type.DOUBLE, true, true, disableRequired);
        assertTypeArray(String.class, Schema.Type.STRING, true, false, disableRequired);
        assertTypeArray(CharSequence.class, Schema.Type.STRING, true, false, disableRequired);

        assertEquals(Schema.Type.BYTES, assemble(byte[].class, null, defaults, EVENT_ADAPTER_SERVICE).getType());
        Schema bytesUnion = assemble(byte[].class, null, disableRequired, EVENT_ADAPTER_SERVICE);
        assertEquals(2, bytesUnion.getTypes().size());
        assertEquals(Schema.Type.NULL, bytesUnion.getTypes().get(0).getType());
        assertEquals(Schema.Type.BYTES, bytesUnion.getTypes().get(1).getType());

        for (Class mapClass : new Class[]{LinkedHashMap.class, Map.class}) {
            Schema schemaReq = assemble(mapClass, null, defaults, EVENT_ADAPTER_SERVICE);
            assertEquals(Schema.Type.MAP, schemaReq.getType());
            System.out.println(schemaReq);

            Schema schemaOpt = assemble(mapClass, null, disableRequired, EVENT_ADAPTER_SERVICE);
            assertEquals(2, schemaOpt.getTypes().size());
            assertEquals(Schema.Type.NULL, schemaOpt.getTypes().get(0).getType());
            assertEquals(Schema.Type.MAP, schemaOpt.getTypes().get(1).getType());
            System.out.println(schemaOpt);
        }
    }

    private void assertTypeArray(Class componentType, Schema.Type expectedElementType, boolean unionOfNull, boolean unionOfNullElements, ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings) {
        Schema schema = assemble(JavaClassHelper.getArrayType(componentType), null, avroSettings, EVENT_ADAPTER_SERVICE);

        Schema elementSchema;
        if (!unionOfNull) {
            assertEquals(Schema.Type.ARRAY, schema.getType());
            elementSchema = schema.getElementType();
        } else {
            assertEquals(2, schema.getTypes().size());
            assertEquals(Schema.Type.NULL, schema.getTypes().get(0).getType());
            assertEquals(Schema.Type.ARRAY, schema.getTypes().get(1).getType());
            elementSchema = schema.getTypes().get(1).getElementType();
        }

        // assert element type
        if (!unionOfNullElements) {
            assertEquals(expectedElementType, elementSchema.getType());
            assertStringNative(elementSchema, avroSettings);
        } else {
            assertEquals(2, elementSchema.getTypes().size());
            assertEquals(Schema.Type.NULL, elementSchema.getTypes().get(0).getType());
            assertEquals(expectedElementType, elementSchema.getTypes().get(1).getType());
        }
    }

    private void assertType(Class clazz, Schema.Type expected, boolean unionOfNull, ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings) {
        Schema schema = assemble(clazz, null, avroSettings, EVENT_ADAPTER_SERVICE);
        if (!unionOfNull) {
            assertEquals(expected, schema.getType());
            assertStringNative(schema, avroSettings);
        } else {
            assertEquals(Schema.Type.UNION, schema.getType());
            assertEquals(2, schema.getTypes().size());
            assertEquals(Schema.Type.NULL, schema.getTypes().get(0).getType());
            assertEquals(expected, schema.getTypes().get(1).getType());
        }
    }

    private Schema assemble(Object value, Annotation[] annotations, ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings, EventAdapterService eventAdapterService) {
        SchemaBuilder.FieldAssembler<Schema> assembler = SchemaBuilder.record("myrecord").fields();
        AvroSchemaUtil.assembleField("somefield", value, assembler, annotations, avroSettings, eventAdapterService, "stmtname", "default", null);
        Schema schema = assembler.endRecord();
        return schema.getField("somefield").schema();
    }

    private void assertStringNative(Schema elementType, ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings) {
        if (elementType.getType() != Schema.Type.STRING) {
            return;
        }
        String prop = elementType.getProp(AvroConstant.PROP_JAVA_STRING_KEY);
        if (avroSettings.isEnableNativeString()) {
            assertEquals(prop, AvroConstant.PROP_JAVA_STRING_VALUE);
        } else {
            assertNull(prop);
        }
    }
}
