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

import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EventInfraPropertyMappedRuntimeKey implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<Map<String, String>> bean = entries -> {
            env.sendEventBean(new LocalEvent(entries));
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<Map<String, String>> map = entries -> {
            env.sendEventMap(Collections.singletonMap("mapped", entries), "LocalEvent");
        };
        String mapepl = "@public @buseventtype create schema LocalEvent(mapped java.util.Map);\n";
        runAssertion(env, mapepl, map);

        // Object-array
        Consumer<Map<String, String>> oa = entries -> {
            env.sendEventObjectArray(new Object[]{entries}, "LocalEvent");
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent(mapped java.util.Map);\n";
        runAssertion(env, oaepl, oa);

        // Json
        Consumer<Map<String, String>> json = entries -> {
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
        Consumer<Map<String, String>> avro = entries -> {
            Schema schema = env.runtimeAvroSchemaByDeployment("schema", "LocalEvent");
            GenericData.Record event = new GenericData.Record(schema);
            event.put("mapped", entries);
            env.sendEventAvro(event, "LocalEvent");
        };
        String avroepl = "@name('schema') @public @buseventtype create avro schema LocalEvent(mapped java.util.Map);\n";
        runAssertion(env, avroepl, avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<Map<String, String>> sender) {

        env.compileDeploy(createSchemaEPL +
            "create constant variable string keyChar = 'a';" +
            "@name('s0') select mapped(keyChar||'1') as c0, mapped(keyChar||'2') as c1 from LocalEvent as e;\n"
        ).addListener("s0");

        Map<String, String> values = new HashMap<>();
        values.put("a1", "x");
        values.put("a2", "y");
        sender.accept(values);
        env.assertPropsNew("s0", "c0,c1".split(","), new Object[]{"x", "y"});

        env.undeployAll();
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = 1684059170777937635L;
        private Map<String, String> mapped;

        public LocalEvent(Map<String, String> mapped) {
            this.mapped = mapped;
        }

        public Map<String, String> getMapped() {
            return mapped;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = -4485185602486101826L;
        public Map<String, String> mapped;
    }
}
