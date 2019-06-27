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
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.util.NullableObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.json.SupportJsonEventTypeUtil;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.function.BiConsumer;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterDynamicSimple implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, NullableObject<String>> bean = (type, nullable) -> {
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
        BiConsumer<EventType, NullableObject<String>> map = (type, nullable) -> {
            if (nullable == null) {
                env.sendEventMap(Collections.emptyMap(), "LocalEvent");
            } else {
                env.sendEventMap(Collections.singletonMap("property", nullable.getObject()), "LocalEvent");
            }
        };
        String mapepl = "@public @buseventtype create schema LocalEvent();\n";
        runAssertion(env, mapepl, map);

        // Object-array
        BiConsumer<EventType, NullableObject<String>> oa = (type, nullable) -> {
            if (nullable == null) {
                env.sendEventObjectArray(new Object[0], "LocalEvent");
            } else {
                env.sendEventObjectArray(new Object[]{nullable.getObject()}, "LocalEventSubA");
            }
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent();\n" +
            "@public @buseventtype create objectarray schema LocalEventSubA (property string) inherits LocalEvent;\n";
        runAssertion(env, oaepl, oa);

        // Json
        BiConsumer<EventType, NullableObject<String>> json = (type, nullable) -> {
            if (nullable == null) {
                env.sendEventJson("{}", "LocalEvent");
            } else if (nullable.getObject() == null) {
                env.sendEventJson(new JsonObject().add("property", Json.NULL).toString(), "LocalEvent");
            } else {
                env.sendEventJson(new JsonObject().add("property", nullable.getObject()).toString(), "LocalEvent");
            }
        };
        runAssertion(env, "@public @buseventtype @JsonSchema(dynamic=true) create json schema LocalEvent();\n", json);

        // Json-Class-Provided
        runAssertion(env, "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n", json);

        // Avro
        BiConsumer<EventType, NullableObject<String>> avro = (type, nullable) -> {
            Schema schema = SchemaBuilder.record("name").fields().name("property").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault().endRecord();
            GenericData.Record event;
            if (nullable == null) {
                // no action
                event = new GenericData.Record(SupportAvroUtil.getAvroSchema(type));
            } else if (nullable.getObject() == null) {
                event = new GenericData.Record(schema);
            } else {
                event = new GenericData.Record(schema);
                event.put("property", nullable.getObject());
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, "@public @buseventtype create avro schema LocalEvent();\n", avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, NullableObject<String>> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");
        EventType eventType = env.statement("s0").getEventType();
        EventPropertyGetter g0 = eventType.getGetter("property?");

        if (sender == null) {
            assertNull(g0);
            env.undeployAll();
            return;
        } else {
            String propepl = "@name('s1') select property? as c0, exists(property?) as c1, typeof(property?) as c2 from LocalEvent;\n";
            env.compileDeploy(propepl, path).addListener("s1");
        }

        sender.accept(eventType, new NullableObject<>("a"));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, "a");
        assertProps(env, eventType, true, "a");

        sender.accept(eventType, new NullableObject<>(null));
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, null);
        assertProps(env, eventType, true, null);

        sender.accept(eventType, null);
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, false, null);
        assertProps(env, eventType, false, null);

        env.undeployAll();
    }

    private void assertGetter(EventBean event, EventPropertyGetter getter, boolean exists, String value) {
        assertEquals(SupportJsonEventTypeUtil.isBeanBackedJson(event.getEventType()) || exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private void assertProps(RegressionEnvironment env, EventType eventType, boolean exists, String value) {
        EventBean event = env.listener("s1").assertOneGetNewAndReset();
        assertEquals(value, event.get("c0"));
        assertEquals(SupportJsonEventTypeUtil.isBeanBackedJson(eventType) || exists, event.get("c1"));
        assertEquals(value != null ? "String" : null, event.get("c2"));
    }

    public static class LocalEvent {
    }

    public static class LocalEventSubA extends LocalEvent {
        private String property;

        public LocalEventSubA(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public String property;
    }
}
