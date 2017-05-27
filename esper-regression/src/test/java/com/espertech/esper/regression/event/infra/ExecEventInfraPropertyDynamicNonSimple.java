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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.event.SupportEventInfra;
import com.espertech.esper.supportregression.event.ValueWithExistsFlag;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static com.espertech.esper.supportregression.event.ValueWithExistsFlag.multipleNotExists;
import static org.junit.Assert.assertEquals;

public class ExecEventInfraPropertyDynamicNonSimple implements RegressionExecution {
    private final static Class BEAN_TYPE = SupportBeanComplexProps.class;

    public void configure(Configuration configuration) {
        addXMLEventType(configuration);
    }

    public void run(EPServiceProvider epService) {
        addMapEventType(epService);
        addOAEventType(epService);
        epService.getEPAdministrator().getConfiguration().addEventType(BEAN_TYPE);
        addAvroEventType(epService);

        final ValueWithExistsFlag[] notExists = multipleNotExists(4);

        // Bean
        SupportBeanComplexProps bean = SupportBeanComplexProps.makeDefaultBean();
        Pair[] beanTests = new Pair[]{
            new Pair<>(bean, ValueWithExistsFlag.allExist(bean.getIndexed(0), bean.getIndexed(1), bean.getMapped("keyOne"), bean.getMapped("keyTwo")))
        };
        runAssertion(epService, BEAN_TYPE.getSimpleName(), FBEAN, null, beanTests, Object.class);

        // Map
        Pair[] mapTests = new Pair[]{
            new Pair<>(Collections.singletonMap("somekey", "10"), notExists),
            new Pair<>(twoEntryMap("indexed", new int[]{1, 2}, "mapped", twoEntryMap("keyOne", 3, "keyTwo", 4)), ValueWithExistsFlag.allExist(1, 2, 3, 4)),
        };
        runAssertion(epService, MAP_TYPENAME, FMAP, null, mapTests, Object.class);

        // Object-Array
        Pair[] oaTests = new Pair[]{
            new Pair<>(new Object[]{null, null}, notExists),
            new Pair<>(new Object[]{new int[]{1, 2}, twoEntryMap("keyOne", 3, "keyTwo", 4)}, ValueWithExistsFlag.allExist(1, 2, 3, 4)),
        };
        runAssertion(epService, OA_TYPENAME, FOA, null, oaTests, Object.class);

        // XML
        Pair[] xmlTests = new Pair[]{
            new Pair<>("", notExists),
            new Pair<>("<indexed>1</indexed><indexed>2</indexed><mapped id=\"keyOne\">3</mapped><mapped id=\"keyTwo\">4</mapped>", ValueWithExistsFlag.allExist("1", "2", "3", "4"))
        };
        runAssertion(epService, XML_TYPENAME, FXML, xmlToValue, xmlTests, Node.class);

        // Avro
        GenericData.Record datumOne = new GenericData.Record(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord());
        GenericData.Record datumTwo = new GenericData.Record(getAvroSchema());
        datumTwo.put("indexed", Arrays.asList(1, 2));
        datumTwo.put("mapped", twoEntryMap("keyOne", 3, "keyTwo", 4));
        Pair[] avroTests = new Pair[]{
            new Pair<>(datumOne, notExists),
            new Pair<>(datumTwo, ValueWithExistsFlag.allExist(1, 2, 3, 4)),
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
                "indexed[0]? as indexed1, " +
                "exists(indexed[0]?) as exists_indexed1, " +
                "indexed[1]? as indexed2, " +
                "exists(indexed[1]?) as exists_indexed2, " +
                "mapped('keyOne')? as mapped1, " +
                "exists(mapped('keyOne')?) as exists_mapped1, " +
                "mapped('keyTwo')? as mapped2,  " +
                "exists(mapped('keyTwo')?) as exists_mapped2  " +
                "from " + typename;

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] propertyNames = "indexed1,indexed2,mapped1,mapped2".split(",");
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

    private void addMapEventType(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, Collections.emptyMap());
    }

    private void addOAEventType(EPServiceProvider epService) {
        String[] names = {"indexed", "mapped"};
        Object[] types = {int[].class, Map.class};
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
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord()));
    }

    private static Schema getAvroSchema() {
        return SchemaBuilder.record(AVRO_TYPENAME).fields()
                .name("indexed").type().unionOf().nullType().and().intType().and().array().items().intType().endUnion().noDefault()
                .name("mapped").type().unionOf().nullType().and().intType().and().map().values().intType().endUnion().noDefault()
                .endRecord();
    }
}
