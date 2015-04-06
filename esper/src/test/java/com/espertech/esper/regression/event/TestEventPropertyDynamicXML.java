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
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class TestEventPropertyDynamicXML extends TestCase
{
    private static String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";
    private SupportUpdateListener listener;
    private EPServiceProvider epService;

    private static String NOSCHEMA_XML = "<simpleEvent>\n" +
            "\t<type>abc</type>\n" +
            "\t<dyn>1</dyn>\n" +
            "\t<dyn>2</dyn>\n" +
            "\t<nested>\n" +
            "\t\t<nes2>3</nes2>\n" +
            "\t</nested>\n" +
            "\t<map id='a'>4</map>\n" +
            "</simpleEvent>";

    private static String SCHEMA_XML = "<simpleEvent xmlns=\"samples:schemas:simpleSchema\" \n" +
            "  xmlns:ss=\"samples:schemas:simpleSchema\" \n" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
            "  xsi:schemaLocation=\"samples:schemas:simpleSchema simpleSchema.xsd\">" +
            "<type>abc</type>\n" +
            "<dyn>1</dyn>\n" +
            "<dyn>2</dyn>\n" +
            "<nested>\n" +
            "<nes2>3</nes2>\n" +
            "</nested>\n" +
            "<map id='a'>4</map>\n" +
            "</simpleEvent>";

    public void setUp()
    {
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testSchemaXPathGetter() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("simpleEvent");
        String schemaUri = TestSchemaXMLEvent.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        desc.setSchemaResource(schemaUri);
        desc.setXPathPropertyExpr(true);
        desc.setEventSenderValidatesRoot(false);
        desc.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        desc.setDefaultNamespace("samples:schemas:simpleSchema");
        configuration.addEventType("MyEvent", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "select type?,dyn[1]?,nested.nes2?,map('a')? from MyEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("type?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("dyn[1]?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested.nes2?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("map('a')?", Node.class, null, false, false, false, false, false),
        }, stmt.getEventType().getPropertyDescriptors());
        EventTypeAssertionUtil.assertConsistency(stmt.getEventType());

        EventSender sender = epService.getEPRuntime().getEventSender("MyEvent");
        Document root = SupportXML.sendEvent(sender, SCHEMA_XML);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertSame(root.getDocumentElement().getChildNodes().item(0), theEvent.get("type?"));
        assertSame(root.getDocumentElement().getChildNodes().item(4), theEvent.get("dyn[1]?"));
        assertSame(root.getDocumentElement().getChildNodes().item(6).getChildNodes().item(1), theEvent.get("nested.nes2?"));
        assertSame(root.getDocumentElement().getChildNodes().item(8), theEvent.get("map('a')?"));
        EventTypeAssertionUtil.assertConsistency(theEvent);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testSchemaDOMGetter() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("simpleEvent");
        String schemaUri = TestSchemaXMLEvent.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        desc.setSchemaResource(schemaUri);
        desc.setXPathPropertyExpr(false);
        desc.setEventSenderValidatesRoot(false);
        desc.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        desc.setDefaultNamespace("samples:schemas:simpleSchema");
        configuration.addEventType("MyEvent", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "select type?,dyn[1]?,nested.nes2?,map('a')? from MyEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("type?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("dyn[1]?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested.nes2?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("map('a')?", Node.class, null, false, false, false, false, false),
        }, stmt.getEventType().getPropertyDescriptors());
        EventTypeAssertionUtil.assertConsistency(stmt.getEventType());

        EventSender sender = epService.getEPRuntime().getEventSender("MyEvent");
        Document root = SupportXML.sendEvent(sender, SCHEMA_XML);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertSame(root.getDocumentElement().getChildNodes().item(0), theEvent.get("type?"));
        assertSame(root.getDocumentElement().getChildNodes().item(4), theEvent.get("dyn[1]?"));
        assertSame(root.getDocumentElement().getChildNodes().item(6).getChildNodes().item(1), theEvent.get("nested.nes2?"));
        assertSame(root.getDocumentElement().getChildNodes().item(8), theEvent.get("map('a')?"));
        EventTypeAssertionUtil.assertConsistency(theEvent);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNoSchemaXPathGetter() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("simpleEvent");
        desc.setXPathPropertyExpr(true);
        configuration.addEventType("MyEvent", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "select type?,dyn[1]?,nested.nes2?,map('a')?,other? from MyEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("type?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("dyn[1]?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested.nes2?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("map('a')?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("other?", Node.class, null, false, false, false, false, false),
        }, stmt.getEventType().getPropertyDescriptors());
        EventTypeAssertionUtil.assertConsistency(stmt.getEventType());

        Document root = SupportXML.sendEvent(epService.getEPRuntime(), NOSCHEMA_XML);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertSame(root.getDocumentElement().getChildNodes().item(1), theEvent.get("type?"));
        assertSame(root.getDocumentElement().getChildNodes().item(5), theEvent.get("dyn[1]?"));
        assertSame(root.getDocumentElement().getChildNodes().item(7).getChildNodes().item(1), theEvent.get("nested.nes2?"));
        assertSame(root.getDocumentElement().getChildNodes().item(9), theEvent.get("map('a')?"));
        assertNull(theEvent.get("other?"));
        EventTypeAssertionUtil.assertConsistency(theEvent);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testNoSchemaDOMGetter() throws Exception
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("simpleEvent");
        configuration.addEventType("MyEvent", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}

        String stmtText = "select type?,dyn[1]?,nested.nes2?,map('a')? from MyEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("type?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("dyn[1]?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested.nes2?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("map('a')?", Node.class, null, false, false, false, false, false),
        }, stmt.getEventType().getPropertyDescriptors());
        EventTypeAssertionUtil.assertConsistency(stmt.getEventType());

        Document root = SupportXML.sendEvent(epService.getEPRuntime(), NOSCHEMA_XML);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertSame(root.getDocumentElement().getChildNodes().item(1), theEvent.get("type?"));
        assertSame(root.getDocumentElement().getChildNodes().item(5), theEvent.get("dyn[1]?"));
        assertSame(root.getDocumentElement().getChildNodes().item(7).getChildNodes().item(1), theEvent.get("nested.nes2?"));
        assertSame(root.getDocumentElement().getChildNodes().item(9), theEvent.get("map('a')?"));
        EventTypeAssertionUtil.assertConsistency(theEvent);

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }
}
