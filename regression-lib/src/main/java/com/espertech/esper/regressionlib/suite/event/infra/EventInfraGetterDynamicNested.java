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
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterDynamicNested implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<NullableObject<String>> bean = nullable -> {
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
        Consumer<NullableObject<String>> map = nullable -> {
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
        Consumer<NullableObject<String>> json = nullable -> {
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
        Consumer<NullableObject<String>> avro = nullable -> {
            GenericData.Record event;
            Schema innerSchema = SchemaBuilder.record("inner").fields().name("id").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault().endRecord();
            Schema schema = SchemaBuilder.record("name").fields().name("property").type(innerSchema).noDefault().endRecord();
            if (nullable == null) {
                event = new GenericData.Record(schema);
            } else {
                GenericData.Record inner = new GenericData.Record(innerSchema);
                inner.put("id", nullable.getObject());
                event = new GenericData.Record(schema);
                event.put("property", inner);
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        env.assertThat(() -> runAssertion(env, getEPL("avro"), avro)); // Avro may not serialize well when incomplete
    }

    private String getEPL(String underlying) {
        return "@public @buseventtype @JsonSchema(dynamic=true) create " + underlying + " schema LocalEvent();\n";
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<NullableObject<String>> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");

        if (sender == null) {
            env.assertStatement("s0", statement -> {
                EventType eventType = statement.getEventType();
                EventPropertyGetter g0 = eventType.getGetter("property?.id");
                assertNull(g0);
            });
            env.undeployAll();
            return;
        }

        String propepl = "@name('s1') select property?.id as c0, exists(property?.id) as c1, typeof(property?.id) as c2 from LocalEvent;\n";
        env.compileDeploy(propepl, path).addListener("s1");

        sender.accept(new NullableObject<>("a"));
        env.assertEventNew("s0", event -> assertGetter(event, true, "a"));
        assertProps(env, true, "a");

        sender.accept(new NullableObject<>(null));
        env.assertEventNew("s0", event -> assertGetter(event, true, null));
        assertProps(env, true, null);

        sender.accept(null);
        env.assertEventNew("s0", event -> assertGetter(event, false, null));
        assertProps(env, false, null);

        env.undeployAll();
    }

    private void assertGetter(EventBean event, boolean exists, String value) {
        EventPropertyGetter getter = event.getEventType().getGetter("property?.id");
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private void assertProps(RegressionEnvironment env, boolean exists, String value) {
        env.assertEventNew("s1", event -> {
            assertEquals(value, event.get("c0"));
            assertEquals(exists, event.get("c1"));
            assertEquals(value != null ? "String" : null, event.get("c2"));
        });
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = 4298632712292306496L;
    }

    public static class LocalInnerEvent implements Serializable {
        private static final long serialVersionUID = -974306285646221306L;
        private final String id;

        public LocalInnerEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class LocalEventSubA extends LocalEvent {
        private static final long serialVersionUID = 95689302094837621L;
        private LocalInnerEvent property;

        public LocalEventSubA(LocalInnerEvent property) {
            this.property = property;
        }

        public LocalInnerEvent getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = -1319153580083315531L;
        public MyLocalJsonProvidedInnerEvent property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        private static final long serialVersionUID = -6680527239888218034L;
        public String id;
    }
}
