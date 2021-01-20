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
import com.espertech.esper.common.client.json.minimaljson.Json;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterNestedSimpleDeep implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<Nullable2Lvl> bean = val -> {
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
        Consumer<Nullable2Lvl> oa = val -> {
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
            Schema schema = env.runtimeAvroSchemaByDeployment("schema", "LocalEvent");
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
        env.assertThat(() -> runAssertion(env, getEpl("avro"), avro));
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<Nullable2Lvl> sender) {

        String epl = createSchemaEPL +
            "@name('s0') select * from LocalEvent;\n" +
            "@name('s1') select property.leaf.id as c0, exists(property.leaf.id) as c1, typeof(property.leaf.id) as c2 from LocalEvent;\n";
        env.compileDeploy(epl).addListener("s0").addListener("s1");

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

    private void assertProps(RegressionEnvironment env, boolean exists, String expected) {
        env.assertPropsNew("s1", "c0,c1,c2".split(","),
            new Object[]{expected, exists, expected != null ? String.class.getSimpleName() : null});
    }

    private void assertGetter(EventBean event, boolean exists, String value) {
        EventPropertyGetter getter = event.getEventType().getGetter("property.leaf.id");
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private String getEpl(String underlying) {
        return "@public @buseventtype create " + underlying + " schema LocalLeafEvent(id string);\n" +
            "@public @buseventtype create " + underlying + " schema LocalInnerEvent(leaf LocalLeafEvent);\n" +
            "@name('schema') @public @buseventtype create " + underlying + " schema LocalEvent(property LocalInnerEvent);\n";
    }

    public static class LocalLeafEvent implements Serializable {
        private static final long serialVersionUID = 1882923000981895938L;
        private final String id;

        public LocalLeafEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class LocalInnerEvent implements Serializable {
        private static final long serialVersionUID = 4682265371414981401L;
        private final LocalLeafEvent leaf;

        public LocalInnerEvent(LocalLeafEvent leaf) {
            this.leaf = leaf;
        }

        public LocalLeafEvent getLeaf() {
            return leaf;
        }
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = -1315383268249956930L;
        private LocalInnerEvent property;

        public LocalEvent(LocalInnerEvent property) {
            this.property = property;
        }

        public LocalInnerEvent getProperty() {
            return property;
        }
    }

    private static class Nullable2Lvl implements Serializable {
        private static final long serialVersionUID = 1175450645798351344L;
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
        private static final long serialVersionUID = -3507245469422648353L;
        public MyLocalJsonProvidedInnerEvent property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        private static final long serialVersionUID = 238505898641597341L;
        public MyLocalJsonProvidedLeafEvent leaf;
    }

    public static class MyLocalJsonProvidedLeafEvent implements Serializable {
        private static final long serialVersionUID = -4618532340851736222L;
        public String id;
    }
}
