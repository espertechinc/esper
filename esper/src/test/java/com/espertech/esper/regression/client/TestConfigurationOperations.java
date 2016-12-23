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

package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.io.StringReader;

import org.xml.sax.InputSource;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;

public class TestConfigurationOperations extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener testListener;
    private ConfigurationOperations configOps;

    public void setUp()
    {
        testListener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        configOps = epService.getEPAdministrator().getConfiguration();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testAutoNamePackage()
    {
        configOps.addEventTypeAutoName(this.getClass().getPackage().getName());

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from " + MyAutoNamedEventType.class.getSimpleName());
        stmt.addListener(testListener);

        MyAutoNamedEventType eventOne = new MyAutoNamedEventType(10);
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());
    }

    public void testAutoNamePackageAmbigous()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventTypeAutoName(this.getClass().getPackage().getName());
        configOps.addEventTypeAutoName(this.getClass().getPackage().getName());
        configOps.addEventTypeAutoName(SupportBean.class.getPackage().getName());

        SupportMessageAssertUtil.tryInvalid(epService, "select * from " + SupportAmbigousEventType.class.getSimpleName(),
                "Failed to resolve event type: Failed to resolve name 'SupportAmbigousEventType', the class was ambigously found both in package 'com.espertech.esper.regression.client' and in package 'com.espertech.esper.supportregression.bean'");

        SupportMessageAssertUtil.tryInvalid(epService, "select * from XXXX",
                "Failed to resolve event type: Event type or class named 'XXXX' was not found");
    }

    public void testAddDOMType() throws Exception
    {
        tryInvalid("AddedDOMOne");

        // First statement with new name
        ConfigurationEventTypeXMLDOM domConfig = new ConfigurationEventTypeXMLDOM();
        domConfig.setRootElementName("RootAddedDOMOne");
        configOps.addEventType("AddedDOMOne", domConfig);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedDOMOne");
        stmt.addListener(testListener);

        Document eventOne = makeDOMEvent("RootAddedDOMOne");
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne.getDocumentElement(), testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid("AddedMapNameSecond");

        // Second statement using a new name to the same type, should both receive
        domConfig = new ConfigurationEventTypeXMLDOM();
        domConfig.setRootElementName("RootAddedDOMOne");
        configOps.addEventType("AddedDOMSecond", domConfig);

        configOps.addEventType("AddedMapNameSecond", domConfig);
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmt = epService.getEPAdministrator().createEPL("select * from AddedMapNameSecond");
        stmt.addListener(testListenerTwo);

        Document eventTwo = makeDOMEvent("RootAddedDOMOne");
        epService.getEPRuntime().sendEvent(eventTwo);
        assertTrue(testListener.isInvoked());
        assertEquals(eventTwo.getDocumentElement(), testListenerTwo.assertOneGetNewAndReset().getUnderlying());

        // Add the same name and type again
        domConfig = new ConfigurationEventTypeXMLDOM();
        domConfig.setRootElementName("RootAddedDOMOne");
        configOps.addEventType("AddedDOMSecond", domConfig);

        // Add the same name and a different type
        try
        {
            domConfig = new ConfigurationEventTypeXMLDOM();
            domConfig.setRootElementName("RootAddedDOMXXX");
            configOps.addEventType("AddedDOMSecond", domConfig);
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    public void testAddMapByClass()
    {
        tryInvalid("AddedMapOne");

        // First statement with new name
        Map<String, Object> mapProps = new HashMap<String, Object>();
        mapProps.put("prop1", int.class);
        configOps.addEventType("AddedMapOne", mapProps);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedMapOne");
        stmt.addListener(testListener);

        Map<String, Object> eventOne = new HashMap<String, Object>();
        eventOne.put("prop1", 1);
        epService.getEPRuntime().sendEvent(eventOne, "AddedMapOne");
        assertEquals(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid("AddedMapNameSecond");

        // Second statement using a new name to the same type, should only one receive
        configOps.addEventType("AddedMapNameSecond", mapProps);
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmt = epService.getEPAdministrator().createEPL("select * from AddedMapNameSecond");
        stmt.addListener(testListenerTwo);

        Map<String, Object> eventTwo = new HashMap<String, Object>();
        eventTwo.put("prop1", 1);
        epService.getEPRuntime().sendEvent(eventTwo, "AddedMapNameSecond");
        assertFalse(testListener.isInvoked());
        assertEquals(eventTwo, testListenerTwo.assertOneGetNewAndReset().getUnderlying());

        // Add the same name and type again
        mapProps.clear();
        mapProps.put("prop1", int.class);
        configOps.addEventType("AddedNameSecond", mapProps);

        // Add the same name and a different type
        try
        {
            mapProps.put("XX", int.class);
            configOps.addEventType("AddedNameSecond", mapProps);
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    public void testAddMapProperties()
    {
        tryInvalid("AddedMapOne");

        // First statement with new name
        Map<String, Object> mapProps = new HashMap<String,Object>();
        mapProps.put("prop1", int.class.getName());
        configOps.addEventType("AddedMapOne", mapProps);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedMapOne");
        stmt.addListener(testListener);

        Map<String, Object> eventOne = new HashMap<String, Object>();
        eventOne.put("prop1", 1);
        epService.getEPRuntime().sendEvent(eventOne, "AddedMapOne");
        assertEquals(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid("AddedMapNameSecond");

        // Second statement using a new alias to the same type, should only one receive
        configOps.addEventType("AddedMapNameSecond", mapProps);
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmt = epService.getEPAdministrator().createEPL("select * from AddedMapNameSecond");
        stmt.addListener(testListenerTwo);

        Map<String, Object> eventTwo = new HashMap<String, Object>();
        eventTwo.put("prop1", 1);
        epService.getEPRuntime().sendEvent(eventTwo, "AddedMapNameSecond");
        assertFalse(testListener.isInvoked());
        assertEquals(eventTwo, testListenerTwo.assertOneGetNewAndReset().getUnderlying());

        // Add the same name and type again
        mapProps.clear();
        mapProps.put("prop1", int.class.getName());
        configOps.addEventType("AddedNameSecond", mapProps);

        // Add the same name and a different type
        try
        {
            mapProps.put("XX", int.class.getName());
            configOps.addEventType("AddedNameSecond", mapProps);
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    public void testAddAliasClassName()
    {
        tryInvalid("AddedName");

        // First statement with new name
        configOps.addEventType("AddedName", SupportBean.class.getName());
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedName");
        stmt.addListener(testListener);

        SupportBean eventOne = new SupportBean("a", 1);
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid("AddedNameSecond");

        // Second statement using a new alias to the same type, should both receive
        configOps.addEventType("AddedNameSecond", SupportBean.class.getName());
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmt = epService.getEPAdministrator().createEPL("select * from AddedNameSecond");
        stmt.addListener(testListenerTwo);

        SupportBean eventTwo = new SupportBean("b", 2);
        epService.getEPRuntime().sendEvent(eventTwo);
        assertSame(eventTwo, testListener.assertOneGetNewAndReset().getUnderlying());
        assertSame(eventTwo, testListenerTwo.assertOneGetNewAndReset().getUnderlying());

        // Add the same name and type again
        configOps.addEventType("AddedNameSecond", SupportBean.class.getName());

        // Add the same name and a different type
        try
        {
            configOps.addEventType("AddedNameSecond", SupportBean_A.class.getName());
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    public void testAddNameClass()
    {
        tryInvalid("AddedName");

        // First statement with new name
        configOps.addEventType("AddedName", SupportBean.class);
        assertTrue(configOps.isEventTypeExists("AddedName"));
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedName");
        stmt.addListener(testListener);

        SupportBean eventOne = new SupportBean("a", 1);
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid("AddedNameSecond");

        // Second statement using a new alias to the same type, should both receive
        configOps.addEventType("AddedNameSecond", SupportBean.class);
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmt = epService.getEPAdministrator().createEPL("select * from AddedNameSecond");
        stmt.addListener(testListenerTwo);

        SupportBean eventTwo = new SupportBean("b", 2);
        epService.getEPRuntime().sendEvent(eventTwo);
        assertSame(eventTwo, testListener.assertOneGetNewAndReset().getUnderlying());
        assertSame(eventTwo, testListenerTwo.assertOneGetNewAndReset().getUnderlying());

        // Add the same name and type again
        configOps.addEventType("AddedNameSecond", SupportBean.class);

        // Add the same name and a different type
        try
        {
            configOps.addEventType("AddedNameSecond", SupportBean_A.class);
            fail();
        }
        catch (ConfigurationException ex)
        {
            // expected
        }
    }

    private void tryInvalid(String name)
    {
        try
        {
            epService.getEPAdministrator().createEPL("select * from " + name);
            fail();
        }
        catch (EPStatementException ex)
        {
            // expected
        }
    }

    private Document makeDOMEvent(String rootElementName) throws Exception
    {
        String XML =
            "<VAL1>\n" +
            "  <someelement/>\n" +
            "</VAL1>";

        String xml = XML.replaceAll("VAL1", rootElementName);

        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);
        return simpleDoc;
    }
}
