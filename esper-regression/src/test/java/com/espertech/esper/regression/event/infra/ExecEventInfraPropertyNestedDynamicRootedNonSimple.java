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
import com.espertech.esper.supportregression.bean.SupportBeanDynRoot;
import com.espertech.esper.supportregression.event.SupportEventInfra;
import com.espertech.esper.supportregression.event.ValueWithExistsFlag;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static com.espertech.esper.supportregression.event.ValueWithExistsFlag.*;
import static org.junit.Assert.assertEquals;

public class ExecEventInfraPropertyNestedDynamicRootedNonSimple implements RegressionExecution {
    private final static Class BEAN_TYPE = SupportBeanDynRoot.class;

    public void configure(Configuration configuration) {
        addXMLEventType(configuration);
    }

    public void run(EPServiceProvider epService) {
        addMapEventType(epService);
        addOAEventType(epService);
        epService.getEPAdministrator().getConfiguration().addEventType(BEAN_TYPE);
        addAvroEventType(epService);

        final ValueWithExistsFlag[] notExists = ValueWithExistsFlag.multipleNotExists(6);

        // Bean
        SupportBeanComplexProps inner = SupportBeanComplexProps.makeDefaultBean();
        Pair[] beanTests = new Pair[]{
            new Pair<>(new SupportBeanDynRoot("xxx"), notExists),
            new Pair<>(new SupportBeanDynRoot(inner), allExist(inner.getIndexed(0), inner.getIndexed(1), inner.getArrayProperty()[1], inner.getMapped("keyOne"), inner.getMapped("keyTwo"), inner.getMapProperty().get("xOne"))),
        };
        runAssertion(epService, BEAN_TYPE.getSimpleName(), FBEAN, null, beanTests, Object.class);

        // Map
        Map<String, Object> mapNestedOne = new HashMap();
        mapNestedOne.put("indexed", new int[]{1, 2});
        mapNestedOne.put("arrayProperty", null);
        mapNestedOne.put("mapped", twoEntryMap("keyOne", 100, "keyTwo", 200));
        mapNestedOne.put("mapProperty", null);
        Map<String, Object> mapOne = Collections.singletonMap("item", mapNestedOne);
        Pair[] mapTests = new Pair[]{
            new Pair<>(Collections.emptyMap(), notExists),
            new Pair<>(mapOne, new ValueWithExistsFlag[]{exists(1), exists(2), notExists(), exists(100), exists(200), notExists()}),
        };
        runAssertion(epService, MAP_TYPENAME, FMAP, null, mapTests, Object.class);

        // Object-Array
        Object[] oaNestedOne = new Object[]{new int[]{1, 2}, twoEntryMap("keyOne", 100, "keyTwo", 200), new int[]{1000, 2000}, Collections.singletonMap("xOne", "abc")};
        Object[] oaOne = new Object[]{null, oaNestedOne};
        Pair[] oaTests = new Pair[]{
            new Pair<>(new Object[]{null, null}, notExists),
            new Pair<>(oaOne, allExist(1, 2, 2000, 100, 200, "abc")),
        };
        runAssertion(epService, OA_TYPENAME, FOA, null, oaTests, Object.class);

        // XML
        Pair[] xmlTests = new Pair[]{
            new Pair<>("", notExists),
            new Pair<>("<item>" +
                    "<indexed>1</indexed><indexed>2</indexed><mapped id=\"keyOne\">3</mapped><mapped id=\"keyTwo\">4</mapped>" +
                    "</item>", new ValueWithExistsFlag[]{exists("1"), exists("2"), notExists(), exists("3"), exists("4"), notExists()})
        };
        runAssertion(epService, XML_TYPENAME, FXML, xmlToValue, xmlTests, Node.class);

        // Avro
        Schema schema = getAvroSchema();
        Schema itemSchema = AvroSchemaUtil.findUnionRecordSchemaSingle(schema.getField("item").schema());
        GenericData.Record datumOne = new GenericData.Record(schema);
        datumOne.put("item", null);
        GenericData.Record datumItemTwo = new GenericData.Record(itemSchema);
        datumItemTwo.put("indexed", Arrays.asList(1, 2));
        datumItemTwo.put("mapped", twoEntryMap("keyOne", 3, "keyTwo", 4));
        GenericData.Record datumTwo = new GenericData.Record(schema);
        datumTwo.put("item", datumItemTwo);
        Pair[] avroTests = new Pair[]{
            new Pair<>(new GenericData.Record(schema), notExists),
            new Pair<>(datumOne, notExists),
            new Pair<>(datumTwo, new ValueWithExistsFlag[]{exists(1), exists(2), notExists(), exists(3), exists(4), notExists()}),
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
                "item?.indexed[0] as indexed1, " +
                "exists(item?.indexed[0]) as exists_indexed1, " +
                "item?.indexed[1]? as indexed2, " +
                "exists(item?.indexed[1]?) as exists_indexed2, " +
                "item?.arrayProperty[1]? as array, " +
                "exists(item?.arrayProperty[1]?) as exists_array, " +
                "item?.mapped('keyOne') as mapped1, " +
                "exists(item?.mapped('keyOne')) as exists_mapped1, " +
                "item?.mapped('keyTwo')? as mapped2,  " +
                "exists(item?.mapped('keyTwo')?) as exists_mapped2,  " +
                "item?.mapProperty('xOne')? as map, " +
                "exists(item?.mapProperty('xOne')?) as exists_map " +
                " from " + typename;

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] propertyNames = "indexed1,indexed2,array,mapped1,mapped2,map".split(",");
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
        String nestedName = OA_TYPENAME + "_1";
        String[] namesNested = {"indexed", "mapped", "arrayProperty", "mapProperty"};
        Object[] typesNested = {int[].class, Map.class, int[].class, Map.class};
        epService.getEPAdministrator().getConfiguration().addEventType(nestedName, namesNested, typesNested);
        String[] names = {"someprop", "item"};
        Object[] types = {String.class, nestedName};
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
        Schema s1 = SchemaBuilder.record(AVRO_TYPENAME + "_1").fields()
                .name("indexed").type().unionOf().nullType().and().intType().and().array().items().intType().endUnion().noDefault()
                .name("mapped").type().unionOf().nullType().and().intType().and().map().values().intType().endUnion().noDefault()
                .endRecord();
        return SchemaBuilder.record(AVRO_TYPENAME).fields()
                .name("item").type().unionOf().intType().and().type(s1).endUnion().noDefault()
                .endRecord();
    }
}
