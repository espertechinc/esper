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

import com.espertech.esper.avro.core.AvroSchemaUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportMarkerImplA;
import com.espertech.esper.supportregression.bean.SupportMarkerInterface;
import com.espertech.esper.supportregression.event.SupportEventInfra;
import com.espertech.esper.supportregression.event.ValueWithExistsFlag;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static com.espertech.esper.supportregression.event.ValueWithExistsFlag.*;
import static org.junit.Assert.assertEquals;

public class ExecEventInfraPropertyNestedDynamicRootedSimple implements RegressionExecution {
    private final static Class BEAN_TYPE = SupportMarkerInterface.class;
    private final static ValueWithExistsFlag[] NOT_EXISTS = multipleNotExists(3);

    public void configure(Configuration configuration) {
        addXMLEventType(configuration);
    }

    public void run(EPServiceProvider epService) {
        addMapEventType(epService);
        addOAEventType(epService);
        epService.getEPAdministrator().getConfiguration().addEventType(BEAN_TYPE);
        addAvroEventType(epService);

        // Bean
        Pair[] beanTests = new Pair[]{
            new Pair<>(SupportBeanComplexProps.makeDefaultBean(), allExist("simple", "nestedValue", "nestedNestedValue")),
            new Pair<>(new SupportMarkerImplA("x"), NOT_EXISTS),
        };
        runAssertion(epService, BEAN_TYPE.getSimpleName(), FBEAN, null, beanTests, Object.class);

        // Map
        Map<String, Object> mapNestedNestedOne = Collections.singletonMap("nestedNestedValue", 101);
        Map<String, Object> mapNestedOne = twoEntryMap("nestedNested", mapNestedNestedOne, "nestedValue", "abc");
        Map<String, Object> mapOne = twoEntryMap("simpleProperty", 5, "nested", mapNestedOne);
        Pair[] mapTests = new Pair[]{
            new Pair<>(Collections.singletonMap("simpleProperty", "a"), new ValueWithExistsFlag[]{exists("a"), notExists(), notExists()}),
            new Pair<>(mapOne, allExist(5, "abc", 101)),
        };
        runAssertion(epService, MAP_TYPENAME, FMAP, null, mapTests, Object.class);

        // Object-Array
        Object[] oaNestedNestedOne = new Object[]{101};
        Object[] oaNestedOne = new Object[]{"abc", oaNestedNestedOne};
        Object[] oaOne = new Object[]{5, oaNestedOne};
        Pair[] oaTests = new Pair[]{
            new Pair<>(new Object[]{"a", null}, new ValueWithExistsFlag[]{exists("a"), notExists(), notExists()}),
            new Pair<>(oaOne, allExist(5, "abc", 101)),
        };
        runAssertion(epService, OA_TYPENAME, FOA, null, oaTests, Object.class);

        // XML
        Pair[] xmlTests = new Pair[]{
            new Pair<>("<simpleProperty>abc</simpleProperty>" +
                    "<nested nestedValue=\"100\">\n" +
                    "\t<nestedNested nestedNestedValue=\"101\">\n" +
                    "\t</nestedNested>\n" +
                    "</nested>\n", allExist("abc", "100", "101")),
            new Pair<>("<nested/>", NOT_EXISTS),
        };
        runAssertion(epService, XML_TYPENAME, FXML, xmlToValue, xmlTests, Node.class);

        // Avro
        GenericData.Record datumNull = new GenericData.Record(getAvroSchema());
        Schema schema = getAvroSchema();
        Schema nestedSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(schema.getField("nested").schema());
        Schema nestedNestedSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(nestedSchema.getField("nestedNested").schema());
        GenericData.Record nestedNestedDatum = new GenericData.Record(nestedNestedSchema);
        nestedNestedDatum.put("nestedNestedValue", 101);
        GenericData.Record nestedDatum = new GenericData.Record(nestedSchema);
        nestedDatum.put("nestedValue", 100);
        nestedDatum.put("nestedNested", nestedNestedDatum);
        GenericData.Record datumOne = new GenericData.Record(schema);
        datumOne.put("simpleProperty", "abc");
        datumOne.put("nested", nestedDatum);
        Pair[] avroTests = new Pair[]{
            new Pair<>(new GenericData.Record(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord()), NOT_EXISTS),
            new Pair<>(datumNull, new ValueWithExistsFlag[]{exists(null), notExists(), notExists()}),
            new Pair<>(datumOne, allExist("abc", 100, 101)),
        };
        runAssertion(epService, AVRO_TYPENAME, FAVRO, null, avroTests, Object.class);
    }

    private void runAssertion(EPServiceProvider epService,
                              String typename,
                              FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType) {

        String stmtText = "select " +
                "simpleProperty? as simple, " +
                "exists(simpleProperty?) as exists_simple, " +
                "nested?.nestedValue as nested, " +
                "exists(nested?.nestedValue) as exists_nested, " +
                "nested?.nestedNested.nestedNestedValue as nestedNested, " +
                "exists(nested?.nestedNested.nestedNestedValue) as exists_nestedNested " +
                "from " + typename;
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] propertyNames = "simple,nested,nestedNested".split(",");
        for (String propertyName : propertyNames) {
            assertEquals(expectedPropertyType, stmt.getEventType().getPropertyType(propertyName));
            assertEquals(Boolean.class, stmt.getEventType().getPropertyType("exists_" + propertyName));
        }

        for (Pair pair : tests) {
            send.apply(epService, pair.getFirst());
            SupportEventInfra.assertValuesMayConvert(listener.assertOneGetNewAndReset(), propertyNames, (ValueWithExistsFlag[]) pair.getSecond(), optionalValueConversion);
        }

        stmt.destroy();
    }

    private void addMapEventType(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, Collections.emptyMap());
    }

    private void addOAEventType(EPServiceProvider epService) {
        String type_2 = OA_TYPENAME + "_2";
        String[] names_2 = {"nestedNestedValue"};
        Object[] types_2 = {Object.class};
        epService.getEPAdministrator().getConfiguration().addEventType(type_2, names_2, types_2);
        String type_1 = OA_TYPENAME + "_1";
        String[] names_1 = {"nestedValue", "nestedNested"};
        Object[] types_1 = {Object.class, type_2};
        epService.getEPAdministrator().getConfiguration().addEventType(type_1, names_1, types_1);
        String[] names = {"simpleProperty", "nested"};
        Object[] types = {Object.class, type_1};
        epService.getEPAdministrator().getConfiguration().addEventType(OA_TYPENAME, names, types);
    }

    private void addXMLEventType(Configuration configuration) {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "\t<xs:element name=\"myevent\">\n" +
                "\t\t<xs:complexType>\n" +
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
        Schema s3 = SchemaBuilder.record(AVRO_TYPENAME + "_3").fields()
                .optionalInt("nestedNestedValue")
                .endRecord();
        Schema s2 = SchemaBuilder.record(AVRO_TYPENAME + "_2").fields()
                .optionalInt("nestedValue")
                .name("nestedNested").type().unionOf()
                .intType().and().type(s3).endUnion().noDefault()
                .endRecord();
        return SchemaBuilder.record(AVRO_TYPENAME + "_1").fields()
                .name("simpleProperty").type().unionOf().intType().and().stringType().endUnion().noDefault()
                .name("nested").type().unionOf().intType().and().type(s2).endUnion().noDefault()
                .endRecord();
    }
}
