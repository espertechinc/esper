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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.Json;
import com.espertech.esper.common.client.json.minimaljson.JsonArray;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterIndexed implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, String[]> bean = (type, array) -> {
            env.sendEventBean(new LocalEvent(array));
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, String[]> map = (type, array) -> {
            env.sendEventMap(Collections.singletonMap("array", array), "LocalEvent");
        };
        String mapepl = "@public @buseventtype create schema LocalEvent(array string[]);\n";
        runAssertion(env, mapepl, map);

        // Object-array
        BiConsumer<EventType, String[]> oa = (type, array) -> {
            env.sendEventObjectArray(new Object[]{array}, "LocalEvent");
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent(array string[]);\n";
        runAssertion(env, oaepl, oa);

        // Json
        BiConsumer<EventType, String[]> json = (type, array) -> {
            if (array == null) {
                env.sendEventJson(new JsonObject().add("array", Json.NULL).toString(), "LocalEvent");
            } else {
                JsonObject event = new JsonObject();
                JsonArray jsonarray = new JsonArray();
                event.add("array", jsonarray);
                for (String string : array) {
                    jsonarray.add(string);
                }
                env.sendEventJson(event.toString(), "LocalEvent");
            }
        };
        runAssertion(env, "@public @buseventtype create json schema LocalEvent(array string[]);\n", json);

        // Json-Class-Provided
        runAssertion(env, "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n", json);

        // Avro
        BiConsumer<EventType, String[]> avro = (type, array) -> {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(type));
            event.put("array", array == null ? null : Arrays.asList(array));
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, "@public @buseventtype create avro schema LocalEvent(array string[]);\n", avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, String[]> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");
        EventType eventType = env.statement("s0").getEventType();
        EventPropertyGetter g0 = eventType.getGetter("array[0]");
        EventPropertyGetter g1 = eventType.getGetter("array[1]");

        String propepl = "@name('s1') select array[0] as c0, array[1] as c1," +
            "exists(array[0]) as c2, exists(array[1]) as c3, " +
            "typeof(array[0]) as c4, typeof(array[1]) as c5 from LocalEvent;\n";
        env.compileDeploy(propepl, path).addListener("s1");

        sender.accept(eventType, new String[]{"a", "b"});
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, "a");
        assertGetter(event, g1, true, "b");
        assertProps(env, "a", "b");

        sender.accept(eventType, new String[]{"a"});
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, "a");
        assertGetter(event, g1, false, null);
        assertProps(env, "a", null);

        sender.accept(eventType, new String[0]);
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, false, null);
        assertGetter(event, g1, false, null);
        assertProps(env, null, null);

        sender.accept(eventType, null);
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, false, null);
        assertGetter(event, g1, false, null);
        assertProps(env, null, null);

        sender.accept(eventType, null);
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, false, null);
        assertGetter(event, g1, false, null);
        assertProps(env, null, null);

        env.undeployAll();
    }

    private void assertGetter(EventBean event, EventPropertyGetter getter, boolean exists, String value) {
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private void assertProps(RegressionEnvironment env, String valueA, String valueB) {
        EventBean event = env.listener("s1").assertOneGetNewAndReset();
        assertEquals(valueA, event.get("c0"));
        assertEquals(valueB, event.get("c1"));
        assertEquals(valueA != null, event.get("c2"));
        assertEquals(valueB != null, event.get("c3"));
        assertEquals(valueA == null ? null : "String", event.get("c4"));
        assertEquals(valueB == null ? null : "String", event.get("c5"));
    }

    public static class LocalEvent {
        private String[] array;

        public LocalEvent(String[] array) {
            this.array = array;
        }

        public String[] getArray() {
            return array;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public String[] array;
    }
}
