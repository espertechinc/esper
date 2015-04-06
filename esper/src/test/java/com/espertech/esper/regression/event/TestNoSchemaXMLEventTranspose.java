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
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.event.EventTypeAssertionUtil;
import junit.framework.TestCase;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;

public class TestNoSchemaXMLEventTranspose extends TestCase
{
    private static String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";

    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setIterableUnbound(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testXPathConfigured() throws Exception
    {
        ConfigurationEventTypeXMLDOM rootMeta = new ConfigurationEventTypeXMLDOM();
        rootMeta.setRootElementName("simpleEvent");
        rootMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        rootMeta.addXPathPropertyFragment("nested1simple", "/ss:simpleEvent/ss:nested1", XPathConstants.NODE, "MyNestedEvent");
        rootMeta.addXPathPropertyFragment("nested4array", "//ss:nested4", XPathConstants.NODESET, "MyNestedArrayEvent");
        epService.getEPAdministrator().getConfiguration().addEventType("MyXMLEvent", rootMeta);

        ConfigurationEventTypeXMLDOM metaNested = new ConfigurationEventTypeXMLDOM();
        metaNested.setRootElementName("nested1");
        epService.getEPAdministrator().getConfiguration().addEventType("MyNestedEvent", metaNested);

        ConfigurationEventTypeXMLDOM metaNestedArray = new ConfigurationEventTypeXMLDOM();
        metaNestedArray.setRootElementName("nested4");
        epService.getEPAdministrator().getConfiguration().addEventType("MyNestedArrayEvent", metaNestedArray);

        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into Nested3Stream select nested1simple, nested4array from MyXMLEvent");
        EPStatement stmtWildcard = epService.getEPAdministrator().createEPL("select * from MyXMLEvent");
        EventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());
        EventTypeAssertionUtil.assertConsistency(stmtWildcard.getEventType());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nested1simple", Node.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("nested4array", Node[].class, Node.class, false, false, true, false, true),
        }, stmtInsert.getEventType().getPropertyDescriptors());

        FragmentEventType fragmentTypeNested1 = stmtInsert.getEventType().getFragmentType("nested1simple");
        assertFalse(fragmentTypeNested1.isIndexed());
        assertEquals(0, fragmentTypeNested1.getFragmentType().getPropertyDescriptors().length);
        EventTypeAssertionUtil.assertConsistency(fragmentTypeNested1.getFragmentType());

        FragmentEventType fragmentTypeNested4 = stmtInsert.getEventType().getFragmentType("nested4array");
        assertTrue(fragmentTypeNested4.isIndexed());
        assertEquals(0, fragmentTypeNested4.getFragmentType().getPropertyDescriptors().length);
        EventTypeAssertionUtil.assertConsistency(fragmentTypeNested4.getFragmentType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "ABC");

        EventBean received = stmtInsert.iterator().next();
        EPAssertionUtil.assertProps(received, "nested1simple.prop1,nested1simple.prop2,nested1simple.attr1,nested1simple.nested2.prop3[1]".split(","), new Object[]{"SAMPLE_V1", "true", "SAMPLE_ATTR1", "4"});
        EPAssertionUtil.assertProps(received, "nested4array[0].id,nested4array[0].prop5[1],nested4array[1].id".split(","), new Object[]{"a", "SAMPLE_V8", "b"});

        // assert event and fragments alone
        EventBean wildcardStmtEvent = stmtWildcard.iterator().next();
        EventTypeAssertionUtil.assertConsistency(wildcardStmtEvent);

        FragmentEventType eventType = wildcardStmtEvent.getEventType().getFragmentType("nested1simple");
        assertFalse(eventType.isIndexed());
        assertFalse(eventType.isNative());
        assertEquals("MyNestedEvent", eventType.getFragmentType().getName());
        assertTrue(wildcardStmtEvent.get("nested1simple") instanceof Node);
        assertEquals("SAMPLE_V1", ((EventBean)wildcardStmtEvent.getFragment("nested1simple")).get("prop1"));

        eventType = wildcardStmtEvent.getEventType().getFragmentType("nested4array");
        assertTrue(eventType.isIndexed());
        assertFalse(eventType.isNative());
        assertEquals("MyNestedArrayEvent", eventType.getFragmentType().getName());
        EventBean[] eventsArray = (EventBean[])wildcardStmtEvent.getFragment("nested4array");
        assertEquals(3, eventsArray.length);
        assertEquals("SAMPLE_V8", eventsArray[0].get("prop5[1]"));
        assertEquals("SAMPLE_V9", eventsArray[1].get("prop5[0]"));
        assertEquals(NodeList.class, wildcardStmtEvent.getEventType().getPropertyType("nested4array"));
        assertTrue(wildcardStmtEvent.get("nested4array") instanceof NodeList);
    }

    public void testExpressionSimpleDOMGetter() throws Exception
    {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        // eventTypeMeta.setXPathPropertyExpr(false); <== the default
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLSchemaType", eventTypeMeta);

        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyNestedStream select nested1 from TestXMLSchemaType");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nested1", String.class, null, false, false, false, false, false),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        EventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        EPStatement stmtSelectWildcard = epService.getEPAdministrator().createEPL("select * from TestXMLSchemaType");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[0], stmtSelectWildcard.getEventType().getPropertyDescriptors());
        EventTypeAssertionUtil.assertConsistency(stmtSelectWildcard.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");
        EventBean stmtInsertWildcardBean = stmtInsert.iterator().next();
        EventBean stmtSelectWildcardBean = stmtSelectWildcard.iterator().next();
        assertNotNull(stmtInsertWildcardBean.get("nested1"));
        EventTypeAssertionUtil.assertConsistency(stmtSelectWildcardBean);
        EventTypeAssertionUtil.assertConsistency(stmtInsert.iterator().next());
        
        assertEquals(0, stmtSelectWildcardBean.getEventType().getPropertyNames().length);
    }

    // Note that XPath Node results when transposed must be queried by XPath that is also absolute.
    // For example: "nested1" => "/n0:simpleEvent/n0:nested1" results in a Node.
    // That result Node's "prop1" =>  "/n0:simpleEvent/n0:nested1/n0:prop1" and "/n0:nested1/n0:prop1" does NOT result in a value.
    // Therefore property transposal is disabled for Property-XPath expressions.
    public void testExpressionSimpleXPathGetter() throws Exception
    {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = TestNoSchemaXMLEventTranspose.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        eventTypeMeta.setXPathPropertyExpr(true);       // <== note this
        eventTypeMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLSchemaType", eventTypeMeta);

        // note class not a fragment
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyNestedStream select nested1 from TestXMLSchemaType");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, false),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        EventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        EventType type = ((EPServiceProviderSPI)epService).getEventAdapterService().getExistsTypeByName("TestXMLSchemaType");
        EventTypeAssertionUtil.assertConsistency(type);
        assertNull(type.getFragmentType("nested1"));
        assertNull(type.getFragmentType("nested1.nested2"));

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "ABC");
        EventTypeAssertionUtil.assertConsistency(stmtInsert.iterator().next());
    }
}
