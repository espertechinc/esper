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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiConsumer;

public class EventInfraPropertyIndexedRuntimeIndex implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, String[]> bean = (type, values) -> {
            env.sendEventBean(new LocalEvent(values));
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, String[]> map = (type, values) -> {
            env.sendEventMap(Collections.singletonMap("indexed", values), "LocalEvent");
        };
        String mapepl = "@public @buseventtype create schema LocalEvent(indexed string[]);\n";
        runAssertion(env, mapepl, map);

        // Object-array
        BiConsumer<EventType, String[]> oa = (type, values) -> {
            env.sendEventObjectArray(new Object[]{values}, "LocalEvent");
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent(indexed string[]);\n";
        runAssertion(env, oaepl, oa);

        // Json
        BiConsumer<EventType, String[]> json = (type, values) -> {
            JsonArray array = new JsonArray();
            for (int i = 0; i < values.length; i++) {
                array.add(values[i]);
            }
            JsonObject event = new JsonObject().add("indexed", array);
            env.sendEventJson(event.toString(), "LocalEvent");
        };
        String jsonepl = "@public @buseventtype create json schema LocalEvent(indexed string[]);\n";
        runAssertion(env, jsonepl, json);

        // Json-Class-Provided
        String jsonProvidedEpl = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, jsonProvidedEpl, json);

        // Avro
        BiConsumer<EventType, String[]> avro = (type, ids) -> {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(type));
            event.put("indexed", Arrays.asList(ids));
            env.sendEventAvro(event, "LocalEvent");
        };
        String avroepl = "@public @buseventtype create avro schema LocalEvent(indexed string[]);\n";
        runAssertion(env, avroepl, avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, String[]> sender) {

        env.compileDeploy(createSchemaEPL +
            "create constant variable int offsetNum = 0;" +
            "@name('s0') select indexed(offsetNum+0) as c0, indexed(offsetNum+1) as c1 from LocalEvent as e;\n"
        ).addListener("s0");
        EventType eventType = env.runtime().getEventTypeService().getEventType(env.deploymentId("s0"), "LocalEvent");

        sender.accept(eventType, new String[]{"a", "b"});
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{"a", "b"});

        env.undeployAll();
    }

    public static class LocalEvent {
        private String[] indexed;

        public LocalEvent(String[] indexed) {
            this.indexed = indexed;
        }

        public String[] getIndexed() {
            return indexed;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public String[] indexed;
    }
}
