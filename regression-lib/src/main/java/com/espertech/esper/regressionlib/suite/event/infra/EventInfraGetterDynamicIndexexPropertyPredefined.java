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
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.util.NullableObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.json.SupportJsonEventTypeUtil;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterDynamicIndexexPropertyPredefined implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, NullableObject<Integer>> bean = (type, nullable) -> {
            if (nullable == null) {
                env.sendEventBean(new LocalEvent());
            } else if (nullable.getObject() == null) {
                env.sendEventBean(new LocalEventSubA(null));
            } else {
                LocalInnerEvent[] array = new LocalInnerEvent[nullable.getObject()];
                for (int i = 0; i < array.length; i++) {
                    array[i] = new LocalInnerEvent();
                }
                env.sendEventBean(new LocalEventSubA(array));
            }
        };
        String beanepl =
            "@public @buseventtype create schema LocalInnerEvent as " + LocalInnerEvent.class.getName() + ";\n" +
                "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n" +
                "@public @buseventtype create schema LocalEventSubA as " + LocalEventSubA.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, NullableObject<Integer>> map = (type, nullable) -> {
            if (nullable == null) {
                env.sendEventMap(Collections.emptyMap(), "LocalEvent");
            } else if (nullable.getObject() == null) {
                env.sendEventMap(Collections.singletonMap("array", null), "LocalEvent");
            } else {
                Map[] array = new Map[nullable.getObject()];
                for (int i = 0; i < array.length; i++) {
                    array[i] = new HashMap();
                }
                env.sendEventMap(Collections.singletonMap("array", array), "LocalEvent");
            }
        };
        String mapepl =
            "@public @buseventtype create schema LocalInnerEvent();\n" +
                "@public @buseventtype create schema LocalEvent(array LocalInnerEvent[]);\n";
        runAssertion(env, mapepl, map);

        // Object-array
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent();\n" +
            "@public @buseventtype create objectarray schema LocalEventSubA (array string[]) inherits LocalEvent;\n";
        runAssertion(env, oaepl, null);

        // Json
        BiConsumer<EventType, NullableObject<Integer>> json = (type, nullable) -> {
            if (nullable == null) {
                env.sendEventJson("{}", "LocalEvent");
            } else if (nullable.getObject() == null) {
                env.sendEventJson(new JsonObject().add("array", Json.NULL).toString(), "LocalEvent");
            } else {
                JsonObject event = new JsonObject();
                JsonArray array = new JsonArray();
                event.add("array", array);
                for (int i = 0; i < nullable.getObject(); i++) {
                    array.add(new JsonObject());
                }
                env.sendEventJson(event.toString(), "LocalEvent");
            }
        };
        String epl = "@public @buseventtype create json schema LocalInnerEvent();\n" +
            "@public @buseventtype create json schema LocalEvent(array LocalInnerEvent[]);\n";
        runAssertion(env, epl, json);

        // Json-Class-Provided
        String eplJsonProvided = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, eplJsonProvided, json);

        // Avro
        BiConsumer<EventType, NullableObject<Integer>> avro = (type, nullable) -> {
            Schema inner = SchemaBuilder.record("name").fields().endRecord();
            Schema schema = SchemaBuilder.record("name").fields()
                .name("array").type(SchemaBuilder.array().items(inner)).noDefault()
                .endRecord();
            GenericData.Record event;
            if (nullable == null) {
                // no action
                event = new GenericData.Record(SupportAvroUtil.getAvroSchema(type));
            } else if (nullable.getObject() == null) {
                event = new GenericData.Record(schema);
            } else {
                event = new GenericData.Record(schema);
                Collection<GenericData.Record> inners = new ArrayList<>();
                for (int i = 0; i < nullable.getObject(); i++) {
                    inners.add(new GenericData.Record(inner));
                }
                event.put("array", inners);
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        String avroepl = "@public @buseventtype create avro schema LocalInnerEvent();\n" +
            "@public @buseventtype create avro schema LocalEvent(array LocalInnerEvent[]);\n";
        runAssertion(env, avroepl, avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, NullableObject<Integer>> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");
        EventType eventType = env.statement("s0").getEventType();
        EventPropertyGetter g0 = eventType.getGetter("array[0]?");
        EventPropertyGetter g1 = eventType.getGetter("array[1]?");

        if (sender == null) {
            assertNull(g0);
            assertNull(g1);
            env.undeployAll();
            return;
        } else {
            String propepl = "@name('s1') select array[0]? as c0, array[1]? as c1," +
                "exists(array[0]?) as c2, exists(array[1]?) as c3, " +
                "typeof(array[0]?) as c4, typeof(array[1]?) as c5 from LocalEvent;\n";
            env.compileDeploy(propepl, path).addListener("s1");
        }

        sender.accept(eventType, new NullableObject<>(2));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true);
        assertGetter(event, g1, true);
        assertProps(env, true, true);

        sender.accept(eventType, new NullableObject<>(1));
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true);
        assertGetter(event, g1, false);
        assertProps(env, true, false);

        sender.accept(eventType, new NullableObject<>(0));
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, false);
        assertGetter(event, g1, false);
        assertProps(env, false, false);

        sender.accept(eventType, new NullableObject<>(null));
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, false);
        assertGetter(event, g1, false);
        assertProps(env, false, false);

        sender.accept(eventType, null);
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, false);
        assertGetter(event, g1, false);
        assertProps(env, false, false);

        env.undeployAll();
    }

    private void assertGetter(EventBean event, EventPropertyGetter getter, boolean exists) {
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(exists, getter.get(event) != null);
        boolean beanBacked = event.getEventType() instanceof BeanEventType || SupportJsonEventTypeUtil.isBeanBackedJson(event.getEventType());
        assertEquals(beanBacked && exists, getter.getFragment(event) != null);
    }

    private void assertProps(RegressionEnvironment env, boolean hasA, boolean hasB) {
        EventBean event = env.listener("s1").assertOneGetNewAndReset();
        assertEquals(hasA, event.get("c0") != null);
        assertEquals(hasB, event.get("c1") != null);
        assertEquals(hasA, event.get("c2"));
        assertEquals(hasB, event.get("c3"));
        assertEquals(hasA, event.get("c4") != null);
        assertEquals(hasB, event.get("c5") != null);
    }

    public static class LocalInnerEvent {
    }

    public static class LocalEvent {
    }

    public static class LocalEventSubA extends LocalEvent {
        private LocalInnerEvent[] array;

        public LocalEventSubA(LocalInnerEvent[] array) {
            this.array = array;
        }

        public LocalInnerEvent[] getArray() {
            return array;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public MyLocalJsonProvidedInnerEvent[] array;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
    }
}
