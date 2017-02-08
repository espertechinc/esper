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
package com.espertech.esper.regression.event;

import com.espertech.esper.avro.core.AvroSchemaUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.SupportEventInfra;
import com.espertech.esper.supportregression.event.ValueWithExistsFlag;
import com.espertech.esper.util.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.util.support.SupportEventTypeAssertionUtil;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static com.espertech.esper.supportregression.event.ValueWithExistsFlag.*;

public class TestEventInfraPropertyNestedDynamicDeep extends TestCase {
    private final static String BEAN_TYPENAME = SupportBeanDynRoot.class.getName();

    private static final SupportEventInfra.FunctionSendEvent FAVRO = (epService, value) -> {
        Schema schema = getAvroSchema();
        Schema itemSchema = schema.getField("item").schema();
        GenericData.Record itemDatum = new GenericData.Record(itemSchema);
        itemDatum.put("nested", value);
        GenericData.Record datum = new GenericData.Record(schema);
        datum.put("item", itemDatum);
        epService.getEPRuntime().sendEventAvro(datum, AVRO_TYPENAME);
    };

    private EPServiceProvider epService;

    protected void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        addXMLEventType(configuration);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        addMapEventType();
        addOAEventType();
        epService.getEPAdministrator().getConfiguration().addEventType(BEAN_TYPENAME, SupportBeanDynRoot.class);
        addAvroEventType();

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testIt() {
        final ValueWithExistsFlag[] NOT_EXISTS = multipleNotExists(6);

        // Bean
        SupportBeanComplexProps beanOne = SupportBeanComplexProps.makeDefaultBean();
        String n1_v = beanOne.getNested().getNestedValue();
        String n1_n_v = beanOne.getNested().getNestedNested().getNestedNestedValue();
        SupportBeanComplexProps beanTwo = SupportBeanComplexProps.makeDefaultBean();
        beanTwo.getNested().setNestedValue("nested1");
        beanTwo.getNested().getNestedNested().setNestedNestedValue("nested2");
        Pair[] beanTests = new Pair[]{
                new Pair<>(new SupportBeanDynRoot(beanOne), allExist(n1_v, n1_v, n1_n_v, n1_n_v, n1_n_v, n1_n_v)),
                new Pair<>(new SupportBeanDynRoot(beanTwo), allExist("nested1", "nested1", "nested2", "nested2", "nested2", "nested2")),
                new Pair<>(new SupportBeanDynRoot("abc"), NOT_EXISTS)
        };
        runAssertion(BEAN_TYPENAME, FBEAN, null, beanTests, Object.class);

        // Map
        Map<String,Object> mapOneL2 = new HashMap<>();
        mapOneL2.put("nestedNestedValue", 101);
        Map<String,Object> mapOneL1 = new HashMap<>();
        mapOneL1.put("nestedNested", mapOneL2);
        mapOneL1.put("nestedValue", 100);
        Map<String,Object> mapOneL0 = new HashMap<>();
        mapOneL0.put("nested", mapOneL1);
        Map<String,Object> mapOne = Collections.singletonMap("item", mapOneL0);
        Pair[] mapTests = new Pair[]{
                new Pair<>(mapOne, allExist(100, 100, 101, 101, 101, 101)),
                new Pair<>(Collections.emptyMap(), NOT_EXISTS),
        };
        runAssertion(MAP_TYPENAME, FMAP, null, mapTests, Object.class);

        // Object-Array
        Object[] oaOneL2 = new Object[] {101};
        Object[] oaOneL1 = new Object[] {oaOneL2, 100};
        Object[] oaOneL0 = new Object[] {oaOneL1};
        Object[] oaOne = new Object[] {oaOneL0};
        Pair[] oaTests = new Pair[]{
                new Pair<>(oaOne, allExist(100, 100, 101, 101, 101, 101)),
                new Pair<>(new Object[] {null}, NOT_EXISTS),
        };
        runAssertion(OA_TYPENAME, FOA, null, oaTests, Object.class);

        // XML
        Pair[] xmlTests = new Pair[]{
                new Pair<>("<item>\n" +
                        "\t<nested nestedValue=\"100\">\n" +
                        "\t\t<nestedNested nestedNestedValue=\"101\">\n" +
                        "\t\t</nestedNested>\n" +
                        "\t</nested>\n" +
                        "</item>\n", allExist("100", "100", "101", "101", "101", "101")),
                new Pair<>("<item/>", NOT_EXISTS),
        };
        runAssertion(XML_TYPENAME, FXML, XML_TO_VALUE, xmlTests, Node.class);

        // Avro
        Schema schema = getAvroSchema();
        Schema nestedSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(schema.getField("item").schema().getField("nested").schema());
        Schema nestedNestedSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(nestedSchema.getField("nestedNested").schema());
        GenericData.Record nestedNestedDatum = new GenericData.Record(nestedNestedSchema);
        nestedNestedDatum.put("nestedNestedValue", 101);
        GenericData.Record nestedDatum = new GenericData.Record(nestedSchema);
        nestedDatum.put("nestedValue", 100);
        nestedDatum.put("nestedNested", nestedNestedDatum);
        GenericData.Record emptyDatum = new GenericData.Record(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord());
        Pair[] avroTests = new Pair[]{
                new Pair<>(nestedDatum, allExist(100, 100, 101, 101, 101, 101)),
                new Pair<>(emptyDatum, NOT_EXISTS),
                new Pair<>(null, NOT_EXISTS)
        };
        runAssertion(AVRO_TYPENAME, FAVRO, null, avroTests, Object.class);
    }

