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
import com.espertech.esper.client.util.JSONEventRenderer;
import com.espertech.esper.client.util.XMLEventRenderer;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;
import static com.espertech.esper.supportregression.event.SupportEventInfra.*;
import static org.junit.Assert.assertEquals;

public class ExecEventInfraEventRenderer implements RegressionExecution {
    private final static Class BEAN_TYPE = ExecEventInfraEventRenderer.MyEvent.class;

    public void configure(Configuration configuration) {
        addXMLEventType(configuration);
    }

    public void run(EPServiceProvider epService) {
        addMapEventType(epService);
        addOAEventType(epService);
        epService.getEPAdministrator().getConfiguration().addEventType(BEAN_TYPE);
        addAvroEventType(epService);

        // Bean
        runAssertion(epService, BEAN_TYPE.getName(), FBEAN, new MyEvent(1, "abc", new MyInsideEvent(10)));

        // Map
        Map<String, Object> mapInner = new HashMap();
        mapInner.put("myInsideInt", 10);
        Map<String, Object> topInner = new HashMap();
        topInner.put("myInt", 1);
        topInner.put("myString", "abc");
        topInner.put("nested", mapInner);
        runAssertion(epService, MAP_TYPENAME, FMAP, topInner);

        // Object-array
        Object[] oaInner = new Object[]{10};
        Object[] oaTop = new Object[]{1, "abc", oaInner};
        runAssertion(epService, OA_TYPENAME, FOA, oaTop);

        // XML
        String xml = "<myevent myInt=\"1\" myString=\"abc\"><nested myInsideInt=\"10\"/></myevent>";
        runAssertion(epService, XML_TYPENAME, FXML, xml);

        // Avro
        Schema schema = getAvroSchema();
        Schema innerSchema = schema.getField("nested").schema();
        GenericData.Record avroInner = new GenericData.Record(innerSchema);
        avroInner.put("myInsideInt", 10);
        GenericData.Record avro = new GenericData.Record(schema);
        avro.put("myInt", 1);
        avro.put("myString", "abc");
        avro.put("nested", avroInner);
        runAssertion(epService, AVRO_TYPENAME, FAVRO, avro);
    }

    private void runAssertion(EPServiceProvider epService, String typename, FunctionSendEvent send, Object event) {
        String epl = "select * from " + typename;
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        send.apply(epService, event);

        EventBean eventBean = listener.assertOneGetNewAndReset();

        JSONEventRenderer jsonEventRenderer = epService.getEPRuntime().getEventRenderer().getJSONRenderer(statement.getEventType());
        String json = jsonEventRenderer.render(eventBean).replaceAll("(\\s|\\n|\\t)", "");
        assertEquals("{\"myInt\":1,\"myString\":\"abc\",\"nested\":{\"myInsideInt\":10}}", json);

        XMLEventRenderer xmlEventRenderer = epService.getEPRuntime().getEventRenderer().getXMLRenderer(statement.getEventType());
        String xml = xmlEventRenderer.render("root", eventBean).replaceAll("(\\s|\\n|\\t)", "");
        assertEquals("<?xmlversion=\"1.0\"encoding=\"UTF-8\"?><root><myInt>1</myInt><myString>abc</myString><nested><myInsideInt>10</myInsideInt></nested></root>", xml);

        statement.destroy();
    }

    private void addMapEventType(EPServiceProvider epService) {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("myInsideInt", "int");
        Map<String, Object> top = new LinkedHashMap<>();
        top.put("myInt", "int");
        top.put("myString", "string");
        top.put("nested", inner);
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, top);
    }

    private void addOAEventType(EPServiceProvider epService) {
        String[] namesInner = new String[]{"myInsideInt"};
        Object[] typesInner = new Object[]{int.class};
        epService.getEPAdministrator().getConfiguration().addEventType(OA_TYPENAME + "_1", namesInner, typesInner);

        String[] names = new String[]{"myInt", "myString", "nested"};
        Object[] types = new Object[]{int.class, String.class, OA_TYPENAME + "_1"};
        epService.getEPAdministrator().getConfiguration().addEventType(OA_TYPENAME, names, types);
    }

    private void addXMLEventType(Configuration configuration) {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
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
        eventTypeMeta.setSchemaText(schema);
        configuration.addEventType(XML_TYPENAME, eventTypeMeta);
    }

    private void addAvroEventType(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(getAvroSchema()));
    }

    private static Schema getAvroSchema() {
        Schema inner = SchemaBuilder.record(AVRO_TYPENAME + "_inside")
                .fields()
                .name("myInsideInt").type().intType().noDefault()
                .endRecord();

        return SchemaBuilder.record(AVRO_TYPENAME)
                .fields()
                .name("myInt").type().intType().noDefault()
                .name("myString").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
                .name("nested").type(inner).noDefault()
                .endRecord();
    }

    public final static class MyInsideEvent {
        private int myInsideInt;

        public MyInsideEvent(int myInsideInt) {
            this.myInsideInt = myInsideInt;
        }

        public int getMyInsideInt() {
            return myInsideInt;
        }
    }

    public final static class MyEvent {
        private int myInt;
        private String myString;
        private MyInsideEvent nested;

        public MyEvent(int myInt, String myString, MyInsideEvent nested) {
            this.myInt = myInt;
            this.myString = myString;
            this.nested = nested;
        }

        public int getMyInt() {
            return myInt;
        }

        public String getMyString() {
            return myString;
        }

        public MyInsideEvent getNested() {
            return nested;
        }
    }

}
