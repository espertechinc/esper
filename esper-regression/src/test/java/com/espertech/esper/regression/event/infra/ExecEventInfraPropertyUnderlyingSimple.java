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
import com.espertech.esper.collection.Pair;
import com.espertech.esper.supportregression.bean.SupportBeanSimple;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static org.junit.Assert.*;

public class ExecEventInfraPropertyUnderlyingSimple implements RegressionExecution {

    private static final Logger log = LoggerFactory.getLogger(ExecEventInfraPropertyUnderlyingSimple.class);

    public void configure(Configuration configuration) {
        addMapEventType(configuration);
        addOAEventType(configuration);
        configuration.addEventType(BEAN_TYPENAME, SupportBeanSimple.class);
        addXMLEventType(configuration);
        addAvroEventType(configuration);
    }

    public void run(EPServiceProvider epService) {
        Pair<String, FunctionSendEventIntString>[] pairs = new Pair[]{
            new Pair<>(MAP_TYPENAME, FMAP),
            new Pair<>(OA_TYPENAME, FOA),
            new Pair<>(BEAN_TYPENAME, FBEAN),
            new Pair<>(XML_TYPENAME, FXML),
            new Pair<>(AVRO_TYPENAME, FAVRO)
        };

        for (Pair<String, FunctionSendEventIntString> pair : pairs) {
            log.info("Asserting type " + pair.getFirst());
            runAssertionPassUnderlying(epService, pair.getFirst(), pair.getSecond());
            runAssertionPropertiesWGetter(epService, pair.getFirst(), pair.getSecond());
            runAssertionTypeValidProp(epService, pair.getFirst(), pair.getSecond() == FMAP || pair.getSecond() == FXML || pair.getSecond() == FOA || pair.getSecond() == FAVRO);
            runAssertionTypeInvalidProp(epService, pair.getFirst(), pair.getSecond() == FXML);
        }
    }

    private void runAssertionPassUnderlying(EPServiceProvider epService, String typename, FunctionSendEventIntString send) {
        String epl = "select * from " + typename;
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        String[] fields = "myInt,myString".split(",");

        assertEquals(Integer.class, JavaClassHelper.getBoxedType(statement.getEventType().getPropertyType("myInt")));
        assertEquals(String.class, statement.getEventType().getPropertyType("myString"));

        Object eventOne = send.apply(epService, 3, "some string");

        EventBean event = listener.assertOneGetNewAndReset();
        SupportEventTypeAssertionUtil.assertConsistency(event);
        assertEquals(eventOne, event.getUnderlying());
        EPAssertionUtil.assertProps(event, fields, new Object[]{3, "some string"});

        Object eventTwo = send.apply(epService, 4, "other string");
        event = listener.assertOneGetNewAndReset();
        assertEquals(eventTwo, event.getUnderlying());
        EPAssertionUtil.assertProps(event, fields, new Object[]{4, "other string"});

        statement.destroy();
    }

    private void runAssertionPropertiesWGetter(EPServiceProvider epService, String typename, FunctionSendEventIntString send) {
        String epl = "select myInt, exists(myInt) as exists_myInt, myString, exists(myString) as exists_myString from " + typename;
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        String[] fields = "myInt,exists_myInt,myString,exists_myString".split(",");

        assertEquals(Integer.class, JavaClassHelper.getBoxedType(statement.getEventType().getPropertyType("myInt")));
        assertEquals(String.class, statement.getEventType().getPropertyType("myString"));
        assertEquals(Boolean.class, statement.getEventType().getPropertyType("exists_myInt"));
        assertEquals(Boolean.class, statement.getEventType().getPropertyType("exists_myString"));

        send.apply(epService, 3, "some string");

        EventBean event = listener.assertOneGetNewAndReset();
        runAssertionEventInvalidProp(event);
        EPAssertionUtil.assertProps(event, fields, new Object[]{3, true, "some string", true});

        send.apply(epService, 4, "other string");
        event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[]{4, true, "other string", true});

