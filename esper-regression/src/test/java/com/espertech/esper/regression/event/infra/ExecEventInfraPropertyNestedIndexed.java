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
package com.espertech.esper.regression.event.infra;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static junit.framework.TestCase.*;
import static org.apache.avro.SchemaBuilder.array;
import static org.junit.Assert.assertNotNull;

public class ExecEventInfraPropertyNestedIndexed implements RegressionExecution {
    private final static String BEAN_TYPENAME = InfraNestedIndexPropTop.class.getName();

    private static final FunctionSendEvent4Int FBEAN = (epService, lvl1, lvl2, lvl3, lvl4) -> {
        InfraNestedIndexedPropLvl4 l4 = new InfraNestedIndexedPropLvl4(lvl4);
        InfraNestedIndexedPropLvl3 l3 = new InfraNestedIndexedPropLvl3(new InfraNestedIndexedPropLvl4[]{l4}, lvl3);
        InfraNestedIndexedPropLvl2 l2 = new InfraNestedIndexedPropLvl2(new InfraNestedIndexedPropLvl3[]{l3}, lvl2);
        InfraNestedIndexedPropLvl1 l1 = new InfraNestedIndexedPropLvl1(new InfraNestedIndexedPropLvl2[]{l2}, lvl1);
        InfraNestedIndexPropTop top = new InfraNestedIndexPropTop(new InfraNestedIndexedPropLvl1[]{l1});
        epService.getEPRuntime().sendEvent(top);
    };

    private static final FunctionSendEvent4Int FMAP = (epService, lvl1, lvl2, lvl3, lvl4) -> {
        Map<String, Object> l4 = Collections.singletonMap("lvl4", lvl4);
        Map<String, Object> l3 = twoEntryMap("l4", new Map[]{l4}, "lvl3", lvl3);
        Map<String, Object> l2 = twoEntryMap("l3", new Map[]{l3}, "lvl2", lvl2);
        Map<String, Object> l1 = twoEntryMap("l2", new Map[]{l2}, "lvl1", lvl1);
        Map<String, Object> top = Collections.singletonMap("l1", new Map[]{l1});
        epService.getEPRuntime().sendEvent(top, MAP_TYPENAME);
    };

    private static final FunctionSendEvent4Int FOA = (epService, lvl1, lvl2, lvl3, lvl4) -> {
        Object[] l4 = new Object[]{lvl4};
        Object[] l3 = new Object[]{new Object[]{l4}, lvl3};
        Object[] l2 = new Object[]{new Object[]{l3}, lvl2};
        Object[] l1 = new Object[]{new Object[]{l2}, lvl1};
        Object[] top = new Object[]{new Object[]{l1}};
        epService.getEPRuntime().sendEvent(top, OA_TYPENAME);
    };

