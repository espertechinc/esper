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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.util.NullableObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterNestedSimple implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, NullableObject<String>> bean = (type, nullable) -> {
            LocalInnerEvent property = nullable == null ? null : new LocalInnerEvent(nullable.getObject());
            env.sendEventBean(new LocalEvent(property));
        };
        String beanepl = "@public @buseventtype create schema LocalInnerEvent as " + LocalInnerEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, NullableObject<String>> map = (type, nullable) -> {
            Map<String, Object> property = nullable == null ? null : Collections.singletonMap("id", nullable.getObject());
            env.sendEventMap(Collections.singletonMap("property", property), "LocalEvent");
        };
        runAssertion(env, getEpl("map"), map);

        // Object-array
        BiConsumer<EventType, NullableObject<String>> oa = (type, nullable) -> {
            Object[] property = nullable == null ? null : new Object[]{nullable.getObject()};
            env.sendEventObjectArray(new Object[]{property}, "LocalEvent");
        };
        runAssertion(env, getEpl("objectarray"), oa);

        // Json
        BiConsumer<EventType, NullableObject<String>> json = (type, nullable) -> {
            JsonObject event = new JsonObject();
            if (nullable != null) {
                if (nullable.getObject() != null) {
                    event.add("property", new JsonObject().add("id", nullable.getObject()));
                } else {
                    event.add("property", new JsonObject().add("id", Json.NULL));
                }
            }
            env.sendEventJson(event.toString(), "LocalEvent");
        };
        runAssertion(env, getEpl("json"), json);

        // Json-Class-Provided
        String eplJsonProvided = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, eplJsonProvided, json);

        // Avro
        BiConsumer<EventType, NullableObject<String>> avro = (type, nullable) -> {
            Schema schema = SupportAvroUtil.getAvroSchema(type);
            GenericData.Record event = new GenericData.Record(schema);
            if (nullable != null) {
                GenericData.Record inside = new GenericData.Record(schema.getField("property").schema());
                inside.put("id", nullable.getObject());
                event.put("property", inside);
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, getEpl("avro"), avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, NullableObject<String>> sender) {

        String epl = createSchemaEPL +
            "@name('s0') select * from LocalEvent;\n" +
            "@name('s1') select property.id as c0, exists(property.id) as c1, typeof(property.id) as c2 from LocalEvent;\n";
        env.compileDeploy(epl).addListener("s0").addListener("s1");
        EventType eventType = env.statement("s0").getEventType();

        EventPropertyGetter g0 = eventType.getGetter("property.id");

        sender.accept(eventType, new NullableObject<>("a"));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, "a");
        assertProps(env, true, "a");

        sender.accept(eventType, new NullableObject<>(null));
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, null);
        assertProps(env, true, null);

        sender.accept(eventType, null);
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, false, null);
        assertProps(env, false, null);

        env.undeployAll();
    }

    private void assertProps(RegressionEnvironment env, boolean exists, String expected) {
        EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "c0,c1,c2".split(","),
            new Object[]{expected, exists, expected != null ? String.class.getSimpleName() : null});
    }

    private void assertGetter(EventBean event, EventPropertyGetter getter, boolean exists, String value) {
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private String getEpl(String underlying) {
        return "@public @buseventtype create " + underlying + " schema LocalInnerEvent(id string);\n" +
            "@public @buseventtype create " + underlying + " schema LocalEvent(property LocalInnerEvent);\n";
    }

    public static class LocalInnerEvent {
        private final String id;

        public LocalInnerEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class LocalEvent {
        private LocalInnerEvent property;

        public LocalEvent(LocalInnerEvent property) {
            this.property = property;
        }

        public LocalInnerEvent getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public MyLocalJsonProvidedInnerEvent property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        public String id;
    }
}
