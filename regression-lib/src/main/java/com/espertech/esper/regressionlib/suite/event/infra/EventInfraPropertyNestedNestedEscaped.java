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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class EventInfraPropertyNestedNestedEscaped implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        String eplSchema = "@name('types') @public @buseventtype create schema BeanLvl0 as " + SupportLvl0.class.getName() + ";\n" +
            "\n" +
            "create schema MapLvl3(vlvl3 string);\n" +
            "create schema MapLvl2(vlvl2 string, lvl3 MapLvl3);\n" +
            "create schema MapLvl1(lvl2 MapLvl2);\n" +
            "@public @buseventtype create schema MapLvl0(lvl1 MapLvl1);\n" +
            "\n" +
            "create objectarray schema OALvl3(vlvl3 string);\n" +
            "create objectarray schema OALvl2(vlvl2 string, vlvl2dyn string, lvl3 OALvl3);\n" +
            "create objectarray schema OALvl1(lvl2 OALvl2);\n" +
            "@public @buseventtype create objectarray schema OALvl0(lvl1 OALvl1);\n" +
            "\n" +
            "@JsonSchema(dynamic=true) create json schema JSONLvl3(vlvl3 string);\n" +
            "@JsonSchema(dynamic=true) create json schema JSONLvl2(vlvl2 string, lvl3 JSONLvl3);\n" +
            "create json schema JSONLvl1(lvl2 JSONLvl2);\n" +
            "@public @buseventtype create json schema JSONLvl0(lvl1 JSONLvl1);\n" +
            "\n" +
            "create avro schema AvroLvl3(vlvl3 string);\n" +
            "create avro schema AvroLvl2(vlvl2 string, vlvl2dyn string, lvl3 AvroLvl3);\n" +
            "create avro schema AvroLvl1(lvl2 AvroLvl2);\n" +
            "@public @buseventtype create avro schema AvroLvl0(lvl1 AvroLvl1);\n";
        env.compileDeploy(eplSchema, path);

        runAssertion(env, path, "BeanLvl0", FBEAN);
        runAssertion(env, path, "MapLvl0", FMAP);
        runAssertion(env, path, "OALvl0", FOA);
        runAssertion(env, path, "JSONLvl0", FJSON);
        runAssertion(env, path, "AvroLvl0", FAVRO);

        env.undeployAll();
    }

    private void runAssertion(RegressionEnvironment env, RegressionPath path, String eventTypeName, FunctionSendEvent sendEvent) {
        runAssertionGetter(env, path, eventTypeName, sendEvent);
        runAssertionSelect(env, path, eventTypeName, sendEvent);
    }

    private void runAssertionSelect(RegressionEnvironment env, RegressionPath path, String eventTypeName, FunctionSendEvent sendEvent) {
        env.compileDeploy("@name('s0') select " +
                "lvl1.lvl2.`vlvl2` as c0, " +
                "lvl1.lvl2.lvl3.`vlvl3` as c1, " +
                "`lvl1`.`lvl2`.`lvl3`.`vlvl3` as c2, " +
                "lvl1.lvl2.`vlvl2dyn`? as c3 " +
                " from " + eventTypeName, path).addListener("s0");

        sendEvent.apply(eventTypeName, env, "v2", "v2dyn", "v3");
        env.assertPropsNew("s0", "c0,c1,c2,c3".split(","), new Object[] {"v2", "v3", "v3", "v2dyn"});

        env.undeployModuleContaining("s0");
    }

    private void runAssertionGetter(RegressionEnvironment env, RegressionPath path, String eventTypeName, FunctionSendEvent sendEvent) {
        env.compileDeploy("@name('s0') select * from " + eventTypeName, path).addListener("s0");
        sendEvent.apply(eventTypeName, env, "v2", "v2dyn", "v3");

        env.assertEventNew("s0", event -> {
            assertProperty(event, "v2", "lvl1.lvl2.`vlvl2`");
            assertProperty(event, "v3", "lvl1.lvl2.lvl3.`vlvl3`");
            assertProperty(event, "v3", "`lvl1`.`lvl2`.`lvl3`.`vlvl3`");
            assertProperty(event, "v2dyn", "lvl1.lvl2.`vlvl2dyn`?");

            String fragmentName = "lvl1.lvl2.`lvl3`";
            assertPropertyFragment(event, fragmentName);
            assertNotNull(event.getEventType().getFragmentType(fragmentName));
        });

        env.undeployModuleContaining("s0");
    }

    private void assertProperty(EventBean event, String value, String name) {
        assertEquals(value, event.get(name));
        EventType eventType = event.getEventType();
        assertNotNull(eventType.getPropertyType(name));
        assertTrue(eventType.isProperty(name));
        assertNotNull(eventType.getGetter(name));
    }

    private void assertPropertyFragment(EventBean event, String name) {
        assertNotNull(event.get(name));
        EventType eventType = event.getEventType();
        assertNotNull(eventType.getPropertyType(name));
        assertTrue(eventType.isProperty(name));
        assertNotNull(eventType.getGetter(name));
    }

    public static class SupportLvl3 implements Serializable {
        private final String vlvl3;

        public SupportLvl3(String vlvl3) {
            this.vlvl3 = vlvl3;
        }

        public String getVlvl3() {
            return vlvl3;
        }
    }

    public static class SupportLvl2 implements Serializable {
        private final String vlvl2;
        private final String vlvl2dyn;
        private final SupportLvl3 lvl3;

        public SupportLvl2(String vlvl2, String vlvl2dyn, SupportLvl3 lvl3) {
            this.vlvl2 = vlvl2;
            this.vlvl2dyn = vlvl2dyn;
            this.lvl3 = lvl3;
        }

        public String getVlvl2() {
            return vlvl2;
        }

        public SupportLvl3 getLvl3() {
            return lvl3;
        }

        public String getVlvl2dyn() {
            return vlvl2dyn;
        }
    }

    public static class SupportLvl1 implements Serializable {
        private final SupportLvl2 lvl2;

        public SupportLvl1(SupportLvl2 lvl2) {
            this.lvl2 = lvl2;
        }

        public SupportLvl2 getLvl2() {
            return lvl2;
        }
    }

    public static class SupportLvl0 implements Serializable {
        private final SupportLvl1 lvl1;

        public SupportLvl0(SupportLvl1 lvl1) {
            this.lvl1 = lvl1;
        }

        public SupportLvl1 getLvl1() {
            return lvl1;
        }
    }

    private static final FunctionSendEvent FBEAN = (eventTypeName, env, vlvl2, vlvl2dyn, vlvl3) -> {
        SupportLvl3 l3 = new SupportLvl3(vlvl3);
        SupportLvl2 l2 = new SupportLvl2(vlvl2, vlvl2dyn, l3);
        SupportLvl1 l1 = new SupportLvl1(l2);
        SupportLvl0 l0 = new SupportLvl0(l1);
        env.sendEventBean(l0, eventTypeName);
    };

    private static final FunctionSendEvent FMAP = (eventTypeName, env, vlvl2, vlvl2dyn, vlvl3) -> {
        Map<String, Object> l3 = Collections.singletonMap("vlvl3", vlvl3);
        Map<String, Object> l2 = CollectionUtil.buildMap("vlvl2", vlvl2, "lvl3", l3, "vlvl2dyn", vlvl2dyn);
        Map<String, Object> l1 = Collections.singletonMap("lvl2", l2);
        Map<String, Object> l0 = Collections.singletonMap("lvl1", l1);
        env.sendEventMap(l0, eventTypeName);
    };

    private static final FunctionSendEvent FOA = (eventTypeName, env, vlvl2, vlvl2dyn, vlvl3) -> {
        Object[] l3 = new Object[] {vlvl3};
        Object[] l2 = new Object[] {vlvl2, vlvl2dyn, l3};
        Object[] l1 = new Object[] {l2};
        Object[] l0 = new Object[] {l1};
        env.sendEventObjectArray(l0, eventTypeName);
    };

    private static final FunctionSendEvent FJSON = (eventTypeName, env, vlvl2, vlvl2dyn, vlvl3) -> {
        JsonObject lvl3 = new JsonObject();
        lvl3.add("vlvl3", vlvl3);
        JsonObject lvl2 = new JsonObject();
        lvl2.add("vlvl2", vlvl2);
        lvl2.add("vlvl2dyn", vlvl2dyn);
        lvl2.add("lvl3", lvl3);
        JsonObject lvl1 = new JsonObject();
        lvl1.add("lvl2", lvl2);
        JsonObject lvl0 = new JsonObject();
        lvl0.add("lvl1", lvl1);
        env.sendEventJson(lvl0.toString(), eventTypeName);
    };

    private static final FunctionSendEvent FAVRO = (eventTypeName, env, vlvl2, vlvl2dyn, vlvl3) -> {
        Schema schema = env.runtimeAvroSchemaByDeployment("types", eventTypeName);
        Schema lvl1Schema = schema.getField("lvl1").schema();
        Schema lvl2Schema = lvl1Schema.getField("lvl2").schema();
        Schema lvl3Schema = lvl2Schema.getField("lvl3").schema();
        GenericData.Record lvl3 = new GenericData.Record(lvl3Schema);
        lvl3.put("vlvl3", vlvl3);
        GenericData.Record lvl2 = new GenericData.Record(lvl2Schema);
        lvl2.put("lvl3", lvl3);
        lvl2.put("vlvl2", vlvl2);
        lvl2.put("vlvl2dyn", vlvl2dyn);
        GenericData.Record lvl1 = new GenericData.Record(lvl1Schema);
        lvl1.put("lvl2", lvl2);
        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("lvl1", lvl1);
        env.sendEventAvro(datum, eventTypeName);
    };

    @FunctionalInterface
    interface FunctionSendEvent {
        void apply(String eventTypeName, RegressionEnvironment env, String vlvl2, String vlvl2dyn, String vlvl3);
    }
}
