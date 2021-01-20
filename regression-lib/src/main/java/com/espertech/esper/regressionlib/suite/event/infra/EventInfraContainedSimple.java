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
import com.espertech.esper.common.internal.avro.core.AvroConstant;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.function.Consumer;

public class EventInfraContainedSimple implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<String> bean = id -> {
            env.sendEventBean(new LocalEvent(new LocalInnerEvent(id)));
        };
        String beanepl = "@public @buseventtype create schema LocalInnerEvent as " + LocalInnerEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<String> map = id -> {
            env.sendEventMap(Collections.singletonMap("property", Collections.singletonMap("id", id)), "LocalEvent");
        };
        String mapepl = "@public @buseventtype create schema LocalInnerEvent(id string);\n" +
            "@public @buseventtype create schema LocalEvent(property LocalInnerEvent);\n";
        runAssertion(env, mapepl, map);

        // Object-array
        Consumer<String> oa = id -> {
            env.sendEventObjectArray(new Object[]{new Object[]{id}}, "LocalEvent");
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalInnerEvent(id string);\n" +
            "@public @buseventtype create objectarray schema LocalEvent(property LocalInnerEvent);\n";
        runAssertion(env, oaepl, oa);

        // Json
        Consumer<String> json = id -> {
            JsonObject event = new JsonObject().add("property", new JsonObject().add("id", id));
            env.sendEventJson(event.toString(), "LocalEvent");
        };
        String jsonepl = "@public @buseventtype create json schema LocalInnerEvent(id string);\n" +
            "@public @buseventtype create json schema LocalEvent(property LocalInnerEvent);\n";
        runAssertion(env, jsonepl, json);

        // Json-Class-Provided
        String jsonProvidedEpl = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, jsonProvidedEpl, json);

        // Avro
        Consumer<String> avro = id -> {
            Schema schema = SchemaBuilder.record("name").fields()
                .name("id").type(SchemaBuilder.builder().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
                .endRecord();
            GenericData.Record inside = new GenericData.Record(schema);
            inside.put("id", id);
            Schema schemaEvent = env.runtimeAvroSchemaByDeployment("schema", "LocalEvent");
            GenericData.Record event = new GenericData.Record(schemaEvent);
            event.put("property", inside);
            env.sendEventAvro(event, "LocalEvent");
        };
        String avroepl = "@name('schema') @public @buseventtype create avro schema LocalInnerEvent(id string);\n" +
            "@public @buseventtype create avro schema LocalEvent(property LocalInnerEvent);\n";
        runAssertion(env, avroepl, avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<String> sender) {

        env.compileDeploy(createSchemaEPL + "@name('s0') select * from LocalEvent[property];\n").addListener("s0");

        sender.accept("a");
        env.assertEqualsNew("s0", "id", "a");

        env.undeployAll();
    }

    public static class LocalInnerEvent implements Serializable {
        private static final long serialVersionUID = -8402717095861140133L;
        private final String id;

        public LocalInnerEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = -3833339455439261422L;
        private LocalInnerEvent property;

        public LocalEvent(LocalInnerEvent property) {
            this.property = property;
        }

        public LocalInnerEvent getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = 8150364130999059884L;
        public MyLocalJsonProvidedInnerEvent property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        private static final long serialVersionUID = 6257529978131050247L;
        public String id;
    }
}
