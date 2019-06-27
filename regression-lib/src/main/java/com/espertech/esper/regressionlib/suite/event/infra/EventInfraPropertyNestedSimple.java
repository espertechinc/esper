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
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.core.AvroSchemaUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.json.SupportJsonEventTypeUtil;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.util.CollectionUtil.twoEntryMap;
import static org.junit.Assert.*;

public class EventInfraPropertyNestedSimple implements RegressionExecution {
    public final static String XML_TYPENAME = EventInfraPropertyNestedSimple.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyNestedSimple.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyNestedSimple.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyNestedSimple.class.getSimpleName() + "Avro";
    private final static String BEAN_TYPENAME = InfraNestedSimplePropTop.class.getSimpleName();
    private final static String JSON_TYPENAME = EventInfraPropertyNestedSimple.class.getSimpleName() + "Json";
    private final static String JSONPROVIDED_TYPENAME = EventInfraPropertyNestedSimple.class.getSimpleName() + "JsonProvided";

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        runAssertion(env, BEAN_TYPENAME, FBEAN, InfraNestedSimplePropLvl1.class, InfraNestedSimplePropLvl1.class.getTypeName(), path);
        runAssertion(env, MAP_TYPENAME, FMAP, Map.class, MAP_TYPENAME + "_1", path);
        runAssertion(env, OA_TYPENAME, FOA, Object[].class, OA_TYPENAME + "_1", path);
        runAssertion(env, XML_TYPENAME, FXML, Node.class, XML_TYPENAME + ".l1", path);
        runAssertion(env, AVRO_TYPENAME, FAVRO, GenericData.Record.class, AVRO_TYPENAME + "_1", path);

        String epl =
            "create json schema " + JSON_TYPENAME + "_4(lvl4 int);\n" +
                "create json schema " + JSON_TYPENAME + "_3(lvl3 int, l4 " + JSON_TYPENAME + "_4);\n" +
                "create json schema " + JSON_TYPENAME + "_2(lvl2 int, l3 " + JSON_TYPENAME + "_3);\n" +
                "create json schema " + JSON_TYPENAME + "_1(lvl1 int, l2 " + JSON_TYPENAME + "_2);\n" +
                "@name('types') @public @buseventtype create json schema " + JSON_TYPENAME + "(l1 " + JSON_TYPENAME + "_1);\n";
        env.compileDeploy(epl, path);
        Class nestedClass = SupportJsonEventTypeUtil.getUnderlyingType(env, "types", JSON_TYPENAME + "_1");
        runAssertion(env, JSON_TYPENAME, FJSON, nestedClass, JSON_TYPENAME + "_1", path);

        epl = "@JsonSchema(className='" + MyLocalJSONProvidedTop.class.getName() + "') @name('types') @public @buseventtype create json schema " + JSONPROVIDED_TYPENAME + "();\n";
        env.compileDeploy(epl, path);
        runAssertion(env, JSONPROVIDED_TYPENAME, FJSON, MyLocalJSONProvidedLvl1.class, MyLocalJSONProvidedLvl1.class.getName(), path);

