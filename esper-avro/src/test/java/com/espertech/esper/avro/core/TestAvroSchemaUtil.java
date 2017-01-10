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
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.avro.AvroConstantsNoDep;
import com.espertech.esper.util.JavaClassHelper;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestAvroSchemaUtil extends TestCase {
    private final static EventAdapterService eventAdapterService = SupportEventAdapterService.getService();

    public void testAssemble() {
        ConfigurationEngineDefaults.EventMeta.AvroSettings defaults = new ConfigurationEngineDefaults.EventMeta.AvroSettings();
        ConfigurationEngineDefaults.EventMeta.AvroSettings disableNativeString = new ConfigurationEngineDefaults.EventMeta.AvroSettings();
        disableNativeString.setEnableNativeString(false);

        assertType(boolean.class, Schema.Type.BOOLEAN, false, defaults);
        assertType(Boolean.class, Schema.Type.BOOLEAN, true, defaults);
        assertType(int.class, Schema.Type.INT, false, defaults);
        assertType(Integer.class, Schema.Type.INT, true, defaults);
        assertType(byte.class, Schema.Type.INT, false, defaults);
        assertType(Byte.class, Schema.Type.INT, true, defaults);
        assertType(long.class, Schema.Type.LONG, false, defaults);
        assertType(Long.class, Schema.Type.LONG, true, defaults);
        assertType(float.class, Schema.Type.FLOAT, false, defaults);
        assertType(Float.class, Schema.Type.FLOAT, true, defaults);
        assertType(double.class, Schema.Type.DOUBLE, false, defaults);
        assertType(Double.class, Schema.Type.DOUBLE, true, defaults);
        assertType(String.class, Schema.Type.STRING, true, defaults);
        assertType(String.class, Schema.Type.STRING, true, disableNativeString);
        assertType(CharSequence.class, Schema.Type.STRING, true, defaults);

        assertTypeArray(boolean.class, Schema.Type.BOOLEAN, false, defaults);
        assertTypeArray(Boolean.class, Schema.Type.BOOLEAN, true, defaults);
        assertTypeArray(int.class, Schema.Type.INT, false, defaults);
        assertTypeArray(Integer.class, Schema.Type.INT, true, defaults);
        assertTypeArray(Byte.class, Schema.Type.INT, true, defaults);
        assertTypeArray(long.class, Schema.Type.LONG, false, defaults);
        assertTypeArray(Long.class, Schema.Type.LONG, true, defaults);
        assertTypeArray(float.class, Schema.Type.FLOAT, false, defaults);
        assertTypeArray(Float.class, Schema.Type.FLOAT, true, defaults);
        assertTypeArray(double.class, Schema.Type.DOUBLE, false, defaults);
        assertTypeArray(Double.class, Schema.Type.DOUBLE, true, defaults);
        assertTypeArray(String.class, Schema.Type.STRING, true, defaults);
        assertTypeArray(String.class, Schema.Type.STRING, true, disableNativeString);
        assertTypeArray(CharSequence.class, Schema.Type.STRING, true, defaults);

        Schema schema = assemble(byte[].class, null, defaults, eventAdapterService);
        assertEquals(Schema.Type.BYTES, schema.getType());

        for (Class mapClass : new Class[] {LinkedHashMap.class, Map.class}) {
            schema = assemble(mapClass, null, defaults, eventAdapterService);
            assertEquals(Schema.Type.MAP, schema.getType());
        }
    }

    private void assertTypeArray(Class componentType, Schema.Type expectedElementType, boolean unionOfNull, ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings) {
        Schema schema = assemble(JavaClassHelper.getArrayType(componentType), null, avroSettings, eventAdapterService);
        assertEquals(Schema.Type.ARRAY, schema.getType());
        if (!unionOfNull) {
            assertEquals(expectedElementType, schema.getElementType().getType());
            assertStringNative(schema.getElementType(), avroSettings);
        }
        else {
            assertEquals(2, schema.getElementType().getTypes().size());
            assertEquals(Schema.Type.NULL, schema.getElementType().getTypes().get(0).getType());
            assertEquals(expectedElementType, schema.getElementType().getTypes().get(1).getType());
        }
    }

    private void assertType(Class clazz, Schema.Type expected, boolean unionOfNull, ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings) {
        Schema schema = assemble(clazz, null, avroSettings, eventAdapterService);
        if (!unionOfNull) {
            assertEquals(expected, schema.getType());
            assertStringNative(schema, avroSettings);
        }
        else {
            assertEquals(Schema.Type.UNION, schema.getType());
            assertEquals(2, schema.getTypes().size());
            assertEquals(Schema.Type.NULL, schema.getTypes().get(0).getType());
            assertEquals(expected, schema.getTypes().get(1).getType());
        }
    }

    private Schema assemble(Object value, Annotation[] annotations, ConfigurationEngineDefaults.EventMeta.AvroSettings avroSettings, EventAdapterService eventAdapterService) {
        SchemaBuilder.FieldAssembler<Schema> assembler = SchemaBuilder.record("myrecord").fields();
        AvroSchemaUtil.assembleField("somefield", value, assembler, annotations, avroSettings, eventAdapterService);
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
        }
        else {
            assertNull(prop);
        }
    }
}
