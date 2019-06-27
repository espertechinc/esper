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
package com.espertech.esper.regressionlib.suite.event.infra;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportBeanSimple;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class EventInfraPropertyUnderlyingSimple implements RegressionExecution {

    public final static String XML_TYPENAME = EventInfraPropertyUnderlyingSimple.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyUnderlyingSimple.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyUnderlyingSimple.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyUnderlyingSimple.class.getSimpleName() + "Avro";
    public final static String JSON_TYPENAME = EventInfraPropertyUnderlyingSimple.class.getSimpleName() + "Json";
    public final static String JSONPROVIDEDBEAN_TYPENAME = EventInfraPropertyUnderlyingSimple.class.getSimpleName() + "JsonWProvided";

    private static final Logger log = LoggerFactory.getLogger(EventInfraPropertyUnderlyingSimple.class);

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        String eplJson =
            "@public @buseventtype @name('schema') create json schema " + JSON_TYPENAME + "(myInt int, myString string);\n" +
                "@public @buseventtype @name('schema') @JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') create json schema " + JSONPROVIDEDBEAN_TYPENAME + "();\n";
        env.compileDeploy(eplJson, path);

        Pair<String, FunctionSendEventIntString>[] pairs = new Pair[]{
            new Pair<>(MAP_TYPENAME, FMAP),
            new Pair<>(OA_TYPENAME, FOA),
            new Pair<>(BEAN_TYPENAME, FBEAN),
            new Pair<>(XML_TYPENAME, FXML),
            new Pair<>(AVRO_TYPENAME, FAVRO),
            new Pair<>(JSON_TYPENAME, FJSON),
            new Pair<>(JSONPROVIDEDBEAN_TYPENAME, FJSON)
        };

        for (Pair<String, FunctionSendEventIntString> pair : pairs) {
            log.info("Asserting type " + pair.getFirst());
            runAssertionPassUnderlying(env, pair.getFirst(), pair.getSecond(), path);
            runAssertionPropertiesWGetter(env, pair.getFirst(), pair.getSecond(), path);
            runAssertionTypeValidProp(env, pair.getFirst(), pair.getSecond() != FBEAN);
            runAssertionTypeInvalidProp(env, pair.getFirst(), pair.getSecond() == FXML);
        }

        env.undeployAll();
    }

    private void runAssertionPassUnderlying(RegressionEnvironment env, String typename, FunctionSendEventIntString send, RegressionPath path) {
        String epl = "@name('s0') select * from " + typename;
        env.compileDeploy(epl, path).addListener("s0");

        String[] fields = "myInt,myString".split(",");

        assertEquals(Integer.class, JavaClassHelper.getBoxedType(env.statement("s0").getEventType().getPropertyType("myInt")));
        assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("myString"));

        Object eventOne = send.apply(typename, env, 3, "some string");

        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        SupportEventTypeAssertionUtil.assertConsistency(event);
        assertUnderlying(typename, eventOne, event.getUnderlying());
        EPAssertionUtil.assertProps(event, fields, new Object[]{3, "some string"});

        Object eventTwo = send.apply(typename, env, 4, "other string");
        event = env.listener("s0").assertOneGetNewAndReset();
        assertUnderlying(typename, eventTwo, event.getUnderlying());
        EPAssertionUtil.assertProps(event, fields, new Object[]{4, "other string"});

        env.undeployModuleContaining("s0");
    }

    private void assertUnderlying(String typename, Object expected, Object received) {
        if (typename.equals(JSONPROVIDEDBEAN_TYPENAME)) {
            assertTrue(received instanceof MyLocalJsonProvided);
        } else if (typename.equals(JSON_TYPENAME)) {
            assertEquals(expected, received.toString());
        } else {
            assertEquals(expected, received);
        }
    }

    private void runAssertionPropertiesWGetter(RegressionEnvironment env, String typename, FunctionSendEventIntString send, RegressionPath path) {
        String epl = "@name('s0') select myInt, exists(myInt) as exists_myInt, myString, exists(myString) as exists_myString from " + typename;
        env.compileDeploy(epl, path).addListener("s0");

        String[] fields = "myInt,exists_myInt,myString,exists_myString".split(",");

        EventType eventType = env.statement("s0").getEventType();
        assertEquals(Integer.class, JavaClassHelper.getBoxedType(eventType.getPropertyType("myInt")));
        assertEquals(String.class, eventType.getPropertyType("myString"));
        assertEquals(Boolean.class, eventType.getPropertyType("exists_myInt"));
        assertEquals(Boolean.class, eventType.getPropertyType("exists_myString"));

        send.apply(typename, env, 3, "some string");

        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        runAssertionEventInvalidProp(event);
        EPAssertionUtil.assertProps(event, fields, new Object[]{3, true, "some string", true});

        send.apply(typename, env, 4, "other string");
        event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[]{4, true, "other string", true});

        env.undeployModuleContaining("s0");
    }

    private void runAssertionEventInvalidProp(EventBean event) {
        for (String prop : Arrays.asList("xxxx", "myString[1]", "myString('a')", "x.y", "myString.x")) {
            SupportMessageAssertUtil.tryInvalidProperty(event, prop);
            SupportMessageAssertUtil.tryInvalidGetFragment(event, prop);
        }
    }

    private void runAssertionTypeValidProp(RegressionEnvironment env, String typeName, boolean boxed) {
        EventType eventType = !typeName.equals(JSON_TYPENAME) ?
            env.runtime().getEventTypeService().getEventTypePreconfigured(typeName) :
            env.runtime().getEventTypeService().getEventType(env.deploymentId("schema"), typeName);

        Object[][] expectedType = new Object[][]{{"myInt", boxed ? Integer.class : int.class, null, null}, {"myString", String.class, null, null}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, eventType, SupportEventTypeAssertionEnum.getSetWithFragment());

        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"myString", "myInt"}, eventType.getPropertyNames());

        assertNotNull(eventType.getGetter("myInt"));
        assertTrue(eventType.isProperty("myInt"));
        assertEquals(boxed ? Integer.class : int.class, eventType.getPropertyType("myInt"));
        assertEquals(new EventPropertyDescriptor("myString", String.class, null, false, false, false, false, false), eventType.getPropertyDescriptor("myString"));
    }

    private void runAssertionTypeInvalidProp(RegressionEnvironment env, String typeName, boolean xml) {
        EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured(typeName);

        for (String prop : Arrays.asList("xxxx", "myString[0]", "myString('a')", "myString.x", "myString.x.y", "myString.x")) {
            assertEquals(false, eventType.isProperty(prop));
            Class expected = null;
            if (xml) {
                if (prop.equals("myString[0]")) {
                    expected = String.class;
                }
                if (prop.equals("myString.x?")) {
                    expected = Node.class;
                }
            }
            assertEquals(expected, eventType.getPropertyType(prop));
            assertNull(eventType.getPropertyDescriptor(prop));
            assertNull(eventType.getFragmentType(prop));
        }
    }

    @FunctionalInterface
    interface FunctionSendEventIntString {
        Object apply(String eventTypeName, RegressionEnvironment env, Integer intValue, String stringValue);
    }

    private final static String BEAN_TYPENAME = SupportBeanSimple.class.getSimpleName();

    private static final FunctionSendEventIntString FMAP = (eventTypeName, env, a, b) -> {
        Map<String, Object> map = new HashMap<>();
        map.put("myInt", a);
        map.put("myString", b);
        env.sendEventMap(map, eventTypeName);
        return map;
    };

    private static final FunctionSendEventIntString FOA = (eventTypeName, env, a, b) -> {
        Object[] oa = new Object[]{a, b};
        env.sendEventObjectArray(oa, eventTypeName);
        return oa;
    };

    private static final FunctionSendEventIntString FBEAN = (eventTypeName, env, a, b) -> {
        SupportBeanSimple bean = new SupportBeanSimple(b, a);
        env.sendEventBean(bean);
        return bean;
    };

    private static final FunctionSendEventIntString FXML = (eventTypeName, env, a, b) -> {
        String xml = "<myevent myInt=\"XXXXXX\" myString=\"YYYYYY\">\n" +
            "</myevent>\n";
        xml = xml.replace("XXXXXX", a.toString());
        xml = xml.replace("YYYYYY", b);
        try {
            Document d = SupportXML.sendXMLEvent(env, xml, eventTypeName);
            return d.getDocumentElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    private static final FunctionSendEventIntString FAVRO = (eventTypeName, env, a, b) -> {
        Schema avroSchema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        GenericData.Record datum = new GenericData.Record(avroSchema);
        datum.put("myInt", a);
        datum.put("myString", b);
        env.sendEventAvro(datum, eventTypeName);
        return datum;
    };

    private static final FunctionSendEventIntString FJSON = (eventTypeName, env, a, b) -> {
        JsonObject object = new JsonObject();
        object.add("myInt", a);
        object.add("myString", b);
        String json = object.toString();
        env.sendEventJson(json, eventTypeName);
        return json;
    };

    public static class MyLocalJsonProvided implements Serializable {
        public Integer myInt;
        public String myString;
    }
}
