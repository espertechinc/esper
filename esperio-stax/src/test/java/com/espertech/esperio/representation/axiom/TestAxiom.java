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
package com.espertech.esperio.representation.axiom;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;

import javax.xml.xpath.XPathConstants;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class TestAxiom extends TestCase {
    private static final String AXIOM_URI = "types://xml/apacheaxiom/OMNode";

    private SupportUpdateListener updateListener;

    private static String XML =
            "<myevent>\n" +
                    "  <element1>VAL1</element1>\n" +
                    "  <element2>\n" +
                    "    <element21 id=\"e21_1\">VAL21-1</element21>\n" +
                    "    <element21 id=\"e21_2\">VAL21-2</element21>\n" +
                    "  </element2>\n" +
                    "  <element3 attrString=\"VAL3\" attrNum=\"5.6\" attrBool=\"true\"/>\n" +
                    "  <element4><element41>VAL4-1</element41></element4>\n" +
                    "</myevent>";

    public void testSimpleXML() throws Exception {
        Configuration configuration = getConfiguration();
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);

        ConfigurationEventTypeAxiom axiomType = new ConfigurationEventTypeAxiom();
        axiomType.setRootElementName("myevent");
        axiomType.addXPathProperty("xpathElement1", "/myevent/element1", XPathConstants.STRING);
        axiomType.addXPathProperty("xpathCountE21", "count(/myevent/element2/element21)", XPathConstants.NUMBER);
        axiomType.addXPathProperty("xpathAttrString", "/myevent/element3/@attrString", XPathConstants.STRING);
        axiomType.addXPathProperty("xpathAttrNum", "/myevent/element3/@attrNum", XPathConstants.NUMBER);
        axiomType.addXPathProperty("xpathAttrBool", "/myevent/element3/@attrBool", XPathConstants.BOOLEAN);
        configuration.addPlugInEventType("TestXMLNoSchemaType", new URI[]{new URI(AXIOM_URI)}, axiomType);

        EPServiceProvider epService = EPServiceProviderManager.getProvider("TestNoSchemaXML", configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));

        String stmt =
                "select element1," +
                        "element4.element41 as nestedElement," +
                        "element2.element21('e21_2') as mappedElement," +
                        "element2.element21[1] as indexedElement," +
                        "xpathElement1, xpathCountE21, xpathAttrString, xpathAttrNum, xpathAttrBool, " +
                        "invalidelement," +
                        "element3.myattribute as invalidattr " +
                        "from TestXMLNoSchemaType#length(100)";

        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        // Generate document with the specified in element1 to confirm we have independent events
        sendEvent(epService, "TestXMLNoSchemaType", "EventA");
        assertData("EventA");

        sendEvent(epService, "TestXMLNoSchemaType", "EventB");
        assertData("EventB");

        EventType eventType = ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("TestXMLNoSchemaType");
        assertEquals(5, eventType.getPropertyDescriptors().length);
        assertEquals(5, eventType.getPropertyNames().length);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("xpathElement1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("xpathCountE21", Double.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("xpathAttrString", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("xpathAttrNum", Double.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("xpathAttrBool", Boolean.class, null, false, false, false, false, false),
        }, eventType.getPropertyDescriptors());
    }

    public void testConfigurationXML() throws Exception {
        String sampleXML = "esper-axiom-sample-configuration.xml";
        URL url = this.getClass().getClassLoader().getResource(sampleXML);
        if (url == null) {
            throw new RuntimeException("Cannot find XML configuration: " + sampleXML);
        }

        Configuration config = new Configuration();
        config.getEngineDefaults().getByteCodeGeneration().setEnablePropertyGetter(false);
        config.configure(url);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select temp, sensorId from SensorEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent(epService, "SensorEvent", "<measurement><temperature>98.6</temperature><sensorid>8374744</sensorid></measurement>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals(98.6, theEvent.get("temp"));
        assertEquals(8374744L, theEvent.get("sensorId"));
    }

    public void testNestedXML() throws Exception {
        Configuration configuration = getConfiguration();
        ConfigurationEventTypeAxiom axiomType = new ConfigurationEventTypeAxiom();
        axiomType.setRootElementName("a");
        axiomType.addXPathProperty("element1", "/a/b/c", XPathConstants.STRING);
        configuration.addPlugInEventType("AEvent", new URI[]{new URI(AXIOM_URI)}, axiomType);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select b.c as type, element1, result1 from AEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent(epService, "AEvent", "<a><b><c></c></b></a>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("", theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent(epService, "AEvent", "<a><b></b></a>");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("", theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent(epService, "AEvent", "<a><b><c>text</c></b></a>");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("text", theEvent.get("type"));
        assertEquals("text", theEvent.get("element1"));

        // Use a URI sender list
        String xml = "<a><b><c>hype</c></b></a>";
        EventSender sender = epService.getEPRuntime().getEventSender(new URI[]{new URI(AXIOM_URI)});
        InputStream s = new ByteArrayInputStream(xml.getBytes());
        OMElement documentElement = new StAXOMBuilder(s).getDocumentElement();
        sender.sendEvent(documentElement);
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("hype", theEvent.get("type"));
        assertEquals("hype", theEvent.get("element1"));
    }

    public void testDotEscapeSyntax() throws Exception {
        Configuration configuration = getConfiguration();
        ConfigurationEventTypeAxiom axiomType = new ConfigurationEventTypeAxiom();
        axiomType.setRootElementName("myroot");
        configuration.addPlugInEventType("AEvent", new URI[]{new URI(AXIOM_URI)}, axiomType);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select a\\.b.c\\.d as val from AEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent(epService, "AEvent", "<myroot><a.b><c.d>value</c.d></a.b></myroot>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("value", theEvent.get("val"));
    }

    public void testEventXML() throws Exception {
        Configuration configuration = getConfiguration();
        ConfigurationEventTypeAxiom desc = new ConfigurationEventTypeAxiom();
        desc.addXPathProperty("eventtype", "/event/@type", XPathConstants.STRING);
        desc.addXPathProperty("eventuid", "/event/@uid", XPathConstants.STRING);
        desc.setRootElementName("event");
        configuration.addPlugInEventType("MyEvent", new URI[]{new URI(AXIOM_URI)}, desc);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select eventtype as type, eventuid as uid from MyEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent(epService, "MyEvent", "<event type=\"a-f-G\" uid=\"terminal.55\" time=\"2007-04-19T13:05:20.22Z\" version=\"2.0\"></event>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("a-f-G", theEvent.get("type"));
        assertEquals("terminal.55", theEvent.get("uid"));
    }

    public void testElementNode() throws Exception {
        // test for Esper-129
        Configuration configuration = getConfiguration();
        ConfigurationEventTypeAxiom desc = new ConfigurationEventTypeAxiom();
        desc.addXPathProperty("event.type", "//event/@type", XPathConstants.STRING);
        desc.addXPathProperty("event.uid", "//event/@uid", XPathConstants.STRING);
        desc.setRootElementName("batch-event");
        configuration.addPlugInEventType("MyEvent", new URI[]{new URI(AXIOM_URI)}, desc);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select event.type as type, event.uid as uid from MyEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        String xml = "<batch-event>" +
                "<event type=\"a-f-G\" uid=\"terminal.55\" time=\"2007-04-19T13:05:20.22Z\" version=\"2.0\"/>" +
                "</batch-event>";

        sendXMLEvent(epService, "MyEvent", xml);

        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("a-f-G", theEvent.get("type"));
        assertEquals("terminal.55", theEvent.get("uid"));
    }

    public void testNamespaceXPathRelative() throws Exception {
        Configuration configuration = getConfiguration();
        ConfigurationEventTypeAxiom desc = new ConfigurationEventTypeAxiom();
        desc.setRootElementName("getQuote");
        desc.setDefaultNamespace("http://services.samples/xsd");
        desc.setRootElementNamespace("http://services.samples/xsd");
        desc.addNamespacePrefix("m0", "http://services.samples/xsd");
        desc.setResolvePropertiesAbsolute(false);
        configuration.addPlugInEventType("StockQuote", new URI[]{new URI(AXIOM_URI)}, desc);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select request.symbol as symbol_a, symbol as symbol_b from StockQuote";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        String xml = "<m0:getQuote xmlns:m0=\"http://services.samples/xsd\"><m0:request><m0:symbol>IBM</m0:symbol></m0:request></m0:getQuote>";
        sendXMLEvent(epService, "StockQuote", xml);

        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("IBM", theEvent.get("symbol_a"));
        assertEquals("IBM", theEvent.get("symbol_b"));
    }

    public void testNamespaceXPathAbsolute() throws Exception {
        Configuration configuration = getConfiguration();
        ConfigurationEventTypeAxiom desc = new ConfigurationEventTypeAxiom();
        desc.addXPathProperty("symbol_a", "//m0:symbol", XPathConstants.STRING);
        desc.addXPathProperty("symbol_c", "/m0:getQuote/m0:request/m0:symbol", XPathConstants.STRING);
        desc.setRootElementName("getQuote");
        desc.setDefaultNamespace("http://services.samples/xsd");
        desc.setRootElementNamespace("http://services.samples/xsd");
        desc.addNamespacePrefix("m0", "http://services.samples/xsd");
        desc.setResolvePropertiesAbsolute(true);
        configuration.addPlugInEventType("StockQuote", new URI[]{new URI(AXIOM_URI)}, desc);

        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select symbol_a, symbol_b, symbol_c, request.symbol as symbol_d, symbol as symbol_e from StockQuote";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        String xml = "<m0:getQuote xmlns:m0=\"http://services.samples/xsd\"><m0:request><m0:symbol>IBM</m0:symbol></m0:request></m0:getQuote>";
        sendXMLEvent(epService, "StockQuote", xml);

        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("IBM", theEvent.get("symbol_a"));
        assertEquals("IBM", theEvent.get("symbol_c"));
        assertEquals("IBM", theEvent.get("symbol_d"));
        assertEquals("", theEvent.get("symbol_e"));    // should be empty string as we are doing absolute XPath
    }

    private void assertData(String element1) {
        assertNotNull(updateListener.getLastNewData());
        EventBean theEvent = updateListener.getLastNewData()[0];

        assertEquals(element1, theEvent.get("element1"));
        assertEquals("VAL4-1", theEvent.get("nestedElement"));
        assertEquals("VAL21-2", theEvent.get("mappedElement"));
        assertEquals("VAL21-2", theEvent.get("indexedElement"));

        assertEquals(element1, theEvent.get("xpathElement1"));
        assertEquals(2.0, theEvent.get("xpathCountE21"));
        assertEquals("VAL3", theEvent.get("xpathAttrString"));
        assertEquals(5.6, theEvent.get("xpathAttrNum"));
        assertEquals(true, theEvent.get("xpathAttrBool"));

        assertEquals("", theEvent.get("invalidelement"));        // properties not found come back as empty string without schema
        assertEquals("", theEvent.get("invalidattr"));     // attributes not supported when no schema supplied, use XPath
    }

    private void sendEvent(EPServiceProvider engine, String alias, String value) throws Exception {
        String xml = XML.replaceAll("VAL1", value);
        sendXMLEvent(engine, alias, xml);
    }

    private void sendXMLEvent(EPServiceProvider engine, String alias, String xml) throws Exception {
        InputStream s = new ByteArrayInputStream(xml.getBytes());
        OMElement documentElement = new StAXOMBuilder(s).getDocumentElement();
        EventSender sender = engine.getEPRuntime().getEventSender(alias);
        sender.sendEvent(documentElement);
    }

    private Configuration getConfiguration() throws URISyntaxException {
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        config.getEngineDefaults().getByteCodeGeneration().setEnablePropertyGetter(false);

        // register new representation of events
        config.addPlugInEventRepresentation(new URI(AXIOM_URI),
                AxiomEventRepresentation.class.getName(), null);

        return config;
    }
}
