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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.function.Consumer;

import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventInfraGetterDynamicSimplePropertyPredefined implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<String> bean = property -> {
            env.sendEventBean(new LocalEvent(property));
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<String> map = property -> {
            env.sendEventMap(Collections.singletonMap("property", property), "LocalEvent");
        };
        runAssertion(env, getEpl("map"), map);

        // Object-array
        Consumer<String> oa = property -> {
            env.sendEventObjectArray(new Object[]{property}, "LocalEvent");
        };
        runAssertion(env, getEpl("objectarray"), oa);

        // Json
        Consumer<String> json = property -> {
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
        Consumer<String> avro = property -> {
            Schema schema = record("name").fields().optionalString("property").endRecord();
            GenericData.Record event = new GenericData.Record(schema);
            event.put("property", property);
            env.sendEventAvro(event, "LocalEvent");
        };
        runAssertion(env, getEpl("avro"), avro);
    }

    private String getEpl(String underlying) {
        return "@name('schema') @buseventtype @public create " + underlying + " schema LocalEvent(property string);\n";
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<String> sender) {

        RegressionPath path = new RegressionPath();
        env.compileDeploy(createSchemaEPL, path);

        env.compileDeploy("@name('s0') select * from LocalEvent", path).addListener("s0");

        if (sender == null) {
            env.assertStatement("s0", statement -> {
                EventType eventType = statement.getEventType();
                EventPropertyGetter g0 = eventType.getGetter("property?");
                assertNull(g0);
            });
            env.undeployAll();
            return;
        }

        String propepl = "@name('s1') select property? as c0, exists(property?) as c1, typeof(property?) as c2 from LocalEvent;\n";
        env.compileDeploy(propepl, path).addListener("s1");

        sender.accept("a");
        env.assertEventNew("s0", event -> assertGetter(event, true, "a"));
        assertProps(env, true, "a");

        sender.accept(null);
        env.assertEventNew("s0", event -> assertGetter(event, true, null));
        assertProps(env, true, null);

        env.undeployAll();
    }

    private void assertGetter(EventBean event, boolean exists, String value) {
        EventPropertyGetter getter = event.getEventType().getGetter("property?");
        assertEquals(exists, getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
    }

    private void assertProps(RegressionEnvironment env, boolean exists, String value) {
        env.assertEventNew("s1", event -> {
            assertEquals(value, event.get("c0"));
            assertEquals(exists, event.get("c1"));
            assertEquals(value != null ? "String" : null, event.get("c2"));
        });
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = 4884110772777891153L;
        private String property;

        public LocalEvent(String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = 2417236172750455921L;
        public String property;
    }
}
