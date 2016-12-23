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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.xml.SupportXPathFunctionResolver;
import com.espertech.esper.supportregression.xml.SupportXPathVariableResolver;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import java.io.StringReader;

public class TestNoSchemaXMLEvent extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    private static String XML =
        "<myevent>\n" +
        "  <element1>VAL1</element1>\n" +
        "  <element2>\n" +
        "    <element21 id=\"e21_1\">VAL21-1</element21>\n" +
        "    <element21 id=\"e21_2\">VAL21-2</element21>\n" +
        "  </element2>\n" +
        "  <element3 attrString=\"VAL3\" attrNum=\"5\" attrBool=\"true\"/>\n" +
        "  <element4><element41>VAL4-1</element41></element4>\n" +
        "</myevent>";

    protected void tearDown() throws Exception {
        updateListener = null;
    }

    public void testVariableAndDotMethodResolution() throws Exception
    {
        // test for ESPER-341 
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addVariable("var", int.class, 0);

        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrNum", "/myevent/@attrnum", XPathConstants.STRING, "long");
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrNumTwo", "/myevent/@attrnumtwo", XPathConstants.STRING, "long");
        configuration.addEventType("TestXMLNoSchemaType", xmlDOMEventTypeDesc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmtTextOne = "select var, xpathAttrNum.after(xpathAttrNumTwo) from TestXMLNoSchemaType#length(100)";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testSimpleXMLXPathProperties() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        xmlDOMEventTypeDesc.addXPathProperty("xpathElement1", "/myevent/element1", XPathConstants.STRING);
        xmlDOMEventTypeDesc.addXPathProperty("xpathCountE21", "count(/myevent/element2/element21)", XPathConstants.NUMBER);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrString", "/myevent/element3/@attrString", XPathConstants.STRING);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrNum", "/myevent/element3/@attrNum", XPathConstants.NUMBER);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrBool", "/myevent/element3/@attrBool", XPathConstants.BOOLEAN);
        xmlDOMEventTypeDesc.addXPathProperty("stringCastLong", "/myevent/element3/@attrNum", XPathConstants.STRING, "long");
        xmlDOMEventTypeDesc.addXPathProperty("stringCastDouble", "/myevent/element3/@attrNum", XPathConstants.STRING, "double");
        xmlDOMEventTypeDesc.addXPathProperty("numCastInt", "/myevent/element3/@attrNum", XPathConstants.NUMBER, "int");
        xmlDOMEventTypeDesc.setXPathFunctionResolver(SupportXPathFunctionResolver.class.getName());
        xmlDOMEventTypeDesc.setXPathVariableResolver(SupportXPathVariableResolver.class.getName());
        configuration.addEventType("TestXMLNoSchemaType", xmlDOMEventTypeDesc);

        xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("my.event2");
        configuration.addEventType("TestXMLWithDots", xmlDOMEventTypeDesc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI)epService).getEventAdapterService().getExistsTypeByName("TestXMLNoSchemaType");
        assertEquals(EventTypeMetadata.ApplicationType.XML, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("TestXMLNoSchemaType", type.getMetadata().getPrimaryName());
        assertEquals("TestXMLNoSchemaType", type.getMetadata().getPublicName());
        assertEquals("TestXMLNoSchemaType", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, type.getMetadata().getTypeClass());
        assertEquals(true, type.getMetadata().isApplicationConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfiguredStatic());

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("xpathElement1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("xpathCountE21", Double.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("xpathAttrString", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("xpathAttrNum", Double.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("xpathAttrBool", Boolean.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("stringCastLong", Long.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("stringCastDouble", Double.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("numCastInt", Integer.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        String stmt =
                "select xpathElement1, xpathCountE21, xpathAttrString, xpathAttrNum, xpathAttrBool," +
                       "stringCastLong," +
                       "stringCastDouble," +
                       "numCastInt " +
                       "from TestXMLNoSchemaType#length(100)";

        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        // Generate document with the specified in element1 to confirm we have independent events
        sendEvent("EventA");
        assertDataSimpleXPath("EventA");

        sendEvent("EventB");
        assertDataSimpleXPath("EventB");

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testSimpleXMLDOMGetter() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        xmlDOMEventTypeDesc.setXPathPropertyExpr(false);    // <== DOM getter
        configuration.addEventType("TestXMLNoSchemaType", xmlDOMEventTypeDesc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmt =
                "select element1, invalidelement, " +
                       "element4.element41 as nestedElement," +
                       "element2.element21('e21_2') as mappedElement," +
                       "element2.element21[1] as indexedElement," +
                       "element3.myattribute as invalidattribute " +
                       "from TestXMLNoSchemaType#length(100)";

        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        // Generate document with the specified in element1 to confirm we have independent events
        sendEvent("EventA");
        assertDataGetter("EventA", false);

        sendEvent("EventB");
        assertDataGetter("EventB", false);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testSimpleXMLXPathGetter() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();

        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        xmlDOMEventTypeDesc.setXPathPropertyExpr(true);    // <== XPath getter
        configuration.addEventType("TestXMLNoSchemaType", xmlDOMEventTypeDesc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmt =
                "select element1, invalidelement, " +
                       "element4.element41 as nestedElement," +
                       "element2.element21('e21_2') as mappedElement," +
                       "element2.element21[1] as indexedElement," +
                       "element3.myattribute as invalidattribute " +
                       "from TestXMLNoSchemaType#length(100)";

        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        // Generate document with the specified in element1 to confirm we have independent events
        sendEvent("EventA");
        assertDataGetter("EventA", true);

        sendEvent("EventB");
        assertDataGetter("EventB", true);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNestedXMLDOMGetter() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("a");
        xmlDOMEventTypeDesc.addXPathProperty("element1", "/a/b/c", XPathConstants.STRING);
        configuration.addEventType("AEvent", xmlDOMEventTypeDesc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmt = "select b.c as type, element1, result1 from AEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent("<a><b><c></c></b></a>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("", theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent("<a><b></b></a>");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent("<a><b><c>text</c></b></a>");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("text", theEvent.get("type"));
        assertEquals("text", theEvent.get("element1"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNestedXMLXPathGetter() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("a");
        xmlDOMEventTypeDesc.setXPathPropertyExpr(true);
        xmlDOMEventTypeDesc.addXPathProperty("element1", "/a/b/c", XPathConstants.STRING);
        configuration.addEventType("AEvent", xmlDOMEventTypeDesc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmt = "select b.c as type, element1, result1 from AEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent("<a><b><c></c></b></a>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("", theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent("<a><b></b></a>");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("", theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent("<a><b><c>text</c></b></a>");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("text", theEvent.get("type"));
        assertEquals("text", theEvent.get("element1"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testDotEscapeSyntax() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myroot");
        configuration.addEventType("AEvent", xmlDOMEventTypeDesc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmt = "select a\\.b.c\\.d as val from AEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent("<myroot><a.b><c.d>value</c.d></a.b></myroot>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("value", theEvent.get("val"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testEventXML() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.addXPathProperty("event.type", "/event/@type", XPathConstants.STRING);
        desc.addXPathProperty("event.uid", "/event/@uid", XPathConstants.STRING);
        desc.setRootElementName("event");
        configuration.addEventType("MyEvent", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmt = "select event.type as type, event.uid as uid from MyEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent("<event type=\"a-f-G\" uid=\"terminal.55\" time=\"2007-04-19T13:05:20.22Z\" version=\"2.0\"></event>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("a-f-G", theEvent.get("type"));
        assertEquals("terminal.55", theEvent.get("uid"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testElementNode() throws Exception
    {
        // test for Esper-129
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.addXPathProperty("event.type", "//event/@type", XPathConstants.STRING);
        desc.addXPathProperty("event.uid", "//event/@uid", XPathConstants.STRING);
        desc.setRootElementName("batch-event");
        configuration.addEventType("MyEvent", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmt = "select event.type as type, event.uid as uid from MyEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        String xml = "<batch-event>" +
                     "<event type=\"a-f-G\" uid=\"terminal.55\" time=\"2007-04-19T13:05:20.22Z\" version=\"2.0\"/>" +
                        "</batch-event>";
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document doc = builderFactory.newDocumentBuilder().parse(source);
        Element topElement = doc.getDocumentElement();

        epService.getEPRuntime().sendEvent(topElement);
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("a-f-G", theEvent.get("type"));
        assertEquals("terminal.55", theEvent.get("uid"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNamespaceXPathRelative() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("getQuote");
        desc.setDefaultNamespace("http://services.samples/xsd");
        desc.setRootElementNamespace("http://services.samples/xsd");
        desc.addNamespacePrefix("m0", "http://services.samples/xsd");
        desc.setXPathResolvePropertiesAbsolute(false);
        desc.setXPathPropertyExpr(true);
        configuration.addEventType("StockQuote", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmt = "select request.symbol as symbol_a, symbol as symbol_b from StockQuote";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        String xml = "<m0:getQuote xmlns:m0=\"http://services.samples/xsd\"><m0:request><m0:symbol>IBM</m0:symbol></m0:request></m0:getQuote>";
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document doc = builderFactory.newDocumentBuilder().parse(source);

        epService.getEPRuntime().sendEvent(doc);
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("IBM", theEvent.get("symbol_a"));
        assertEquals("IBM", theEvent.get("symbol_b"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNamespaceXPathAbsolute() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.addXPathProperty("symbol_a", "//m0:symbol", XPathConstants.STRING);
        desc.addXPathProperty("symbol_b", "//*[local-name(.) = 'getQuote' and namespace-uri(.) = 'http://services.samples/xsd']", XPathConstants.STRING);
        desc.addXPathProperty("symbol_c", "/m0:getQuote/m0:request/m0:symbol", XPathConstants.STRING);
        desc.setRootElementName("getQuote");
        desc.setDefaultNamespace("http://services.samples/xsd");
        desc.setRootElementNamespace("http://services.samples/xsd");
        desc.addNamespacePrefix("m0", "http://services.samples/xsd");
        desc.setXPathResolvePropertiesAbsolute(true);
        desc.setXPathPropertyExpr(true);
        configuration.addEventType("StockQuote", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmt = "select symbol_a, symbol_b, symbol_c, request.symbol as symbol_d, symbol as symbol_e from StockQuote";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        String xml = "<m0:getQuote xmlns:m0=\"http://services.samples/xsd\"><m0:request><m0:symbol>IBM</m0:symbol></m0:request></m0:getQuote>";
        //String xml = "<getQuote><request><symbol>IBM</symbol></request></getQuote>";
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document doc = builderFactory.newDocumentBuilder().parse(source);

        // For XPath resolution testing and namespaces...
        /*
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        XPathNamespaceContext ctx = new XPathNamespaceContext();
        ctx.addPrefix("m0", "http://services.samples/xsd");
        xPath.setNamespaceContext(ctx);
        XPathExpression expression = xPath.compile("/m0:getQuote/m0:request/m0:symbol");
        xPath.setNamespaceContext(ctx);
        System.out.println("result=" + expression.evaluate(doc,XPathConstants.STRING));
        */

        epService.getEPRuntime().sendEvent(doc);
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("IBM", theEvent.get("symbol_a"));
        assertEquals("IBM", theEvent.get("symbol_b"));
        assertEquals("IBM", theEvent.get("symbol_c"));
        assertEquals("IBM", theEvent.get("symbol_d"));
        assertEquals("", theEvent.get("symbol_e"));    // should be empty string as we are doing absolute XPath

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testXPathArray() throws Exception
    {
        String xml = "<Event IsTriggering=\"True\">\n" +
                "<Field Name=\"A\" Value=\"987654321\"/>\n" +
                "<Field Name=\"B\" Value=\"2196958725202\"/>\n" +
                "<Field Name=\"C\" Value=\"1232363702\"/>\n" +
                "<Participants>\n" +
                "<Participant>\n" +
                "<Field Name=\"A\" Value=\"9876543210\"/>\n" +
                "<Field Name=\"B\" Value=\"966607340\"/>\n" +
                "<Field Name=\"D\" Value=\"353263010930650\"/>\n" +
                "</Participant>\n" +
                "</Participants>\n" +
                "</Event>";

        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("Event");
        desc.addXPathProperty("A", "//Field[@Name='A']/@Value", XPathConstants.NODESET, "String[]");

        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addEventType("Event", desc);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from Event");
        updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        Document doc = SupportXML.getDocument(xml);
        epService.getEPRuntime().sendEvent(doc);

        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        Object value = theEvent.get("A");
        EPAssertionUtil.assertProps(theEvent, "A".split(","), new Object[]{new Object[]{"987654321", "9876543210"}});

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    private void assertDataSimpleXPath(String element1)
    {
        assertNotNull(updateListener.getLastNewData());
        EventBean theEvent = updateListener.getLastNewData()[0];

        assertEquals(element1, theEvent.get("xpathElement1"));
        assertEquals(2.0, theEvent.get("xpathCountE21"));
        assertEquals("VAL3", theEvent.get("xpathAttrString"));
        assertEquals(5d, theEvent.get("xpathAttrNum"));
        assertEquals(true, theEvent.get("xpathAttrBool"));
        assertEquals(5L, theEvent.get("stringCastLong"));
        assertEquals(5d, theEvent.get("stringCastDouble"));
        assertEquals(5, theEvent.get("numCastInt"));
    }

    private void assertDataGetter(String element1, boolean isInvalidReturnsEmptyString)
    {
        assertNotNull(updateListener.getLastNewData());
        EventBean theEvent = updateListener.getLastNewData()[0];

        assertEquals(element1, theEvent.get("element1"));
        assertEquals("VAL4-1", theEvent.get("nestedElement"));
        assertEquals("VAL21-2", theEvent.get("mappedElement"));
        assertEquals("VAL21-2", theEvent.get("indexedElement"));

        if (isInvalidReturnsEmptyString)
        {
            assertEquals("", theEvent.get("invalidelement"));
            assertEquals("", theEvent.get("invalidattribute"));
        }
        else
        {
            assertEquals(null, theEvent.get("invalidelement"));
            assertEquals(null, theEvent.get("invalidattribute"));
        }
    }

    private void sendEvent(String value) throws Exception
    {
        String xml = XML.replaceAll("VAL1", value);
        log.debug(".sendEvent value=" + value);

        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);

        epService.getEPRuntime().sendEvent(simpleDoc);
    }

    private void sendXMLEvent(String xml) throws Exception
    {
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);

        epService.getEPRuntime().sendEvent(simpleDoc);
    }

    private static final Logger log = LoggerFactory.getLogger(TestNoSchemaXMLEvent.class);
}





