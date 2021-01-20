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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterDynamicNestedDeep implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<Nullable2Lvl> bean = val -> {
            LocalEvent event;
            if (val.isNullAtRoot()) {
                event = new LocalEvent();
            } else if (val.isNullAtInner()) {
                event = new LocalEventSubA(new LocalInnerEvent(null));
            } else {
                event = new LocalEventSubA(new LocalInnerEvent(new LocalLeafEvent(val.id)));
            }
            env.sendEventBean(event, "LocalEvent");
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + EventInfraGetterDynamicNested.LocalEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEventSubA as " + EventInfraGetterDynamicNested.LocalEventSubA.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<Nullable2Lvl> map = val -> {
            Map<String, Object> event = new LinkedHashMap<>();
            if (val.isNullAtRoot()) {
                // no change
            } else if (val.isNullAtInner()) {
                Map<String, Object> inner = Collections.singletonMap("leaf", null);
                event.put("property", inner);
            } else {
                Map<String, Object> leaf = Collections.singletonMap("id", val.id);
                Map<String, Object> inner = Collections.singletonMap("leaf", leaf);
                event.put("property", inner);
            }
            env.sendEventMap(event, "LocalEvent");
        };
        runAssertion(env, getEpl("map"), map);

        // Object-array
        runAssertion(env, getEpl("objectarray"), null);

        // Json
        Consumer<Nullable2Lvl> json = val -> {
            JsonObject event = new JsonObject();
            if (val.isNullAtRoot()) {
                // no change
            } else if (val.isNullAtInner()) {
                event.add("property", new JsonObject().add("leaf", Json.NULL));
            } else {
                JsonObject leaf = new JsonObject().add("id", val.id);
                JsonObject inner = new JsonObject().add("leaf", leaf);
                event.add("property", inner);
            }
            env.sendEventJson(event.toString(), "LocalEvent");
        };
        runAssertion(env, getEpl("json"), json);

        // Json-Class-Provided
        String eplJsonProvided = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, eplJsonProvided, json);

        // Avro
        Consumer<Nullable2Lvl> avro = val -> {
            Schema emptySchema = SchemaBuilder.record("name").fields().endRecord();
            GenericData.Record event;
            if (val.isNullAtRoot()) {
                event = new GenericData.Record(emptySchema);
            } else if (val.isNullAtInner()) {
                GenericData.Record inner = new GenericData.Record(emptySchema);
                Schema topSchema = SchemaBuilder.record("name").fields().name("property").type(emptySchema).noDefault().endRecord();
                event = new GenericData.Record(topSchema);
                event.put("property", inner);
            } else {
                Schema leafSchema = SchemaBuilder.record("leaf").fields().name("id").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault().endRecord();
                Schema innerSchema = SchemaBuilder.record("inner").fields().name("leaf").type(leafSchema).noDefault().endRecord();
                Schema topSchema = SchemaBuilder.record("top").fields().name("property").type(innerSchema).noDefault().endRecord();
                GenericData.Record leaf = new GenericData.Record(leafSchema);
                leaf.put("id", val.id);
                GenericData.Record inner = new GenericData.Record(innerSchema);
                inner.put("leaf", leaf);
                event = new GenericData.Record(topSchema);
                event.put("property", inner);
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        env.assertThat(() -> runAssertion(env, getEpl("avro"), avro)); // Avro assertion localized for serialization of null values not according to schema
    }

    private String getEpl(String underlying) {
        return "@public @buseventtype @JsonSchema(dynamic=true) create " + underlying + " schema LocalEvent();\n";
    }

    private void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<Nullable2Lvl> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");

        if (sender == null) {
            env.assertStatement("s0", statement -> {
                EventType eventType = statement.getEventType();
                EventPropertyGetter g0 = eventType.getGetter("property?.leaf.id");
                assertNull(g0);
            });
            env.undeployAll();
            return;
        }

        String propepl = "@name('s1') select property?.leaf.id as c0, exists(property?.leaf.id) as c1, typeof(property?.leaf.id) as c2 from LocalEvent;\n";
        env.compileDeploy(propepl, path).addListener("s1");

        sender.accept(new Nullable2Lvl(false, false, "a"));
        env.assertEventNew("s0", event -> assertGetter(event, true, "a"));
        assertProps(env, true, "a");

        sender.accept(new Nullable2Lvl(false, false, null));
        env.assertEventNew("s0", event -> assertGetter(event, true, null));
        assertProps(env, true, null);

        sender.accept(new Nullable2Lvl(false, true, null));
        env.assertEventNew("s0", event -> assertGetter(event, false, null));
        assertProps(env, false, null);

        sender.accept(new Nullable2Lvl(true, false, null));
        env.assertEventNew("s0", event -> assertGetter(event, false, null));
        assertProps(env, false, null);

        env.undeployAll();
    }

    private void assertGetter(EventBean event, boolean exists, String value) {
        EventPropertyGetter getter = event.getEventType().getGetter("property?.leaf.id");
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

    public static class LocalLeafEvent implements Serializable {
        private static final long serialVersionUID = 7234619046709300355L;
        private final String id;

        public LocalLeafEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class LocalInnerEvent implements Serializable {
        private static final long serialVersionUID = 5426780299052174694L;
        private final LocalLeafEvent leaf;

        public LocalInnerEvent(LocalLeafEvent leaf) {
            this.leaf = leaf;
        }

        public LocalLeafEvent getLeaf() {
            return leaf;
        }
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = 3358018943773479873L;
    }

    public static class LocalEventSubA extends LocalEvent {
        private static final long serialVersionUID = -2328210779006348506L;
        private LocalInnerEvent property;

        public LocalEventSubA(LocalInnerEvent property) {
            this.property = property;
        }

        public LocalInnerEvent getProperty() {
            return property;
        }
    }

    private static class Nullable2Lvl implements Serializable {
        private static final long serialVersionUID = -6248581197548229626L;
        private final boolean nullAtRoot;
        private final boolean nullAtInner;
        private final String id;

        public Nullable2Lvl(boolean nullAtRoot, boolean nullAtInner, String id) {
            this.nullAtRoot = nullAtRoot;
            this.nullAtInner = nullAtInner;
            this.id = id;
        }

        public boolean isNullAtRoot() {
            return nullAtRoot;
        }

        public boolean isNullAtInner() {
            return nullAtInner;
        }

        public String getId() {
            return id;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = -975155961980154782L;
        public EventInfraGetterNestedSimpleDeep.MyLocalJsonProvidedInnerEvent property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        private static final long serialVersionUID = -258817098392586181L;
        public EventInfraGetterNestedSimpleDeep.MyLocalJsonProvidedLeafEvent leaf;
    }

    public static class MyLocalJsonProvidedLeafEvent implements Serializable {
        private static final long serialVersionUID = 2709285016679843872L;
        public String id;
    }
}
