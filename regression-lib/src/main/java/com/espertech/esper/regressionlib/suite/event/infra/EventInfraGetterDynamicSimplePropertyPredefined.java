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
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterDynamicSimplePropertyPredefined implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, String> bean = (type, property) -> {
            env.sendEventBean(new LocalEvent(property));
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        BiConsumer<EventType, String> map = (type, property) -> {
            env.sendEventMap(Collections.singletonMap("property", property), "LocalEvent");
        };
        runAssertion(env, getEpl("map"), map);

        // Object-array
        BiConsumer<EventType, String> oa = (type, property) -> {
            env.sendEventObjectArray(new Object[]{property}, "LocalEvent");
        };
        runAssertion(env, getEpl("objectarray"), oa);

        // Json
        BiConsumer<EventType, String> json = (type, property) -> {
            if (property == null) {
                env.sendEventJson(new JsonObject().add("property", Json.NULL).toString(), "LocalEvent");
            } else {
                env.sendEventJson(new JsonObject().add("property", property).toString(), "LocalEvent");
            }
        };
        runAssertion(env, getEpl("json"), json);

        // Json-Class-Predefined
        String eplJsonPredefined = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @buseventtype @public " +
            "create json schema LocalEvent();\n";
        runAssertion(env, eplJsonPredefined, json);

        // Avro
        BiConsumer<EventType, String> avro = (type, property) -> {
            GenericData.Record event = new GenericData.Record(SupportAvroUtil.getAvroSchema(type));
            event.put("property", property);
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, getEpl("avro"), avro);
    }

    private String getEpl(String underlying) {
        return "@buseventtype @public create " + underlying + " schema LocalEvent(property string);\n";
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             BiConsumer<EventType, String> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");
        EventType eventType = env.statement("s0").getEventType();
        EventPropertyGetter g0 = eventType.getGetter("property?");

        if (sender == null) {
            assertNull(g0);
            env.undeployAll();
            return;
        } else {
            String propepl = "@name('s1') select property? as c0, exists(property?) as c1, typeof(property?) as c2 from LocalEvent;\n";
            env.compileDeploy(propepl, path).addListener("s1");
        }

        sender.accept(eventType, "a");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, "a");
        assertProps(env, true, "a");

        sender.accept(eventType, null);
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, true, null);
        assertProps(env, true, null);

        env.undeployAll();
    }

    private void assertGetter(EventBean event, EventPropertyGetter getter, boolean exists, String value) {
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private void assertProps(RegressionEnvironment env, boolean exists, String value) {
        EventBean event = env.listener("s1").assertOneGetNewAndReset();
        assertEquals(value, event.get("c0"));
        assertEquals(exists, event.get("c1"));
        assertEquals(value != null ? "String" : null, event.get("c2"));
    }

    public static class LocalEvent {
        private String property;

        public LocalEvent(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        public String property;
    }
}
