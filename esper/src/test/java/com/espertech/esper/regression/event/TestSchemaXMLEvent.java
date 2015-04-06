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
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.event.EventTypeAssertionUtil;
import com.espertech.esper.util.FileUtil;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathConstants;
import java.io.InputStream;

public class TestSchemaXMLEvent extends TestCase
{
    public static final String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";
    public static final String CLASSLOADER_SCHEMA_WITH_ALL_URI = "regression/simpleSchemaWithAll.xsd";
    public static final String CLASSLOADER_SCHEMA_WITH_RESTRICTION_URI = "regression/simpleSchemaWithRestriction.xsd";

    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    public void testSchemaXMLWSchemaWithRestriction() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("order");
        InputStream schemaStream = TestSchemaXMLEvent.class.getClassLoader().getResourceAsStream(CLASSLOADER_SCHEMA_WITH_RESTRICTION_URI);
        assertNotNull(schemaStream);
        String schemaText = FileUtil.linesToText(FileUtil.readFile(schemaStream));
        eventTypeMeta.setSchemaText(schemaText);
        config.addEventType("OrderEvent", eventTypeMeta);

        epService = EPServiceProviderManager.getProvider("TestSchemaXML", config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String text = "select order_amount from OrderEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(updateListener);

        SupportXML.sendEvent(epService.getEPRuntime(),
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<order>\n" +
            "<order_amount>202.1</order_amount>" +
            "</order>");
        EventBean theEvent = updateListener.getLastNewData()[0];
        assertEquals(Double.class, theEvent.get("order_amount").getClass());
        assertEquals(202.1d, theEvent.get("order_amount"));
        updateListener.reset();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        updateListener = null;
    }