    private void runAssertion(String typename,
                              FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType) {
        runAssertionSelectNested(typename, send, optionalValueConversion, tests, expectedPropertyType);
        runAssertionBeanNav(typename, send, tests[0].getFirst());
    }

    private void runAssertionBeanNav(String typename,
                                          FunctionSendEvent send,
                                          Object underlyingComplete) {
        String stmtText = "select * from " + typename;

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        send.apply(epService, underlyingComplete);
        EventBean event = listener.assertOneGetNewAndReset();
        SupportEventTypeAssertionUtil.assertConsistency(event);

        stmt.destroy();
    }

    private void runAssertionSelectNested(String typename,
                              FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType) {

        String stmtText = "select " +
                " item.nested?.nestedValue as n1, " +
                " exists(item.nested?.nestedValue) as exists_n1, " +
                " item.nested?.nestedValue? as n2, " +
                " exists(item.nested?.nestedValue?) as exists_n2, " +
                " item.nested?.nestedNested.nestedNestedValue as n3, " +
                " exists(item.nested?.nestedNested.nestedNestedValue) as exists_n3, " +
                " item.nested?.nestedNested?.nestedNestedValue as n4, " +
                " exists(item.nested?.nestedNested?.nestedNestedValue) as exists_n4, " +
                " item.nested?.nestedNested.nestedNestedValue? as n5, " +
                " exists(item.nested?.nestedNested.nestedNestedValue?) as exists_n5, " +
                " item.nested?.nestedNested?.nestedNestedValue? as n6, " +
                " exists(item.nested?.nestedNested?.nestedNestedValue?) as exists_n6 " +
                " from " + typename;

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] propertyNames = "n1,n2,n3,n4,n5,n6".split(",");
        for (String propertyName : propertyNames) {
            assertEquals(expectedPropertyType, stmt.getEventType().getPropertyType(propertyName));
            assertEquals(Boolean.class, stmt.getEventType().getPropertyType("exists_" + propertyName));
        }

        for (Pair pair : tests) {
            send.apply(epService, pair.getFirst());
            EventBean event = listener.assertOneGetNewAndReset();
            SupportEventInfra.assertValuesMayConvert(event, propertyNames, (ValueWithExistsFlag[]) pair.getSecond(), optionalValueConversion);
        }

        stmt.destroy();
    }

    private void addMapEventType() {
        Map<String, Object> top = Collections.singletonMap("item", Map.class);
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, top);
    }

    private void addOAEventType() {
        String type_3 = OA_TYPENAME + "_3";
        String[] names_3 = {"nestedNestedValue"};
        Object[] types_3 = {Object.class};
        epService.getEPAdministrator().getConfiguration().addEventType(type_3, names_3, types_3);
        String type_2 = OA_TYPENAME + "_2";
        String[] names_2 = {"nestedNested", "nestedValue"};
        Object[] types_2 = {type_3, Object.class};
        epService.getEPAdministrator().getConfiguration().addEventType(type_2, names_2, types_2);
        String type_1 = OA_TYPENAME + "_1";
        String[] names_1 = {"nested"};
        Object[] types_1 = {type_2};
        epService.getEPAdministrator().getConfiguration().addEventType(type_1, names_1, types_1);
        String[] names = {"item"};
        Object[] types = {type_1};
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
        configuration.addEventType(XML_TYPENAME, eventTypeMeta);
    }

    private void addAvroEventType() {
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(getAvroSchema()));
    }

    private static Schema getAvroSchema() {
        Schema s3 = SchemaBuilder.record(AVRO_TYPENAME+"_3").fields()
                .optionalInt("nestedNestedValue")
                .endRecord();
        Schema s2 = SchemaBuilder.record(AVRO_TYPENAME+"_2").fields()
                .optionalInt("nestedValue")
                .name("nestedNested").type().unionOf()
                .intType().and().type(s3).endUnion().noDefault()
                .endRecord();
        Schema s1 = SchemaBuilder.record(AVRO_TYPENAME+"_1").fields()
                .name("nested").type().unionOf()
                .intType().and().type(s2).endUnion().noDefault()
                .endRecord();
        return SchemaBuilder.record(AVRO_TYPENAME).fields().name("item").type(s1).noDefault().endRecord();
    }
}
