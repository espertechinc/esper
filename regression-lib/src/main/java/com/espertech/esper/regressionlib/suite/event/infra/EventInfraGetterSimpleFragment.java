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
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventInfraGetterSimpleFragment implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        // Bean
        Consumer<Boolean> bean = hasValue -> {
            env.sendEventBean(new LocalEvent(hasValue ? new LocalInnerEvent() : null));
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, beanepl, bean);

        // Map
        Consumer<Boolean> map = hasValue -> {
            env.sendEventMap(Collections.singletonMap("property", hasValue ? Collections.emptyMap() : null), "LocalEvent");
        };
        String mapepl = "@public @buseventtype create schema LocalInnerEvent();\n" +
            "@public @buseventtype create schema LocalEvent(property LocalInnerEvent);\n";
        runAssertion(env, mapepl, map);

        // Object-array
        Consumer<Boolean> oa = hasValue -> {
            env.sendEventObjectArray(new Object[]{hasValue ? new Object[0] : null}, "LocalEvent");
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalInnerEvent();\n" +
            "@public @buseventtype create objectarray schema LocalEvent(property LocalInnerEvent);\n";
        runAssertion(env, oaepl, oa);

        // Json
        Consumer<Boolean> json = hasValue -> {
            env.sendEventJson(new JsonObject().add("property", hasValue ? new JsonObject() : Json.NULL).toString(), "LocalEvent");
        };
        String jsonepl = "@public @buseventtype create json schema LocalInnerEvent();\n" +
            "@public @buseventtype create json schema LocalEvent(property LocalInnerEvent);\n";
        runAssertion(env, jsonepl, json);

        // Json-Class-Provided
        String jsonprovidedepl = "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n";
        runAssertion(env, jsonprovidedepl, json);

        // Avro
        Consumer<Boolean> avro = hasValue -> {
            Schema schema = env.runtimeAvroSchemaByDeployment("schema", "LocalEvent");
            GenericData.Record theEvent = new GenericData.Record(schema);
            theEvent.put("property", hasValue ? new GenericData.Record(schema.getField("property").schema()) : null);
            env.sendEventAvro(theEvent, "LocalEvent");
        };
        String avroepl = "@public @buseventtype create avro schema LocalInnerEvent();\n" +
            "@name('schema') @public @buseventtype create avro schema LocalEvent(property LocalInnerEvent);\n";
        runAssertion(env, avroepl, avro);
    }

    public void runAssertion(RegressionEnvironment env,
                             String createSchemaEPL,
                             Consumer<Boolean> sender) {

        String epl = createSchemaEPL +
            "@name('s0') select * from LocalEvent;\n" +
            "@name('s1') select property as c0, exists(property) as c1, typeof(property) as c2 from LocalEvent;\n";
        env.compileDeploy(epl).addListener("s0").addListener("s1");

        sender.accept(true);
        env.assertEventNew("s0", event -> assertGetter(event, true));
        assertProps(env, true);

        sender.accept(false);
        env.assertEventNew("s0", event -> assertGetter(event, false));
        assertProps(env, false);

        env.undeployAll();
    }

    private void assertGetter(EventBean event, boolean hasValue) {
        EventPropertyGetter getter = event.getEventType().getGetter("property");
        assertTrue(getter.isExistsProperty(event));
        assertEquals(hasValue, getter.get(event) != null);
        assertEquals(hasValue, getter.getFragment(event) != null);
    }

    private void assertProps(RegressionEnvironment env, boolean hasValue) {
        env.assertEventNew("s1", event -> {
            assertTrue((Boolean) event.get("c1"));
            assertEquals(hasValue, event.get("c0") != null);
            assertEquals(hasValue, event.get("c2") != null);
        });
    }

    public static class LocalInnerEvent implements Serializable {
        private static final long serialVersionUID = -1818290290814185163L;
    }

    public static class LocalEvent implements Serializable {
        private static final long serialVersionUID = -69193879723770600L;
        private LocalInnerEvent property;

        public LocalEvent(LocalInnerEvent property) {
            this.property = property;
        }

        public LocalInnerEvent getProperty() {
            return property;
        }
    }

    public static class MyLocalJsonProvided implements Serializable {
        private static final long serialVersionUID = 2481753919988769188L;
        public MyLocalJsonProvidedInnerEvent property;
    }

    public static class MyLocalJsonProvidedInnerEvent implements Serializable {
        private static final long serialVersionUID = -6885135388455456817L;
    }
}
