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
package com.espertech.esper.regressionrun.suite.event;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeObjectArray;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.internal.avro.core.AvroConstant;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.event.infra.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static com.espertech.esper.common.internal.util.CollectionUtil.twoEntryMap;
import static org.apache.avro.SchemaBuilder.*;

public class TestSuiteEventInfra extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEventInfraPropertyUnderlyingSimple() {
        RegressionRunner.run(session, new EventInfraPropertyUnderlyingSimple());
    }

    public void testEventInfraPropertyMappedIndexed() {
        RegressionRunner.run(session, new EventInfraPropertyMappedIndexed());
    }

    public void testEventInfraPropertyDynamicSimple() {
        RegressionRunner.run(session, new EventInfraPropertyDynamicSimple());
    }

    public void testEventInfraPropertyNestedSimple() {
        RegressionRunner.run(session, new EventInfraPropertyNestedSimple());
    }

    public void testEventInfraPropertyDynamicNonSimple() {
        RegressionRunner.run(session, new EventInfraPropertyDynamicNonSimple());
    }

    public void testEventInfraPropertyNestedIndexed() {
        RegressionRunner.run(session, new EventInfraPropertyNestedIndexed());
    }

    public void testEventInfraPropertyDynamicNested() {
        RegressionRunner.run(session, new EventInfraPropertyDynamicNested());
    }

    public void testEventInfraPropertyDynamicNestedRootedSimple() {
        RegressionRunner.run(session, new EventInfraPropertyDynamicNestedRootedSimple());
    }

    public void testEventInfraPropertyDynamicNestedDeep() {
        RegressionRunner.run(session, new EventInfraPropertyDynamicNestedDeep());
    }

    public void testEventInfraPropertyDynamicNestedRootedNonSimple() {
        RegressionRunner.run(session, new EventInfraPropertyDynamicNestedRootedNonSimple());
    }

    public void testEventInfraEventRenderer() {
        RegressionRunner.run(session, new EventInfraEventRenderer());
    }

    public void testEventInfraEventSender() {
        RegressionRunner.run(session, new EventInfraEventSender());
    }

    public void testEventInfraSuperType() {
        RegressionRunner.run(session, new EventInfraSuperType());
    }

    public void testEventInfraPropertyIndexedKeyExpr() {
        RegressionRunner.run(session, new EventInfraPropertyIndexedKeyExpr());
    }

    public void testEventInfraManufacturer() {
        RegressionRunner.run(session, new EventInfraManufacturer());
    }

    public void testEventInfraPropertyAccessPerformance() {
        RegressionRunner.run(session, new EventInfraPropertyAccessPerformance());
    }

    public void testEventInfraGetterSimpleNoFragment() {
        RegressionRunner.run(session, new EventInfraGetterSimpleNoFragment());
    }

    public void testEventInfraGetterSimpleFragment() {
        RegressionRunner.run(session, new EventInfraGetterSimpleFragment());
    }

    public void testEventInfraGetterMapped() {
        RegressionRunner.run(session, new EventInfraGetterMapped());
    }

    public void testEventInfraGetterIndexed() {
        RegressionRunner.run(session, new EventInfraGetterIndexed());
    }

    public void testEventInfraGetterDynamicIndexed() {
        RegressionRunner.run(session, new EventInfraGetterDynamicIndexed());
    }

    public void testEventInfraGetterDynamicIndexexPropertyPredefined() {
        RegressionRunner.run(session, new EventInfraGetterDynamicIndexexPropertyPredefined());
    }

    public void testEventInfraGetterDynamicSimplePropertyPredefined() {
        RegressionRunner.run(session, new EventInfraGetterDynamicSimplePropertyPredefined());
    }

    public void testEventInfraGetterDynamicMapped() {
        RegressionRunner.run(session, new EventInfraGetterDynamicMapped());
    }

    public void testEventInfraGetterDynamicSimple() {
        RegressionRunner.run(session, new EventInfraGetterDynamicSimple());
    }

    public void testEventInfraGetterDynamicNested() {
        RegressionRunner.run(session, new EventInfraGetterDynamicNested());
    }

    public void testEventInfraGetterNestedSimpleNoFragment() {
        RegressionRunner.run(session, new EventInfraGetterNestedSimple());
    }

    public void testEventInfraGetterNestedArray() {
        RegressionRunner.run(session, new EventInfraGetterNestedArray());
    }

    public void testEventInfraGetterNestedSimpleDeep() {
        RegressionRunner.run(session, new EventInfraGetterNestedSimpleDeep());
    }

    public void testEventInfraGetterDynamicNestedDeep() {
        RegressionRunner.run(session, new EventInfraGetterDynamicNestedDeep());
    }

    public void testEventInfraContainedSimple() {
        RegressionRunner.run(session, new EventInfraContainedSimple());
    }

    public void testEventInfraContainedIndexedWithIndex() {
        RegressionRunner.run(session, new EventInfraContainedIndexedWithIndex());
    }

    public void testEventInfraContainedNested() {
        RegressionRunner.run(session, new EventInfraContainedNested());
    }

    public void testEventInfraContainedNestedArray() {
        RegressionRunner.run(session, new EventInfraContainedNestedArray());
    }

    public void testEventInfraPropertyIndexedRuntimeIndex() {
        RegressionRunner.run(session, new EventInfraPropertyIndexedRuntimeIndex());
    }

    public void testEventInfraPropertyMappedRuntimeKey() {
        RegressionRunner.run(session, new EventInfraPropertyMappedRuntimeKey());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportMarkerInterface.class, SupportBeanSimple.class,
            SupportBeanComplexProps.class, SupportBeanDynRoot.class, SupportBeanCombinedProps.class,
            EventInfraPropertyNestedSimple.InfraNestedSimplePropTop.class,
            EventInfraPropertyNestedIndexed.InfraNestedIndexPropTop.class,
            EventInfraEventRenderer.MyEvent.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configureRenderTypes(configuration);
        configureSenderTypes(configuration);
        configureDynamicNonSimpleTypes(configuration);
        configureDynamicSimpleTypes(configuration);
        configureMappedIndexed(configuration);
        configureNestedDynamic(configuration);
        configureNestedDynamicDeep(configuration);
        configuredNestedDynamicRootedSimple(configuration);
        configureNestedDynamicRootedNonSimple(configuration);
        configureNestedIndexed(configuration);
        configureNestedSimple(configuration);
        configureUnderlyingSimple(configuration);
        configureSuperType(configuration);
        configureManufacturerTypes(configuration);
        configureGetterTypes(configuration);
    }

    private static void configureGetterTypes(Configuration configuration) {
        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName(EventInfraGetterSimpleNoFragment.XMLTYPENAME);
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"" + EventInfraGetterSimpleNoFragment.XMLTYPENAME + "\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:attribute name=\"property\" type=\"xs:string\" use=\"required\"/>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraGetterSimpleNoFragment.XMLTYPENAME, eventTypeMeta);
    }

    private static void configureSuperType(Configuration configuration) {
        configuration.getCommon().addEventType("Map_Type_Root", Collections.emptyMap());
        configuration.getCommon().addEventType("Map_Type_1", Collections.emptyMap(), new String[]{"Map_Type_Root"});
        configuration.getCommon().addEventType("Map_Type_2", Collections.emptyMap(), new String[]{"Map_Type_Root"});
        configuration.getCommon().addEventType("Map_Type_2_1", Collections.emptyMap(), new String[]{"Map_Type_2"});

        configuration.getCommon().addEventType("OA_Type_Root", new String[0], new Object[0]);

        ConfigurationCommonEventTypeObjectArray array_1 = new ConfigurationCommonEventTypeObjectArray();
        array_1.setSuperTypes(Collections.singleton("OA_Type_Root"));
        configuration.getCommon().addEventType("OA_Type_1", new String[0], new Object[0], array_1);

        ConfigurationCommonEventTypeObjectArray array_2 = new ConfigurationCommonEventTypeObjectArray();
        array_2.setSuperTypes(Collections.singleton("OA_Type_Root"));
        configuration.getCommon().addEventType("OA_Type_2", new String[0], new Object[0], array_2);

        ConfigurationCommonEventTypeObjectArray array_2_1 = new ConfigurationCommonEventTypeObjectArray();
        array_2_1.setSuperTypes(Collections.singleton("OA_Type_2"));
        configuration.getCommon().addEventType("OA_Type_2_1", new String[0], new Object[0], array_2_1);

        Schema fake = record("fake").fields().endRecord();
        ConfigurationCommonEventTypeAvro avro_root = new ConfigurationCommonEventTypeAvro();
        avro_root.setAvroSchema(fake);
        configuration.getCommon().addEventTypeAvro("Avro_Type_Root", avro_root);
        ConfigurationCommonEventTypeAvro avro_1 = new ConfigurationCommonEventTypeAvro();
        avro_1.setSuperTypes(Collections.singleton("Avro_Type_Root"));
        avro_1.setAvroSchema(fake);
        configuration.getCommon().addEventTypeAvro("Avro_Type_1", avro_1);
        ConfigurationCommonEventTypeAvro avro_2 = new ConfigurationCommonEventTypeAvro();
        avro_2.setSuperTypes(Collections.singleton("Avro_Type_Root"));
        avro_2.setAvroSchema(fake);
        configuration.getCommon().addEventTypeAvro("Avro_Type_2", avro_2);
        ConfigurationCommonEventTypeAvro avro_2_1 = new ConfigurationCommonEventTypeAvro();
        avro_2_1.setSuperTypes(Collections.singleton("Avro_Type_2"));
        avro_2_1.setAvroSchema(fake);
        configuration.getCommon().addEventTypeAvro("Avro_Type_2_1", avro_2_1);

        for (Class clazz : Arrays.asList(EventInfraSuperType.Bean_Type_Root.class, EventInfraSuperType.Bean_Type_1.class, EventInfraSuperType.Bean_Type_2.class, EventInfraSuperType.Bean_Type_2_1.class)) {
            configuration.getCommon().addEventType(clazz);
        }
    }

    private static void configureUnderlyingSimple(Configuration configuration) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("myInt", Integer.class);
        properties.put("myString", "string");
        configuration.getCommon().addEventType(EventInfraPropertyUnderlyingSimple.MAP_TYPENAME, properties);

        String[] names = {"myInt", "myString"};
        Object[] types = {Integer.class, String.class};
        configuration.getCommon().addEventType(EventInfraPropertyUnderlyingSimple.OA_TYPENAME, names, types);

        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:attribute name=\"myInt\" type=\"xs:int\" use=\"required\"/>\n" +
            "\t\t\t<xs:attribute name=\"myString\" type=\"xs:string\" use=\"required\"/>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraPropertyUnderlyingSimple.XML_TYPENAME, eventTypeMeta);

        Schema avroSchema = SchemaBuilder.record(EventInfraPropertyUnderlyingSimple.AVRO_TYPENAME)
            .fields()
            .name("myInt").type().intType().noDefault()
            .name("myString").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro(EventInfraPropertyUnderlyingSimple.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

    private static void configureNestedSimple(Configuration configuration) {

        String mapTypeName = EventInfraPropertyNestedSimple.MAP_TYPENAME;
        configuration.getCommon().addEventType(mapTypeName + "_4", Collections.singletonMap("lvl4", int.class));
        configuration.getCommon().addEventType(mapTypeName + "_3", twoEntryMap("l4", mapTypeName + "_4", "lvl3", int.class));
        configuration.getCommon().addEventType(mapTypeName + "_2", twoEntryMap("l3", mapTypeName + "_3", "lvl2", int.class));
        configuration.getCommon().addEventType(mapTypeName + "_1", twoEntryMap("l2", mapTypeName + "_2", "lvl1", int.class));
        configuration.getCommon().addEventType(mapTypeName, Collections.singletonMap("l1", mapTypeName + "_1"));

        String oaTypeName = EventInfraPropertyNestedSimple.OA_TYPENAME;
        String type_4 = oaTypeName + "_4";
        String[] names_4 = {"lvl4"};
        Object[] types_4 = {int.class};
        configuration.getCommon().addEventType(type_4, names_4, types_4);
        String type_3 = oaTypeName + "_3";
        String[] names_3 = {"l4", "lvl3"};
        Object[] types_3 = {type_4, int.class};
        configuration.getCommon().addEventType(type_3, names_3, types_3);
        String type_2 = oaTypeName + "_2";
        String[] names_2 = {"l3", "lvl2"};
        Object[] types_2 = {type_3, int.class};
        configuration.getCommon().addEventType(type_2, names_2, types_2);
        String type_1 = oaTypeName + "_1";
        String[] names_1 = {"l2", "lvl1"};
        Object[] types_1 = {type_2, int.class};
        configuration.getCommon().addEventType(type_1, names_1, types_1);
        String[] names = {"l1"};
        Object[] types = {type_1};
        configuration.getCommon().addEventType(oaTypeName, names, types);

        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:sequence>\n" +
            "\t\t\t\t<xs:element ref=\"esper:l1\"/>\n" +
            "\t\t\t</xs:sequence>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "\t<xs:element name=\"l1\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:sequence>\n" +
            "\t\t\t\t<xs:element ref=\"esper:l2\"/>\n" +
            "\t\t\t</xs:sequence>\n" +
            "\t\t\t<xs:attribute name=\"lvl1\" type=\"xs:int\" use=\"required\"/>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "\t<xs:element name=\"l2\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:sequence>\n" +
            "\t\t\t\t<xs:element ref=\"esper:l3\"/>\n" +
            "\t\t\t</xs:sequence>\n" +
            "\t\t\t<xs:attribute name=\"lvl2\" type=\"xs:int\" use=\"required\"/>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "\t<xs:element name=\"l3\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:sequence>\n" +
            "\t\t\t\t<xs:element ref=\"esper:l4\"/>\n" +
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
        configuration.getCommon().addEventType(EventInfraPropertyNestedSimple.XML_TYPENAME, eventTypeMeta);

        String avroTypeName = EventInfraPropertyNestedSimple.AVRO_TYPENAME;
        Schema s4 = SchemaBuilder.record(avroTypeName + "_4").fields().requiredInt("lvl4").endRecord();
        Schema s3 = SchemaBuilder.record(avroTypeName + "_3").fields()
            .name("l4").type(s4).noDefault()
            .requiredInt("lvl3")
            .endRecord();
        Schema s2 = SchemaBuilder.record(avroTypeName + "_2").fields()
            .name("l3").type(s3).noDefault()
            .requiredInt("lvl2")
            .endRecord();
        Schema s1 = SchemaBuilder.record(avroTypeName + "_1").fields()
            .name("l2").type(s2).noDefault()
            .requiredInt("lvl1")
            .endRecord();
        Schema avroSchema = SchemaBuilder.record(avroTypeName).fields().name("l1").type(s1).noDefault().endRecord();
        configuration.getCommon().addEventTypeAvro(avroTypeName, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

    private static void configureNestedIndexed(Configuration configuration) {
        configuration.getCommon().addEventType(EventInfraPropertyNestedIndexed.InfraNestedIndexPropTop.class);

        String mapTypeName = EventInfraPropertyNestedIndexed.MAP_TYPENAME;
        configuration.getCommon().addEventType(mapTypeName + "_4", Collections.singletonMap("lvl4", int.class));
        configuration.getCommon().addEventType(mapTypeName + "_3", twoEntryMap("l4", mapTypeName + "_4[]", "lvl3", int.class));
        configuration.getCommon().addEventType(mapTypeName + "_2", twoEntryMap("l3", mapTypeName + "_3[]", "lvl2", int.class));
        configuration.getCommon().addEventType(mapTypeName + "_1", twoEntryMap("l2", mapTypeName + "_2[]", "lvl1", int.class));
        configuration.getCommon().addEventType(mapTypeName, Collections.singletonMap("l1", mapTypeName + "_1[]"));

        String oaTypeName = EventInfraPropertyNestedIndexed.OA_TYPENAME;
        String type_4 = oaTypeName + "_4";
        String[] names_4 = {"lvl4"};
        Object[] types_4 = {int.class};
        configuration.getCommon().addEventType(type_4, names_4, types_4);
        String type_3 = oaTypeName + "_3";
        String[] names_3 = {"l4", "lvl3"};
        Object[] types_3 = {type_4 + "[]", int.class};
        configuration.getCommon().addEventType(type_3, names_3, types_3);
        String type_2 = oaTypeName + "_2";
        String[] names_2 = {"l3", "lvl2"};
        Object[] types_2 = {type_3 + "[]", int.class};
        configuration.getCommon().addEventType(type_2, names_2, types_2);
        String type_1 = oaTypeName + "_1";
        String[] names_1 = {"l2", "lvl1"};
        Object[] types_1 = {type_2 + "[]", int.class};
        configuration.getCommon().addEventType(type_1, names_1, types_1);
        String[] names = {"l1"};
        Object[] types = {type_1 + "[]"};
        configuration.getCommon().addEventType(oaTypeName, names, types);

        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
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
        configuration.getCommon().addEventType(EventInfraPropertyNestedIndexed.XML_TYPENAME, eventTypeMeta);

        Schema s4 = SchemaBuilder.record(EventInfraPropertyNestedIndexed.AVRO_TYPENAME + "_4").fields().requiredInt("lvl4").endRecord();
        Schema s3 = SchemaBuilder.record(EventInfraPropertyNestedIndexed.AVRO_TYPENAME + "_3").fields()
            .name("l4").type(array().items(s4)).noDefault()
            .requiredInt("lvl3")
            .endRecord();
        Schema s2 = SchemaBuilder.record(EventInfraPropertyNestedIndexed.AVRO_TYPENAME + "_2").fields()
            .name("l3").type(array().items(s3)).noDefault()
            .requiredInt("lvl2")
            .endRecord();
        Schema s1 = SchemaBuilder.record(EventInfraPropertyNestedIndexed.AVRO_TYPENAME + "_1").fields()
            .name("l2").type(array().items(s2)).noDefault()
            .requiredInt("lvl1")
            .endRecord();
        Schema avroSchema = SchemaBuilder.record(EventInfraPropertyNestedIndexed.AVRO_TYPENAME).fields().name("l1").type(array().items(s1)).noDefault().endRecord();

        configuration.getCommon().addEventTypeAvro(EventInfraPropertyNestedIndexed.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

    private static void configuredNestedDynamicRootedSimple(Configuration configuration) {
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNestedRootedSimple.MAP_TYPENAME, Collections.emptyMap());

        String type_2 = EventInfraPropertyDynamicNestedRootedSimple.OA_TYPENAME + "_2";
        String[] names_2 = {"nestedNestedValue"};
        Object[] types_2 = {Object.class};
        configuration.getCommon().addEventType(type_2, names_2, types_2);
        String type_1 = EventInfraPropertyDynamicNestedRootedSimple.OA_TYPENAME + "_1";
        String[] names_1 = {"nestedValue", "nestedNested"};
        Object[] types_1 = {Object.class, type_2};
        configuration.getCommon().addEventType(type_1, names_1, types_1);
        String[] names = {"simpleProperty", "nested"};
        Object[] types = {Object.class, type_1};
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNestedRootedSimple.OA_TYPENAME, names, types);

        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNestedRootedSimple.XML_TYPENAME, eventTypeMeta);

        Schema s3 = SchemaBuilder.record(EventInfraPropertyDynamicNestedRootedSimple.AVRO_TYPENAME + "_3").fields()
            .optionalInt("nestedNestedValue")
            .endRecord();
        Schema s2 = SchemaBuilder.record(EventInfraPropertyDynamicNestedRootedSimple.AVRO_TYPENAME + "_2").fields()
            .optionalInt("nestedValue")
            .name("nestedNested").type().unionOf()
            .intType().and().type(s3).endUnion().noDefault()
            .endRecord();
        Schema avroSchema = SchemaBuilder.record(EventInfraPropertyDynamicNestedRootedSimple.AVRO_TYPENAME + "_1").fields()
            .name("simpleProperty").type().unionOf().intType().and().stringType().endUnion().noDefault()
            .name("nested").type().unionOf().intType().and().type(s2).endUnion().noDefault()
            .endRecord();

        configuration.getCommon().addEventTypeAvro(EventInfraPropertyDynamicNestedRootedSimple.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

    private static void configureNestedDynamicRootedNonSimple(Configuration configuration) {
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNestedRootedNonSimple.MAP_TYPENAME, Collections.emptyMap());

        String nestedName = EventInfraPropertyDynamicNestedRootedNonSimple.OA_TYPENAME + "_1";
        String[] namesNested = {"indexed", "mapped", "arrayProperty", "mapProperty"};
        Object[] typesNested = {int[].class, Map.class, int[].class, Map.class};
        configuration.getCommon().addEventType(nestedName, namesNested, typesNested);
        String[] names = {"someprop", "item"};
        Object[] types = {String.class, nestedName};
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNestedRootedNonSimple.OA_TYPENAME, names, types);

        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNestedRootedNonSimple.XML_TYPENAME, eventTypeMeta);

        Schema s1 = SchemaBuilder.record(EventInfraPropertyDynamicNestedRootedNonSimple.AVRO_TYPENAME + "_1").fields()
            .name("indexed").type().unionOf().nullType().and().intType().and().array().items().intType().endUnion().noDefault()
            .name("mapped").type().unionOf().nullType().and().intType().and().map().values().intType().endUnion().noDefault()
            .endRecord();
        Schema avroSchema = SchemaBuilder.record(EventInfraPropertyDynamicNestedRootedNonSimple.AVRO_TYPENAME).fields()
            .name("item").type().unionOf().intType().and().type(s1).endUnion().noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro(EventInfraPropertyDynamicNestedRootedNonSimple.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

    private static void configureNestedDynamicDeep(Configuration configuration) {
        Map<String, Object> top = Collections.singletonMap("item", Map.class);
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNestedDeep.MAP_TYPENAME, top);

        String type_3 = EventInfraPropertyDynamicNestedDeep.OA_TYPENAME + "_3";
        String[] names_3 = {"nestedNestedValue"};
        Object[] types_3 = {Object.class};
        configuration.getCommon().addEventType(type_3, names_3, types_3);
        String type_2 = EventInfraPropertyDynamicNestedDeep.OA_TYPENAME + "_2";
        String[] names_2 = {"nestedNested", "nestedValue"};
        Object[] types_2 = {type_3, Object.class};
        configuration.getCommon().addEventType(type_2, names_2, types_2);
        String type_1 = EventInfraPropertyDynamicNestedDeep.OA_TYPENAME + "_1";
        String[] names_1 = {"nested"};
        Object[] types_1 = {type_2};
        configuration.getCommon().addEventType(type_1, names_1, types_1);
        String[] names = {"item"};
        Object[] types = {type_1};
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNestedDeep.OA_TYPENAME, names, types);

        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:sequence>\n" +
            "\t\t\t\t<xs:element ref=\"esper:item\"/>\n" +
            "\t\t\t</xs:sequence>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "\t<xs:element name=\"item\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNestedDeep.XML_TYPENAME, eventTypeMeta);

        Schema s3 = SchemaBuilder.record(EventInfraPropertyDynamicNestedDeep.AVRO_TYPENAME + "_3").fields()
            .optionalInt("nestedNestedValue")
            .endRecord();
        Schema s2 = SchemaBuilder.record(EventInfraPropertyDynamicNestedDeep.AVRO_TYPENAME + "_2").fields()
            .optionalInt("nestedValue")
            .name("nestedNested").type().unionOf()
            .intType().and().type(s3).endUnion().noDefault()
            .endRecord();
        Schema s1 = SchemaBuilder.record(EventInfraPropertyDynamicNestedDeep.AVRO_TYPENAME + "_1").fields()
            .name("nested").type().unionOf()
            .intType().and().type(s2).endUnion().noDefault()
            .endRecord();
        Schema avroSchema = SchemaBuilder.record(EventInfraPropertyDynamicNestedDeep.AVRO_TYPENAME).fields().name("item").type(s1).noDefault().endRecord();
        configuration.getCommon().addEventTypeAvro(EventInfraPropertyDynamicNestedDeep.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

    private static void configureNestedDynamic(Configuration configuration) {
        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:sequence>\n" +
            "\t\t\t\t<xs:element ref=\"esper:item\"/>\n" +
            "\t\t\t</xs:sequence>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "\t<xs:element name=\"item\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNested.XML_TYPENAME, eventTypeMeta);

        Map<String, Object> top = Collections.singletonMap("item", Map.class);
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNested.MAP_TYPENAME, top);

        String[] names = {"item"};
        Object[] types = {Object.class};
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNested.OA_TYPENAME, names, types);

        Schema s1 = SchemaBuilder.record(EventInfraPropertyDynamicNested.AVRO_TYPENAME + "_1").fields()
            .name("id").type().unionOf()
            .intBuilder().endInt()
            .and()
            .stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString()
            .and()
            .nullType()
            .endUnion().noDefault()
            .endRecord();
        Schema avroSchema = SchemaBuilder.record(EventInfraPropertyDynamicNested.AVRO_TYPENAME).fields().name("item").type(s1).noDefault().endRecord();
        configuration.getCommon().addEventTypeAvro(EventInfraPropertyDynamicNested.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

    private static void configureMappedIndexed(Configuration configuration) {
        configuration.getCommon().addEventType(EventInfraPropertyMappedIndexed.MyIMEvent.class);

        configuration.getCommon().addEventType(EventInfraPropertyMappedIndexed.MAP_TYPENAME, twoEntryMap("indexed", String[].class, "mapped", Map.class));

        String[] names = {"indexed", "mapped"};
        Object[] types = {String[].class, Map.class};
        configuration.getCommon().addEventType(EventInfraPropertyMappedIndexed.OA_TYPENAME, names, types);

        Schema avroSchema = record("AvroSchema").fields()
            .name("indexed").type(array().items().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
            .name("mapped").type(map().values().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro(EventInfraPropertyMappedIndexed.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));

    }

    private static void configureDynamicSimpleTypes(Configuration configuration) {
        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraPropertyDynamicSimple.XML_TYPENAME, eventTypeMeta);

        configuration.getCommon().addEventType(EventInfraPropertyDynamicSimple.MAP_TYPENAME, Collections.emptyMap());
        String[] names = {"somefield", "id"};
        Object[] types = {Object.class, Object.class};
        configuration.getCommon().addEventType(EventInfraPropertyDynamicSimple.OA_TYPENAME, names, types);

        Schema avroSchema = SchemaBuilder.record(EventInfraPropertyDynamicSimple.AVRO_TYPENAME).fields()
            .name("id").type().unionOf()
            .nullType().and().intType().and().booleanType().endUnion().noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro(EventInfraPropertyDynamicSimple.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

    private static void configureDynamicNonSimpleTypes(Configuration configuration) {
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNonSimple.MAP_TYPENAME, Collections.emptyMap());

        String[] names = {"indexed", "mapped"};
        Object[] types = {int[].class, Map.class};
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNonSimple.OA_TYPENAME, names, types);

        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraPropertyDynamicNonSimple.XML_TYPENAME, eventTypeMeta);

        Schema avroSchema = SchemaBuilder.record(EventInfraPropertyDynamicNonSimple.AVRO_TYPENAME).fields()
            .name("indexed").type().unionOf().nullType().and().intType().and().array().items().intType().endUnion().noDefault()
            .name("mapped").type().unionOf().nullType().and().intType().and().map().values().intType().endUnion().noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro(EventInfraPropertyDynamicNonSimple.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

    private static void configureSenderTypes(Configuration configuration) {
        ConfigurationCommonEventTypeXMLDOM eventInfraEventSenderMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventInfraEventSenderMeta.setRootElementName("myevent");
        String eventInfraEventSenderSchema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventInfraEventSenderMeta.setSchemaText(eventInfraEventSenderSchema);
        configuration.getCommon().addEventType(EventInfraEventSender.XML_TYPENAME, eventInfraEventSenderMeta);

        configuration.getCommon().addEventType(EventInfraEventSender.MAP_TYPENAME, Collections.emptyMap());

        String[] names = {};
        Object[] types = {};
        configuration.getCommon().addEventType(EventInfraEventSender.OA_TYPENAME, names, types);
        configuration.getCommon().addEventTypeAvro(EventInfraEventSender.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(SchemaBuilder.record(EventInfraEventSender.AVRO_TYPENAME).fields().endRecord()));
    }

    private static void configureRenderTypes(Configuration configuration) {
        ConfigurationCommonEventTypeXMLDOM myXMLEventConfig = new ConfigurationCommonEventTypeXMLDOM();
        myXMLEventConfig.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:sequence minOccurs=\"0\" maxOccurs=\"unbounded\">\n" +
            "\t\t\t\t<xs:choice>\n" +
            "\t\t\t\t\t<xs:element ref=\"esper:nested\" minOccurs=\"1\" maxOccurs=\"1\"/>\n" +
            "\t\t\t\t</xs:choice>\n" +
            "\t\t\t</xs:sequence>\n" +
            "\t\t\t<xs:attribute name=\"myInt\" type=\"xs:int\" use=\"required\"/>\n" +
            "\t\t\t<xs:attribute name=\"myString\" type=\"xs:string\" use=\"required\"/>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "\t<xs:element name=\"nested\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:attribute name=\"myInsideInt\" type=\"xs:int\" use=\"required\"/>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        myXMLEventConfig.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraEventRenderer.XML_TYPENAME, myXMLEventConfig);

        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("myInsideInt", "int");
        Map<String, Object> top = new LinkedHashMap<>();
        top.put("myInt", "int");
        top.put("myString", "string");
        top.put("nested", inner);
        configuration.getCommon().addEventType(EventInfraEventRenderer.MAP_TYPENAME, top);

        String[] namesInner = new String[]{"myInsideInt"};
        Object[] typesInner = new Object[]{int.class};
        configuration.getCommon().addEventType(EventInfraEventRenderer.OA_TYPENAME + "_1", namesInner, typesInner);

        String[] names = new String[]{"myInt", "myString", "nested"};
        Object[] types = new Object[]{int.class, String.class, EventInfraEventRenderer.OA_TYPENAME + "_1"};
        configuration.getCommon().addEventType(EventInfraEventRenderer.OA_TYPENAME, names, types);

        Schema eventInfraEventRenderSchemaInner = SchemaBuilder.record(EventInfraEventRenderer.AVRO_TYPENAME + "_inside")
            .fields()
            .name("myInsideInt").type().intType().noDefault()
            .endRecord();
        Schema eventInfraEventRenderSchema = SchemaBuilder.record(EventInfraEventRenderer.AVRO_TYPENAME)
            .fields()
            .name("myInt").type().intType().noDefault()
            .name("myString").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
            .name("nested").type(eventInfraEventRenderSchemaInner).noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro(EventInfraEventRenderer.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(eventInfraEventRenderSchema));
    }

    private static void configureManufacturerTypes(Configuration configuration) {
        ConfigurationCommonEventTypeXMLDOM myXMLEventConfig = new ConfigurationCommonEventTypeXMLDOM();
        myXMLEventConfig.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:attribute name=\"p0\" type=\"xs:string\" use=\"required\"/>\n" +
            "\t\t\t<xs:attribute name=\"p1\" type=\"xs:int\" use=\"required\"/>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        myXMLEventConfig.setSchemaText(schema);
        configuration.getCommon().addEventType(EventInfraManufacturer.XML_TYPENAME, myXMLEventConfig);

        Schema avroSchema = SchemaBuilder.record(EventInfraManufacturer.AVRO_TYPENAME)
            .fields()
            .name("p1").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
            .name("p2").type().intType().noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro(EventInfraManufacturer.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));
    }

}
