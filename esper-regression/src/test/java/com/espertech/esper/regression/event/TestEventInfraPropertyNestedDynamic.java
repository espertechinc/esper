/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.SupportEventInfra;
import com.espertech.esper.supportregression.event.ValueWithExistsFlag;
import com.espertech.esper.util.EventRepresentationChoice;
import com.espertech.esper.util.JavaClassHelper;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static com.espertech.esper.supportregression.event.SupportEventInfra.AVRO_TYPENAME;
import static com.espertech.esper.supportregression.event.ValueWithExistsFlag.exists;
import static com.espertech.esper.supportregression.event.ValueWithExistsFlag.notExists;

public class TestEventInfraPropertyNestedDynamic extends TestCase {
    private final static String BEAN_TYPENAME = SupportBeanDynRoot.class.getName();

    private static final FunctionSendEvent FAVRO = (epService, value) -> {
        Schema schema = getAvroSchema();
        Schema itemSchema = schema.getField("item").schema();
        GenericData.Record itemDatum = new GenericData.Record(itemSchema);
        itemDatum.put("id", value);
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

    public void testDynamicProp() {
        runAssertion(EventRepresentationChoice.ARRAY, "");
        runAssertion(EventRepresentationChoice.MAP, "");
        runAssertion(EventRepresentationChoice.AVRO, "@AvroSchemaField(name='myid',schema='[\"int\",{\"type\":\"string\",\"avro.java.string\":\"String\"},\"null\"]')");
        runAssertion(EventRepresentationChoice.DEFAULT, "");
    }

    private void runAssertion(EventRepresentationChoice outputEventRep, String additionalAnnotations) {

        // Bean
        Pair[] beanTests = new Pair[]{
                new Pair<>(new SupportBeanDynRoot(new SupportBean_S0(101)), exists(101)),
                new Pair<>(new SupportBeanDynRoot("abc"), notExists()),
                new Pair<>(new SupportBeanDynRoot(new SupportBean_A("e1")), exists("e1")),
                new Pair<>(new SupportBeanDynRoot(new SupportBean_B("e2")), exists("e2")),
                new Pair<>(new SupportBeanDynRoot(new SupportBean_S1(102)), exists(102))
        };
        runAssertion(outputEventRep, additionalAnnotations, BEAN_TYPENAME, FBEAN, null, beanTests, Object.class);

        // Map
        Pair[] mapTests = new Pair[]{
                new Pair<>(Collections.emptyMap(), notExists()),
                new Pair<>(Collections.singletonMap("item", Collections.singletonMap("id", 101)), exists(101)),
                new Pair<>(Collections.singletonMap("item", Collections.emptyMap()), notExists()),
        };
        runAssertion(outputEventRep, additionalAnnotations, MAP_TYPENAME, FMAP, null, mapTests, Object.class);

        // Object array
        Pair[] oaTests = new Pair[]{
                new Pair<>(new Object[] {null}, notExists()),
                //new Pair<>(new Object[] {new SupportBean_S0(101)}, exists(101)),
                //new Pair<>(new Object[] {"abc"}, notExists()),
        };
        runAssertion(outputEventRep, additionalAnnotations, OA_TYPENAME, FOA, null, oaTests, Object.class);

        // XML
        Pair[] xmlTests = new Pair[]{
                new Pair<>("<item id=\"101\"/>", exists("101")),
                new Pair<>("<item/>", notExists()),
        };
        if (!outputEventRep.isAvroEvent()) {
            runAssertion(outputEventRep, additionalAnnotations, XML_TYPENAME, FXML, XML_TO_VALUE, xmlTests, Node.class);
        }

        // Avro
        Pair[] avroTests = new Pair[]{
                new Pair<>(null, exists(null)),
                new Pair<>(101, exists(101)),
                new Pair<>("abc", exists("abc")),
        };
        runAssertion(outputEventRep, additionalAnnotations, AVRO_TYPENAME, FAVRO, null, avroTests, Object.class);
    }

    private void runAssertion(EventRepresentationChoice eventRepresentationEnum,
                              String additionalAnnotations,
                              String typename,
                              FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType) {
        String stmtText = eventRepresentationEnum.getAnnotationText() + additionalAnnotations + " select " +
                "item.id? as myid, " +
                "exists(item.id?) as exists_myid " +
                "from " + typename;
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(expectedPropertyType, stmt.getEventType().getPropertyType("myid"));
        assertEquals(Boolean.class, JavaClassHelper.getBoxedType(stmt.getEventType().getPropertyType("exists_myid")));
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        for (Pair pair : tests) {
            send.apply(epService, pair.getFirst());
            EventBean event = listener.assertOneGetNewAndReset();
            SupportEventInfra.assertValueMayConvert(event, "myid", (ValueWithExistsFlag) pair.getSecond(), optionalValueConversion);
        }

        stmt.destroy();
    }

    private void addMapEventType() {
        Map<String, Object> top = Collections.singletonMap("item", Map.class);
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, top);
    }

    private void addOAEventType() {
        String[] names = {"item"};
        Object[] types = {Object.class};
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
        Schema s1 = SchemaBuilder.record(AVRO_TYPENAME+"_1").fields()
                .name("id").type().unionOf()
                .intBuilder().endInt()
                .and()
                .stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString()
                .and()
                .nullType()
                .endUnion().noDefault()
                .endRecord();
        return SchemaBuilder.record(AVRO_TYPENAME).fields().name("item").type(s1).noDefault().endRecord();
    }
}
