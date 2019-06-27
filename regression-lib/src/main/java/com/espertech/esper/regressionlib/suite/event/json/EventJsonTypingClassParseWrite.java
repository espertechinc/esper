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
import com.espertech.esper.common.client.json.minimaljson.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.assertMessage;
import static org.junit.Assert.*;

public class EventJsonTypingClassParseWrite {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventJsonTypingClassSimple());
        execs.add(new EventJsonTypingListBuiltinType());
        execs.add(new EventJsonTypingListEnumType());
        execs.add(new EventJsonTypingVMClass());
        execs.add(new EventJsonTypingClassWArrayAndColl());
        execs.add(new EventJsonTypingNestedRecursive());
        return execs;
    }

    private static class EventJsonTypingNestedRecursive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent(local " + MyLocalEventNestedRecursive.class.getName() + ");\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            JsonObject depthTwo = new JsonObject().add("local", makeNested("a,b"));
            env.sendEventJson(depthTwo.toString(), "JsonEvent");
            assertDepthTwo(env, env.listener("s0").assertOneGetNewAndReset(), depthTwo);

            JsonObject depthThree = new JsonObject().add("local", makeNested("a,b,c"));
            env.sendEventJson(depthThree.toString(), "JsonEvent");
            assertDepthThree(env, env.listener("s0").assertOneGetNewAndReset(), depthThree);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertDepthTwo(env, it.next(), depthTwo);
            assertDepthThree(env, it.next(), depthThree);

            env.undeployAll();
        }

        private void assertDepthTwo(RegressionEnvironment env, EventBean event, JsonObject json) {
            MyLocalEventNestedRecursive result = (MyLocalEventNestedRecursive) event.get("local");
            assertEquals("a", result.id);
            assertEquals("b", result.child.id);
            String rendered = env.runtime().getRenderEventService().getJSONRenderer(event.getEventType()).render(event);
            assertEquals(json.toString(), rendered);
        }

        private void assertDepthThree(RegressionEnvironment env, EventBean event, JsonObject json) {
            MyLocalEventNestedRecursive result = (MyLocalEventNestedRecursive) event.get("local");
            assertEquals("a", result.id);
            assertEquals("b", result.child.id);
            assertEquals("c", result.child.child.id);
            String rendered = env.runtime().getRenderEventService().getJSONRenderer(event.getEventType()).render(event);
            assertEquals(json.toString(), rendered);
        }

        private JsonObject makeNested(String csv) {
            String[] split = csv.split(",");
            if (split.length == 0) {
                return new JsonObject();
            }
            JsonObject parent = new JsonObject().add("id", split[0]);
            JsonObject current = parent;
            for (int i = 1; i < split.length; i++) {
                JsonObject child = new JsonObject().add("id", split[i]);
                current.add("child", child);
                current = child;
            }
            current.add("child", Json.NULL);
            return parent;
        }
    }

    private static class EventJsonTypingClassWArrayAndColl implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent(local " + MyLocalEventWArrayColl.class.getName() + ");\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            JsonObject localFilled = new JsonObject()
                .add("c0", makeJson("E1", 1))
                .add("c0Arr", new JsonArray().add(makeJson("E2", 2)))
                .add("c0Arr2Dim", new JsonArray().add(new JsonArray().add(makeJson("E3", 3))))
                .add("c0Coll", new JsonArray().add(makeJson("E4", 4)));
            JsonObject jsonFilled = new JsonObject().add("local", localFilled);
            env.sendEventJson(jsonFilled.toString(), "JsonEvent");
            assertFilled(env, env.listener("s0").assertOneGetNewAndReset(), jsonFilled);

            JsonObject localNull = new JsonObject()
                .add("c0", Json.NULL)
                .add("c0Arr", new JsonArray().add(Json.NULL))
                .add("c0Arr2Dim", new JsonArray().add(new JsonArray().add(Json.NULL)))
                .add("c0Coll", new JsonArray().add(Json.NULL));
            JsonObject jsonNulled = new JsonObject().add("local", localNull);
            env.sendEventJson(jsonNulled.toString(), "JsonEvent");
            assertNulled(env, env.listener("s0").assertOneGetNewAndReset(), jsonNulled);

            JsonObject localHalfFilled = new JsonObject()
                .add("c0", makeJson("E1", 1))
                .add("c0Arr", new JsonArray().add(Json.NULL).add(makeJson("E2", 2)))
                .add("c0Arr2Dim", new JsonArray().add(Json.NULL).add(new JsonArray().add(makeJson("E3", 3)).add(Json.NULL)).add(Json.NULL))
                .add("c0Coll", new JsonArray().add(makeJson("E4", 4)).add(Json.NULL));
            JsonObject jsonHalfFilled = new JsonObject().add("local", localHalfFilled);
            env.sendEventJson(jsonHalfFilled.toString(), "JsonEvent");
            assertHalfFilled(env, env.listener("s0").assertOneGetNewAndReset(), jsonHalfFilled);

            JsonObject localEmpty = new JsonObject()
                .add("c0", Json.NULL)
                .add("c0Arr", new JsonArray())
                .add("c0Arr2Dim", new JsonArray())
                .add("c0Coll", new JsonArray());
            JsonObject jsonEmpty = new JsonObject().add("local", localEmpty);
            env.sendEventJson(jsonEmpty.toString(), "JsonEvent");
            assertEmpty(env, env.listener("s0").assertOneGetNewAndReset(), jsonEmpty);

            JsonObject localFilledMultiple = new JsonObject()
                .add("c0", makeJson("E1", 1))
                .add("c0Arr", new JsonArray().add(makeJson("E2", 10)).add(makeJson("E2", 11)).add(makeJson("E2", 12)))
                .add("c0Arr2Dim", new JsonArray().add(new JsonArray().add(makeJson("E3", 30)).add(makeJson("E3", 31))).add(new JsonArray().add(makeJson("E3", 32)).add(makeJson("E3", 33))))
                .add("c0Coll", new JsonArray().add(makeJson("E4", 40)).add(makeJson("E4", 41)));
            JsonObject jsonFilledMultiple = new JsonObject().add("local", localFilledMultiple);
            env.sendEventJson(jsonFilledMultiple.toString(), "JsonEvent");
            assertFilledMultiple(env, env.listener("s0").assertOneGetNewAndReset(), jsonFilledMultiple);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFilled(env, it.next(), jsonFilled);
            assertNulled(env, it.next(), jsonNulled);
            assertHalfFilled(env, it.next(), jsonHalfFilled);
            assertEmpty(env, it.next(), jsonEmpty);
            assertFilledMultiple(env, it.next(), jsonFilledMultiple);

            env.undeployAll();
        }

        private void assertEmpty(RegressionEnvironment env, EventBean event, JsonObject json) {
            assertEvent(env, event, json, null, new Object[0], new Object[0][], new Object[0]);
        }

        private void assertNulled(RegressionEnvironment env, EventBean event, JsonObject json) {
            assertEvent(env, event, json, null, new Object[]{null}, new Object[][]{{null}}, new Object[]{null});
        }

        private void assertFilled(RegressionEnvironment env, EventBean event, JsonObject json) {
            assertEvent(env, event, json,
                makeLocal("E1", 1), new Object[]{makeLocal("E2", 2)}, new Object[][]{{makeLocal("E3", 3)}}, new Object[]{makeLocal("E4", 4)});
        }

        private void assertFilledMultiple(RegressionEnvironment env, EventBean event, JsonObject json) {
            assertEvent(env, event, json,
                makeLocal("E1", 1),
                new Object[]{makeLocal("E2", 10), makeLocal("E2", 11), makeLocal("E2", 12)},
                new Object[][]{{makeLocal("E3", 30), makeLocal("E3", 31)}, {makeLocal("E3", 32), makeLocal("E3", 33)}},
                new Object[]{makeLocal("E4", 40), makeLocal("E4", 41)});
        }

        private void assertHalfFilled(RegressionEnvironment env, EventBean event, JsonObject json) {
            assertEvent(env, event, json,
                makeLocal("E1", 1), new Object[]{null, makeLocal("E2", 2)}, new Object[][]{null, {makeLocal("E3", 3), null}, null}, new Object[]{makeLocal("E4", 4), null});
        }

        private void assertEvent(RegressionEnvironment env, EventBean event, JsonObject json, Object c0, Object[] c0Arr, Object[][] c0Arr2Dim, Object[] c0Coll) {
            MyLocalEventWArrayColl result = (MyLocalEventWArrayColl) event.get("local");
            assertEquals(c0, result.c0);
            EPAssertionUtil.assertEqualsExactOrder(c0Arr, result.c0Arr);
            EPAssertionUtil.assertEqualsExactOrder(c0Arr2Dim, result.c0Arr2Dim);
            EPAssertionUtil.assertEqualsExactOrder(c0Coll, result.c0Coll.toArray());
            String rendered = env.runtime().getRenderEventService().getJSONRenderer(event.getEventType()).render(event);
            assertEquals(json.toString(), rendered);
        }

        private JsonObject makeJson(String theString, int intPrimitive) {
            return new JsonObject().add("theString", theString).add("intPrimitive", intPrimitive);
        }

        private MyLocalEvent makeLocal(String theString, int intPrimitive) {
            return new MyLocalEvent(theString, intPrimitive);
        }
    }

    private static class EventJsonTypingVMClass implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            AtomicInteger milestone = new AtomicInteger();
            JsonValue valueOfOne = new JsonNumber("1");

            UUID uuid = UUID.fromString("b7dc7f66-4f6d-4f03-14d7-83da210dfba6");
            runAssertion(env, MyLocalVMTypeUUID.class, UUID.class, uuid.toString(), uuid, valueOfOne, milestone);

            OffsetDateTime odt = OffsetDateTime.now();
            runAssertion(env, MyLocalVMTypeOffsetDateTime.class, OffsetDateTime.class, odt.toString(), odt, valueOfOne, milestone);

            LocalDate ld = LocalDate.now();
            runAssertion(env, MyLocalVMTypeLocalDate.class, LocalDate.class, ld.toString(), ld, valueOfOne, milestone);

            LocalDateTime ldt = LocalDateTime.now();
            runAssertion(env, MyLocalVMTypeLocalDateTime.class, LocalDateTime.class, ldt.toString(), ldt, valueOfOne, milestone);

            ZonedDateTime zdt = ZonedDateTime.now();
            runAssertion(env, MyLocalVMTypeZonedDateTime.class, ZonedDateTime.class, zdt.toString(), zdt, valueOfOne, milestone);

            URL url;
            try {
                url = new URL("http", "host", 1000, "/file");
                assertEquals(url, new URL(url.toString()));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            runAssertion(env, MyLocalVMTypeURL.class, URL.class, url.toString(), url, valueOfOne, milestone);

            URI uri;
            try {
                uri = new URI("ftp://ftp.is.co.za/rfc/rfc1808.txt");
                assertEquals(uri, new URI(uri.toString()));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            runAssertion(env, MyLocalVMTypeURI.class, URI.class, uri.toString(), uri, new JsonString("a b"), milestone);
        }

        private void runAssertion(RegressionEnvironment env, Class localType, Class fieldType, String jsonText, Object expected, JsonValue invalidJson, AtomicInteger milestone) {
            String epl = "@public @buseventtype create json schema JsonEvent(local " + localType.getName() + ");\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            JsonObject localFilled = new JsonObject()
                .add("c0", jsonText)
                .add("c0Arr", new JsonArray().add(jsonText))
                .add("c0Arr2Dim", new JsonArray().add(new JsonArray().add(jsonText)))
                .add("c0Coll", new JsonArray().add(jsonText));
            JsonObject jsonFilledObject = new JsonObject().add("local", localFilled);
            String jsonFilled = jsonFilledObject.toString();
            env.sendEventJson(jsonFilled, "JsonEvent");
            assertEvent(env, env.listener("s0").assertOneGetNewAndReset(), jsonFilled, expected);

            JsonObject localNull = new JsonObject()
                .add("c0", Json.NULL)
                .add("c0Arr", new JsonArray().add(Json.NULL))
                .add("c0Arr2Dim", new JsonArray().add(new JsonArray().add(Json.NULL)))
                .add("c0Coll", new JsonArray().add(Json.NULL));
            JsonObject jsonNullObject = new JsonObject().add("local", localNull);
            String jsonNull = jsonNullObject.toString();
            env.sendEventJson(jsonNull, "JsonEvent");
            assertEvent(env, env.listener("s0").assertOneGetNewAndReset(), jsonNull, null);

            try {
                JsonObject localInvalid = new JsonObject().add("c0", invalidJson);
                JsonObject jsonInvalidObject = new JsonObject().add("local", localInvalid);
                env.sendEventJson(jsonInvalidObject.toString(), "JsonEvent");
                fail();
            } catch (EPException ex) {
                String value = invalidJson instanceof JsonNumber ? invalidJson.toString() : invalidJson.asString();
                assertMessage(ex, "Failed to parse json member name 'c0' as a " + fieldType.getSimpleName() + "-type from value '" + value + "'");
            }

            env.milestoneInc(milestone);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertEvent(env, it.next(), jsonFilled, expected);
            assertEvent(env, it.next(), jsonNull, null);

            env.undeployAll();
        }

        private void assertEvent(RegressionEnvironment env, EventBean event, String json, Object expected) {
            MyLocalVMType result = (MyLocalVMType) event.get("local");
            assertEquals(expected, result.getC0());
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{expected}, result.getC0Array());
            EPAssertionUtil.assertEqualsExactOrder(new Object[][]{{expected}}, result.getC0Array2Dim());
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{expected}, result.getC0Collection());
            String rendered = env.runtime().getRenderEventService().getJSONRenderer(event.getEventType()).render(event);
            assertEquals(json, rendered);
        }
    }

    private static class EventJsonTypingListEnumType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent(local " + MyLocalEventCollectionEnumType.class.getName() + ");\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";

            env.compileDeploy(epl).addListener("s0");

            String jsonFilled = "{ \"local\" : { \"c0\": [\"ENUM_VALUE_1\", \"ENUM_VALUE_2\"] } }\n";
            env.sendEventJson(jsonFilled, "JsonEvent");
            assertFilled(env, env.listener("s0").assertOneGetNewAndReset(), jsonFilled);

            String jsonUnfilled = "{ \"local\" : {}}";
            env.sendEventJson(jsonUnfilled, "JsonEvent");
            assertUnfilled(env, env.listener("s0").assertOneGetNewAndReset());

            String jsonEmpty = "{ \"local\" : { \"c0\": []}}\n";
            env.sendEventJson(jsonEmpty, "JsonEvent");
            assertEmpty(env, env.listener("s0").assertOneGetNewAndReset(), jsonEmpty);

            String jsonNull = "{ \"local\" : { \"c0\": null}}\n";
            env.sendEventJson(jsonNull, "JsonEvent");
            assertUnfilled(env, env.listener("s0").assertOneGetNewAndReset());

            String jsonPartiallyFilled = "{ \"local\" : { \"c0\": [\"ENUM_VALUE_3\", null] }}\n";
            env.sendEventJson(jsonPartiallyFilled, "JsonEvent");
            assertPartiallyFilled(env, env.listener("s0").assertOneGetNewAndReset(), jsonPartiallyFilled);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFilled(env, it.next(), jsonFilled);
            assertUnfilled(env, it.next());
            assertEmpty(env, it.next(), jsonEmpty);
            assertUnfilled(env, it.next());
            assertPartiallyFilled(env, it.next(), jsonPartiallyFilled);

            env.undeployAll();
        }

        private void assertFilled(RegressionEnvironment env, EventBean event, String json) {
            assertCollection(event, local -> local.c0, SupportEnum.ENUM_VALUE_1, SupportEnum.ENUM_VALUE_2);
            assertJson(env, event, json);
        }

        private void assertPartiallyFilled(RegressionEnvironment env, EventBean event, String json) {
            assertCollection(event, local -> local.c0, SupportEnum.ENUM_VALUE_3, null);
            assertJson(env, event, json);
        }

        private void assertEmpty(RegressionEnvironment env, EventBean event, String json) {
            assertCollection(event, local -> local.c0);
            assertJson(env, event, json);
        }

        private void assertUnfilled(RegressionEnvironment env, EventBean event) {
            assertNull(collectionValue(event, local -> local.c0));
            assertJson(env, event, "{\"local\":{\"c0\":null}}");
        }

        private static void assertCollection(EventBean event, Function<MyLocalEventCollectionEnumType, Collection> function, Object... values) {
            EPAssertionUtil.assertEqualsExactOrder(values, collectionValue(event, function).toArray());
        }

        private static Collection collectionValue(EventBean event, Function<MyLocalEventCollectionEnumType, Collection> function) {
            MyLocalEventCollectionEnumType bt = (MyLocalEventCollectionEnumType) event.get("local");
            return function.apply(bt);
        }
    }

    private static class EventJsonTypingListBuiltinType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent(local " + MyLocalEventCollectionBuiltinType.class.getName() + ");\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";

            env.compileDeploy(epl).addListener("s0");

            String jsonFilled = "{ \"local\" : { " +
                "\"c0\": [\"abc\", \"def\"],\n" +
                "\"c1\": [\"x\", \"y\"],\n" +
                "\"c2\": [true, false],\n" +
                "\"c3\": [10, 11],\n" +
                "\"c4\": [20, 21],\n" +
                "\"c5\": [30, 31],\n" +
                "\"c6\": [40, 41],\n" +
                "\"c7\": [50.0, 51.0],\n" +
                "\"c8\": [60.0, 61.0],\n" +
                "\"c9\": [70, 71],\n" +
                "\"c10\": [80, 81]\n" +
                "}}\n";
            env.sendEventJson(jsonFilled, "JsonEvent");
            assertFilled(env, env.listener("s0").assertOneGetNewAndReset(), jsonFilled);

            String jsonUnfilled = "{ \"local\" : {}}";
            env.sendEventJson(jsonUnfilled, "JsonEvent");
            assertUnfilled(env, env.listener("s0").assertOneGetNewAndReset());

            String jsonEmpty = "{ \"local\" : { " +
                "\"c0\": [],\n" +
                "\"c1\": [],\n" +
                "\"c2\": [],\n" +
                "\"c3\": [],\n" +
                "\"c4\": [],\n" +
                "\"c5\": [],\n" +
                "\"c6\": [],\n" +
                "\"c7\": [],\n" +
                "\"c8\": [],\n" +
                "\"c9\": [],\n" +
                "\"c10\": []\n" +
                "}}\n";
            env.sendEventJson(jsonEmpty, "JsonEvent");
            assertEmpty(env, env.listener("s0").assertOneGetNewAndReset(), jsonEmpty);

            String jsonNull = "{ \"local\" : { " +
                "\"c0\": null,\n" +
                "\"c1\": null,\n" +
                "\"c2\": null,\n" +
                "\"c3\": null,\n" +
                "\"c4\": null,\n" +
                "\"c5\": null,\n" +
                "\"c6\": null,\n" +
                "\"c7\": null,\n" +
                "\"c8\": null,\n" +
                "\"c9\": null,\n" +
                "\"c10\": null\n" +
                "}}\n";
            env.sendEventJson(jsonNull, "JsonEvent");
            assertUnfilled(env, env.listener("s0").assertOneGetNewAndReset());

            String jsonPartiallyFilled = "{ \"local\" : { " +
                "\"c0\": [\"abc\", null],\n" +
                "\"c1\": [\"x\", null],\n" +
                "\"c2\": [true, null],\n" +
                "\"c3\": [10, null],\n" +
                "\"c4\": [20, null],\n" +
                "\"c5\": [30, null],\n" +
                "\"c6\": [40, null],\n" +
                "\"c7\": [50.0, null],\n" +
                "\"c8\": [60.0, null],\n" +
                "\"c9\": [70, null],\n" +
                "\"c10\": [80, null]\n" +
                "}}\n";
            env.sendEventJson(jsonPartiallyFilled, "JsonEvent");
            assertPartiallyFilled(env, env.listener("s0").assertOneGetNewAndReset(), jsonPartiallyFilled);

            env.milestone(0);

            Iterator<EventBean> it = env.statement("s0").iterator();
            assertFilled(env, it.next(), jsonFilled);
            assertUnfilled(env, it.next());
            assertEmpty(env, it.next(), jsonEmpty);
            assertUnfilled(env, it.next());
            assertPartiallyFilled(env, it.next(), jsonPartiallyFilled);

            env.undeployAll();
        }

        private void assertFilled(RegressionEnvironment env, EventBean event, String json) {
            assertCollection(event, local -> local.c0, "abc", "def");
            assertCollection(event, local -> local.c1, 'x', 'y');
            assertCollection(event, local -> local.c2, true, false);
            assertCollection(event, local -> local.c3, (byte) 10, (byte) 11);
            assertCollection(event, local -> local.c4, (short) 20, (short) 21);
            assertCollection(event, local -> local.c5, 30, 31);
            assertCollection(event, local -> local.c6, 40L, 41L);
            assertCollection(event, local -> local.c7, 50d, 51d);
            assertCollection(event, local -> local.c8, 60f, 61f);
            assertCollection(event, local -> local.c9, new BigInteger("70"), new BigInteger("71"));
            assertCollection(event, local -> local.c10, new BigDecimal("80"), new BigDecimal("81"));
            assertJson(env, event, json);
        }

        private void assertPartiallyFilled(RegressionEnvironment env, EventBean event, String json) {
            assertCollection(event, local -> local.c0, "abc", null);
            assertCollection(event, local -> local.c1, 'x', null);
            assertCollection(event, local -> local.c2, true, null);
            assertCollection(event, local -> local.c3, (byte) 10, null);
            assertCollection(event, local -> local.c4, (short) 20, null);
            assertCollection(event, local -> local.c5, 30, null);
            assertCollection(event, local -> local.c6, 40L, null);
            assertCollection(event, local -> local.c7, 50d, null);
            assertCollection(event, local -> local.c8, 60f, null);
            assertCollection(event, local -> local.c9, new BigInteger("70"), null);
            assertCollection(event, local -> local.c10, new BigDecimal("80"), null);
            assertJson(env, event, json);
        }

        private void assertEmpty(RegressionEnvironment env, EventBean event, String json) {
            assertCollection(event, local -> local.c0);
            assertCollection(event, local -> local.c1);
            assertCollection(event, local -> local.c2);
            assertCollection(event, local -> local.c3);
            assertCollection(event, local -> local.c4);
            assertCollection(event, local -> local.c5);
            assertCollection(event, local -> local.c6);
            assertCollection(event, local -> local.c7);
            assertCollection(event, local -> local.c8);
            assertCollection(event, local -> local.c9);
            assertCollection(event, local -> local.c10);
            assertJson(env, event, json);
        }

        private void assertUnfilled(RegressionEnvironment env, EventBean event) {
            assertNull(collectionValue(event, local -> local.c0));
            assertNull(collectionValue(event, local -> local.c1));
            assertNull(collectionValue(event, local -> local.c2));
            assertNull(collectionValue(event, local -> local.c3));
            assertNull(collectionValue(event, local -> local.c4));
            assertNull(collectionValue(event, local -> local.c5));
            assertNull(collectionValue(event, local -> local.c6));
            assertNull(collectionValue(event, local -> local.c7));
            assertNull(collectionValue(event, local -> local.c8));
            assertNull(collectionValue(event, local -> local.c9));
            assertNull(collectionValue(event, local -> local.c10));
            assertJson(env, event, "{\"local\":{\"c0\":null,\"c1\":null,\"c2\":null,\"c3\":null,\"c4\":null,\"c5\":null,\"c6\":null,\"c7\":null,\"c8\":null,\"c9\":null,\"c10\":null}}");
        }

        private static void assertCollection(EventBean event, Function<MyLocalEventCollectionBuiltinType, Collection> function, Object... values) {
            EPAssertionUtil.assertEqualsExactOrder(values, collectionValue(event, function).toArray());
        }

        private static Collection collectionValue(EventBean event, Function<MyLocalEventCollectionBuiltinType, Collection> function) {
            MyLocalEventCollectionBuiltinType bt = (MyLocalEventCollectionBuiltinType) event.get("local");
            return function.apply(bt);
        }
    }

    private static class EventJsonTypingClassSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype create json schema JsonEvent(local " + MyLocalEvent.class.getName() + ");\n" +
                "@name('s0') select * from JsonEvent#keepall;\n";
            env.compileDeploy(epl).addListener("s0");

            String json = "{\n" +
                "  \"local\": {\n" +
                "    \"theString\": \"abc\",\n" +
                "    \"intPrimitive\" : 10\n" +
                "  }\n" +
                "}";
            env.sendEventJson(json, "JsonEvent");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "local".split(","), new Object[]{new MyLocalEvent("abc", 10)});

            env.milestone(0);

            EPAssertionUtil.assertProps(env.iterator("s0").next(), "local".split(","), new Object[]{new MyLocalEvent("abc", 10)});

            env.undeployAll();
        }
    }

    private static void assertJson(RegressionEnvironment env, EventBean event, String json) {
        String rendered = env.runtime().getRenderEventService().getJSONRenderer(event.getEventType()).render(event);
        assertEquals(json.replaceAll(" ", "").replaceAll("\n", ""), rendered);
    }

    public static class MyLocalEvent implements Serializable {
        public String theString;
        public int intPrimitive;

        public MyLocalEvent() {
        }

        public MyLocalEvent(String theString, int intPrimitive) {
            this.theString = theString;
            this.intPrimitive = intPrimitive;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyLocalEvent that = (MyLocalEvent) o;

            if (intPrimitive != that.intPrimitive) return false;
            return theString != null ? theString.equals(that.theString) : that.theString == null;

        }

        public int hashCode() {
            int result = theString != null ? theString.hashCode() : 0;
            result = 31 * result + intPrimitive;
            return result;
        }
    }

    public static class MyLocalEventCollectionBuiltinType implements Serializable {
        public List<String> c0;
        public List<Character> c1;
        public List<Boolean> c2;
        public List<Byte> c3;
        public List<Short> c4;
        public List<Integer> c5;
        public List<Long> c6;
        public List<Double> c7;
        public List<Float> c8;
        public List<BigInteger> c9;
        public List<BigDecimal> c10;
    }

    public static class MyLocalEventCollectionEnumType implements Serializable {
        public List<SupportEnum> c0;
    }

    public interface MyLocalVMType extends Serializable {
        Object getC0();

        Object[] getC0Array();

        Object[][] getC0Array2Dim();

        Collection getC0Collection();
    }

    public static class MyLocalVMTypeUUID implements MyLocalVMType {
        public UUID c0;
        public UUID[] c0Arr;
        public UUID[][] c0Arr2Dim;
        public List<UUID> c0Coll;

        public Object getC0() {
            return c0;
        }

        public Object[] getC0Array() {
            return c0Arr;
        }

        public Object[][] getC0Array2Dim() {
            return c0Arr2Dim;
        }

        public Collection getC0Collection() {
            return c0Coll;
        }
    }

    public static class MyLocalVMTypeOffsetDateTime implements MyLocalVMType {
        public OffsetDateTime c0;
        public OffsetDateTime[] c0Arr;
        public OffsetDateTime[][] c0Arr2Dim;
        public List<OffsetDateTime> c0Coll;

        public Object getC0() {
            return c0;
        }

        public Object[] getC0Array() {
            return c0Arr;
        }

        public Object[][] getC0Array2Dim() {
            return c0Arr2Dim;
        }

        public Collection getC0Collection() {
            return c0Coll;
        }
    }

    public static class MyLocalVMTypeLocalDate implements MyLocalVMType {
        public LocalDate c0;
        public LocalDate[] c0Arr;
        public LocalDate[][] c0Arr2Dim;
        public List<LocalDate> c0Coll;

        public Object getC0() {
            return c0;
        }

        public Object[] getC0Array() {
            return c0Arr;
        }

        public Object[][] getC0Array2Dim() {
            return c0Arr2Dim;
        }

        public Collection getC0Collection() {
            return c0Coll;
        }
    }

    public static class MyLocalVMTypeLocalDateTime implements MyLocalVMType {
        public LocalDateTime c0;
        public LocalDateTime[] c0Arr;
        public LocalDateTime[][] c0Arr2Dim;
        public List<LocalDateTime> c0Coll;

        public Object getC0() {
            return c0;
        }

        public Object[] getC0Array() {
            return c0Arr;
        }

        public Object[][] getC0Array2Dim() {
            return c0Arr2Dim;
        }

        public Collection getC0Collection() {
            return c0Coll;
        }
    }

    public static class MyLocalVMTypeZonedDateTime implements MyLocalVMType {
        public ZonedDateTime c0;
        public ZonedDateTime[] c0Arr;
        public ZonedDateTime[][] c0Arr2Dim;
        public List<ZonedDateTime> c0Coll;

        public Object getC0() {
            return c0;
        }

        public Object[] getC0Array() {
            return c0Arr;
        }

        public Object[][] getC0Array2Dim() {
            return c0Arr2Dim;
        }

        public Collection getC0Collection() {
            return c0Coll;
        }
    }

    public static class MyLocalVMTypeURL implements MyLocalVMType {
        public URL c0;
        public URL[] c0Arr;
        public URL[][] c0Arr2Dim;
        public List<URL> c0Coll;

        public Object getC0() {
            return c0;
        }

        public Object[] getC0Array() {
            return c0Arr;
        }

        public Object[][] getC0Array2Dim() {
            return c0Arr2Dim;
        }

        public Collection getC0Collection() {
            return c0Coll;
        }
    }

    public static class MyLocalVMTypeURI implements MyLocalVMType {
        public URI c0;
        public URI[] c0Arr;
        public URI[][] c0Arr2Dim;
        public List<URI> c0Coll;

        public Object getC0() {
            return c0;
        }

        public Object[] getC0Array() {
            return c0Arr;
        }

        public Object[][] getC0Array2Dim() {
            return c0Arr2Dim;
        }

        public Collection getC0Collection() {
            return c0Coll;
        }
    }

    public static class MyLocalEventWArrayColl implements Serializable {
        public MyLocalEvent c0;
        public MyLocalEvent[] c0Arr;
        public MyLocalEvent[][] c0Arr2Dim;
        public List<MyLocalEvent> c0Coll;

        public Object getC0() {
            return c0;
        }

        public Object[] getC0Array() {
            return c0Arr;
        }

        public Object[][] getC0Array2Dim() {
            return c0Arr2Dim;
        }

        public Collection getC0Collection() {
            return c0Coll;
        }
    }

    public static class MyLocalEventNestedRecursive implements Serializable {
        public String id;
        public MyLocalEventNestedRecursive child;
    }
}
