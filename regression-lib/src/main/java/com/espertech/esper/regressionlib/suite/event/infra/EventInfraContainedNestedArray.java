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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;

public class EventInfraContainedNestedArray implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, String[]> bean = (type, ids) -> {
            LocalInnerEvent[] property = new LocalInnerEvent[ids.length];
            for (int i = 0; i < ids.length; i++) {
                property[i] = new LocalInnerEvent(new LocalLeafEvent(ids[i]));
            }
            env.sendEventBean(new LocalEvent(property));
        };
        String beanepl = "@public @buseventtype create schema LocalLeafEvent as " + LocalLeafEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalInnerEvent as " + LocalInnerEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, String[]> map = (type, ids) -> {
            Map[] property = new Map[ids.length];
            for (int i = 0; i < ids.length; i++) {
                property[i] = Collections.singletonMap("leaf", Collections.singletonMap("id", ids[i]));
            }
            env.sendEventMap(Collections.singletonMap("property", property), "LocalEvent");
        };
        runAssertion(env, getEpl("map"), map);

        // Object-array
        BiConsumer<EventType, String[]> oa = (type, ids) -> {
            Object[][] property = new Object[ids.length][];
            for (int i = 0; i < ids.length; i++) {
                property[i] = new Object[]{new Object[]{ids[i]}};
            }
            env.sendEventObjectArray(new Object[]{property}, "LocalEvent");
        };
        runAssertion(env, getEpl("objectarray"), oa);

        // Json
        BiConsumer<EventType, String[]> json = (type, ids) -> {
            JsonArray property = new JsonArray();
            for (int i = 0; i < ids.length; i++) {
                JsonObject inner = new JsonObject().add("leaf", new JsonObject().add("id", ids[i]));
                property.add(inner);
            }
            env.sendEventJson(new JsonObject().add("property", property).toString(), "LocalEvent");
        };
        runAssertion(env, getEpl("json"), json);

        // Json-Class-Provided
        String eplJsonProvided = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, eplJsonProvided, json);

        // Avro
        BiConsumer<EventType, String[]> avro = (type, ids) -> {
            Schema schema = SupportAvroUtil.getAvroSchema(type);
            Collection property = new ArrayList();
            for (int i = 0; i < ids.length; i++) {
                GenericData.Record leaf = new GenericData.Record(schema.getField("property").schema().getElementType().getField("leaf").schema());
                leaf.put("id", ids[i]);
                GenericData.Record inner = new GenericData.Record(schema.getField("property").schema().getElementType());
                inner.put("leaf", leaf);
                property.add(inner);
            }
            GenericData.Record event = new GenericData.Record(schema);
            event.put("property", property);
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, getEpl("avro"), avro);
    }

    private String getEpl(String underlying) {
        return "create " + underlying + " schema LocalLeafEvent(id string);\n" +
            "create " + underlying + " schema LocalInnerEvent(leaf LocalLeafEvent);\n" +
            "@public @buseventtype create " + underlying + " schema LocalEvent(property LocalInnerEvent[]);\n";
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, String[]> sender) {

        env.compileDeploy(createSchemaEPL +
            "@name('s0') select * from LocalEvent[property[0].leaf];\n" +
            "@name('s1') select * from LocalEvent[property[1].leaf];\n").addListener("s0").addListener("s1");
        EventType eventType = env.runtime().getEventTypeService().getEventType(env.deploymentId("s0"), "LocalEvent");

        sender.accept(eventType, "a,b".split(","));
        assertEquals("a", env.listener("s0").assertOneGetNewAndReset().get("id"));
        assertEquals("b", env.listener("s1").assertOneGetNewAndReset().get("id"));

        env.undeployAll();
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
        private LocalInnerEvent[] property;

        public LocalEvent(LocalInnerEvent[] property) {
            this.property = property;
        }

        public LocalInnerEvent[] getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public MyLocalJsonProvidedInnerEvent[] property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        public MyLocalJsonProvidedLeafEvent leaf;
    }

    public static class MyLocalJsonProvidedLeafEvent implements Serializable {
        public String id;
    }
}
