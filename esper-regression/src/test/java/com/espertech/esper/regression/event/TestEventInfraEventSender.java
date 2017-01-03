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
import com.espertech.esper.supportregression.bean.SupportBean_G;
import com.espertech.esper.supportregression.bean.SupportMarkerImplA;
import com.espertech.esper.supportregression.bean.SupportMarkerInterface;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;

import java.util.Collections;
import java.util.HashMap;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;

public class TestEventInfraEventSender extends TestCase {
    private EPServiceProvider epService;

    protected void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        addXMLEventType(configuration);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        addMapEventType();
        addOAEventType();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("Marker", SupportMarkerInterface.class);
        addAvroEventType();

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testIt() throws Exception {

        // Bean
        runAssertionSuccess(SupportBean.class.getName(), new SupportBean());
        runAssertionInvalid(SupportBean.class.getName(), new SupportBean_G("G1"),
                "Event object of type " + SupportBean_G.class.getName() + " does not equal, extend or implement the type " + SupportBean.class.getName() + " of event type 'SupportBean'");
        runAssertionSuccess("Marker", new SupportMarkerImplA("Q2"), new SupportBean_G("Q3"));

        // Map
        runAssertionSuccess(MAP_TYPENAME, new HashMap());
        runAssertionInvalid(MAP_TYPENAME, new SupportBean(),
                "Unexpected event object of type " + SupportBean.class.getName() + ", expected java.util.Map");

        // Object-Array
        runAssertionSuccess(OA_TYPENAME, new Object[]{});
        runAssertionInvalid(OA_TYPENAME, new SupportBean(),
                "Unexpected event object of type " + SupportBean.class.getName() + ", expected Object[]");

        // XML
        runAssertionSuccess(XML_TYPENAME, SupportXML.getDocument("<myevent/>").getDocumentElement());
        runAssertionInvalid(XML_TYPENAME, new SupportBean(),
                "Unexpected event object type '" + SupportBean.class.getName() + "' encountered, please supply a org.w3c.dom.Document or Element node");
        runAssertionInvalid(XML_TYPENAME, SupportXML.getDocument("<xxxx/>"),
                "Unexpected root element name 'xxxx' encountered, expected a root element name of 'myevent'");

        // Avro
        runAssertionSuccess(AVRO_TYPENAME, new GenericData.Record(getAvroSchema()));
        runAssertionInvalid(AVRO_TYPENAME, new SupportBean(),
                "Unexpected event object type '" + SupportBean.class.getName() + "' encountered, please supply a GenericData.Record");

        // No such type
        try {
            epService.getEPRuntime().getEventSender("ABC");
            fail();
        }
        catch (EventTypeException ex){
            assertEquals("Event type named 'ABC' could not be found", ex.getMessage());
        }

        // Internal implicit wrapper type
        epService.getEPAdministrator().createEPL("insert into ABC select *, theString as value from SupportBean");
        try
        {
            epService.getEPRuntime().getEventSender("ABC");
            fail("Event type named 'ABC' could not be found");
        }
        catch (EventTypeException ex)
        {
            assertEquals("An event sender for event type named 'ABC' could not be created as the type is internal", ex.getMessage());
        }
    }

    private void runAssertionSuccess(String typename,
                                     Object... correctUnderlyings) {

        String stmtText = "select * from " + typename;
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EventSender sender = epService.getEPRuntime().getEventSender(typename);
        for (Object underlying : correctUnderlyings) {
            sender.sendEvent(underlying);
            assertSame(underlying, listener.assertOneGetNewAndReset().getUnderlying());
        }

        stmt.destroy();
    }

    private void runAssertionInvalid(String typename,
                                     Object incorrectUnderlying,
                                     String message) {
        EventSender sender = epService.getEPRuntime().getEventSender(typename);

        try {
            sender.sendEvent(incorrectUnderlying);
            fail();
        }
        catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, message);
        }
    }

    private void addMapEventType() {
        epService.getEPAdministrator().getConfiguration().addEventType(MAP_TYPENAME, Collections.emptyMap());
    }

    private void addOAEventType() {
        String[] names = {};
        Object[] types = {};
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
        epService.getEPAdministrator().getConfiguration().addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(getAvroSchema()));
    }

    private static Schema getAvroSchema() {
        return SchemaBuilder.record(AVRO_TYPENAME).fields().endRecord();
    }
}
