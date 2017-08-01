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

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.util.Utf8;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static com.espertech.esper.avro.util.support.SupportAvroUtil.makeAvroSupportEventType;
import static org.apache.avro.SchemaBuilder.record;

public class TestAvroEventType extends TestCase {
    public void testGetPropertyType() {
        Schema lvl2Schema = record("lvl2Schema").fields()
                .name("nestedValue").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
                .name("nestedIndexed").type().array().items().intBuilder().endInt().arrayDefault(Collections.emptyList())
                .name("nestedMapped").type().map().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).values().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().mapDefault(null)
                .endRecord();

        Schema lvl1Schema = record("lvl1Schema").fields()
                .name("lvl2").type(lvl2Schema).noDefault()
                .requiredInt("intPrimitive")
                .name("indexed").type().array().items().intBuilder().endInt().arrayDefault(Collections.emptyList())
                .name("mapped").type().map().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).values().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().mapDefault(null)
                .endRecord();

        Schema schema = record("typename").fields()
                .requiredInt("myInt")
                .optionalInt("myIntBoxed")
                .name("myString").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
                .name("lvl1").type(lvl1Schema).noDefault()
                .name("myNullValue").type().nullType().noDefault()
                .endRecord();

        EventType eventType = makeAvroSupportEventType(schema);

        assertPropertyType(Integer.class, null, eventType, "myInt");
        assertPropertyType(Integer.class, null, eventType, "myIntBoxed");
        assertPropertyType(String.class, null, eventType, "myString");
        assertPropertyType(null, null, eventType, "myNullValue");
        assertPropertyType(GenericData.Record.class, null, eventType, "lvl1");
        assertPropertyType(Integer.class, null, eventType, "lvl1.intPrimitive");
        assertPropertyType(String.class, null, eventType, "lvl1.lvl2.nestedValue");
        assertPropertyType(Integer.class, null, eventType, "lvl1.indexed[1]");
        assertPropertyType(String.class, null, eventType, "lvl1.mapped('a')");
        assertPropertyType(String.class, null, eventType, "lvl1.lvl2.nestedMapped('a')");
        assertPropertyType(Integer.class, null, eventType, "lvl1.lvl2.nestedIndexed[1]");

        assertNotAProperty(eventType, "dummy");
        assertNotAProperty(eventType, "lvl1.dfgdg");
        assertNotAProperty(eventType, "xxx.intPrimitive");
        assertNotAProperty(eventType, "lvl1.lvl2.nestedValueXXX");
        assertNotAProperty(eventType, "myInt[1]");
        assertNotAProperty(eventType, "lvl1.intPrimitive[1]");
        assertNotAProperty(eventType, "myInt('a')");
        assertNotAProperty(eventType, "lvl1.intPrimitive('a')");
        assertNotAProperty(eventType, "lvl1.lvl2.nestedIndexed('a')");
        assertNotAProperty(eventType, "lvl1.lvl2.nestedMapped[1]");

        GenericData.Record lvl2Rec = new GenericData.Record(lvl2Schema);
        lvl2Rec.put("nestedValue", 100);
        lvl2Rec.put("nestedIndexed", Arrays.asList(19, 21));
        lvl2Rec.put("nestedMapped", Collections.singletonMap("nestedkey", "nestedvalue"));
        GenericData.Record lvl1Rec = new GenericData.Record(lvl1Schema);
        lvl1Rec.put("lvl2", lvl2Rec);
        lvl1Rec.put("intPrimitive", 10);
        lvl1Rec.put("indexed", Arrays.asList(1, 2, 3));
        lvl1Rec.put("mapped", Collections.singletonMap("key", "value"));
        GenericData.Record record = new GenericData.Record(schema);
        record.put("lvl1", lvl1Rec);
        record.put("myInt", 99);
        record.put("myIntBoxed", 554);
        record.put("myString", "hugo");
        record.put("myNullValue", null);

        AvroGenericDataEventBean eventBean = new AvroGenericDataEventBean(record, eventType);
        assertEquals(99, eventBean.get("myInt"));
        assertEquals(554, eventBean.get("myIntBoxed"));
        assertEquals("hugo", eventBean.get("myString"));
        assertEquals(lvl1Rec, eventBean.get("lvl1"));
        assertEquals(10, eventBean.get("lvl1.intPrimitive"));
        assertEquals(100, eventBean.get("lvl1.lvl2.nestedValue"));
        assertEquals(2, eventBean.get("lvl1.indexed[1]"));
        assertEquals("value", eventBean.get("lvl1.mapped('key')"));
        assertEquals(null, eventBean.get("myNullValue"));
        assertEquals("nestedvalue", eventBean.get("lvl1.lvl2.nestedMapped('nestedkey')"));
        assertEquals(21, eventBean.get("lvl1.lvl2.nestedIndexed[1]"));
    }

    public void testRequiredType() {
        Schema schema = record("typename").fields()
                .requiredInt("myInt")
                .requiredString("myCharSeq")
                .name("myString").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
                .requiredBoolean("myBoolean")
                .requiredBytes("myBytes")
                .requiredDouble("myDouble")
                .requiredFloat("myFloat")
                .requiredLong("myLong")
                .endRecord();
        String[] propNames = "myInt,myCharSeq,myString,myBoolean,myBytes,myDouble,myFloat,myLong".split(",");
        EventType eventType = makeAvroSupportEventType(schema);
        EPAssertionUtil.assertEqualsExactOrder(eventType.getPropertyNames(), propNames);
        assertEquals(GenericData.Record.class, eventType.getUnderlyingType());
        assertNull(eventType.getSuperTypes());

        assertPropertyType(Integer.class, null, eventType, "myInt");
        assertPropertyType(CharSequence.class, null, eventType, "myCharSeq");
        assertPropertyType(String.class, null, eventType, "myString");
        assertPropertyType(Boolean.class, null, eventType, "myBoolean");
        assertPropertyType(ByteBuffer.class, null, eventType, "myBytes");
        assertPropertyType(Double.class, null, eventType, "myDouble");
        assertPropertyType(Float.class, null, eventType, "myFloat");
        assertPropertyType(Long.class, null, eventType, "myLong");

        for (String propName : propNames) {
            assertTrue(eventType.isProperty(propName));
        }

        GenericData.Record datum = getRecordWithValues(schema);
        assertValuesRequired(new AvroGenericDataEventBean(datum, eventType));

        String jsonWValues = "{'myInt': 10, 'myCharSeq': 'x', 'myString': 'y', 'myBoolean': true, 'myBytes': '\\u00AA\'," +
                "'myDouble' : 50, 'myFloat':100, 'myLong':20}";
        assertValuesRequired(new AvroGenericDataEventBean(SupportAvroUtil.parseQuoted(schema, jsonWValues), eventType));
    }

    public void testOptionalType() {
        Schema schema = record("typename").fields()
                .optionalInt("myInt")
                .optionalString("myCharSeq")
                .name("myString").type().optional().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString()
                .optionalBoolean("myBoolean")
                .optionalBytes("myBytes")
                .optionalDouble("myDouble")
                .optionalFloat("myFloat")
                .optionalLong("myLong")
                .endRecord();
        runAssertionNullableOrOptTypes(schema);
    }

    public void testNullableType() {
        Schema schema = record("typename").fields()
                .nullableInt("myInt", Integer.MIN_VALUE)
                .nullableString("myCharSeq", null)
                .name("myString").type().nullable().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().stringDefault(null)
                .nullableBoolean("myBoolean", false)
                .nullableBytes("myBytes", new byte[0])
                .nullableDouble("myDouble", Double.MIN_VALUE)
                .nullableFloat("myFloat", Float.MIN_VALUE)
                .nullableLong("myLong", Long.MIN_VALUE)
                .endRecord();
        runAssertionNullableOrOptTypes(schema);
    }

    public void testNestedSimple() {
        String schemaText = "{" +
                "  'type' : 'record'," +
                "  'name' : 'MyEvent'," +
                "  'fields' : [ {" +
                "    'name' : 'innerEvent'," +
                "    'type' : {" +
                "      'type' : 'record'," +
                "      'name' : 'innerEventTypeName'," +
                "      'fields' : [ {" +
                "        'name' : 'innerValue'," +
                "        'type' : {'type':'string','avro.java.string':'String'}" +
                "      } ]" +
                "    }" +
                "  }]" +
                "}";
        Schema schema = new Schema.Parser().parse(schemaText.replace("'", "\""));
        EventType eventType = makeAvroSupportEventType(schema);

        assertPropertyType(GenericData.Record.class, null, eventType, "innerEvent");

        String[] propNames = "innerEvent".split(",");
        EPAssertionUtil.assertEqualsExactOrder(eventType.getPropertyNames(), propNames);
        assertTrue(eventType.isProperty("innerEvent"));

        GenericData.Record datumInner = new GenericData.Record(schema.getField("innerEvent").schema());
        datumInner.put("innerValue", "i1");
        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("innerEvent", datumInner);

        assertValuesNested(datum, new AvroGenericDataEventBean(datum, eventType));

        String jsonWValues = "{'innerEvent': {'innerValue' : 'i1'}}}";
        datum = SupportAvroUtil.parseQuoted(schema, jsonWValues);
        assertValuesNested(datum, new AvroGenericDataEventBean(datum, eventType));
    }

    public void testArrayOfPrimitive() {
        Schema schema = record("typename").fields()
                .name("intArray").type().array().items().intBuilder().endInt().arrayDefault(Collections.emptyList())
                .endRecord();
        EventType eventType = makeAvroSupportEventType(schema);

        assertPropertyType(Collection.class, Integer.class, eventType, "intArray");

        Consumer<EventBean> asserter = eventBean -> {
            assertEquals(1, eventBean.get("intArray[0]"));
            assertEquals(2, eventBean.get("intArray[1]"));
            assertEquals(1, eventType.getGetter("intArray[0]").get(eventBean));
            assertEquals(2, eventType.getGetter("intArray[1]").get(eventBean));
        };

        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("intArray", Arrays.asList(1, 2));
        asserter.accept(new AvroGenericDataEventBean(datum, eventType));

        String jsonWValues = "{'intArray':[1,2]}}";
        datum = SupportAvroUtil.parseQuoted(schema, jsonWValues);
        asserter.accept(new AvroGenericDataEventBean(datum, eventType));
    }

    public void testMapOfString() {
        Schema schema = record("typename").fields()
                .name("anMap").type().map().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).values().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().mapDefault(null)
                .endRecord();
        EventType eventType = makeAvroSupportEventType(schema);

        assertPropertyType(Map.class, String.class, eventType, "anMap");

        Consumer<EventBean> asserter = eventBean -> {
            assertEquals("myValue", eventBean.get("anMap('myKey')"));
            assertEquals("myValue", eventType.getGetter("anMap('myKey')").get(eventBean));
        };

        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("anMap", Collections.singletonMap("myKey", "myValue"));
        asserter.accept(new AvroGenericDataEventBean(datum, eventType));

        String jsonWValues = "{'anMap':{'myKey':'myValue'}}";
        datum = SupportAvroUtil.parseQuoted(schema, jsonWValues);
        asserter.accept(new AvroGenericDataEventBean(datum, eventType));
    }

    public void testFixed() {
        Schema schema = record("typename").fields()
                .name("aFixed").type().fixed("abc").size(2).fixedDefault(ByteBuffer.wrap(new byte[0]))
                .endRecord();
        EventType eventType = makeAvroSupportEventType(schema);

        assertPropertyType(GenericFixed.class, null, eventType, "aFixed");

        Consumer<EventBean> asserter = eventBean -> {
            GenericData.Fixed fixed = (GenericData.Fixed) eventBean.get("aFixed");
            assertTrue(Arrays.equals(fixed.bytes(), new byte[]{1, 2}));
        };

        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("aFixed", new GenericData.Fixed(schema.getField("aFixed").schema(), new byte[]{1, 2}));
        asserter.accept(new AvroGenericDataEventBean(datum, eventType));

        String jsonWValues = "{'aFixed': '\\u0001\\u0002\'}";
        datum = SupportAvroUtil.parseQuoted(schema, jsonWValues);
        asserter.accept(new AvroGenericDataEventBean(datum, eventType));
    }

    public void testEnumSymbol() {
        Schema schema = record("typename").fields()
                .name("aEnum").type().enumeration("myEnum").symbols("a", "b").enumDefault("x")
                .endRecord();
        EventType eventType = makeAvroSupportEventType(schema);

        assertPropertyType(GenericEnumSymbol.class, null, eventType, "aEnum");

        Consumer<EventBean> asserter = eventBean -> {
            GenericEnumSymbol v = (GenericEnumSymbol) eventBean.get("aEnum");
            assertEquals("b", v.toString());
        };

        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("aEnum", new GenericData.EnumSymbol(schema.getField("aEnum").schema(), "b"));
        asserter.accept(new AvroGenericDataEventBean(datum, eventType));

        String jsonWValues = "{'aEnum': 'b'}";
        datum = SupportAvroUtil.parseQuoted(schema, jsonWValues);
        asserter.accept(new AvroGenericDataEventBean(datum, eventType));
    }

    public void testUnionResultingInObject() {
        Schema schema = record("typename").fields()
                .name("anUnion").type().unionOf()
                .intBuilder().endInt()
                .and()
                .stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString()
                .and()
                .nullType()
                .endUnion().noDefault()
                .endRecord();
        EventType eventType = makeAvroSupportEventType(schema);

        assertPropertyType(Object.class, null, eventType, "anUnion");

        Consumer<Object> asserterFromDatum = (value) -> {
            GenericData.Record datum = new GenericData.Record(schema);
            datum.put("anUnion", value);
            assertEquals(value, new AvroGenericDataEventBean(datum, eventType).get("anUnion"));
        };
        asserterFromDatum.accept("a");
        asserterFromDatum.accept(1);
        asserterFromDatum.accept(null);

        BiConsumer<String, Object> asserterFromJson = (json, value) -> {
            GenericData.Record datum = SupportAvroUtil.parseQuoted(schema, json);
            assertEquals(value, new AvroGenericDataEventBean(datum, eventType).get("anUnion"));
        };
        asserterFromJson.accept("{'anUnion':{'int':1}}", 1);
        asserterFromJson.accept("{'anUnion':{'string':'abc'}}", "abc");
        asserterFromJson.accept("{'anUnion':null}", null);
    }

    public void testUnionResultingInNumber() {
        Schema schema = record("typename").fields()
                .name("anUnion").type().unionOf()
                .intBuilder().endInt()
                .and()
                .floatBuilder().endFloat()
                .endUnion().noDefault()
                .endRecord();
        EventType eventType = makeAvroSupportEventType(schema);

        assertPropertyType(Number.class, null, eventType, "anUnion");

        Consumer<Object> asserterFromDatum = (value) -> {
            GenericData.Record datum = new GenericData.Record(schema);
            datum.put("anUnion", value);
            assertEquals(value, new AvroGenericDataEventBean(datum, eventType).get("anUnion"));
        };
        asserterFromDatum.accept(1);
        asserterFromDatum.accept(2f);

        BiConsumer<String, Object> asserterFromJson = (json, value) -> {
            GenericData.Record datum = SupportAvroUtil.parseQuoted(schema, json);
            assertEquals(value, new AvroGenericDataEventBean(datum, eventType).get("anUnion"));
        };
        asserterFromJson.accept("{'anUnion':{'int':1}}", 1);
        asserterFromJson.accept("{'anUnion':{'float':2}}", 2f);
    }

    private void assertValuesNested(GenericData.Record datum, AvroGenericDataEventBean bean) {
        assertEquals("i1", bean.get("innerEvent.innerValue"));
        assertEquals("i1", bean.getEventType().getGetter("innerEvent.innerValue").get(bean));

        assertSame(datum.get("innerEvent"), bean.get("innerEvent"));
        assertSame(datum.get("innerEvent"), bean.getEventType().getGetter("innerEvent").get(bean));
    }

    private void runAssertionNullableOrOptTypes(Schema schema) {
        EventType eventType = makeAvroSupportEventType(schema);

        assertTypesBoxed(eventType);

        GenericData.Record datum = getRecordWithValues(schema);
        assertValuesRequired(new AvroGenericDataEventBean(datum, eventType));

        String jsonWValues = "{'myInt': {'int': 10}, 'myCharSeq': {'string': 'x'}, 'myString':{'string': 'y'}," +
                "'myBoolean': {'boolean': true}, 'myBytes': {'bytes': '\\u00AA\'}, " +
                "'myDouble': {'double': 50}, 'myFloat': {'float': 100}, 'myLong': {'long': 20}}";
        assertValuesRequired(new AvroGenericDataEventBean(SupportAvroUtil.parseQuoted(schema, jsonWValues), eventType));

        String jsonWNull = "{'myInt': null, 'myCharSeq': null, 'myString':null," +
                "'myBoolean': null, 'myBytes': null, " +
                "'myDouble': null, 'myFloat': null, 'myLong': null}";
        assertValuesNull(new AvroGenericDataEventBean(SupportAvroUtil.parseQuoted(schema, jsonWNull), eventType));
    }

    private void assertValuesRequired(AvroGenericDataEventBean bean) {
        assertValue(10, bean, "myInt");
        assertValue(new Utf8("x"), bean, "myCharSeq");
        assertValue("y", bean, "myString");
        assertValue(true, bean, "myBoolean");
        assertValue(ByteBuffer.wrap(new byte[]{(byte) 170}), bean, "myBytes");
        assertValue(50d, bean, "myDouble");
        assertValue(100f, bean, "myFloat");
        assertValue(20L, bean, "myLong");
    }

    private void assertValuesNull(AvroGenericDataEventBean bean) {
        assertValue(null, bean, "myInt");
        assertValue(null, bean, "myCharSeq");
        assertValue(null, bean, "myString");
        assertValue(null, bean, "myBoolean");
        assertValue(null, bean, "myBytes");
        assertValue(null, bean, "myDouble");
        assertValue(null, bean, "myFloat");
        assertValue(null, bean, "myLong");
    }

    private void assertValue(Object expected, AvroGenericDataEventBean bean, String propertyName) {
        if (expected instanceof ByteBuffer) {
            assertEqualsByteBuf((ByteBuffer) expected, (ByteBuffer) bean.get(propertyName));
            EventPropertyGetter getter = bean.getEventType().getGetter(propertyName);
            assertEqualsByteBuf((ByteBuffer) expected, (ByteBuffer) getter.get(bean));
        } else {
            assertEquals(expected, bean.get(propertyName));
            EventPropertyGetter getter = bean.getEventType().getGetter(propertyName);
            assertEquals(expected, getter.get(bean));
        }
    }

    private void assertPropertyType(Class expectedType, Class expectedComponentType, EventType eventType, String propertyName) {
        assertEquals(expectedType, eventType.getPropertyType(propertyName));
        assertTrue(eventType.isProperty(propertyName));

        if (!propertyName.contains(".")) {
            EventPropertyDescriptor descriptor = eventType.getPropertyDescriptor(propertyName);
            assertEquals(expectedType, descriptor.getPropertyType());
            assertEquals(expectedComponentType, descriptor.getPropertyComponentType());
        }
    }

    private void assertEqualsByteBuf(ByteBuffer expected, ByteBuffer received) {
        assertTrue(Arrays.equals(expected.array(), received.array()));
    }

    private GenericData.Record getRecordWithValues(Schema schema) {
        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("myInt", 10);
        datum.put("myCharSeq", new Utf8("x"));
        datum.put("myString", "y");
        datum.put("myBoolean", true);
        datum.put("myBytes", ByteBuffer.wrap(new byte[]{(byte) 170}));
        datum.put("myDouble", 50d);
        datum.put("myFloat", 100f);
        datum.put("myLong", 20L);
        return datum;
    }

    private void assertTypesBoxed(EventType eventType) {
        assertPropertyType(Integer.class, null, eventType, "myInt");
        assertPropertyType(CharSequence.class, null, eventType, "myCharSeq");
        assertPropertyType(String.class, null, eventType, "myString");
        assertPropertyType(Boolean.class, null, eventType, "myBoolean");
        assertPropertyType(ByteBuffer.class, null, eventType, "myBytes");
        assertPropertyType(Double.class, null, eventType, "myDouble");
        assertPropertyType(Float.class, null, eventType, "myFloat");
        assertPropertyType(Long.class, null, eventType, "myLong");
    }

    private void assertNotAProperty(EventType type, String propertyName) {
        assertFalse(type.isProperty(propertyName));
        assertNull(type.getPropertyType(propertyName));
        assertNull(type.getGetter(propertyName));
        assertNull(type.getPropertyDescriptor(propertyName));
    }
}