        env.undeployAll();
    }

    private void runAssertion(RegressionEnvironment env, String typename, FunctionSendEvent4Int send, Class nestedClass, String fragmentTypeName, RegressionPath path) {
        runAssertionSelectNested(env, typename, send, path);
        runAssertionBeanNav(env, typename, send, path);
        runAssertionTypeValidProp(env, typename, send, nestedClass, fragmentTypeName);
        runAssertionTypeInvalidProp(env, typename);
    }

    private void runAssertionBeanNav(RegressionEnvironment env, String typename, FunctionSendEvent4Int send, RegressionPath path) {
        String epl = "@name('s0') select * from " + typename;
        env.compileDeploy(epl, path).addListener("s0");

        send.apply(typename, env, 1, 2, 3, 4);
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "l1.lvl1,l1.l2.lvl2,l1.l2.l3.lvl3,l1.l2.l3.l4.lvl4".split(","), new Object[]{1, 2, 3, 4});
        SupportEventTypeAssertionUtil.assertConsistency(event);
        boolean nativeFragment = typename.equals(BEAN_TYPENAME) || typename.equals(JSONPROVIDED_TYPENAME);
        SupportEventTypeAssertionUtil.assertFragments(event, nativeFragment, false, "l1.l2");
        SupportEventTypeAssertionUtil.assertFragments(event, nativeFragment, false, "l1,l1.l2,l1.l2.l3,l1.l2.l3.l4");
        runAssertionEventInvalidProp(event);

        env.undeployModuleContaining("s0");
    }

    private void runAssertionSelectNested(RegressionEnvironment env, String typename, FunctionSendEvent4Int send, RegressionPath path) {
        String epl = "@name('s0') select " +
            "l1.lvl1 as c0, " +
            "exists(l1.lvl1) as exists_c0, " +
            "l1.l2.lvl2 as c1, " +
            "exists(l1.l2.lvl2) as exists_c1, " +
            "l1.l2.l3.lvl3 as c2, " +
            "exists(l1.l2.l3.lvl3) as exists_c2, " +
            "l1.l2.l3.l4.lvl4 as c3, " +
            "exists(l1.l2.l3.l4.lvl4) as exists_c3 " +
            "from " + typename;
        env.compileDeploy(epl, path).addListener("s0");
        String[] fields = "c0,exists_c0,c1,exists_c1,c2,exists_c2,c3,exists_c3".split(",");

        EventType eventType = env.statement("s0").getEventType();
        for (String property : fields) {
            assertEquals(property.startsWith("exists") ? Boolean.class : Integer.class, JavaClassHelper.getBoxedType(eventType.getPropertyType(property)));
        }

        send.apply(typename, env, 1, 2, 3, 4);
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[]{1, true, 2, true, 3, true, 4, true});
        SupportEventTypeAssertionUtil.assertConsistency(event);

        send.apply(typename, env, 10, 5, 50, 400);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{10, true, 5, true, 50, true, 400, true});

        env.undeployModuleContaining("s0");
    }

    private void runAssertionEventInvalidProp(EventBean event) {
        for (String prop : Arrays.asList("l2", "l1.l3", "l1.xxx", "l1.l2.x", "l1.l2.l3.x", "l1.lvl1.x")) {
            SupportMessageAssertUtil.tryInvalidProperty(event, prop);
            SupportMessageAssertUtil.tryInvalidGetFragment(event, prop);
        }
    }

    private void runAssertionTypeValidProp(RegressionEnvironment env, String typeName, FunctionSendEvent4Int send, Class nestedClass, String fragmentTypeName) {
        EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured(typeName);

        Object[][] expectedType = new Object[][]{{"l1", nestedClass, fragmentTypeName, false}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, eventType, SupportEventTypeAssertionEnum.getSetWithFragment());

        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"l1"}, eventType.getPropertyNames());

        for (String prop : Arrays.asList("l1", "l1.lvl1", "l1.l2", "l1.l2.lvl2")) {
            assertNotNull(eventType.getGetter(prop));
            assertTrue(eventType.isProperty(prop));
        }

        assertEquals(nestedClass, eventType.getPropertyType("l1"));
        for (String prop : Arrays.asList("l1.lvl1", "l1.l2.lvl2", "l1.l2.l3.lvl3")) {
            assertEquals(Integer.class, JavaClassHelper.getBoxedType(eventType.getPropertyType(prop)));
        }

        FragmentEventType lvl1Fragment = eventType.getFragmentType("l1");
        assertFalse(lvl1Fragment.isIndexed());
        boolean isNative = typeName.equals(BEAN_TYPENAME) || typeName.equals(JSONPROVIDED_TYPENAME);
        assertEquals(isNative, lvl1Fragment.isNative());
        assertEquals(fragmentTypeName, lvl1Fragment.getFragmentType().getName());

        FragmentEventType lvl2Fragment = eventType.getFragmentType("l1.l2");
        assertFalse(lvl2Fragment.isIndexed());
        assertEquals(isNative, lvl2Fragment.isNative());

        assertEquals(new EventPropertyDescriptor("l1", nestedClass, null, false, false, false, false, true), eventType.getPropertyDescriptor("l1"));
    }

    private void runAssertionTypeInvalidProp(RegressionEnvironment env, String typeName) {
        EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured(typeName);

        for (String prop : Arrays.asList("l2", "l1.l3", "l1.lvl1.lvl1", "l1.l2.l4", "l1.l2.xx", "l1.l2.l3.lvl5")) {
            assertEquals(false, eventType.isProperty(prop));
            assertEquals(null, eventType.getPropertyType(prop));
            assertNull(eventType.getPropertyDescriptor(prop));
        }
    }

    private static final FunctionSendEvent4Int FMAP = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        Map<String, Object> l4 = Collections.singletonMap("lvl4", lvl4);
        Map<String, Object> l3 = twoEntryMap("l4", l4, "lvl3", lvl3);
        Map<String, Object> l2 = twoEntryMap("l3", l3, "lvl2", lvl2);
        Map<String, Object> l1 = twoEntryMap("l2", l2, "lvl1", lvl1);
        Map<String, Object> top = Collections.singletonMap("l1", l1);
        env.sendEventMap(top, eventTypeName);
    };

    private static final FunctionSendEvent4Int FOA = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        Object[] l4 = new Object[]{lvl4};
        Object[] l3 = new Object[]{l4, lvl3};
        Object[] l2 = new Object[]{l3, lvl2};
        Object[] l1 = new Object[]{l2, lvl1};
        Object[] top = new Object[]{l1};
        env.sendEventObjectArray(top, eventTypeName);
    };

    private static final FunctionSendEvent4Int FBEAN = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        InfraNestedSimplePropLvl4 l4 = new InfraNestedSimplePropLvl4(lvl4);
        InfraNestedSimplePropLvl3 l3 = new InfraNestedSimplePropLvl3(l4, lvl3);
        InfraNestedSimplePropLvl2 l2 = new InfraNestedSimplePropLvl2(l3, lvl2);
        InfraNestedSimplePropLvl1 l1 = new InfraNestedSimplePropLvl1(l2, lvl1);
        InfraNestedSimplePropTop top = new InfraNestedSimplePropTop(l1);
        env.sendEventBean(top);
    };

    private static final FunctionSendEvent4Int FXML = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<myevent>\n" +
            "\t<l1 lvl1=\"${lvl1}\">\n" +
            "\t\t<l2 lvl2=\"${lvl2}\">\n" +
            "\t\t\t<l3 lvl3=\"${lvl3}\">\n" +
            "\t\t\t\t<l4 lvl4=\"${lvl4}\">\n" +
            "\t\t\t\t</l4>\n" +
            "\t\t\t</l3>\n" +
            "\t\t</l2>\n" +
            "\t</l1>\n" +
            "</myevent>";
        xml = xml.replace("${lvl1}", Integer.toString(lvl1));
        xml = xml.replace("${lvl2}", Integer.toString(lvl2));
        xml = xml.replace("${lvl3}", Integer.toString(lvl3));
        xml = xml.replace("${lvl4}", Integer.toString(lvl4));
        try {
            SupportXML.sendXMLEvent(env, xml, eventTypeName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    private static final FunctionSendEvent4Int FAVRO = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        Schema lvl1Schema = schema.getField("l1").schema();
        Schema lvl2Schema = lvl1Schema.getField("l2").schema();
        Schema lvl3Schema = lvl2Schema.getField("l3").schema();
        Schema lvl4Schema = lvl3Schema.getField("l4").schema();
        GenericData.Record lvl4Rec = new GenericData.Record(lvl4Schema);
        lvl4Rec.put("lvl4", lvl4);
        GenericData.Record lvl3Rec = new GenericData.Record(lvl3Schema);
        lvl3Rec.put("l4", lvl4Rec);
        lvl3Rec.put("lvl3", lvl3);
        GenericData.Record lvl2Rec = new GenericData.Record(lvl2Schema);
        lvl2Rec.put("l3", lvl3Rec);
        lvl2Rec.put("lvl2", lvl2);
        GenericData.Record lvl1Rec = new GenericData.Record(lvl1Schema);
        lvl1Rec.put("l2", lvl2Rec);
        lvl1Rec.put("lvl1", lvl1);
        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("l1", lvl1Rec);
        env.sendEventAvro(datum, eventTypeName);
    };

    private static final FunctionSendEvent4Int FJSON = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        String json = "{\n" +
            "  \"l1\": {\n" +
            "    \"lvl1\": ${lvl1},\n" +
            "    \"l2\": {\n" +
            "      \"lvl2\": ${lvl2},\n" +
            "      \"l3\": {\n" +
            "        \"lvl3\": ${lvl3},\n" +
            "        \"l4\": {\n" +
            "          \"lvl4\": ${lvl4}\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
        json = json.replace("${lvl1}", Integer.toString(lvl1));
        json = json.replace("${lvl2}", Integer.toString(lvl2));
        json = json.replace("${lvl3}", Integer.toString(lvl3));
        json = json.replace("${lvl4}", Integer.toString(lvl4));
        env.sendEventJson(json, eventTypeName);
    };

    @FunctionalInterface
    interface FunctionSendEvent4Int {
        void apply(String eventTypeName, RegressionEnvironment env, int lvl1, int lvl2, int lvl3, int lvl4);
    }

    public final static class InfraNestedSimplePropTop implements Serializable {
        private InfraNestedSimplePropLvl1 l1;

        public InfraNestedSimplePropTop(InfraNestedSimplePropLvl1 l1) {
            this.l1 = l1;
        }

        public InfraNestedSimplePropLvl1 getL1() {
            return l1;
        }
    }

    public final static class InfraNestedSimplePropLvl1 {
        private InfraNestedSimplePropLvl2 l2;
        private int lvl1;

        public InfraNestedSimplePropLvl1(InfraNestedSimplePropLvl2 l2, int lvl1) {
            this.l2 = l2;
            this.lvl1 = lvl1;
        }

        public InfraNestedSimplePropLvl2 getL2() {
            return l2;
        }

        public int getLvl1() {
            return lvl1;
        }
    }

    public final static class InfraNestedSimplePropLvl2 {
        private InfraNestedSimplePropLvl3 l3;
        private int lvl2;

        public InfraNestedSimplePropLvl2(InfraNestedSimplePropLvl3 l3, int lvl2) {
            this.l3 = l3;
            this.lvl2 = lvl2;
        }

        public InfraNestedSimplePropLvl3 getL3() {
            return l3;
        }

        public int getLvl2() {
            return lvl2;
        }
    }

    public final static class InfraNestedSimplePropLvl3 {
        private InfraNestedSimplePropLvl4 l4;
        private int lvl3;

        public InfraNestedSimplePropLvl3(InfraNestedSimplePropLvl4 l4, int lvl3) {
            this.l4 = l4;
            this.lvl3 = lvl3;
        }

        public InfraNestedSimplePropLvl4 getL4() {
            return l4;
        }

        public int getLvl3() {
            return lvl3;
        }
    }

    public final static class InfraNestedSimplePropLvl4 {
        private int lvl4;

        public InfraNestedSimplePropLvl4(int lvl4) {
            this.lvl4 = lvl4;
        }

        public int getLvl4() {
            return lvl4;
        }
    }

    public final static class MyLocalJSONProvidedTop implements Serializable {
        public MyLocalJSONProvidedLvl1 l1;
    }

    public final static class MyLocalJSONProvidedLvl1 {
        public MyLocalJSONProvidedLvl2 l2;
        public int lvl1;
    }

    public final static class MyLocalJSONProvidedLvl2 {
        public MyLocalJSONProvidedLvl3 l3;
        public int lvl2;
    }

    public final static class MyLocalJSONProvidedLvl3 {
        public MyLocalJSONProvidedLvl4 l4;
        public int lvl3;
    }

    public final static class MyLocalJSONProvidedLvl4 {
        public int lvl4;
    }
}
