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
package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.xml.XPathNamespaceContext;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.util.support.SupportEventTypeAssertionUtil;
import junit.framework.TestCase;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

public class TestXMLSchemaEventTranspose extends TestCase
{
    private static String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";

    private EPServiceProvider epService;
    private String schemaURI;
    private SupportUpdateListener listener;

    public void setUp()
    {
        schemaURI = TestXMLSchemaEventTranspose.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        listener = new SupportUpdateListener();

        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testXPathConfigured() throws Exception
    {
        ConfigurationEventTypeXMLDOM rootMeta = new ConfigurationEventTypeXMLDOM();
        rootMeta.setRootElementName("simpleEvent");
        rootMeta.setSchemaResource(schemaURI);
        rootMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        rootMeta.addXPathPropertyFragment("nested1simple", "/ss:simpleEvent/ss:nested1", XPathConstants.NODE, "MyNestedEvent");
        rootMeta.addXPathPropertyFragment("nested4array", "//ss:nested4", XPathConstants.NODESET, "MyNestedArrayEvent");
        rootMeta.setAutoFragment(false);
        epService.getEPAdministrator().getConfiguration().addEventType("MyXMLEvent", rootMeta);

        ConfigurationEventTypeXMLDOM metaNested = new ConfigurationEventTypeXMLDOM();
        metaNested.setRootElementName("//nested1");
        metaNested.setSchemaResource(schemaURI);
        metaNested.setAutoFragment(false);
        epService.getEPAdministrator().getConfiguration().addEventType("MyNestedEvent", metaNested);

        ConfigurationEventTypeXMLDOM metaNestedArray = new ConfigurationEventTypeXMLDOM();
        metaNestedArray.setRootElementName("//nested4");
        metaNestedArray.setSchemaResource(schemaURI);
        epService.getEPAdministrator().getConfiguration().addEventType("MyNestedArrayEvent", metaNestedArray);

        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into Nested3Stream select nested1simple, nested4array from MyXMLEvent#lastevent");
        EPStatement stmtWildcard = epService.getEPAdministrator().createEPL("select * from MyXMLEvent#lastevent");
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());
        SupportEventTypeAssertionUtil.assertConsistency(stmtWildcard.getEventType());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nested1simple", Node.class, null, false, false, false, false, true),
                new EventPropertyDescriptor("nested4array", Node[].class, Node.class, false, false, true, false, true),
        }, stmtInsert.getEventType().getPropertyDescriptors());

        FragmentEventType fragmentTypeNested1 = stmtInsert.getEventType().getFragmentType("nested1simple");
        assertFalse(fragmentTypeNested1.isIndexed());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("prop1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("prop2", Boolean.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("attr1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested2", Node.class, null, false, false, false, false, false),
        }, fragmentTypeNested1.getFragmentType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(fragmentTypeNested1.getFragmentType());

        FragmentEventType fragmentTypeNested4 = stmtInsert.getEventType().getFragmentType("nested4array");
        assertTrue(fragmentTypeNested4.isIndexed());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("prop5", String[].class, null, false, false, true, false, false),
                new EventPropertyDescriptor("prop6", String[].class, null, false, false, true, false, false),
                new EventPropertyDescriptor("prop7", String[].class, null, false, false, true, false, false),
                new EventPropertyDescriptor("prop8", String[].class, null, false, false, true, false, false),
                new EventPropertyDescriptor("id", String.class, null, false, false, false, false, false),
        }, fragmentTypeNested4.getFragmentType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(fragmentTypeNested4.getFragmentType());

        FragmentEventType fragmentTypeNested4Item = stmtInsert.getEventType().getFragmentType("nested4array[0]");
        assertFalse(fragmentTypeNested4Item.isIndexed());
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("prop5", String[].class, null, false, false, true, false, false),
                new EventPropertyDescriptor("prop6", String[].class, null, false, false, true, false, false),
                new EventPropertyDescriptor("prop7", String[].class, null, false, false, true, false, false),
                new EventPropertyDescriptor("prop8", String[].class, null, false, false, true, false, false),
                new EventPropertyDescriptor("id", String.class, null, false, false, false, false, false),
        }, fragmentTypeNested4Item.getFragmentType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(fragmentTypeNested4Item.getFragmentType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "ABC");

        EventBean received = stmtInsert.iterator().next();
        EPAssertionUtil.assertProps(received, "nested1simple.prop1,nested1simple.prop2,nested1simple.attr1,nested1simple.nested2.prop3[1]".split(","), new Object[]{"SAMPLE_V1", true, "SAMPLE_ATTR1", 4});
        EPAssertionUtil.assertProps(received, "nested4array[0].id,nested4array[0].prop5[1],nested4array[1].id".split(","), new Object[]{"a", "SAMPLE_V8", "b"});

        // assert event and fragments alone
        EventBean wildcardStmtEvent = stmtWildcard.iterator().next();
        SupportEventTypeAssertionUtil.assertConsistency(wildcardStmtEvent);

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
        
        EventBean nested4arrayItem = (EventBean)wildcardStmtEvent.getFragment("nested4array[1]");
        assertEquals("b", nested4arrayItem.get("id"));
    }

    public void testExpressionSimpleDOMGetter() throws Exception
    {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = TestXMLSchemaEventTranspose.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        // eventTypeMeta.setXPathPropertyExpr(false); <== the default
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLSchemaType", eventTypeMeta);

        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyNestedStream select nested1 from TestXMLSchemaType#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, true),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select nested1.attr1 as attr1, nested1.prop1 as prop1, nested1.prop2 as prop2, nested1.nested2.prop3 as prop3, nested1.nested2.prop3[0] as prop3_0, nested1.nested2 as nested2 from MyNestedStream#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("prop1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("prop2", Boolean.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("attr1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("prop3", Integer[].class, Integer.class, false, false, true, false, false),
                new EventPropertyDescriptor("prop3_0", Integer.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested2", Node.class, null, false, false, false, false, true),
        }, stmtSelect.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelect.getEventType());

        EPStatement stmtSelectWildcard = epService.getEPAdministrator().createEPL("select * from MyNestedStream");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, true),
        }, stmtSelectWildcard.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelectWildcard.getEventType());

        EPStatement stmtInsertWildcard = epService.getEPAdministrator().createEPL("insert into MyNestedStreamTwo select nested1.* from TestXMLSchemaType#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("prop1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("prop2", Boolean.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("attr1", String.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested2", Node.class, null, false, false, false, false, true),
        }, stmtInsertWildcard.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsertWildcard.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");
        EventBean stmtInsertWildcardBean = stmtInsertWildcard.iterator().next();
        EPAssertionUtil.assertProps(stmtInsertWildcardBean, "prop1,prop2,attr1".split(","),
                new Object[]{"SAMPLE_V1", true, "SAMPLE_ATTR1"});

        SupportEventTypeAssertionUtil.assertConsistency(stmtSelect.iterator().next());
        EventBean stmtInsertBean = stmtInsert.iterator().next();
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsertWildcard.iterator().next());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.iterator().next());

        EventBean fragmentNested1 = (EventBean) stmtInsertBean.getFragment("nested1");
        assertEquals(5, fragmentNested1.get("nested2.prop3[2]"));
        assertEquals("TestXMLSchemaType.nested1", fragmentNested1.getEventType().getName());

        EventBean fragmentNested2 = (EventBean) stmtInsertWildcardBean.getFragment("nested2");
        assertEquals(4, fragmentNested2.get("prop3[1]"));
        assertEquals("TestXMLSchemaType.nested1.nested2", fragmentNested2.getEventType().getName());
    }

    // Note that XPath Node results when transposed must be queried by XPath that is also absolute.
    // For example: "nested1" => "/n0:simpleEvent/n0:nested1" results in a Node.
    // That result Node's "prop1" =>  "/n0:simpleEvent/n0:nested1/n0:prop1" and "/n0:nested1/n0:prop1" does NOT result in a value.
    // Therefore property transposal is disabled for Property-XPath expressions. 
    public void testExpressionSimpleXPathGetter() throws Exception
    {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = TestXMLSchemaEventTranspose.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        eventTypeMeta.setXPathPropertyExpr(true);       // <== note this
        eventTypeMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLSchemaType", eventTypeMeta);

        // note class not a fragment
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("insert into MyNestedStream select nested1 from TestXMLSchemaType#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("nested1", Node.class, null, false, false, false, false, false),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());
        
        EventType type = ((EPServiceProviderSPI)epService).getEventAdapterService().getExistsTypeByName("TestXMLSchemaType");
        SupportEventTypeAssertionUtil.assertConsistency(type);
        assertNull(type.getFragmentType("nested1"));
        assertNull(type.getFragmentType("nested1.nested2"));

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "ABC");
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.iterator().next());
    }

    public void testExpressionNodeArray() throws Exception
    {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        String schemaUri = TestXMLSchemaEventTranspose.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        epService.getEPAdministrator().getConfiguration().addEventType("TestXMLSchemaType", eventTypeMeta);

        // try array property insert
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("select nested3.nested4 as narr from TestXMLSchemaType#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("narr", Node[].class, Node.class, false, false, true, false, true),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");

        EventBean result = stmtInsert.iterator().next();
        SupportEventTypeAssertionUtil.assertConsistency(result);
        EventBean[] fragments = (EventBean[]) result.getFragment("narr");
        assertEquals(3, fragments.length);
        assertEquals("SAMPLE_V8", fragments[0].get("prop5[1]"));
        assertEquals("SAMPLE_V11", fragments[2].get("prop5[1]"));

        EventBean fragmentItem = (EventBean) result.getFragment("narr[2]");
        assertEquals("TestXMLSchemaType.nested3.nested4", fragmentItem.getEventType().getName());
        assertEquals("SAMPLE_V10", fragmentItem.get("prop5[0]"));

        // try array index property insert
        EPStatement stmtInsertItem = epService.getEPAdministrator().createEPL("select nested3.nested4[1] as narr from TestXMLSchemaType#lastevent");
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("narr", Node.class, null, false, false, false, false, true),
        }, stmtInsertItem.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsertItem.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");

        EventBean resultItem = stmtInsertItem.iterator().next();
        assertEquals("b", resultItem.get("narr.id"));
        SupportEventTypeAssertionUtil.assertConsistency(resultItem);
        EventBean fragmentsInsertItem = (EventBean) resultItem.getFragment("narr");
        SupportEventTypeAssertionUtil.assertConsistency(fragmentsInsertItem);
        assertEquals("b", fragmentsInsertItem.get("id"));
        assertEquals("SAMPLE_V9", fragmentsInsertItem.get("prop5[0]"));
    }

    public void testExpressionPrimitiveArray() throws Exception
    {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        eventTypeMeta.setSchemaResource(schemaURI);
        epService.getEPAdministrator().getConfiguration().addEventType("ABCType", eventTypeMeta);

        eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("//nested2");
        eventTypeMeta.setSchemaResource(schemaURI);
        eventTypeMeta.setEventSenderValidatesRoot(false);
        epService.getEPAdministrator().getConfiguration().addEventType("TestNested2", eventTypeMeta);

        // try array property in select
        EPStatement stmtInsert = epService.getEPAdministrator().createEPL("select * from TestNested2#lastevent");
        stmtInsert.addListener(listener);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("prop3", Integer[].class, null, false, false, true, false, false),
        }, stmtInsert.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmtInsert.getEventType());

        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");
        assertFalse(listener.isInvoked());

        EventSender sender = epService.getEPRuntime().getEventSender("TestNested2");
        sender.sendEvent(SupportXML.getDocument("<nested2><prop3>2</prop3><prop3></prop3><prop3>4</prop3></nested2>"));
        EventBean theEvent = stmtInsert.iterator().next();
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("prop3"), new Object[]{2, null, 4});
        SupportEventTypeAssertionUtil.assertConsistency(theEvent);

        // try array property nested
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select nested3.* from ABCType#lastevent");
        SupportXML.sendDefaultEvent(epService.getEPRuntime(), "test");
        EventBean stmtSelectResult = stmtSelect.iterator().next();
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelectResult);
        assertEquals(String[].class, stmtSelectResult.getEventType().getPropertyType("nested4[2].prop5"));
        assertEquals("SAMPLE_V8", stmtSelectResult.get("nested4[0].prop5[1]"));
        EPAssertionUtil.assertEqualsExactOrder((String[]) stmtSelectResult.get("nested4[2].prop5"), new Object[]{"SAMPLE_V10", "SAMPLE_V11"});

        EventBean fragmentNested4 = (EventBean) stmtSelectResult.getFragment("nested4[2]");
        EPAssertionUtil.assertEqualsExactOrder((String[]) fragmentNested4.get("prop5"), new Object[]{"SAMPLE_V10", "SAMPLE_V11"});
        assertEquals("SAMPLE_V11", fragmentNested4.get("prop5[1]"));
        SupportEventTypeAssertionUtil.assertConsistency(fragmentNested4);
    }

    /**
     * For testing XPath expressions.
     *
     */
    public void testXPathExpression() throws Exception
    {
        XPathNamespaceContext ctx = new XPathNamespaceContext();
        ctx.addPrefix("n0", "samples:schemas:simpleSchema");

        Node node = SupportXML.getDocument().getDocumentElement();

        XPath pathOne = XPathFactory.newInstance().newXPath();
        pathOne.setNamespaceContext(ctx);
        XPathExpression pathExprOne = pathOne.compile("/n0:simpleEvent/n0:nested1");
        Node result = (Node) pathExprOne.evaluate(node, XPathConstants.NODE);
        //System.out.println("Result:\n" + SchemaUtil.serialize(result));

        XPath pathTwo = XPathFactory.newInstance().newXPath();
        pathTwo.setNamespaceContext(ctx);
        XPathExpression pathExprTwo = pathTwo.compile("/n0:simpleEvent/n0:nested1/n0:prop1");
        String resultTwo = (String) pathExprTwo.evaluate(result, XPathConstants.STRING);
        //System.out.println("Result 2: <" + resultTwo + ">");

        XPath pathThree = XPathFactory.newInstance().newXPath();
        pathThree.setNamespaceContext(ctx);
        XPathExpression pathExprThree = pathThree.compile("/n0:simpleEvent/n0:nested3");
        String resultThress = (String) pathExprThree.evaluate(result, XPathConstants.STRING);
        //System.out.println("Result 3: <" + resultThress + ">");
    }
}
