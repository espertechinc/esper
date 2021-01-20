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
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterMapped implements RegressionExecution {
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
            if (entries == null) {
                env.sendEventJson(new JsonObject().add("mapped", Json.NULL).toString(), "LocalEvent");
            } else {
                JsonObject event = new JsonObject();
                JsonObject mapped = new JsonObject();
                event.add("mapped", mapped);
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    mapped.add(entry.getKey(), entry.getValue());
                }
                env.sendEventJson(event.toString(), "LocalEvent");
            }
        };
        runAssertion(env, "@public @buseventtype @JsonSchema(dynamic=true) create json schema LocalEvent(mapped java.util.Map);\n", json);

        // Json-Class-Provided
        runAssertion(env, "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n", json);

        // Avro
        Consumer<Map<String, String>> avro = entries -> {
            Schema schema = env.runtimeAvroSchemaByDeployment("schema", "LocalEvent");
            GenericData.Record event = new GenericData.Record(schema);
            event.put("mapped", entries == null ? Collections.emptyMap() : entries);
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, "@name('schema') @public @buseventtype create avro schema LocalEvent(mapped java.util.Map);\n", avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<Map<String, String>> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");

        String propepl = "@name('s1') select mapped('a') as c0, mapped('b') as c1," +
            "exists(mapped('a')) as c2, exists(mapped('b')) as c3, " +
            "typeof(mapped('a')) as c4, typeof(mapped('b')) as c5 from LocalEvent;\n";
        env.compileDeploy(propepl, path).addListener("s1");

        Map<String, String> values = new HashMap<>();
        values.put("a", "x");
        values.put("b", "y");
        sender.accept(values);
        env.assertEventNew("s0", event -> assertGetters(event, true, "x", true, "y"));
        assertProps(env, "x", "y");

        sender.accept(Collections.singletonMap("a", "x"));
        env.assertEventNew("s0", event -> assertGetters(event, true, "x", false, null));
        assertProps(env, "x", null);

        sender.accept(Collections.emptyMap());
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, null, null);

        sender.accept(null);
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, null, null);

        sender.accept(null);
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, null, null);

        env.undeployAll();
    }

    private void assertGetters(EventBean event, boolean existsZero, String valueZero, boolean existsOne, String valueOne) {
        EventPropertyGetter g0 = event.getEventType().getGetter("mapped('a')");
        EventPropertyGetter g1 = event.getEventType().getGetter("mapped('b')");
        assertGetter(event, g0, existsZero, valueZero);
        assertGetter(event, g1, existsOne, valueOne);
    }

    private void assertGetter(EventBean event, EventPropertyGetter getter, boolean exists, String value) {
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private void assertProps(RegressionEnvironment env, String valueA, String valueB) {
        env.assertEventNew("s1", event -> {
            assertEquals(valueA, event.get("c0"));
            assertEquals(valueB, event.get("c1"));
            assertEquals(valueA != null, event.get("c2"));
            assertEquals(valueB != null, event.get("c3"));
            assertEquals(valueA == null ? null : "String", event.get("c4"));
            assertEquals(valueB == null ? null : "String", event.get("c5"));
        });
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = -5089568739529204564L;
        private Map<String, String> mapped;

        public LocalEvent(Map<String, String> mapped) {
            this.mapped = mapped;
        }

        public Map<String, String> getMapped() {
            return mapped;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = 1826654020623258315L;
        public Map<String, String> mapped;
    }
}
