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
import java.util.Map;
import java.util.function.Consumer;

public class EventInfraContainedNested implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<String> bean = id -> {
            env.sendEventBean(new LocalEvent(new LocalInnerEvent(new LocalLeafEvent(id))));
        };
        String beanepl = "@public @buseventtype create schema LocalLeafEvent as " + LocalLeafEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalInnerEvent as " + LocalInnerEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<String> map = id -> {
            Map<String, Object> leaf = Collections.singletonMap("id", id);
            Map<String, Object> inner = Collections.singletonMap("leaf", leaf);
            env.sendEventMap(Collections.singletonMap("property", inner), "LocalEvent");
        };
        runAssertion(env, getEpl("map"), map);

        // Object-array
        Consumer<String> oa = id -> {
            Object[] leaf = new Object[]{id};
            Object[] inner = new Object[]{leaf};
            env.sendEventObjectArray(new Object[]{inner}, "LocalEvent");
        };
        runAssertion(env, getEpl("objectarray"), oa);

        // Json
        Consumer<String> json = id -> {
            JsonObject leaf = new JsonObject().add("id", id);
            JsonObject inner = new JsonObject().add("leaf", leaf);
            JsonObject event = new JsonObject().add("property", inner);
            env.sendEventJson(event.toString(), "LocalEvent");
        };
        runAssertion(env, getEpl("json"), json);

        // Json-Class-Provided
        String eplJsonProvided = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, eplJsonProvided, json);

        // Avro
        Consumer<String> avro = id -> {
            Schema schema = env.runtimeAvroSchemaByDeployment("schema", "LocalEvent");
            GenericData.Record leaf = new GenericData.Record(schema.getField("property").schema().getField("leaf").schema());
            leaf.put("id", id);
            GenericData.Record inner = new GenericData.Record(schema.getField("property").schema());
            inner.put("leaf", leaf);
            GenericData.Record event = new GenericData.Record(schema);
            event.put("property", inner);
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, getEpl("avro"), avro);
    }

    private String getEpl(String underlying) {
        return "create " + underlying + " schema LocalLeafEvent(id string);\n" +
            "create " + underlying + " schema LocalInnerEvent(leaf LocalLeafEvent);\n" +
            "@name('schema') @public @buseventtype create " + underlying + " schema LocalEvent(property LocalInnerEvent);\n";
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<String> sender) {

        env.compileDeploy(createSchemaEPL + "@name('s0') select * from LocalEvent[property.leaf];\n").addListener("s0");

        sender.accept("a");
        env.assertEqualsNew("s0", "id", "a");

        env.undeployAll();
    }

    public static class LocalLeafEvent implements Serializable {
        private static final long serialVersionUID = 7779087975554576170L;
        private final String id;

        public LocalLeafEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class LocalInnerEvent implements Serializable {
        private static final long serialVersionUID = -4889977768503399480L;
        private final LocalLeafEvent leaf;

        public LocalInnerEvent(LocalLeafEvent leaf) {
            this.leaf = leaf;
        }

        public LocalLeafEvent getLeaf() {
            return leaf;
        }
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = -4864551098528083770L;
        private LocalInnerEvent property;

        public LocalEvent(LocalInnerEvent property) {
            this.property = property;
        }

        public LocalInnerEvent getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = 8684584945249862087L;
        public MyLocalJsonProvidedInnerEvent property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        private static final long serialVersionUID = -8735380574705031430L;
        public MyLocalJsonProvidedLeafEvent leaf;
    }

    public static class MyLocalJsonProvidedLeafEvent implements Serializable {
        private static final long serialVersionUID = -1395024403102032101L;
        public String id;
    }
}
