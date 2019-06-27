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
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class EventInfraPropertyMappedRuntimeKey implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, Map<String, String>> bean = (type, entries) -> {
            env.sendEventBean(new LocalEvent(entries));
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, Map<String, String>> map = (type, entries) -> {
            env.sendEventMap(Collections.singletonMap("mapped", entries), "LocalEvent");
        };
        String mapepl = "@public @buseventtype create schema LocalEvent(mapped java.util.Map);\n";
        runAssertion(env, mapepl, map);

        // Object-array
        BiConsumer<EventType, Map<String, String>> oa = (type, entries) -> {
            env.sendEventObjectArray(new Object[]{entries}, "LocalEvent");
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent(mapped java.util.Map);\n";
        runAssertion(env, oaepl, oa);

        // Json
        BiConsumer<EventType, Map<String, String>> json = (type, entries) -> {
            JsonObject mapValues = new JsonObject();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                mapValues.add(entry.getKey(), entry.getValue());
            }
            JsonObject event = new JsonObject().add("mapped", mapValues);
            env.sendEventJson(event.toString(), "LocalEvent");
        };
        String jsonepl = "@public @buseventtype create json schema LocalEvent(mapped java.util.Map);\n";
        runAssertion(env, jsonepl, json);

        // Json-Class-Provided
        String jsonProvidedEpl = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, jsonProvidedEpl, json);

        // Avro
        BiConsumer<EventType, Map<String, String>> avro = (type, entries) -> {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(type));
            event.put("mapped", entries);
            env.sendEventAvro(event, "LocalEvent");
        };
        String avroepl = "@public @buseventtype create avro schema LocalEvent(mapped java.util.Map);\n";
        runAssertion(env, avroepl, avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, Map<String, String>> sender) {

        env.compileDeploy(createSchemaEPL +
            "create constant variable string keyChar = 'a';" +
            "@name('s0') select mapped(keyChar||'1') as c0, mapped(keyChar||'2') as c1 from LocalEvent as e;\n"
        ).addListener("s0");
        EventType eventType = env.runtime().getEventTypeService().getEventType(env.deploymentId("s0"), "LocalEvent");

        Map<String, String> values = new HashMap<>();
        values.put("a1", "x");
        values.put("a2", "y");
        sender.accept(eventType, values);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{"x", "y"});

        env.undeployAll();
    }

    public static class LocalEvent {
        private Map<String, String> mapped;

        public LocalEvent(Map<String, String> mapped) {
            this.mapped = mapped;
        }

        public Map<String, String> getMapped() {
            return mapped;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public Map<String, String> mapped;
    }
}