    public void testSchemaXMLWSchemaWithAll() throws Exception
    {
        Configuration config = SupportConfigFactory.getConfiguration();        
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("event-page-visit");
        String schemaUri = TestSchemaXMLEvent.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_WITH_ALL_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        eventTypeMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchemaWithAll");
        eventTypeMeta.addXPathProperty("url", "/ss:event-page-visit/ss:url", XPathConstants.STRING);
        config.addEventType("PageVisitEvent", eventTypeMeta);

        epService = EPServiceProviderManager.getProvider("TestSchemaXML", config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        // url='page4'
        String text = "select a.url as sesja from pattern [ every a=PageVisitEvent(url='page1') ]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(updateListener);

        SupportXML.sendEvent(epService.getEPRuntime(),
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<event-page-visit xmlns=\"samples:schemas:simpleSchemaWithAll\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"samples:schemas:simpleSchemaWithAll simpleSchemaWithAll.xsd\">\n" +
            "<url>page1</url>" +
            "</event-page-visit>");
        EventBean theEvent = updateListener.getLastNewData()[0];
        assertEquals("page1", theEvent.get("sesja"));
        updateListener.reset();

        SupportXML.sendEvent(epService.getEPRuntime(),
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<event-page-visit xmlns=\"samples:schemas:simpleSchemaWithAll\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"samples:schemas:simpleSchemaWithAll simpleSchemaWithAll.xsd\">\n" +
            "<url>page2</url>" +
            "</event-page-visit>");
        assertFalse(updateListener.isInvoked());

        EventType type = epService.getEPAdministrator().createEPL("select * from PageVisitEvent").getEventType();
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("sessionId", Node.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("customerId", Node.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("url", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("method", Node.class, null, false, false, false, false, true),
        }, type.getPropertyDescriptors());
    }

    public void testSchemaXMLQuery_XPathBacked() throws Exception
    {
        epService = EPServiceProviderManager.getProvider("TestSchemaXML", getConfig(true));
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmtSelectWild = "select * from TestXMLSchemaType";
        EPStatement wildStmt = epService.getEPAdministrator().createEPL(stmtSelectWild);
        EventType type = wildStmt.getEventType();
        EventTypeAssertionUtil.assertConsistency(type);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("prop4", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested3", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("customProp", Double.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        String stmt =
                "select nested1 as nodeProp," +
                        "prop4 as nested1Prop," +
                        "nested1.prop2 as nested2Prop," +
                        "nested3.nested4('a').prop5[1] as complexProp," +
                        "nested1.nested2.prop3[2] as indexedProp," +
                        "customProp," +
                        "prop4.attr2 as attrOneProp," +
                        "nested3.nested4[2].id as attrTwoProp" +
                " from TestXMLSchemaType.win:length(100)";

        EPStatement selectStmt = epService.getEPAdministrator().createEPL(stmt);
        selectStmt.addListener(updateListener);
        type = selectStmt.getEventType();
        EventTypeAssertionUtil.assertConsistency(type);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nodeProp", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested1Prop", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested2Prop", Boolean.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("complexProp", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("indexedProp", Integer.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("customProp", Double.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("attrOneProp", Boolean.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("attrTwoProp", String.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        Document eventDoc = SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");

        assertNotNull(updateListener.getLastNewData());
        EventBean theEvent = updateListener.getLastNewData()[0];

        assertSame(eventDoc.getDocumentElement().getChildNodes().item(1), theEvent.get("nodeProp"));
        assertEquals("SAMPLE_V6", theEvent.get("nested1Prop"));
        assertEquals(true, theEvent.get("nested2Prop"));
        assertEquals("SAMPLE_V8", theEvent.get("complexProp"));
        assertEquals(5, theEvent.get("indexedProp"));
        assertEquals(3.0, theEvent.get("customProp"));
        assertEquals(true, theEvent.get("attrOneProp"));
        assertEquals("c", theEvent.get("attrTwoProp"));

        /**
         * Comment-in for performance testing
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++)
        {
            sendEvent("test");
        }
        long end = System.nanoTime();
        double delta = (end - start) / 1000d / 1000d / 1000d;
        System.out.println(delta);
         */
    }

    public void testSchemaXMLQuery_DOMGetterBacked() throws Exception
    {
        epService = EPServiceProviderManager.getProvider("TestSchemaXML", getConfig(false));
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();

        String stmtSelectWild = "select * from TestXMLSchemaType";
        EPStatement wildStmt = epService.getEPAdministrator().createEPL(stmtSelectWild);
        EventType type = wildStmt.getEventType();
        EventTypeAssertionUtil.assertConsistency(type);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("prop4", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested3", Node.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("customProp", Double.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        String stmt =
                "select nested1 as nodeProp," +
                        "prop4 as nested1Prop," +
                        "nested1.prop2 as nested2Prop," +
                        "nested3.nested4('a').prop5[1] as complexProp," +
                        "nested1.nested2.prop3[2] as indexedProp," +
                        "customProp," +
                        "prop4.attr2 as attrOneProp," +
                        "nested3.nested4[2].id as attrTwoProp" +
                " from TestXMLSchemaType.win:length(100)";

        EPStatement selectStmt = epService.getEPAdministrator().createEPL(stmt);
        selectStmt.addListener(updateListener);
        type = selectStmt.getEventType();
        EventTypeAssertionUtil.assertConsistency(type);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nodeProp", Node.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("nested1Prop", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested2Prop", Boolean.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("complexProp", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("indexedProp", Integer.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("customProp", Double.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("attrOneProp", Boolean.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("attrTwoProp", String.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        Document eventDoc = SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");

        assertNotNull(updateListener.getLastNewData());
        EventBean theEvent = updateListener.getLastNewData()[0];

        assertSame(eventDoc.getDocumentElement().getChildNodes().item(1), theEvent.get("nodeProp"));
        assertEquals("SAMPLE_V6", theEvent.get("nested1Prop"));
        assertEquals(true, theEvent.get("nested2Prop"));
        assertEquals("SAMPLE_V8", theEvent.get("complexProp"));
        assertEquals(5, theEvent.get("indexedProp"));
        assertEquals(3.0, theEvent.get("customProp"));
        assertEquals(true, theEvent.get("attrOneProp"));
        assertEquals("c", theEvent.get("attrTwoProp"));

        /**
         * Comment-in for performance testing
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++)
        {
            sendEvent("test");
        }
        long end = System.nanoTime();
        double delta = (end - start) / 1000d / 1000d / 1000d;
        System.out.println(delta);
         */
    }

    public void testAddRemoveType()
    {
        epService = EPServiceProviderManager.getProvider("TestSchemaXML", getConfig(false));
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        updateListener = new SupportUpdateListener();
        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();

        // test remove type with statement used (no force)
        configOps.addEventType("MyXMLEvent", getConfigTestType("p01", false));
        EPStatement stmt = epService.getEPAdministrator().createEPL("select p01 from MyXMLEvent", "stmtOne");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyXMLEvent").toArray(), new String[]{"stmtOne"});

        try {
            configOps.removeEventType("MyXMLEvent", false);
        }
        catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyXMLEvent"));
        }

        // destroy statement and type
        stmt.destroy();
        assertTrue(configOps.getEventTypeNameUsedBy("MyXMLEvent").isEmpty());
        assertTrue(configOps.isEventTypeExists("MyXMLEvent"));
        assertTrue(configOps.removeEventType("MyXMLEvent", false));
        assertFalse(configOps.removeEventType("MyXMLEvent", false));    // try double-remove
        assertFalse(configOps.isEventTypeExists("MyXMLEvent"));
        try {
            epService.getEPAdministrator().createEPL("select p01 from MyXMLEvent");
            fail();
        }
        catch (EPException ex) {
            // expected
        }

        // add back the type
        configOps.addEventType("MyXMLEvent", getConfigTestType("p20", false));
        assertTrue(configOps.isEventTypeExists("MyXMLEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyXMLEvent").isEmpty());

        // compile
        epService.getEPAdministrator().createEPL("select p20 from MyXMLEvent", "stmtTwo");
        EPAssertionUtil.assertEqualsExactOrder(configOps.getEventTypeNameUsedBy("MyXMLEvent").toArray(), new String[]{"stmtTwo"});
        try {
            epService.getEPAdministrator().createEPL("select p01 from MyXMLEvent");
            fail();
        }
        catch (EPException ex) {
            // expected
        }

        // remove with force
        try {
            configOps.removeEventType("MyXMLEvent", false);
        }
        catch (ConfigurationException ex) {
            assertTrue(ex.getMessage().contains("MyXMLEvent"));
        }
        assertTrue(configOps.removeEventType("MyXMLEvent", true));
        assertFalse(configOps.isEventTypeExists("MyXMLEvent"));
        assertTrue(configOps.getEventTypeNameUsedBy("MyXMLEvent").isEmpty());

        // add back the type
        configOps.addEventType("MyXMLEvent", getConfigTestType("p03", false));
        assertTrue(configOps.isEventTypeExists("MyXMLEvent"));

        // compile
        epService.getEPAdministrator().createEPL("select p03 from MyXMLEvent");
        try {
            epService.getEPAdministrator().createEPL("select p20 from MyXMLEvent");
            fail();
        }
        catch (EPException ex) {
            // expected
        }
    }

    public void testInvalid()
    {
        epService = EPServiceProviderManager.getProvider("TestSchemaXML", getConfig(false));
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        try
        {
            epService.getEPAdministrator().createEPL("select element1 from TestXMLSchemaType.win:length(100)");
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'element1': Property named 'element1' is not valid in any stream [select element1 from TestXMLSchemaType.win:length(100)]", ex.getMessage());
        }
    }

    private Configuration getConfig(boolean isUseXPathPropertyExpression)
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("TestXMLSchemaType", getConfigTestType(null, isUseXPathPropertyExpression));
        return configuration;
    }

    private ConfigurationEventTypeXMLDOM getConfigTestType(String additionalXPathProperty, boolean isUseXPathPropertyExpression)
    {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = TestSchemaXMLEvent.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        eventTypeMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        eventTypeMeta.addXPathProperty("customProp", "count(/ss:simpleEvent/ss:nested3/ss:nested4)", XPathConstants.NUMBER);
        eventTypeMeta.setXPathPropertyExpr(isUseXPathPropertyExpression);
        if (additionalXPathProperty != null)
        {
            eventTypeMeta.addXPathProperty(additionalXPathProperty, "count(/ss:simpleEvent/ss:nested3/ss:nested4)", XPathConstants.NUMBER);
        }
        return eventTypeMeta;        
    }

    /**
     * Comment-in for namespace-aware testing.
     *
    public void testNamespace() throws Exception
    {
        String xml = "<onens:namespaceEvent xmlns=\"samples:schemas:simpleSchema\" \n" +
                "  xmlns:onens=\"samples:schemas:testNSOne\" \n" +
                "  xmlns:twons=\"samples:schemas:testNSTwo\" \n" +
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "  xsi:schemaLocation=\"samples:schemas:testNSOne namespaceTestSchemaOne.xsd\">\n" +
                "  <onens:myname onens:val=\"1\"/>\n" +
                "  <twons:myname twons:val=\"2\"/>\n" +
                "</onens:namespaceEvent>";
        Document doc = SupportXML.getDocument(xml);
        Node node = doc.getDocumentElement().getChildNodes().item(1);
        System.out.println("local " + node.getLocalName());
        System.out.println("nsURI " + node.getNamespaceURI());
        System.out.println("prefix " + node.getPrefix());

        node = doc.getDocumentElement().getChildNodes().item(3);
        System.out.println("local " + node.getLocalName());
        System.out.println("nsURI " + node.getNamespaceURI());
        System.out.println("prefix " + node.getPrefix());

        SchemaModel model = XSDSchemaMapper.loadAndMap("regression/namespaceTestSchemaOne.xsd", 2);
        System.out.println("local " + node.getLocalName());
    }
     */

    private static final Log log = LogFactory.getLog(TestSchemaXMLEvent.class);
}
