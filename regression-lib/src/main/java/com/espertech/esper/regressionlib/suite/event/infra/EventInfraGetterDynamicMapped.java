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
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.Json;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.util.NullableObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.apache.avro.SchemaBuilder.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterDynamicMapped implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<NullableObject<Map<String, String>>> bean = nullable -> {
            if (nullable == null) {
                env.sendEventBean(new LocalEvent());
            } else {
                env.sendEventBean(new LocalEventSubA(nullable.getObject()));
            }
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEventSubA as " + LocalEventSubA.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<NullableObject<Map<String, String>>> map = nullable -> {
            if (nullable == null) {
                env.sendEventMap(Collections.emptyMap(), "LocalEvent");
            } else {
                env.sendEventMap(Collections.singletonMap("mapped", nullable.getObject()), "LocalEvent");
            }
        };
        String mapepl = "@public @buseventtype create schema LocalEvent();\n";
        runAssertion(env, mapepl, map);

        // Object-array
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent();\n" +
            "@public @buseventtype create objectarray schema LocalEventSubA (mapped java.util.Map) inherits LocalEvent;\n";
        runAssertion(env, oaepl, null);

        // Json
        Consumer<NullableObject<Map<String, String>>> json = nullable -> {
            if (nullable == null) {
                env.sendEventJson("{}", "LocalEvent");
            } else if (nullable.getObject() == null) {
                env.sendEventJson(new JsonObject().add("mapped", Json.NULL).toString(), "LocalEvent");
            } else {
                JsonObject event = new JsonObject();
                JsonObject mapped = new JsonObject();
                event.add("mapped", mapped);
                for (Map.Entry<String, String> entry : nullable.getObject().entrySet()) {
                    mapped.add(entry.getKey(), entry.getValue());
                }
                env.sendEventJson(event.toString(), "LocalEvent");
            }
        };
        runAssertion(env, "@public @buseventtype @JsonSchema(dynamic=true) create json schema LocalEvent();\n", json);

        // Json-Class-Provided
        runAssertion(env, "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n", json);

        // Avro
        Consumer<NullableObject<Map<String, String>>> avro = nullable -> {
            Schema schema = SchemaBuilder.record("name").fields().name("mapped").type(map().values().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString()).noDefault().endRecord();
            GenericData.Record event;
            if (nullable == null) {
                // no action
                event = new GenericData.Record(schema);
                event.put("mapped", Collections.emptyMap());
            } else if (nullable.getObject() == null) {
                event = new GenericData.Record(schema);
                event.put("mapped", Collections.emptyMap());
            } else {
                event = new GenericData.Record(schema);
                event.put("mapped", nullable.getObject());
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, "@public @buseventtype create avro schema LocalEvent();\n", avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<NullableObject<Map<String, String>>> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");

        if (sender == null) {
            env.assertStatement("s0", statement -> {
                EventType eventType = statement.getEventType();
                EventPropertyGetter g0 = eventType.getGetter("mapped('a')?");
                EventPropertyGetter g1 = eventType.getGetter("mapped('b')?");
                assertNull(g0);
                assertNull(g1);
            });
            env.undeployAll();
            return;
        }

        String propepl = "@name('s1') select mapped('a')? as c0, mapped('b')? as c1," +
            "exists(mapped('a')?) as c2, exists(mapped('b')?) as c3, " +
            "typeof(mapped('a')?) as c4, typeof(mapped('b')?) as c5 from LocalEvent;\n";
        env.compileDeploy(propepl, path).addListener("s1");

        Map<String, String> values = new HashMap<>();
        values.put("a", "x");
        values.put("b", "y");
        sender.accept(new NullableObject<>(values));
        env.assertEventNew("s0", event -> assertGetters(event, true, "x", true, "y"));
        assertProps(env, "x", "y");

        sender.accept(new NullableObject<>(Collections.singletonMap("a", "x")));
        env.assertEventNew("s0", event -> assertGetters(event, true, "x", false, null));
        assertProps(env, "x", null);

        sender.accept(new NullableObject<>(Collections.emptyMap()));
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, null, null);

        sender.accept(new NullableObject<>(null));
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, null, null);

        sender.accept(null);
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, null, null);

        env.undeployAll();
    }

    private void assertGetters(EventBean event, boolean existsZero, String valueZero, boolean existsOne, String valueOne) {
        EventPropertyGetter g0 = event.getEventType().getGetter("mapped('a')?");
        EventPropertyGetter g1 = event.getEventType().getGetter("mapped('b')?");
        assertGetter(event, g0, existsZero, valueZero);
        assertGetter(event, g1, existsOne, valueOne);
    }

    private void assertGetter(EventBean event, EventPropertyGetter getter, boolean exists, String value) {
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private void assertProps(RegressionEnvironment env, String valueA, String valueB) {
        env.assertEventNew("s1", event -> {
            assertEquals(valueA, event.get("c0"));
            assertEquals(valueB, event.get("c1"));
            assertEquals(valueA != null, event.get("c2"));
            assertEquals(valueB != null, event.get("c3"));
            assertEquals(valueA == null ? null : "String", event.get("c4"));
            assertEquals(valueB == null ? null : "String", event.get("c5"));
        });
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = -8754383424647226931L;
    }

    public static class LocalEventSubA extends LocalEvent {
        private static final long serialVersionUID = -1406336363162914136L;
        private Map<String, String> mapped;

        public LocalEventSubA(Map<String, String> mapped) {
            this.mapped = mapped;
        }

        public Map<String, String> getMapped() {
            return mapped;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = 8086184628619086520L;
        public Map<String, String> mapped;
    }
}
