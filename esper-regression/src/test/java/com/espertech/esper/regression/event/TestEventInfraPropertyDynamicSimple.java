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
import com.espertech.esper.supportregression.bean.SupportMarkerImplA;
import com.espertech.esper.supportregression.bean.SupportMarkerImplB;
import com.espertech.esper.supportregression.bean.SupportMarkerImplC;
import com.espertech.esper.supportregression.bean.SupportMarkerInterface;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.SupportEventInfra;
import com.espertech.esper.supportregression.event.ValueWithExistsFlag;
import com.espertech.esper.util.support.SupportEventTypeAssertionUtil;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.function.Function;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static com.espertech.esper.supportregression.event.ValueWithExistsFlag.*;

public class TestEventInfraPropertyDynamicSimple extends TestCase {
    private final static Class BEAN_TYPE = SupportMarkerInterface.class;

    private EPServiceProvider epService;

    protected void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        addXMLEventType(configuration);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        addMapEventType();
        addOAEventType();
        epService.getEPAdministrator().getConfiguration().addEventType(BEAN_TYPE);
        addAvroEventType();

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testIt() {

        // Bean
        Pair[] beanTests = new Pair[]{
                new Pair<>(new SupportMarkerImplA("e1"), exists("e1")),
                new Pair<>(new SupportMarkerImplB(1), exists(1)),
                new Pair<>(new SupportMarkerImplC(), notExists())
        };
        runAssertion(BEAN_TYPE.getSimpleName(), FBEAN, null, beanTests, Object.class);

        // Map
        Pair[] mapTests = new Pair[]{
                new Pair<>(Collections.singletonMap("somekey", "10"), notExists()),
                new Pair<>(Collections.singletonMap("id", "abc"), exists("abc")),
                new Pair<>(Collections.singletonMap("id", 10), exists(10)),
        };
        runAssertion(MAP_TYPENAME, FMAP, null, mapTests, Object.class);

        // Object-Array
        Pair[] oaTests = new Pair[]{
                new Pair<>(new Object[] {1, null}, exists(null)),
                new Pair<>(new Object[] {2, "abc"}, exists("abc")),
                new Pair<>(new Object[] {3, 10}, exists(10)),
        };
        runAssertion(OA_TYPENAME, FOA, null, oaTests, Object.class);

        // XML
        Pair[] xmlTests = new Pair[]{
                new Pair<>("", notExists()),
                new Pair<>("<id>10</id>", exists("10")),
                new Pair<>("<id>abc</id>", exists("abc")),
        };
        runAssertion(XML_TYPENAME, FXML, XML_TO_VALUE, xmlTests, Node.class);

        // Avro
        GenericData.Record datumEmpty = new GenericData.Record(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord());
        GenericData.Record datumOne = new GenericData.Record(getAvroSchema());
        datumOne.put("id", 101);
        GenericData.Record datumTwo = new GenericData.Record(getAvroSchema());
        datumTwo.put("id", null);
        Pair[] avroTests = new Pair[]{
                new Pair<>(datumEmpty, notExists()),
                new Pair<>(datumOne, exists(101)),
                new Pair<>(datumTwo, exists(null))
        };
        runAssertion(AVRO_TYPENAME, FAVRO, null, avroTests, Object.class);
    }

    private void runAssertion(String typename,
                              FunctionSendEvent send,
                              Function<Object, Object> optionalValueConversion,
                              Pair[] tests,
                              Class expectedPropertyType) {

        String stmtText = "select id? as myid, exists(id?) as exists_myid from " + typename;
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(expectedPropertyType, stmt.getEventType().getPropertyType("myid"));
        assertEquals(Boolean.class, stmt.getEventType().getPropertyType("exists_myid"));

        for (Pair pair : tests) {
            send.apply(epService, pair.getFirst());
            EventBean event = listener.assertOneGetNewAndReset();
            SupportEventInfra.assertValueMayConvert(event, "myid", (ValueWithExistsFlag) pair.getSecond(), optionalValueConversion);
        }

        stmt.destroy();
    }

    private void addMapEventType() {
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, Collections.emptyMap());
    }

    private void addOAEventType() {
        String[] names = {"somefield", "id"};
        Object[] types = {Object.class, Object.class};
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

    private void addAvroEventType() {
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord()));
    }

    private static Schema getAvroSchema() {
        return SchemaBuilder.record(AVRO_TYPENAME).fields()
                .name("id").type().unionOf()
                .nullType().and().intType().and().booleanType().endUnion().noDefault()
                .endRecord();
    }
}
