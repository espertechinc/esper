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
package com.espertech.esper.regressionlib.suite.event.json;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class EventJsonParserLaxness {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonParserMalformedJson());
        execs.add(new EventJsonParserLaxnessStringType());
        execs.add(new EventJsonParserLaxnessNumberType());
        execs.add(new EventJsonParserLaxnessBooleanType());
        execs.add(new EventJsonParserLaxnessObjectType());
        execs.add(new EventJsonParserUndeclaredContent());
        return execs;
    }

    private static class EventJsonParserUndeclaredContent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent ();\n" +
                "@name('s0') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            String json = "{\n" +
                "  \"users\": [\n" +
                "    {\n" +
                "      \"_id\": \"45166552176594981065\",\n" +
                "      \"longitude\": 110.5363758848371,\n" +
                "      \"tags\": [\n" +
                "        \"ezNI8Gx5vq\"\n" +
                "      ],\n" +
                "      \"friends\": [\n" +
                "        {\n" +
                "          \"id\": \"4673\",\n" +
                "          \"name\": \"EqVIiZyuhSCkWXvqSxgyQihZaiwSra\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"greeting\": \"xfS8vUXYq4wzufBLP6CY\",\n" +
                "      \"favoriteFruit\": \"KT0tVAxXRawtbeQIWAot\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"_id\": \"23504426278646846580\",\n" +
                "      \"favoriteFruit\": \"9aUx0u6G840i0EeKFM4Z\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

            env.sendEventJson(json, "JsonEvent");

            env.undeployAll();
        }
    }

    private static class EventJsonParserLaxnessObjectType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (carray Object[], cobject Map);\n" +
                "@name('s0') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssert(env, new JsonObject().add("carray", new JsonObject()).toString(), event -> assertNull(event.get("carray")));
            sendAssert(env, new JsonObject().add("carray", "abc").toString(), event -> assertNull(event.get("carray")));
            sendAssert(env, new JsonObject().add("cobject", new JsonArray()).toString(), event -> assertNull(event.get("cobject")));
            sendAssert(env, new JsonObject().add("cobject", "abc").toString(), event -> assertNull(event.get("cobject")));

            env.undeployAll();
        }
    }

    private static class EventJsonParserLaxnessBooleanType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (" +
                "cbool boolean, cboola1 boolean[], cboola2 boolean[][]);\n" +
                "@name('s0') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            sendAssert(env, new JsonObject().add("cbool", "true").toString(), event -> assertTrue((Boolean) event.get("cbool")));
            sendAssert(env, new JsonObject().add("cbool", "false").toString(), event -> assertFalse((Boolean) event.get("cbool")));
            sendAssert(env, new JsonObject().add("cboola1", new JsonArray().add("true")).toString(), event -> assertTrue((Boolean) ((Object[]) event.get("cboola1"))[0]));
            sendAssert(env, new JsonObject().add("cboola2", new JsonArray().add(new JsonArray().add("true"))).toString(), event -> assertTrue((Boolean) ((Object[][]) event.get("cboola2"))[0][0]));
            sendAssert(env, new JsonObject().add("cbool", new JsonObject()).toString(), event -> assertNull(event.get("cbool")));
            sendAssert(env, new JsonObject().add("cbool", new JsonArray()).toString(), event -> assertNull(event.get("cbool")));

            tryInvalid(env, new JsonObject().add("cbool", "x").toString(), "Failed to parse json member name 'cbool' as a boolean-type from value 'x'");
            tryInvalid(env, new JsonObject().add("cboola1", new JsonArray().add("x")).toString(), "Failed to parse json member name 'cboola1' as a boolean-type from value 'x'");
            tryInvalid(env, new JsonObject().add("cboola2", new JsonArray().add(new JsonArray().add("x"))).toString(), "Failed to parse json member name 'cboola2' as a boolean-type from value 'x'");
            tryInvalid(env, new JsonObject().add("cbool", "null").toString(), "Failed to parse json member name 'cbool' as a boolean-type from value 'null'");

            env.undeployAll();
        }
    }

    private static class EventJsonParserLaxnessNumberType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent (" +
                "cbyte byte, cshort short, cint int, clong long, cdouble double, cfloat float, cbigint biginteger, cbigdec bigdecimal," +
                "cbytea1 byte[], cshorta1 short[], cinta1 int[], clonga1 long[], cdoublea1 double[], cfloata1 float[], cbiginta1 biginteger[], cbigdeca1 bigdecimal[]," +
                "cbytea2 byte[][], cshorta2 short[][], cinta2 int[][], clonga2 long[][], cdoublea2 double[][], cfloata2 float[][], cbiginta2 biginteger[][], cbigdeca2 bigdecimal[][]);\n" +
                "@name('s0') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0");
            EventType eventType = env.runtime().getEventTypeService().getEventType(env.deploymentId("s0"), "JsonEvent");

            // lax parsing is the default, allowing string values
            for (String propertyName : eventType.getPropertyNames()) {
                EventBean event = makeSendJson(env, propertyName, "1");
                Object value = event.get(propertyName);
                assertNotNull("Null for property " + propertyName, value);
                if (propertyName.endsWith("a2")) {
                    assertAsNumber(propertyName, 1, ((Object[][]) value)[0][0]);
                } else if (propertyName.endsWith("a1")) {
                    assertAsNumber(propertyName, 1, ((Object[]) value)[0]);
                } else {
                    assertAsNumber(propertyName, 1, value);
                }
            }

            // invalid number
            for (String propertyName : eventType.getPropertyNames()) {
                try {
                    makeSendJson(env, propertyName, "x");
                    fail();
                } catch (EPException ex) {
                    String typeName = propertyName.substring(1).replace("a1", "").replace("a2", "");
                    Class type;
                    if (typeName.equals("bigint")) {
                        type = BigInteger.class;
                    } else if (typeName.equals("bigdec")) {
                        type = BigDecimal.class;
                    } else {
                        type = JavaClassHelper.getBoxedType(JavaClassHelper.getPrimitiveClassForName(typeName));
                        assertNotNull("Unrecognized type " + typeName, type);
                    }
                    String expected = "Failed to parse json member name '" + propertyName + "' as a " + type.getSimpleName() + "-type from value 'x': NumberFormatException";
                    assertTrue(ex.getMessage().startsWith(expected));
                }
            }

            // unexpected object type
            sendAssert(env, new JsonObject().add("cint", new JsonObject()).toString(), event -> assertNull(event.get("cint")));
            sendAssert(env, new JsonObject().add("cint", new JsonArray()).toString(), event -> assertNull(event.get("cint")));

            env.undeployAll();
        }

        private EventBean makeSendJson(RegressionEnvironment env, String propertyName, String value) {
            JsonObject json = new JsonObject();
            if (propertyName.endsWith("a2")) {
                json.add(propertyName, new JsonArray().add(new JsonArray().add(value)));
            } else if (propertyName.endsWith("a1")) {
                json.add(propertyName, new JsonArray().add(value));
            } else {
                json.add(propertyName, value);
            }
            env.sendEventJson(json.toString(), "JsonEvent");
            return env.listener("s0").assertOneGetNewAndReset();
        }
    }

    private static class EventJsonParserLaxnessStringType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype @JsonSchema create json schema JsonEvent(p1 string);\n" +
                "@name('s0') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            // lax parsing is the default
            sendAssertP1(env, "{ \"p1\" : 1 }", "1");
            sendAssertP1(env, "{ \"p1\" : 1.1234 }", "1.1234");
            sendAssertP1(env, "{ \"p1\" : true }", "true");
            sendAssertP1(env, "{ \"p1\" : false }", "false");
            sendAssertP1(env, "{ \"p1\" : null }", null);
            sendAssertP1(env, "{ \"p1\" : [\"abc\"] }", null);
            sendAssertP1(env, "{ \"p1\" : {\"abc\": \"def\"} }", null);

            env.undeployAll();
        }
    }

    private static class EventJsonParserMalformedJson implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype @JsonSchema create json schema JsonEvent(p1 string);\n" +
                "@name('s0') select * from JsonEvent;\n";
            env.compileDeploy(epl).addListener("s0");

            tryInvalid(env, "", "Failed to parse Json: Unexpected end of input at 1:1");
            tryInvalid(env, "{}{}", "Failed to parse Json: Unexpected character at 1:3");
            tryInvalid(env, "{{}", "Failed to parse Json: Expected name at 1:2");

            env.undeployAll();
        }
    }

    private static void sendAssertP1(RegressionEnvironment env, String json, Object expected) {
        sendAssert(env, json, event -> assertEquals(expected, event.get("p1")));
    }

    private static void sendAssert(RegressionEnvironment env, String json, Consumer<EventBean> assertion) {
        env.sendEventJson(json, "JsonEvent");
        assertion.accept(env.listener("s0").assertOneGetNewAndReset());
    }

    private static void tryInvalid(RegressionEnvironment env, String json, String message) {
        try {
            env.runtime().getEventService().sendEventJson(json, "JsonEvent");
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(message, ex.getMessage());
        }
    }

    private static void assertAsNumber(String propertyName, Number expected, Object actualNumber) {
        Number actual = (Number) actualNumber;
        if (propertyName.contains("byte")) {
            assertEquals(expected.byteValue(), actual.byteValue());
        } else if (propertyName.contains("short")) {
            assertEquals(expected.shortValue(), actual.shortValue());
        } else if (propertyName.contains("int")) {
            assertEquals(expected.intValue(), actual.intValue());
        } else if (propertyName.contains("long")) {
            assertEquals(expected.longValue(), actual.longValue());
        } else if (propertyName.contains("double")) {
            assertEquals(expected.doubleValue(), actual.doubleValue(), 0.1);
        } else if (propertyName.contains("float")) {
            assertEquals(expected.floatValue(), actual.floatValue(), 0.1);
        } else if (propertyName.contains("bigint")) {
            assertEquals(expected.toString(), actual.toString());
        } else if (propertyName.contains("bigdec")) {
            assertEquals(expected.toString(), actual.toString());
        } else {
            fail("Not recognized '" + propertyName + "'");
        }
    }
}
