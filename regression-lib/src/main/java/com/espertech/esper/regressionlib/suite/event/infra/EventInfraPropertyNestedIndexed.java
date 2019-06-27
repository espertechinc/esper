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
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.common.internal.util.CollectionUtil.twoEntryMap;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNotNull;

public class EventInfraPropertyNestedIndexed implements RegressionExecution {
    public final static String XML_TYPENAME = EventInfraPropertyNestedIndexed.class.getSimpleName() + "XML";
    public final static String MAP_TYPENAME = EventInfraPropertyNestedIndexed.class.getSimpleName() + "Map";
    public final static String OA_TYPENAME = EventInfraPropertyNestedIndexed.class.getSimpleName() + "OA";
    public final static String AVRO_TYPENAME = EventInfraPropertyNestedIndexed.class.getSimpleName() + "Avro";
    private final static String BEAN_TYPENAME = InfraNestedIndexPropTop.class.getSimpleName();
    private final static String JSON_TYPENAME = EventInfraPropertyNestedIndexed.class.getSimpleName() + "Json";
    private final static String JSONPROVIDED_TYPENAME = EventInfraPropertyNestedIndexed.class.getSimpleName() + "JsonProvided";

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();

        runAssertion(env, true, BEAN_TYPENAME, FBEAN, InfraNestedIndexedPropLvl1.class, InfraNestedIndexedPropLvl1.class.getName(), path);
        runAssertion(env, true, MAP_TYPENAME, FMAP, Map.class, MAP_TYPENAME + "_1", path);
        runAssertion(env, true, OA_TYPENAME, FOA, Object[].class, OA_TYPENAME + "_1", path);
        runAssertion(env, true, XML_TYPENAME, FXML, Node.class, XML_TYPENAME + ".l1", path);
        runAssertion(env, true, AVRO_TYPENAME, FAVRO, GenericData.Record.class, AVRO_TYPENAME + "_1", path);

        // Json
        String eplJson = "create json schema " + JSON_TYPENAME + "_4(lvl4 int);\n" +
            "create json schema " + JSON_TYPENAME + "_3(lvl3 int, l4 " + JSON_TYPENAME + "_4[]);\n" +
            "create json schema " + JSON_TYPENAME + "_2(lvl2 int, l3 " + JSON_TYPENAME + "_3[]);\n" +
            "create json schema " + JSON_TYPENAME + "_1(lvl1 int, l2 " + JSON_TYPENAME + "_2[]);\n" +
            "@name('types') @public @buseventtype create json schema " + JSON_TYPENAME + "(l1 " + JSON_TYPENAME + "_1[]);\n";
        env.compileDeploy(eplJson, path);
        runAssertion(env, false, JSON_TYPENAME, FJSON, Object.class, JSON_TYPENAME + "_1", path);
        env.undeployModuleContaining("types");

        // Json-Class-Provided
        String eplJsonProvided =
            "@JsonSchema(className='" + MyLocalJSONProvidedLvl4.class.getName() + "') create json schema " + JSONPROVIDED_TYPENAME + "_4();\n" +
            "@JsonSchema(className='" + MyLocalJSONProvidedLvl3.class.getName() + "') create json schema " + JSONPROVIDED_TYPENAME + "_3(lvl3 int, l4 " + JSONPROVIDED_TYPENAME + "_4[]);\n" +
            "@JsonSchema(className='" + MyLocalJSONProvidedLvl2.class.getName() + "') create json schema " + JSONPROVIDED_TYPENAME + "_2(lvl2 int, l3 " + JSONPROVIDED_TYPENAME + "_3[]);\n" +
            "@JsonSchema(className='" + MyLocalJSONProvidedLvl1.class.getName() + "') create json schema " + JSONPROVIDED_TYPENAME + "_1(lvl1 int, l2 " + JSONPROVIDED_TYPENAME + "_2[]);\n" +
            "@JsonSchema(className='" + MyLocalJSONProvidedTop.class.getName() + "') @name('types') @public @buseventtype create json schema " + JSONPROVIDED_TYPENAME + "(l1 " + JSONPROVIDED_TYPENAME + "_1[]);\n";
        env.compileDeploy(eplJsonProvided, path);
        runAssertion(env, false, JSONPROVIDED_TYPENAME, FJSON, MyLocalJSONProvidedLvl1.class, "EventInfraPropertyNestedIndexedJsonProvided_1", path);

