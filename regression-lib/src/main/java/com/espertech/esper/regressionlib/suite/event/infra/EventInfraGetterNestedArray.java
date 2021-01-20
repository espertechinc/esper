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
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.json.minimaljson.JsonValue;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterNestedArray implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<String[]> bean = array -> {
            LocalInnerEvent[] property;
            if (array == null) {
                property = null;
            } else {
                property = new LocalInnerEvent[array.length];
                for (int i = 0; i < array.length; i++) {
                    property[i] = new LocalInnerEvent(array[i]);
                }
            }
            env.sendEventBean(new LocalEvent(property));
        };
        String beanepl = "@public @buseventtype create schema LocalInnerEvent as " + LocalInnerEvent.class.getName() + ";\n" +
            "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<String[]> map = array -> {
            Map[] property;
            if (array == null) {
                property = null;
            } else {
                property = new Map[array.length];
                for (int i = 0; i < array.length; i++) {
                    property[i] = Collections.singletonMap("id", array[i]);
                }
            }
            env.sendEventMap(Collections.singletonMap("property", property), "LocalEvent");
        };
        runAssertion(env, getEpl("map"), map);

        // Object-array
        Consumer<String[]> oa = array -> {
            Object[][] property;
            if (array == null) {
                property = new Object[][]{null};
            } else {
                property = new Object[array.length][];
                for (int i = 0; i < array.length; i++) {
                    property[i] = new Object[]{array[i]};
                }
            }
            env.sendEventObjectArray(new Object[]{property}, "LocalEvent");
        };
        runAssertion(env, getEpl("objectarray"), oa);

        // Json
        Consumer<String[]> json = array -> {
            JsonValue property;
            if (array == null) {
                property = Json.NULL;
            } else {
                JsonArray arr = new JsonArray();
                for (int i = 0; i < array.length; i++) {
                    arr.add(new JsonObject().add("id", array[i]));
                }
                property = arr;
            }
            env.sendEventJson(new JsonObject().add("property", property).toString(), "LocalEvent");
        };
        runAssertion(env, getEpl("json"), json);

        // Json-Class-Provided
        String eplJsonProvided = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, eplJsonProvided, json);

        // Avro
        Consumer<String[]> avro = array -> {
            Schema schema = env.runtimeAvroSchemaByDeployment("schema", "LocalEvent");
            GenericData.Record event = new GenericData.Record(schema);
            if (array == null) {
                event.put("property", Collections.emptyList());
            } else {
                Collection<GenericData.Record> arr = new ArrayList();
                for (int i = 0; i < array.length; i++) {
                    GenericData.Record inner = new GenericData.Record(schema.getField("property").schema().getElementType());
                    inner.put("id", array[i]);
                    arr.add(inner);
                }
                event.put("property", arr);
            }
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, getEpl("avro"), avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<String[]> sender) {

        String epl = createSchemaEPL +
            "@name('s0') select * from LocalEvent;\n" +
            "@name('s1') select property[0].id as c0, property[1].id as c1," +
            " exists(property[0].id) as c2, exists(property[1].id) as c3," +
            " typeof(property[0].id) as c4, typeof(property[1].id) as c5" +
            " from LocalEvent;\n";
        env.compileDeploy(epl).addListener("s0").addListener("s1");

        sender.accept(new String[]{"a", "b"});
        env.assertEventNew("s0", event -> assertGetters(event, true, "a", true, "b"));
        assertProps(env, true, "a", true, "b");

        sender.accept(new String[]{"a"});
        env.assertEventNew("s0", event -> assertGetters(event, true, "a", false, null));
        assertProps(env, true, "a", false, null);

        sender.accept(new String[0]);
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, false, null, false, null);

        sender.accept(null);
        env.assertEventNew("s0", event -> assertGetters(event, false, null, false, null));
        assertProps(env, false, null, false, null);

        env.undeployAll();
    }

    private void assertProps(RegressionEnvironment env, boolean existsA, String expectedA, boolean existsB, String expectedB) {
        env.assertPropsNew("s1", "c0,c1,c2,c3,c4,c5".split(","),
            new Object[]{expectedA, expectedB, existsA, existsB, existsA ? String.class.getSimpleName() : null, existsB ? String.class.getSimpleName() : null});
    }

    private void assertGetters(EventBean event, boolean existsZero, String valueZero, boolean existsOne, String valueOne) {
        EventPropertyGetter g0 = event.getEventType().getGetter("property[0].id");
        EventPropertyGetter g1 = event.getEventType().getGetter("property[1].id");
        assertGetter(event, g0, existsZero, valueZero);
        assertGetter(event, g1, existsOne, valueOne);
    }

    private void assertGetter(EventBean event, EventPropertyGetter getter, boolean exists, String value) {
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private String getEpl(String underlying) {
        return "@public @buseventtype create " + underlying + " schema LocalInnerEvent(id string);\n" +
            "@name('schema') @public @buseventtype create " + underlying + " schema LocalEvent(property LocalInnerEvent[]);\n";
    }

    public static class LocalInnerEvent implements Serializable {
        private static final long serialVersionUID = 6572664797059739109L;
        private final String id;

        public LocalInnerEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = -7588127580058450233L;
        private LocalInnerEvent[] property;

        public LocalEvent(LocalInnerEvent[] property) {
            this.property = property;
        }

        public LocalInnerEvent[] getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = 694609940326156527L;
        public MyLocalJsonProvidedInner[] property;
    }

    public static class MyLocalJsonProvidedInner implements Serializable {
        public String id;
    }
}
