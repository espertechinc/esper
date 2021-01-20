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
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.util.NullableObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterDynamicIndexed implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<NullableObject<String[]>> bean = nullable -> {
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
        Consumer<NullableObject<String[]>> map = nullable -> {
            if (nullable == null) {
                env.sendEventMap(Collections.emptyMap(), "LocalEvent");
            } else {
                env.sendEventMap(Collections.singletonMap("array", nullable.getObject()), "LocalEvent");
            }
        };
        String mapepl = "@public @buseventtype create schema LocalEvent();\n";
        runAssertion(env, mapepl, map);

        // Object-array
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent();\n" +
            "@public @buseventtype create objectarray schema LocalEventSubA (array string[]) inherits LocalEvent;\n";
        runAssertion(env, oaepl, null);

        // Json
        Consumer<NullableObject<String[]>> json = nullable -> {
            if (nullable == null) {
                env.sendEventJson("{}", "LocalEvent");
            } else if (nullable.getObject() == null) {
                env.sendEventJson(new JsonObject().add("array", Json.NULL).toString(), "LocalEvent");
            } else {
                JsonObject event = new JsonObject();
                JsonArray array = new JsonArray();
                event.add("array", array);
                for (String string : nullable.getObject()) {
                    array.add(string);
                }
                env.sendEventJson(event.toString(), "LocalEvent");
            }
        };
        runAssertion(env, "@public @buseventtype @JsonSchema(dynamic=true) create json schema LocalEvent();\n", json);

        // Json-Class-Provided
        runAssertion(env, "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype @JsonSchema() create json schema LocalEvent();\n", json);

        // Avro
        Consumer<NullableObject<String[]>> avro = nullable -> {
            Schema schema = SchemaBuilder.record("name").fields()
                .name("array").type(SchemaBuilder.array().items().stringType()).noDefault()
                .endRecord();
            GenericData.Record event;
            if (nullable == null) {
                // no action
                event = new GenericData.Record(schema);
                event.put("array", Collections.emptyList());
            } else if (nullable.getObject() == null) {
                event = new GenericData.Record(schema);
                event.put("array", Collections.emptyList());
            } else {
                event = new GenericData.Record(schema);
                event.put("array", Arrays.asList(nullable.getObject()));
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, "@public @buseventtype create avro schema LocalEvent();\n", avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<NullableObject<String[]>> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);
        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");

        if (sender == null) {
            env.assertStatement("s0", statement -> {
                EventType eventType = statement.getEventType();
                EventPropertyGetter g0 = eventType.getGetter("array[0]?");
                EventPropertyGetter g1 = eventType.getGetter("array[1]?");
                assertNull(g0);
                assertNull(g1);
            });
            env.undeployAll();
            return;
        }

        String propepl = "@name('s1') select array[0]? as c0, array[1]? as c1," +
            "exists(array[0]?) as c2, exists(array[1]?) as c3, " +
            "typeof(array[0]?) as c4, typeof(array[1]?) as c5 from LocalEvent;\n";
        env.compileDeploy(propepl, path).addListener("s1");

        sender.accept(new NullableObject<>(new String[]{"a", "b"}));
        env.assertEventNew("s0", event -> assertGetters(event, true, "a", true, "b"));
        assertProps(env, "a", "b");

        sender.accept(new NullableObject<>(new String[]{"a"}));
        env.assertEventNew("s0", event -> assertGetters(event, true, "a", false, null));
        assertProps(env, "a", null);

        sender.accept(new NullableObject<>(new String[0]));
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, null, null);

        sender.accept(new NullableObject<>(null));
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, null, null);

        sender.accept(null);
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));

        env.undeployAll();
    }

    private void assertGetters(EventBean event, boolean existsZero, String valueZero, boolean existsOne, String valueOne) {
        EventPropertyGetter g0 = event.getEventType().getGetter("array[0]?");
        EventPropertyGetter g1 = event.getEventType().getGetter("array[1]?");
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
        private static final long serialVersionUID = -6780954827765049620L;
    }

    public static class LocalEventSubA extends LocalEvent {
        private static final long serialVersionUID = 2119812618250886483L;
        private String[] array;

        public LocalEventSubA(String[] array) {
            this.array = array;
        }

        public String[] getArray() {
            return array;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = -4565597414944608305L;
        public String[] array;
    }
}