    private static final FunctionSendEvent4Int FXML = (epService, lvl1, lvl2, lvl3, lvl4) -> {
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
            SupportXML.sendEvent(epService.getEPRuntime(), xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    private static final FunctionSendEvent4Int FAVRO = (epService, lvl1, lvl2, lvl3, lvl4) -> {
        Schema schema = getAvroSchema();
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
        epService.getEPRuntime().sendEventAvro(datum, AVRO_TYPENAME);
    };

    public void configure(Configuration configuration) {
        addXMLEventType(configuration);
    }

    public void run(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(BEAN_TYPENAME, InfraNestedIndexPropTop.class);
        addMapEventType(epService);
        addOAEventType(epService);
        addAvroEventType(epService);

        runAssertion(epService, BEAN_TYPENAME, FBEAN, InfraNestedIndexedPropLvl1.class, InfraNestedIndexedPropLvl1.class.getName());
        runAssertion(epService, MAP_TYPENAME, FMAP, Map.class, MAP_TYPENAME + "_1");
        runAssertion(epService, OA_TYPENAME, FOA, Object[].class, OA_TYPENAME + "_1");
        runAssertion(epService, XML_TYPENAME, FXML, Node.class, "MyXMLEvent.l1");
        runAssertion(epService, AVRO_TYPENAME, FAVRO, GenericData.Record.class, AVRO_TYPENAME + "_1");
    }

    private void runAssertion(EPServiceProvider epService, String typename, FunctionSendEvent4Int send, Class nestedClass, String fragmentTypeName) {
        runAssertionSelectNested(epService, typename, send);
        runAssertionBeanNav(epService, typename, send);
        runAssertionTypeValidProp(epService, typename, send, nestedClass, fragmentTypeName);
        runAssertionTypeInvalidProp(epService, typename);
    }

    private void runAssertionBeanNav(EPServiceProvider epService, String typename, FunctionSendEvent4Int send) {
        String epl = "select * from " + typename;
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        send.apply(epService, 1, 2, 3, 4);
        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, "l1[0].lvl1,l1[0].l2[0].lvl2,l1[0].l2[0].l3[0].lvl3,l1[0].l2[0].l3[0].l4[0].lvl4".split(","), new Object[]{1, 2, 3, 4});
        SupportEventTypeAssertionUtil.assertConsistency(event);
        boolean isNative = typename.equals(BEAN_TYPENAME);
        SupportEventTypeAssertionUtil.assertFragments(event, isNative, true, "l1,l1[0].l2,l1[0].l2[0].l3,l1[0].l2[0].l3[0].l4");
        SupportEventTypeAssertionUtil.assertFragments(event, isNative, false, "l1[0],l1[0].l2[0],l1[0].l2[0].l3[0],l1[0].l2[0].l3[0].l4[0]");

        runAssertionEventInvalidProp(event);

        statement.destroy();
    }

    private void runAssertionSelectNested(EPServiceProvider epService, String typename, FunctionSendEvent4Int send) {
        String epl = "select " +
                "l1[0].lvl1 as c0, " +
                "exists(l1[0].lvl1) as exists_c0, " +
                "l1[0].l2[0].lvl2 as c1, " +
                "exists(l1[0].l2[0].lvl2) as exists_c1, " +
                "l1[0].l2[0].l3[0].lvl3 as c2, " +
                "exists(l1[0].l2[0].l3[0].lvl3) as exists_c2, " +
                "l1[0].l2[0].l3[0].l4[0].lvl4 as c3, " +
                "exists(l1[0].l2[0].l3[0].l4[0].lvl4) as exists_c3 " +
                "from " + typename;
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        String[] fields = "c0,exists_c0,c1,exists_c1,c2,exists_c2,c3,exists_c3".split(",");

        for (String property : fields) {
            assertEquals(property.startsWith("exists") ? Boolean.class : Integer.class, JavaClassHelper.getBoxedType(statement.getEventType().getPropertyType(property)));
        }

        send.apply(epService, 1, 2, 3, 4);
        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[]{1, true, 2, true, 3, true, 4, true});
        SupportEventTypeAssertionUtil.assertConsistency(event);

        send.apply(epService, 10, 5, 50, 400);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, true, 5, true, 50, true, 400, true});

        statement.destroy();
    }

    private void addMapEventType(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME + "_4", Collections.singletonMap("lvl4", int.class));
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME + "_3", twoEntryMap("l4", MAP_TYPENAME + "_4[]", "lvl3", int.class));
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME + "_2", twoEntryMap("l3", MAP_TYPENAME + "_3[]", "lvl2", int.class));
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME + "_1", twoEntryMap("l2", MAP_TYPENAME + "_2[]", "lvl1", int.class));
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, Collections.singletonMap("l1", MAP_TYPENAME + "_1[]"));
    }

    private void addOAEventType(EPServiceProvider epService) {
        String type_4 = OA_TYPENAME + "_4";
        String[] names_4 = {"lvl4"};
        Object[] types_4 = {int.class};
        epService.getEPAdministrator().getConfiguration().addEventType(type_4, names_4, types_4);
        String type_3 = OA_TYPENAME + "_3";
        String[] names_3 = {"l4", "lvl3"};
        Object[] types_3 = {type_4 + "[]", int.class};
        epService.getEPAdministrator().getConfiguration().addEventType(type_3, names_3, types_3);
        String type_2 = OA_TYPENAME + "_2";
        String[] names_2 = {"l3", "lvl2"};
        Object[] types_2 = {type_3 + "[]", int.class};
        epService.getEPAdministrator().getConfiguration().addEventType(type_2, names_2, types_2);
        String type_1 = OA_TYPENAME + "_1";
        String[] names_1 = {"l2", "lvl1"};
        Object[] types_1 = {type_2 + "[]", int.class};
        epService.getEPAdministrator().getConfiguration().addEventType(type_1, names_1, types_1);
        String[] names = {"l1"};
        Object[] types = {type_1 + "[]"};
        epService.getEPAdministrator().getConfiguration().addEventType(OA_TYPENAME, names, types);
    }

    private void addXMLEventType(Configuration configuration) {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "\t<xs:element name=\"myevent\">\n" +
                "\t\t<xs:complexType>\n" +
                "\t\t\t<xs:sequence>\n" +
                "\t\t\t\t<xs:element ref=\"esper:l1\" maxOccurs=\"unbounded\"/>\n" +
                "\t\t\t</xs:sequence>\n" +
                "\t\t</xs:complexType>\n" +
                "\t</xs:element>\n" +
                "\t<xs:element name=\"l1\">\n" +
                "\t\t<xs:complexType>\n" +
                "\t\t\t<xs:sequence>\n" +
                "\t\t\t\t<xs:element ref=\"esper:l2\" maxOccurs=\"unbounded\"/>\n" +
                "\t\t\t</xs:sequence>\n" +
                "\t\t\t<xs:attribute name=\"lvl1\" type=\"xs:int\" use=\"required\"/>\n" +
                "\t\t</xs:complexType>\n" +
                "\t</xs:element>\n" +
                "\t<xs:element name=\"l2\">\n" +
                "\t\t<xs:complexType>\n" +
                "\t\t\t<xs:sequence>\n" +
                "\t\t\t\t<xs:element ref=\"esper:l3\" maxOccurs=\"unbounded\"/>\n" +
                "\t\t\t</xs:sequence>\n" +
                "\t\t\t<xs:attribute name=\"lvl2\" type=\"xs:int\" use=\"required\"/>\n" +
                "\t\t</xs:complexType>\n" +
                "\t</xs:element>\n" +
                "\t<xs:element name=\"l3\">\n" +
                "\t\t<xs:complexType>\n" +
                "\t\t\t<xs:sequence>\n" +
                "\t\t\t\t<xs:element ref=\"esper:l4\" maxOccurs=\"unbounded\"/>\n" +
                "\t\t\t</xs:sequence>\n" +
                "\t\t\t<xs:attribute name=\"lvl3\" type=\"xs:int\" use=\"required\"/>\n" +
                "\t\t</xs:complexType>\n" +
                "\t</xs:element>\n" +
                "\t<xs:element name=\"l4\">\n" +
                "\t\t<xs:complexType>\n" +
                "\t\t\t<xs:attribute name=\"lvl4\" type=\"xs:int\" use=\"required\"/>\n" +
                "\t\t</xs:complexType>\n" +
                "\t</xs:element>\n" +
                "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.addEventType(XML_TYPENAME, eventTypeMeta);
    }

    private void addAvroEventType(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(getAvroSchema()));
    }

    private static Schema getAvroSchema() {
        Schema s4 = SchemaBuilder.record(AVRO_TYPENAME + "_4").fields().requiredInt("lvl4").endRecord();
        Schema s3 = SchemaBuilder.record(AVRO_TYPENAME + "_3").fields()
                .name("l4").type(array().items(s4)).noDefault()
                .requiredInt("lvl3")
                .endRecord();
        Schema s2 = SchemaBuilder.record(AVRO_TYPENAME + "_2").fields()
                .name("l3").type(array().items(s3)).noDefault()
                .requiredInt("lvl2")
                .endRecord();
        Schema s1 = SchemaBuilder.record(AVRO_TYPENAME + "_1").fields()
                .name("l2").type(array().items(s2)).noDefault()
                .requiredInt("lvl1")
                .endRecord();
        return SchemaBuilder.record(AVRO_TYPENAME).fields().name("l1").type(array().items(s1)).noDefault().endRecord();
    }

    private void runAssertionEventInvalidProp(EventBean event) {
        for (String prop : Arrays.asList("l2", "l2[0]", "l1[0].l3", "l1[0].l2[0].l3[0].x")) {
            SupportMessageAssertUtil.tryInvalidProperty(event, prop);
            SupportMessageAssertUtil.tryInvalidGetFragment(event, prop);
        }
    }

    private void runAssertionTypeValidProp(EPServiceProvider epService, String typeName, FunctionSendEvent4Int send, Class nestedClass, String fragmentTypeName) {
        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType(typeName);

        Class arrayType = nestedClass == Object[].class ? nestedClass : JavaClassHelper.getArrayType(nestedClass);
        arrayType = arrayType == GenericData.Record[].class ? Collection.class : arrayType;
        Object[][] expectedType = new Object[][]{{"l1", arrayType, fragmentTypeName, true}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, eventType, SupportEventTypeAssertionEnum.getSetWithFragment());

        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"l1"}, eventType.getPropertyNames());

        for (String prop : Arrays.asList("l1[0]", "l1[0].lvl1", "l1[0].l2", "l1[0].l2[0]", "l1[0].l2[0].lvl2")) {
            assertNotNull(eventType.getGetter(prop));
            assertTrue(eventType.isProperty(prop));
        }

        assertEquals(arrayType, eventType.getPropertyType("l1"));
        for (String prop : Arrays.asList("l1[0].lvl1", "l1[0].l2[0].lvl2", "l1[0].l2[0].l3[0].lvl3")) {
            assertEquals(Integer.class, JavaClassHelper.getBoxedType(eventType.getPropertyType(prop)));
        }

        FragmentEventType lvl1Fragment = eventType.getFragmentType("l1");
        assertTrue(lvl1Fragment.isIndexed());
        assertEquals(send == FBEAN, lvl1Fragment.isNative());
        assertEquals(fragmentTypeName, lvl1Fragment.getFragmentType().getName());

        FragmentEventType lvl2Fragment = eventType.getFragmentType("l1[0].l2");
        assertTrue(lvl2Fragment.isIndexed());
        assertEquals(send == FBEAN, lvl2Fragment.isNative());

        assertEquals(new EventPropertyDescriptor("l1", arrayType, nestedClass, false, false, true, false, true), eventType.getPropertyDescriptor("l1"));
    }

    private void runAssertionTypeInvalidProp(EPServiceProvider epService, String typeName) {
        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType(typeName);

        for (String prop : Arrays.asList("l2[0]", "l1[0].l3", "l1[0].lvl1.lvl1", "l1[0].l2.l4", "l1[0].l2[0].xx", "l1[0].l2[0].l3[0].lvl5")) {
            assertEquals(false, eventType.isProperty(prop));
            assertEquals(null, eventType.getPropertyType(prop));
            assertNull(eventType.getPropertyDescriptor(prop));
        }
    }

    @FunctionalInterface
    interface FunctionSendEvent4Int {
        public void apply(EPServiceProvider epService, int lvl1, int lvl2, int lvl3, int lvl4);
    }

    public final static class InfraNestedIndexPropTop {
        private InfraNestedIndexedPropLvl1[] l1;

        public InfraNestedIndexPropTop(InfraNestedIndexedPropLvl1[] l1) {
            this.l1 = l1;
        }

        public InfraNestedIndexedPropLvl1[] getL1() {
            return l1;
        }
    }

    public final static class InfraNestedIndexedPropLvl1 {
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

    public final static class InfraNestedIndexedPropLvl2 {
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

    public final static class InfraNestedIndexedPropLvl3 {
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

    public final static class InfraNestedIndexedPropLvl4 {
        private int lvl4;

        public InfraNestedIndexedPropLvl4(int lvl4) {
            this.lvl4 = lvl4;
        }

        public int getLvl4() {
            return lvl4;
        }
    }
}
