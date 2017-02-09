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
package com.espertech.esper.event;

import com.espertech.esper.client.ConfigurationEventTypeXMLDOM;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.bean.SupportBean_A;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.bean.SupportSelfReferenceEvent;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class TestEventAdapterServiceImpl extends TestCase {
    private EventAdapterServiceImpl adapterService;

    public void setUp() {
        adapterService = new EventAdapterServiceImpl(new EventTypeIdGeneratorImpl(), 5, null, SupportEngineImportServiceFactory.make());
    }

    public void testSelfRefEvent() {
        EventBean originalBean = adapterService.adapterForBean(new SupportSelfReferenceEvent());
        assertEquals(null, originalBean.get("selfRef.selfRef.selfRef.value"));
    }

    public void testCreateMapType() {
        Map<String, Object> testTypesMap;
        testTypesMap = new HashMap<String, Object>();
        testTypesMap.put("key1", String.class);
        EventType eventType = adapterService.createAnonymousMapType("test", testTypesMap, true);

        assertEquals(Map.class, eventType.getUnderlyingType());
        assertEquals(1, eventType.getPropertyNames().length);
        assertEquals("key1", eventType.getPropertyNames()[0]);
    }

    public void testGetType() {
        adapterService.addBeanType("NAME", TestEventAdapterServiceImpl.class.getName(), false, false, false, false);

        EventType type = adapterService.getExistsTypeByName("NAME");
        assertEquals(TestEventAdapterServiceImpl.class, type.getUnderlyingType());

        EventType typeTwo = adapterService.getExistsTypeByName(TestEventAdapterServiceImpl.class.getName());
        assertSame(typeTwo, typeTwo);

        assertNull(adapterService.getExistsTypeByName("xx"));
    }

    public void testAddInvalid() {
        try {
            adapterService.addBeanType("x", "xx", false, false, false, false);
            fail();
        } catch (EventAdapterException ex) {
            // Expected
        }
    }

    public void testAddMapType() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("a", Long.class);
        props.put("b", String.class);

        // check result type
        EventType typeOne = adapterService.addNestableMapType("latencyEvent", props, null, true, true, true, false, false);
        assertEquals(Long.class, typeOne.getPropertyType("a"));
        assertEquals(String.class, typeOne.getPropertyType("b"));
        assertEquals(2, typeOne.getPropertyNames().length);

        assertSame(typeOne, adapterService.getExistsTypeByName("latencyEvent"));

        // add the same type with the same name, should succeed and return the same reference
        EventType typeTwo = adapterService.addNestableMapType("latencyEvent", props, null, true, true, true, false, false);
        assertSame(typeOne, typeTwo);

        // add the same name with a different type, should fail
        props.put("b", boolean.class);
        try {
            adapterService.addNestableMapType("latencyEvent", props, null, true, true, true, false, false);
            fail();
        } catch (EventAdapterException ex) {
            // expected
        }
    }

    public void testAddWrapperType() {
        EventType beanEventType = adapterService.addBeanType("mybean", SupportMarketDataBean.class, true, true, true);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("a", Long.class);
        props.put("b", String.class);

        // check result type
        EventType typeOne = adapterService.addWrapperType("latencyEvent", beanEventType, props, false, true);
        assertEquals(Long.class, typeOne.getPropertyType("a"));
        assertEquals(String.class, typeOne.getPropertyType("b"));
        assertEquals(7, typeOne.getPropertyNames().length);

        assertSame(typeOne, adapterService.getExistsTypeByName("latencyEvent"));

        // add the same name with a different type, should fail
        props.put("b", boolean.class);
        try {
            EventType beanTwoEventType = adapterService.addBeanType("mybean", SupportBean.class, true, true, true);
            adapterService.addWrapperType("latencyEvent", beanTwoEventType, props, false, false);
            fail();
        } catch (EventAdapterException ex) {
            // expected
        }
    }

    public void testAddClassName() {
        EventType typeOne = adapterService.addBeanType("latencyEvent", SupportBean.class.getName(), true, false, false, false);
        assertEquals(SupportBean.class, typeOne.getUnderlyingType());

        assertSame(typeOne, adapterService.getExistsTypeByName("latencyEvent"));

        EventType typeTwo = adapterService.addBeanType("latencyEvent", SupportBean.class.getName(), false, false, false, false);
        assertSame(typeOne, typeTwo);

        try {
            adapterService.addBeanType("latencyEvent", SupportBean_A.class.getName(), true, false, false, false);
            fail();
        } catch (EventAdapterException ex) {
            assertEquals("Event type named 'latencyEvent' has already been declared with differing underlying type information: Class " + SupportBean.class.getName() + " versus " + SupportBean_A.class.getName(), ex.getMessage());
        }
    }

    public void testAddClass() {
        EventType typeOne = adapterService.addBeanType("latencyEvent", SupportBean.class, false, false, false);
        assertEquals(SupportBean.class, typeOne.getUnderlyingType());

        assertSame(typeOne, adapterService.getExistsTypeByName("latencyEvent"));

        EventType typeTwo = adapterService.addBeanType("latencyEvent", SupportBean.class, false, false, false);
        assertSame(typeOne, typeTwo);

        try {
            adapterService.addBeanType("latencyEvent", SupportBean_A.class.getName(), false, false, false, false);
            fail();
        } catch (EventAdapterException ex) {
            assertEquals("Event type named 'latencyEvent' has already been declared with differing underlying type information: Class " + SupportBean.class.getName() + " versus " + SupportBean_A.class.getName(), ex.getMessage());
        }
    }

    public void testWrap() {
        SupportBean bean = new SupportBean();
        EventBean theEvent = adapterService.adapterForBean(bean);
        assertSame(theEvent.getUnderlying(), bean);
    }

    public void testAddXMLDOMType() throws Exception {
        adapterService.addXMLDOMType("XMLDOMTypeOne", getXMLDOMConfig(), null, true);
        EventType eventType = adapterService.getExistsTypeByName("XMLDOMTypeOne");
        assertEquals(Node.class, eventType.getUnderlyingType());

        assertSame(eventType, adapterService.getExistsTypeByName("XMLDOMTypeOne"));

        try {
            adapterService.addXMLDOMType("a", new ConfigurationEventTypeXMLDOM(), null, true);
            fail();
        } catch (EventAdapterException ex) {
            // expected
        }
    }

    public void testAdapterForDOM() throws Exception {
        adapterService.addXMLDOMType("XMLDOMTypeOne", getXMLDOMConfig(), null, true);

        String xml =
                "<simpleEvent>\n" +
                        "  <nested1>value</nested1>\n" +
                        "</simpleEvent>";

        InputSource source = new InputSource(new StringReader(xml));
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);

        EventBean bean = adapterService.adapterForDOM(simpleDoc);
        assertEquals("value", bean.get("nested1"));
    }

    private static ConfigurationEventTypeXMLDOM getXMLDOMConfig() {
        ConfigurationEventTypeXMLDOM config = new ConfigurationEventTypeXMLDOM();
        config.setRootElementName("simpleEvent");
        config.addXPathProperty("nested1", "/simpleEvent/nested1", XPathConstants.STRING);
        return config;
    }
}