        statement.destroy();
    }

    private void runAssertionEventInvalidProp(EventBean event) {
        for (String prop : Arrays.asList("xxxx", "myString[1]", "myString('a')", "x.y", "myString.x")) {
            SupportMessageAssertUtil.tryInvalidProperty(event, prop);
            SupportMessageAssertUtil.tryInvalidGetFragment(event, prop);
        }
    }

    private void runAssertionTypeValidProp(EPServiceProvider epService, String typeName, boolean boxed) {
        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType(typeName);

        Object[][] expectedType = new Object[][]{{"myInt", boxed ? Integer.class : int.class, null, null}, {"myString", String.class, null, null}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, eventType, SupportEventTypeAssertionEnum.getSetWithFragment());

        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"myString", "myInt"}, eventType.getPropertyNames());

        assertNotNull(eventType.getGetter("myInt"));
        assertTrue(eventType.isProperty("myInt"));
        assertEquals(boxed ? Integer.class : int.class, eventType.getPropertyType("myInt"));
        assertEquals(new EventPropertyDescriptor("myString", String.class, null, false, false, false, false, false), eventType.getPropertyDescriptor("myString"));
    }

    private void runAssertionTypeInvalidProp(EPServiceProvider epService, String typeName, boolean xml) {
        EventType eventType = epService.getEPAdministrator().getConfiguration().getEventType(typeName);

        for (String prop : Arrays.asList("xxxx", "myString[0]", "myString('a')", "myString.x", "myString.x.y", "myString.x")) {
            assertEquals(false, eventType.isProperty(prop));
            Class expected = null;
            if (xml) {
                if (prop.equals("myString[0]")) {
                    expected = String.class;
                }
                if (prop.equals("myString.x?")) {
                    expected = Node.class;
                }
            }
            assertEquals(expected, eventType.getPropertyType(prop));
            assertNull(eventType.getPropertyDescriptor(prop));
            assertNull(eventType.getFragmentType(prop));
        }
    }

    private void addMapEventType(Configuration configuration) {
        LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
        properties.put("myInt", Integer.class);
        properties.put("myString", "string");
        configuration.addEventType(MAP_TYPENAME, properties);
    }

    private void addOAEventType(Configuration configuration) {
        String[] names = {"myInt", "myString"};
        Object[] types = {Integer.class, String.class};
        configuration.addEventType(OA_TYPENAME, names, types);
    }

    private void addXMLEventType(Configuration configuration) {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
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
        configuration.addEventType(XML_TYPENAME, eventTypeMeta);
    }

    private void addAvroEventType(Configuration configuration) {
        configuration.addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(getAvroSchema()));
    }

    private static Schema getAvroSchema() {
        return SchemaBuilder.record(AVRO_TYPENAME)
                .fields()
                .name("myInt").type().intType().noDefault()
                .name("myString").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
                .endRecord();
    }

    @FunctionalInterface
    interface FunctionSendEventIntString {
        public Object apply(EPServiceProvider epService, Integer intValue, String stringValue);
    }

    private final static String BEAN_TYPENAME = SupportBeanSimple.class.getSimpleName();

    private static final FunctionSendEventIntString FMAP = (epService, a, b) -> {
        Map<String, Object> map = new HashMap<>();
        map.put("myInt", a);
        map.put("myString", b);
        epService.getEPRuntime().sendEvent(map, MAP_TYPENAME);
        return map;
    };

    private static final FunctionSendEventIntString FOA = (epService, a, b) -> {
        Object[] oa = new Object[]{a, b};
        epService.getEPRuntime().sendEvent(oa, OA_TYPENAME);
        return oa;
    };

    private static final FunctionSendEventIntString FBEAN = (epService, a, b) -> {
        SupportBeanSimple bean = new SupportBeanSimple(b, a);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    };

    private static final FunctionSendEventIntString FXML = (epService, a, b) -> {
        String xml = "<myevent myInt=\"XXXXXX\" myString=\"YYYYYY\">\n" +
                "</myevent>\n";
        xml = xml.replace("XXXXXX", a.toString());
        xml = xml.replace("YYYYYY", b);
        try {
            Document d = SupportXML.sendEvent(epService.getEPRuntime(), xml);
            return d.getDocumentElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    private static final FunctionSendEventIntString FAVRO = (epService, a, b) -> {
        GenericData.Record datum = new GenericData.Record(getAvroSchema());
        datum.put("myInt", a);
        datum.put("myString", b);
        epService.getEPRuntime().sendEventAvro(datum, AVRO_TYPENAME);
        return datum;
    };
}
