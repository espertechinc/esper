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
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

public class EventInfraGetterSimpleNoFragment implements RegressionExecution {
    public final static String XMLTYPENAME = EventInfraGetterSimpleNoFragment.class.getSimpleName() + "XML";

    public void run(RegressionEnvironment env) {
        // Bean
        BiConsumer<EventType, String> bean = (type, property) -> {
            env.sendEventBean(new LocalEvent(property));
        };
        String beanepl = "@public @buseventtype create schema LocalEvent as " + LocalEvent.class.getName() + ";\n";
        runAssertion(env, "LocalEvent", beanepl, bean);

        // Map
        BiConsumer<EventType, String> map = (type, property) -> {
            env.sendEventMap(Collections.singletonMap("property", property), "LocalEvent");
        };
        String mapepl = "@public @buseventtype create schema LocalEvent(property string);\n";
        runAssertion(env, "LocalEvent", mapepl, map);

        // Object-array
        BiConsumer<EventType, String> oa = (type, property) -> {
            env.sendEventObjectArray(new Object[]{property}, "LocalEvent");
        };
        String oaepl = "@public @buseventtype create objectarray schema LocalEvent(property string);\n";
        runAssertion(env, "LocalEvent", oaepl, oa);

        // Json
        BiConsumer<EventType, String> json = (type, property) -> {
            env.sendEventJson(new JsonObject().add("property", property).toString(), "LocalEvent");
        };
        runAssertion(env, "LocalEvent", "@public @buseventtype create json schema LocalEvent(property string);\n", json);

        // Json-Class-Provided
        runAssertion(env, "LocalEvent", "@JsonSchema(className='" + MyLocalJsonProvided.class.getName() + "') @public @buseventtype create json schema LocalEvent();\n", json);

        // Avro
        BiConsumer<EventType, String> avro = (type, property) -> {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(type));
            theEvent.put("property", property);
            env.sendEventAvro(theEvent, type.getName());
        };
        runAssertion(env, "LocalEvent", "@public @buseventtype create avro schema LocalEvent(property string);\n", avro);

        // XML
        BiConsumer<EventType, String> xml = (type, property) -> {
            String doc = "<" + XMLTYPENAME + (property != null ? " property=\"" + property + "\"" : "") + "/>";
            SupportXML.sendXMLEvent(env, doc, XMLTYPENAME);
        };
        runAssertion(env, XMLTYPENAME, "", xml);
    }

    public void runAssertion(RegressionEnvironment env,
                             String typeName,
                             String createSchemaEPL,
                             BiConsumer<EventType, String> sender) {

        String epl = createSchemaEPL +
            "@name('s0') select * from " + typeName + ";\n" +
            "@name('s1') select property as c0, exists(property) as c1, typeof(property) as c2 from " + typeName + ";\n";
        env.compileDeploy(epl).addListener("s0").addListener("s1");
        EventType eventType = env.statement("s0").getEventType();

        EventPropertyGetter g0 = eventType.getGetter("property");

        sender.accept(eventType, "a");
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, "a");
        assertProps(env, "a");

        sender.accept(eventType, null);
        event = env.listener("s0").assertOneGetNewAndReset();
        assertGetter(event, g0, null);
        assertProps(env, null);

        env.undeployAll();
    }

    private void assertProps(RegressionEnvironment env, String expected) {
        EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "c0,c1,c2".split(","),
            new Object[]{expected, true, expected == null ? null : String.class.getSimpleName()});
    }

    private void assertGetter(EventBean event, EventPropertyGetter getter, String value) {
        assertTrue(getter.isExistsProperty(event));
        assertEquals(value, getter.get(event));
        assertNull(getter.getFragment(event));
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
