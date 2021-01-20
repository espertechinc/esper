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

import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

public class EventInfraPropertyIndexedRuntimeIndex implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<String[]> bean = values -> {
            env.sendEventBean(new LocalEvent(values));
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<String[]> map = values -> {
            env.sendEventMap(Collections.singletonMap("indexed", values), "LocalEvent");
        };
        String mapepl = "@public @buseventtype create schema LocalEvent(indexed string[]);\n";
        runAssertion(env, mapepl, map);

        // Object-array
        Consumer<String[]> oa = values -> {
            env.sendEventObjectArray(new Object[]{values}, "LocalEvent");
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent(indexed string[]);\n";
        runAssertion(env, oaepl, oa);

        // Json
        Consumer<String[]> json = values -> {
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
        Consumer<String[]> avro = values -> {
            Schema schema = env.runtimeAvroSchemaByDeployment("schema", "LocalEvent");
            GenericData.Record event = new GenericData.Record(schema);
            event.put("indexed", Arrays.asList(values));
            env.sendEventAvro(event, "LocalEvent");
        };
        String avroepl = "@name('schema') @public @buseventtype create avro schema LocalEvent(indexed string[]);\n";
        runAssertion(env, avroepl, avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<String[]> sender) {

        env.compileDeploy(createSchemaEPL +
            "create constant variable int offsetNum = 0;" +
            "@name('s0') select indexed(offsetNum+0) as c0, indexed(offsetNum+1) as c1 from LocalEvent as e;\n"
        ).addListener("s0");

        sender.accept(new String[]{"a", "b"});
        env.assertPropsNew("s0", "c0,c1".split(","), new Object[]{"a", "b"});

        env.undeployAll();
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = 5671940595663495994L;
        private String[] indexed;

        public LocalEvent(String[] indexed) {
            this.indexed = indexed;
        }

        public String[] getIndexed() {
            return indexed;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = -6111414022743294114L;
        public String[] indexed;
    }
}
