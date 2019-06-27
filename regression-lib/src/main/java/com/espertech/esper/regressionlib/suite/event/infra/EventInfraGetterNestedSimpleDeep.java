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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterNestedSimpleDeep implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, Nullable2Lvl> bean = (type, val) -> {
            LocalEvent event;
            if (val.isNullAtRoot()) {
                event = new LocalEvent(null);
            } else if (val.isNullAtInner()) {
                event = new LocalEvent(new LocalInnerEvent(null));
            } else {
                event = new LocalEvent(new LocalInnerEvent(new LocalLeafEvent(val.id)));
            }
            env.sendEventBean(event);
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, Nullable2Lvl> map = (type, val) -> {
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
        BiConsumer<EventType, Nullable2Lvl> oa = (type, val) -> {
            Object[] event = new Object[1];
            if (val.isNullAtRoot()) {
                // no change
            } else if (val.isNullAtInner()) {
                Object[] inner = new Object[]{null};
                event[0] = inner;
            } else {
                Object[] leaf = new Object[]{val.id};
                Object[] inner = new Object[]{leaf};
                event[0] = inner;
            }
            env.sendEventObjectArray(event, "LocalEvent");
        };
        runAssertion(env, getEpl("objectarray"), oa);

        // Json
        BiConsumer<EventType, Nullable2Lvl> json = (type, val) -> {
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
        BiConsumer<EventType, Nullable2Lvl> avro = (type, val) -> {
            Schema schema = SupportAvroUtil.getAvroSchema(type);
            GenericData.Record event = new GenericData.Record(schema);
            if (val.isNullAtRoot()) {
                // no change
            } else if (val.isNullAtInner()) {
                GenericData.Record inner = new GenericData.Record(schema.getField("property").schema());
                event.put("property", inner);
            } else {
                GenericData.Record leaf = new GenericData.Record(schema.getField("property").schema().getField("leaf").schema());
                leaf.put("id", val.id);
                GenericData.Record inner = new GenericData.Record(schema.getField("property").schema());
                inner.put("leaf", leaf);
                event.put("property", inner);
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, getEpl("avro"), avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, Nullable2Lvl> sender) {

        String epl = createSchemaEPL +
            "@name('s0') select * from LocalEvent;\n" +
            "@name('s1') select property.leaf.id as c0, exists(property.leaf.id) as c1, typeof(property.leaf.id) as c2 from LocalEvent;\n";
        env.compileDeploy(epl).addListener("s0").addListener("s1");
        EventType eventType = env.statement("s0").getEventType();

        EventPropertyGetter g0 = eventType.getGetter("property.leaf.id");

        sender.accept(eventType, new Nullable2Lvl(false, false, "a"));
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, "a");
        assertProps(env, true, "a");

        sender.accept(eventType, new Nullable2Lvl(false, false, null));
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, null);
        assertProps(env, true, null);

        sender.accept(eventType, new Nullable2Lvl(false, true, null));
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, false, null);
        assertProps(env, false, null);

        sender.accept(eventType, new Nullable2Lvl(true, false, null));
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
        return "@public @buseventtype create " + underlying + " schema LocalLeafEvent(id string);\n" +
            "@public @buseventtype create " + underlying + " schema LocalInnerEvent(leaf LocalLeafEvent);\n" +
            "@public @buseventtype create " + underlying + " schema LocalEvent(property LocalInnerEvent);\n";
    }

    public static class LocalLeafEvent {
        private final String id;

        public LocalLeafEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class LocalInnerEvent {
        private final LocalLeafEvent leaf;

        public LocalInnerEvent(LocalLeafEvent leaf) {
            this.leaf = leaf;
        }

        public LocalLeafEvent getLeaf() {
            return leaf;
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

    private static class Nullable2Lvl {
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
        public MyLocalJsonProvidedInnerEvent property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        public MyLocalJsonProvidedLeafEvent leaf;
    }

    public static class MyLocalJsonProvidedLeafEvent implements Serializable {
        public String id;
    }
}