        env.undeployAll();
    }

    private void runAssertion(RegressionEnvironment env, boolean preconfigured, String typename, FunctionSendEvent4IntWArrayNested send, Class nestedClass, String fragmentTypeName, RegressionPath path) {
        runAssertionSelectNested(env, typename, send, path);
        runAssertionBeanNav(env, typename, send, path);
        runAssertionTypeValidProp(env, preconfigured, typename, send, nestedClass, fragmentTypeName);
        runAssertionTypeInvalidProp(env, typename);
    }

    private void runAssertionBeanNav(RegressionEnvironment env, String typename, FunctionSendEvent4IntWArrayNested send, RegressionPath path) {
        String epl = "@name('s0') select * from " + typename;
        env.compileDeploy(epl, path).addListener("s0");

        send.apply(typename, env, 1, 2, 3, 4);
        EventBean event = env.listener("s0").assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "l1[0].lvl1,l1[0].l2[0].lvl2,l1[0].l2[0].l3[0].lvl3,l1[0].l2[0].l3[0].l4[0].lvl4".split(","), new Object[]{1, 2, 3, 4});
        SupportEventTypeAssertionUtil.assertConsistency(event);
        boolean isNative = typename.equals(BEAN_TYPENAME);
        SupportEventTypeAssertionUtil.assertFragments(event, isNative, true, "l1,l1[0].l2,l1[0].l2[0].l3,l1[0].l2[0].l3[0].l4");
        SupportEventTypeAssertionUtil.assertFragments(event, isNative, false, "l1[0],l1[0].l2[0],l1[0].l2[0].l3[0],l1[0].l2[0].l3[0].l4[0]");

        runAssertionEventInvalidProp(event);

        env.undeployModuleContaining("s0");
    }

    private void runAssertionSelectNested(RegressionEnvironment env, String typename, FunctionSendEvent4IntWArrayNested send, RegressionPath path) {
        String epl = "@name('s0') select " +
            "l1[0].lvl1 as c0, " +
            "exists(l1[0].lvl1) as exists_c0, " +
            "l1[0].l2[0].lvl2 as c1, " +
            "exists(l1[0].l2[0].lvl2) as exists_c1, " +
            "l1[0].l2[0].l3[0].lvl3 as c2, " +
            "exists(l1[0].l2[0].l3[0].lvl3) as exists_c2, " +
            "l1[0].l2[0].l3[0].l4[0].lvl4 as c3, " +
            "exists(l1[0].l2[0].l3[0].l4[0].lvl4) as exists_c3 " +
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
        for (String prop : Arrays.asList("l2", "l2[0]", "l1[0].l3", "l1[0].l2[0].l3[0].x")) {
            SupportMessageAssertUtil.tryInvalidProperty(event, prop);
            SupportMessageAssertUtil.tryInvalidGetFragment(event, prop);
        }
    }

    private void runAssertionTypeValidProp(RegressionEnvironment env, boolean preconfigured, String typeName, FunctionSendEvent4IntWArrayNested send, Class nestedClass, String fragmentTypeName) {
        EventType eventType = preconfigured ?
            env.runtime().getEventTypeService().getEventTypePreconfigured(typeName) :
            env.runtime().getEventTypeService().getEventType(env.deploymentId("types"), typeName);

        Class arrayType = nestedClass == Object[].class ? nestedClass : JavaClassHelper.getArrayType(nestedClass);
        arrayType = arrayType == GenericData.Record[].class ? Collection.class : arrayType;
        Object[][] expectedType = new Object[][]{{"l1", arrayType, fragmentTypeName, true}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, eventType, SupportEventTypeAssertionEnum.getSetWithFragment());

        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"l1"}, eventType.getPropertyNames());

        for (String prop : Arrays.asList("l1[0]", "l1[0].lvl1", "l1[0].l2", "l1[0].l2[0]", "l1[0].l2[0].lvl2")) {
            assertNotNull(eventType.getGetter(prop));
            assertTrue(eventType.isProperty(prop));
        }

        assertTrue(JavaClassHelper.isSubclassOrImplementsInterface(eventType.getPropertyType("l1"), arrayType));
        for (String prop : Arrays.asList("l1[0].lvl1", "l1[0].l2[0].lvl2", "l1[0].l2[0].l3[0].lvl3")) {
            assertEquals(Integer.class, JavaClassHelper.getBoxedType(eventType.getPropertyType(prop)));
        }

        FragmentEventType lvl1Fragment = eventType.getFragmentType("l1");
        assertTrue(lvl1Fragment.isIndexed());
        boolean isNative = typeName.equals(BEAN_TYPENAME);
        assertEquals(isNative, lvl1Fragment.isNative());
        assertEquals(fragmentTypeName, lvl1Fragment.getFragmentType().getName());

        FragmentEventType lvl2Fragment = eventType.getFragmentType("l1[0].l2");
        assertTrue(lvl2Fragment.isIndexed());
        assertEquals(isNative, lvl2Fragment.isNative());

        if (typeName.equals(JSON_TYPENAME)) {
            arrayType = JavaClassHelper.getArrayType(SupportJsonEventTypeUtil.getNestedUnderlyingType((JsonEventType) eventType, "l1"));
            nestedClass = arrayType.getComponentType();
        }
        assertEquals(new EventPropertyDescriptor("l1", arrayType, nestedClass, false, false, true, false, true), eventType.getPropertyDescriptor("l1"));
    }

    private void runAssertionTypeInvalidProp(RegressionEnvironment env, String typeName) {
        EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured(typeName);

        for (String prop : Arrays.asList("l2[0]", "l1[0].l3", "l1[0].lvl1.lvl1", "l1[0].l2.l4", "l1[0].l2[0].xx", "l1[0].l2[0].l3[0].lvl5")) {
            assertEquals(false, eventType.isProperty(prop));
            assertEquals(null, eventType.getPropertyType(prop));
            assertNull(eventType.getPropertyDescriptor(prop));
        }
    }

    private static final FunctionSendEvent4IntWArrayNested FBEAN = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        InfraNestedIndexedPropLvl4 l4 = new InfraNestedIndexedPropLvl4(lvl4);
        InfraNestedIndexedPropLvl3 l3 = new InfraNestedIndexedPropLvl3(new InfraNestedIndexedPropLvl4[]{l4}, lvl3);
        InfraNestedIndexedPropLvl2 l2 = new InfraNestedIndexedPropLvl2(new InfraNestedIndexedPropLvl3[]{l3}, lvl2);
        InfraNestedIndexedPropLvl1 l1 = new InfraNestedIndexedPropLvl1(new InfraNestedIndexedPropLvl2[]{l2}, lvl1);
        InfraNestedIndexPropTop top = new InfraNestedIndexPropTop(new InfraNestedIndexedPropLvl1[]{l1});
        env.sendEventBean(top);
    };

    private static final FunctionSendEvent4IntWArrayNested FMAP = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        Map<String, Object> l4 = Collections.singletonMap("lvl4", lvl4);
        Map<String, Object> l3 = twoEntryMap("l4", new Map[]{l4}, "lvl3", lvl3);
        Map<String, Object> l2 = twoEntryMap("l3", new Map[]{l3}, "lvl2", lvl2);
        Map<String, Object> l1 = twoEntryMap("l2", new Map[]{l2}, "lvl1", lvl1);
        Map<String, Object> top = Collections.singletonMap("l1", new Map[]{l1});
        env.sendEventMap(top, eventTypeName);
    };

    private static final FunctionSendEvent4IntWArrayNested FOA = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        Object[] l4 = new Object[]{lvl4};
        Object[] l3 = new Object[]{new Object[]{l4}, lvl3};
        Object[] l2 = new Object[]{new Object[]{l3}, lvl2};
        Object[] l1 = new Object[]{new Object[]{l2}, lvl1};
        Object[] top = new Object[]{new Object[]{l1}};
        env.sendEventObjectArray(top, eventTypeName);
    };

    private static final FunctionSendEvent4IntWArrayNested FXML = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
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

    private static final FunctionSendEvent4IntWArrayNested FAVRO = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        Schema schema = AvroSchemaUtil.resolveAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured(AVRO_TYPENAME));
        Schema lvl1Schema = schema.getField("l1").schema().getElementType();
        Schema lvl2Schema = lvl1Schema.getField("l2").schema().getElementType();
        Schema lvl3Schema = lvl2Schema.getField("l3").schema().getElementType();
        Schema lvl4Schema = lvl3Schema.getField("l4").schema().getElementType();
        GenericData.Record lvl4Rec = new GenericData.Record(lvl4Schema);
        lvl4Rec.put("lvl4", lvl4);
        GenericData.Record lvl3Rec = new GenericData.Record(lvl3Schema);
        lvl3Rec.put("l4", Collections.singletonList(lvl4Rec));
        lvl3Rec.put("lvl3", lvl3);
        GenericData.Record lvl2Rec = new GenericData.Record(lvl2Schema);
        lvl2Rec.put("l3", Collections.singletonList(lvl3Rec));
        lvl2Rec.put("lvl2", lvl2);
        GenericData.Record lvl1Rec = new GenericData.Record(lvl1Schema);
        lvl1Rec.put("l2", Collections.singletonList(lvl2Rec));
        lvl1Rec.put("lvl1", lvl1);
        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("l1", Collections.singletonList(lvl1Rec));
        env.sendEventAvro(datum, eventTypeName);
    };

    private static final FunctionSendEvent4IntWArrayNested FJSON = (eventTypeName, env, lvl1, lvl2, lvl3, lvl4) -> {
        String json = "{\n" +
            "  \"l1\": [\n" +
            "    {\n" +
            "      \"lvl1\": \"${lvl1}\",\n" +
            "      \"l2\": [\n" +
            "        {\n" +
            "          \"lvl2\": \"${lvl2}\",\n" +
            "          \"l3\": [\n" +
            "            {\n" +
            "              \"lvl3\": \"${lvl3}\",\n" +
            "              \"l4\": [\n" +
            "                {\n" +
            "                  \"lvl4\": \"${lvl4}\"\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        json = json.replace("${lvl1}", Integer.toString(lvl1));
        json = json.replace("${lvl2}", Integer.toString(lvl2));
        json = json.replace("${lvl3}", Integer.toString(lvl3));
        json = json.replace("${lvl4}", Integer.toString(lvl4));
        env.sendEventJson(json, eventTypeName);
    };

    @FunctionalInterface
    interface FunctionSendEvent4IntWArrayNested {
        public void apply(String eventTypeName, RegressionEnvironment env, int lvl1, int lvl2, int lvl3, int lvl4);
    }

    public static class InfraNestedIndexPropTop {
        private InfraNestedIndexedPropLvl1[] l1;

        public InfraNestedIndexPropTop(InfraNestedIndexedPropLvl1[] l1) {
            this.l1 = l1;
        }

        public InfraNestedIndexedPropLvl1[] getL1() {
            return l1;
        }
    }

    public static class InfraNestedIndexedPropLvl1 {
        private InfraNestedIndexedPropLvl2[] l2;
        private int lvl1;

        public InfraNestedIndexedPropLvl1(InfraNestedIndexedPropLvl2[] l2, int lvl1) {
            this.l2 = l2;
            this.lvl1 = lvl1;
        }

        public InfraNestedIndexedPropLvl2[] getL2() {
            return l2;
        }

        public int getLvl1() {
            return lvl1;
        }
    }

    public static class InfraNestedIndexedPropLvl2 {
        private InfraNestedIndexedPropLvl3[] l3;
        private int lvl2;

        public InfraNestedIndexedPropLvl2(InfraNestedIndexedPropLvl3[] l3, int lvl2) {
            this.l3 = l3;
            this.lvl2 = lvl2;
        }

        public InfraNestedIndexedPropLvl3[] getL3() {
            return l3;
        }

        public int getLvl2() {
            return lvl2;
        }
    }

    public static class InfraNestedIndexedPropLvl3 {
        private InfraNestedIndexedPropLvl4[] l4;
        private int lvl3;

        public InfraNestedIndexedPropLvl3(InfraNestedIndexedPropLvl4[] l4, int lvl3) {
            this.l4 = l4;
            this.lvl3 = lvl3;
        }

        public InfraNestedIndexedPropLvl4[] getL4() {
            return l4;
        }

        public int getLvl3() {
            return lvl3;
        }
    }

    public static class InfraNestedIndexedPropLvl4 {
        private int lvl4;

        public InfraNestedIndexedPropLvl4(int lvl4) {
            this.lvl4 = lvl4;
        }

        public int getLvl4() {
            return lvl4;
        }
    }

    public static class MyLocalJSONProvidedTop implements Serializable {
        public MyLocalJSONProvidedLvl1[] l1;
    }

    public static class MyLocalJSONProvidedLvl1 implements Serializable {
        public MyLocalJSONProvidedLvl2[] l2;
        public int lvl1;
    }

    public static class MyLocalJSONProvidedLvl2 implements Serializable {
        public MyLocalJSONProvidedLvl3[] l3;
        public int lvl2;
    }

    public static class MyLocalJSONProvidedLvl3 implements Serializable {
        public MyLocalJSONProvidedLvl4[] l4;
        public int lvl3;
    }

    public static class MyLocalJSONProvidedLvl4 implements Serializable {
        public int lvl4;
    }
}
