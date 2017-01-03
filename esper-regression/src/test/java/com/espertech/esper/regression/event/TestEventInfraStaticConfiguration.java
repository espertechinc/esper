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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.SupportXML;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.util.Collections;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;

public class TestEventInfraStaticConfiguration extends TestCase {
    private EPServiceProvider epService;

    protected void setUp() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        Schema avroSchema = SchemaBuilder.record(AVRO_TYPENAME).fields()
                .name("intPrimitive").type().intType().noDefault().endRecord();
        String avroSchemaText = avroSchema.toString().replace("\"", "&quot;");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<esper-configuration>\t\n" +
                "\t<event-type name=\"MyStaticBean\" class=\"" + SupportBean.class.getName() + "\"/>\n" +
                "\t<event-type name=\"" + MAP_TYPENAME + "\">\n" +
                "\t\t<java-util-map>\n" +
                "\t  \t\t<map-property name=\"intPrimitive\" class=\"int\"/>\n" +
                "\t  \t</java-util-map>\n" +
                "\t</event-type>\n" +
                "\t\n" +
                "\t<event-type name=\"" + OA_TYPENAME + "\">\n" +
                "\t\t<objectarray>\n" +
                "\t  \t\t<objectarray-property name=\"intPrimitive\" class=\"int\"/>\n" +
                "\t  \t</objectarray>\n" +
                "\t</event-type>\n" +
                "\t<event-type name=\"" + XML_TYPENAME + "\">\n" +
                "\t\t<xml-dom root-element-name=\"myevent\">\n" +
                "\t\t\t<xpath-property property-name=\"intPrimitive\" xpath=\"@intPrimitive\" type=\"number\"/>\n" +
                "\t\t</xml-dom>\n" +
                "\t</event-type>\n" +
                "\t<event-type name=\"" + AVRO_TYPENAME + "\">\n" +
                "\t\t<avro schema-text=\"" + avroSchemaText + "\"/>\n" +
                "\t</event-type>\n" +
                "</esper-configuration>\n";
        configuration.configure(SupportXML.getDocument(xml));

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testIt() throws Exception {

        // Bean
        runAssertion("MyStaticBean", FBEAN, new SupportBean("E1", 10));

        // Map
        runAssertion(MAP_TYPENAME, FMAP, Collections.singletonMap("intPrimitive", 10));

        // Object-Array
        runAssertion(OA_TYPENAME, FOA, new Object[]{10});

        // XML
        runAssertion(XML_TYPENAME, FXML, "<myevent intPrimitive=\"10\"/>");

        // Avro
        Schema schema = SchemaBuilder.record("somename").fields().requiredInt("intPrimitive").endRecord();
        GenericData.Record record = new GenericData.Record(schema);
        record.put("intPrimitive", 10);
        runAssertion(AVRO_TYPENAME, FAVRO, record);
    }

    private void runAssertion(String typename, FunctionSendEvent fsend, Object underlying) {

        String stmtText = "select intPrimitive from " + typename;
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        fsend.apply(epService, underlying);
        Number n = (Number) listener.assertOneGetNewAndReset().get("intPrimitive");
        assertEquals(10, n.intValue());

        stmt.destroy();
    }
}
