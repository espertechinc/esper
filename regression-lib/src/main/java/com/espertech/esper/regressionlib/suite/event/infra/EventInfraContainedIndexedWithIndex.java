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
import com.espertech.esper.common.internal.avro.core.AvroConstant;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;

public class EventInfraContainedIndexedWithIndex implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, String[]> bean = (type, ids) -> {
            LocalInnerEvent[] inners = new LocalInnerEvent[ids.length];
            for (int i = 0; i < ids.length; i++) {
                inners[i] = new LocalInnerEvent(ids[i]);
            }
            env.sendEventBean(new LocalEvent(inners));
        };
        String beanepl = "@public @buseventtype create schema LocalInnerEvent as " + LocalInnerEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, String[]> map = (type, ids) -> {
            Map[] inners = new Map[ids.length];
            for (int i = 0; i < ids.length; i++) {
                inners[i] = Collections.singletonMap("id", ids[i]);
            }
            env.sendEventMap(Collections.singletonMap("indexed", inners), "LocalEvent");
        };
        String mapepl = "@public @buseventtype create schema LocalInnerEvent(id string);\n" +
            "@public @buseventtype create schema LocalEvent(indexed LocalInnerEvent[]);\n";
        runAssertion(env, mapepl, map);

        // Object-array
        BiConsumer<EventType, String[]> oa = (type, ids) -> {
            Object[][] inners = new Object[ids.length][];
            for (int i = 0; i < ids.length; i++) {
                inners[i] = new Object[]{ids[i]};
            }
            env.sendEventObjectArray(new Object[]{inners}, "LocalEvent");
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalInnerEvent(id string);\n" +
            "@public @buseventtype create objectarray schema LocalEvent(indexed LocalInnerEvent[]);\n";
        runAssertion(env, oaepl, oa);

        // Json
        BiConsumer<EventType, String[]> json = (type, ids) -> {
            JsonArray array = new JsonArray();
            for (int i = 0; i < ids.length; i++) {
                array.add(new JsonObject().add("id", ids[i]));
            }
            JsonObject event = new JsonObject().add("indexed", array);
            env.sendEventJson(event.toString(), "LocalEvent");
        };
        String jsonepl = "@public @buseventtype create json schema LocalInnerEvent(id string);\n" +
            "@public @buseventtype create json schema LocalEvent(indexed LocalInnerEvent[]);\n";
        runAssertion(env, jsonepl, json);

        // Json-Class-Provided
        String jsonProvidedEpl = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, jsonProvidedEpl, json);

        // Avro
        BiConsumer<EventType, String[]> avro = (type, ids) -> {
            Schema schema = SchemaBuilder.record("name").fields()
                .name("id").type(SchemaBuilder.builder().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
                .endRecord();
            Collection inners = new ArrayList();
            for (int i = 0; i < ids.length; i++) {
                GenericData.Record inner = new GenericData.Record(schema);
                inner.put("id", ids[i]);
                inners.add(inner);
            }
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(type));
            event.put("indexed", inners);
            env.sendEventAvro(event, "LocalEvent");
        };
        String avroepl = "@public @buseventtype create avro schema LocalInnerEvent(id string);\n" +
            "@public @buseventtype create avro schema LocalEvent(indexed LocalInnerEvent[]);\n";
        runAssertion(env, avroepl, avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, String[]> sender) {

        env.compileDeploy(createSchemaEPL +
            "@name('s0') select * from LocalEvent[indexed[0]];\n" +
            "@name('s1') select * from LocalEvent[indexed[1]];\n"
        ).addListener("s0").addListener("s1");
        EventType eventType = env.runtime().getEventTypeService().getEventType(env.deploymentId("s0"), "LocalEvent");

        sender.accept(eventType, new String[]{"a", "b"});
        assertEquals("a", env.listener("s0").assertOneGetNewAndReset().get("id"));
        assertEquals("b", env.listener("s1").assertOneGetNewAndReset().get("id"));

        env.undeployAll();
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
        private LocalInnerEvent[] indexed;

        public LocalEvent(LocalInnerEvent[] indexed) {
            this.indexed = indexed;
        }

        public LocalInnerEvent[] getIndexed() {
            return indexed;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public MyLocalJsonProvidedInnerEvent[] indexed;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        public String id;
    }
}
