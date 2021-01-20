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
import com.espertech.esper.common.internal.util.NullableObject;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterNestedSimple implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<NullableObject<String>> bean = nullable -> {
            LocalInnerEvent property = nullable == null ? null : new LocalInnerEvent(nullable.getObject());
            env.sendEventBean(new LocalEvent(property));
        };
        String beanepl = "@public @buseventtype create schema LocalInnerEvent as " + LocalInnerEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<NullableObject<String>> map = nullable -> {
            Map<String, Object> property = nullable == null ? null : Collections.singletonMap("id", nullable.getObject());
            env.sendEventMap(Collections.singletonMap("property", property), "LocalEvent");
        };
        runAssertion(env, getEpl("map"), map);

        // Object-array
        Consumer<NullableObject<String>> oa = nullable -> {
            Object[] property = nullable == null ? null : new Object[]{nullable.getObject()};
            env.sendEventObjectArray(new Object[]{property}, "LocalEvent");
        };
        runAssertion(env, getEpl("objectarray"), oa);

        // Json
        Consumer<NullableObject<String>> json = nullable -> {
            JsonObject event = new JsonObject();
            if (nullable != null) {
                if (nullable.getObject() != null) {
                    event.add("property", new JsonObject().add("id", nullable.getObject()));
                } else {
                    event.add("property", new JsonObject().add("id", Json.NULL));
                }
            }
            env.sendEventJson(event.toString(), "LocalEvent");
        };
        runAssertion(env, getEpl("json"), json);

        // Json-Class-Provided
        String eplJsonProvided = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, eplJsonProvided, json);

        // Avro
        Consumer<NullableObject<String>> avro = nullable -> {
            Schema schema = env.runtimeAvroSchemaByDeployment("schema", "LocalEvent");
            GenericData.Record event = new GenericData.Record(schema);
            if (nullable != null) {
                GenericData.Record inside = new GenericData.Record(schema.getField("property").schema());
                inside.put("id", nullable.getObject());
                event.put("property", inside);
            }
            env.sendEventAvro(event, "LocalEvent");
        };
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<NullableObject<String>> sender) {

        String epl = createSchemaEPL +
            "@name('s0') select * from LocalEvent;\n" +
            "@name('s1') select property.id as c0, exists(property.id) as c1, typeof(property.id) as c2 from LocalEvent;\n";
        env.compileDeploy(epl).addListener("s0").addListener("s1");

        sender.accept(new NullableObject<>("a"));
        env.assertEventNew("s0", event -> assertGetter(event, true, "a"));
        assertProps(env, true, "a");

        sender.accept(new NullableObject<>(null));
        env.assertEventNew("s0", event -> assertGetter(event, true, null));
        assertProps(env, true, null);

        sender.accept(null);
        env.assertEventNew("s0", event -> assertGetter(event, false, null));
        assertProps(env, false, null);

        env.undeployAll();
    }

    private void assertProps(RegressionEnvironment env, boolean exists, String expected) {
        env.assertPropsNew("s1", "c0,c1,c2".split(","),
            new Object[]{expected, exists, expected != null ? String.class.getSimpleName() : null});
    }

    private void assertGetter(EventBean event, boolean exists, String value) {
        EventPropertyGetter getter = event.getEventType().getGetter("property.id");
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private String getEpl(String underlying) {
        return "@public @buseventtype create " + underlying + " schema LocalInnerEvent(id string);\n" +
            "@name('schema') @public @buseventtype create " + underlying + " schema LocalEvent(property LocalInnerEvent);\n";
    }

    public static class LocalInnerEvent implements Serializable {
        private static final long serialVersionUID = -3970473956449626105L;
        private final String id;

        public LocalInnerEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = -2633257367226451562L;
        private LocalInnerEvent property;

        public LocalEvent(LocalInnerEvent property) {
            this.property = property;
        }

        public LocalInnerEvent getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = 6626899018998784774L;
        public MyLocalJsonProvidedInnerEvent property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        private static final long serialVersionUID = -8371946832426034453L;
        public String id;
    }
}
