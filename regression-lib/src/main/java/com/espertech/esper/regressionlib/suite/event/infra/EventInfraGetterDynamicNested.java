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
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.util.NullableObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterDynamicNested implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, NullableObject<String>> bean = (type, nullable) -> {
            if (nullable == null) {
                env.sendEventBean(new LocalEvent());
            } else {
                env.sendEventBean(new LocalEventSubA(new LocalInnerEvent(nullable.getObject())));
            }
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEventSubA as " + LocalEventSubA.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, NullableObject<String>> map = (type, nullable) -> {
            if (nullable == null) {
                env.sendEventMap(Collections.emptyMap(), "LocalEvent");
            } else {
                Map<String, Object> inner = Collections.<String, Object>singletonMap("id", nullable.getObject());
                env.sendEventMap(Collections.singletonMap("property", inner), "LocalEvent");
            }
        };
        runAssertion(env, getEPL("map"), map);

        // Object-array
        runAssertion(env, getEPL("objectarray"), null);

        // Json
        BiConsumer<EventType, NullableObject<String>> json = (type, nullable) -> {
            if (nullable == null) {
                env.sendEventJson("{}", "LocalEvent");
            } else {
                JsonObject inner = new JsonObject().add("id", nullable.getObject());
                env.sendEventJson(new JsonObject().add("property", inner).toString(), "LocalEvent");
            }
        };
        runAssertion(env, getEPL("json"), json);

        // Json-Class-Provided
        String jsonProvidedEPL = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, jsonProvidedEPL, json);

        // Avro
        BiConsumer<EventType, NullableObject<String>> avro = (type, nullable) -> {
            GenericData.Record event;
            if (nullable == null) {
                event = new GenericData.Record(SupportAvroUtil.getAvroSchema(type));
            } else {
                Schema innerSchema = SchemaBuilder.record("name").fields().name("id").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault().endRecord();
                GenericData.Record inner = new GenericData.Record(innerSchema);
                inner.put("id", nullable.getObject());
                Schema schema = SchemaBuilder.record("name").fields().name("property").type(innerSchema).noDefault().endRecord();
                event = new GenericData.Record(schema);
                event.put("property", inner);
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, getEPL("avro"), avro);
    }

    private String getEPL(String underlying) {
        return "@public @buseventtype @JsonSchema(dynamic=true) create " + underlying + " schema LocalEvent();\n";
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, NullableObject<String>> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");
        EventType eventType = env.statement("s0").getEventType();
        EventPropertyGetter g0 = eventType.getGetter("property?.id");

        if (sender == null) {
            assertNull(g0);
            env.undeployAll();
            return;
        } else {
            String propepl = "@name('s1') select property?.id as c0, exists(property?.id) as c1, typeof(property?.id) as c2 from LocalEvent;\n";
            env.compileDeploy(propepl, path).addListener("s1");
        }

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

    private void assertGetter(EventBean event, EventPropertyGetter getter, boolean exists, String value) {
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private void assertProps(RegressionEnvironment env, boolean exists, String value) {
        EventBean event = env.listener("s1").assertOneGetNewAndReset();
        assertEquals(value, event.get("c0"));
        assertEquals(exists, event.get("c1"));
        assertEquals(value != null ? "String" : null, event.get("c2"));
    }

    public static class LocalEvent {
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

    public static class LocalEventSubA extends LocalEvent {
        private LocalInnerEvent property;

        public LocalEventSubA(LocalInnerEvent property) {
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
