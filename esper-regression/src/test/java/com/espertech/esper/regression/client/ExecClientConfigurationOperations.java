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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.client.MyAutoNamedEventType;
import com.espertech.esper.supportregression.client.SupportAmbigousEventType;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecClientConfigurationOperations implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventTypeAutoName(this.getClass().getPackage().getName());
        epService.getEPAdministrator().getConfiguration().addEventTypeAutoName(SupportBean.class.getPackage().getName());

        runAssertionAutoNamePackage(epService);
        runAssertionAutoNamePackageAmbigous(epService);
        runAssertionAddDOMType(epService);
        runAssertionAddMapByClass(epService);
        runAssertionAddMapProperties(epService);
        runAssertionAddAliasClassName(epService);
        runAssertionAddNameClass(epService);
    }

    private void runAssertionAutoNamePackage(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventTypeAutoName(MyAutoNamedEventType.class.getPackage().getName());

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from " + MyAutoNamedEventType.class.getSimpleName());
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        MyAutoNamedEventType eventOne = new MyAutoNamedEventType(10);
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());

        stmt.destroy();
    }

    private void runAssertionAutoNamePackageAmbigous(EPServiceProvider epService) {
        SupportMessageAssertUtil.tryInvalid(epService, "select * from " + SupportAmbigousEventType.class.getSimpleName(),
                "Failed to resolve event type: Failed to resolve name 'SupportAmbigousEventType', the class was ambigously found both in package 'com.espertech.esper.supportregression.bean' and in package 'com.espertech.esper.supportregression.client'");

        SupportMessageAssertUtil.tryInvalid(epService, "select * from XXXX",
                "Failed to resolve event type: Event type or class named 'XXXX' was not found");
    }

    private void runAssertionAddDOMType(EPServiceProvider epService) throws Exception {
        tryInvalid(epService, "AddedDOMOne");

        // First statement with new name
        ConfigurationEventTypeXMLDOM domConfig = new ConfigurationEventTypeXMLDOM();
        domConfig.setRootElementName("RootAddedDOMOne");
        epService.getEPAdministrator().getConfiguration().addEventType("AddedDOMOne", domConfig);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedDOMOne");
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        Document eventOne = makeDOMEvent("RootAddedDOMOne");
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne.getDocumentElement(), testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid(epService, "AddedMapNameSecond");

        // Second statement using a new name to the same type, should both receive
        domConfig = new ConfigurationEventTypeXMLDOM();
        domConfig.setRootElementName("RootAddedDOMOne");
        epService.getEPAdministrator().getConfiguration().addEventType("AddedDOMSecond", domConfig);

        epService.getEPAdministrator().getConfiguration().addEventType("AddedMapNameSecond", domConfig);
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
        epService.getEPAdministrator().getConfiguration().addEventType("AddedDOMSecond", domConfig);

        // Add the same name and a different type
        try {
            domConfig = new ConfigurationEventTypeXMLDOM();
            domConfig.setRootElementName("RootAddedDOMXXX");
            epService.getEPAdministrator().getConfiguration().addEventType("AddedDOMSecond", domConfig);
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedMapNameSecond", true);
    }

    private void runAssertionAddMapByClass(EPServiceProvider epService) {
        tryInvalid(epService, "AddedMapOne");

        // First statement with new name
        Map<String, Object> mapProps = new HashMap<>();
        mapProps.put("prop1", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("AddedMapOne", mapProps);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedMapOne");
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        Map<String, Object> eventOne = new HashMap<>();
        eventOne.put("prop1", 1);
        epService.getEPRuntime().sendEvent(eventOne, "AddedMapOne");
        assertEquals(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid(epService, "AddedMapNameSecond");

        // Second statement using a new name to the same type, should only one receive
        epService.getEPAdministrator().getConfiguration().addEventType("AddedMapNameSecond", mapProps);
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmt = epService.getEPAdministrator().createEPL("select * from AddedMapNameSecond");
        stmt.addListener(testListenerTwo);

        Map<String, Object> eventTwo = new HashMap<>();
        eventTwo.put("prop1", 1);
        epService.getEPRuntime().sendEvent(eventTwo, "AddedMapNameSecond");
        assertFalse(testListener.isInvoked());
        assertEquals(eventTwo, testListenerTwo.assertOneGetNewAndReset().getUnderlying());

        // Add the same name and type again
        mapProps.clear();
        mapProps.put("prop1", int.class);
        epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", mapProps);

        // Add the same name and a different type
        try {
            mapProps.put("XX", int.class);
            epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", mapProps);
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedMapOne", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedMapTwo", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedMapNameSecond", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedNameSecond", true);
    }

    private void runAssertionAddMapProperties(EPServiceProvider epService) {
        tryInvalid(epService, "AddedMapOne");

        // First statement with new name
        Map<String, Object> mapProps = new HashMap<>();
        mapProps.put("prop1", int.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("AddedMapOne", mapProps);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedMapOne");
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        Map<String, Object> eventOne = new HashMap<>();
        eventOne.put("prop1", 1);
        epService.getEPRuntime().sendEvent(eventOne, "AddedMapOne");
        assertEquals(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid(epService, "AddedMapNameSecond");

        // Second statement using a new alias to the same type, should only one receive
        epService.getEPAdministrator().getConfiguration().addEventType("AddedMapNameSecond", mapProps);
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmt = epService.getEPAdministrator().createEPL("select * from AddedMapNameSecond");
        stmt.addListener(testListenerTwo);

        Map<String, Object> eventTwo = new HashMap<>();
        eventTwo.put("prop1", 1);
        epService.getEPRuntime().sendEvent(eventTwo, "AddedMapNameSecond");
        assertFalse(testListener.isInvoked());
        assertEquals(eventTwo, testListenerTwo.assertOneGetNewAndReset().getUnderlying());

        // Add the same name and type again
        mapProps.clear();
        mapProps.put("prop1", int.class.getName());
        epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", mapProps);

        // Add the same name and a different type
        try {
            mapProps.put("XX", int.class.getName());
            epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", mapProps);
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedMapOne", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedMapTwo", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedMapNameSecond", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedNameSecond", true);
    }

    private void runAssertionAddAliasClassName(EPServiceProvider epService) {
        tryInvalid(epService, "AddedName");

        // First statement with new name
        epService.getEPAdministrator().getConfiguration().addEventType("AddedName", SupportBean.class.getName());
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedName");
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        SupportBean eventOne = new SupportBean("a", 1);
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid(epService, "AddedNameSecond");

        // Second statement using a new alias to the same type, should both receive
        epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", SupportBean.class.getName());
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmt = epService.getEPAdministrator().createEPL("select * from AddedNameSecond");
        stmt.addListener(testListenerTwo);

        SupportBean eventTwo = new SupportBean("b", 2);
        epService.getEPRuntime().sendEvent(eventTwo);
        assertSame(eventTwo, testListener.assertOneGetNewAndReset().getUnderlying());
        assertSame(eventTwo, testListenerTwo.assertOneGetNewAndReset().getUnderlying());

        // Add the same name and type again
        epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", SupportBean.class.getName());

        // Add the same name and a different type
        try {
            epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", SupportBean_A.class.getName());
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedName", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedNameSecond", true);
    }

    private void runAssertionAddNameClass(EPServiceProvider epService) {
        tryInvalid(epService, "AddedName");

        // First statement with new name
        epService.getEPAdministrator().getConfiguration().addEventType("AddedName", SupportBean.class);
        assertTrue(epService.getEPAdministrator().getConfiguration().isEventTypeExists("AddedName"));
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from AddedName");
        SupportUpdateListener testListener = new SupportUpdateListener();
        stmt.addListener(testListener);

        SupportBean eventOne = new SupportBean("a", 1);
        epService.getEPRuntime().sendEvent(eventOne);
        assertSame(eventOne, testListener.assertOneGetNewAndReset().getUnderlying());

        tryInvalid(epService, "AddedNameSecond");

        // Second statement using a new alias to the same type, should both receive
        epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", SupportBean.class);
        SupportUpdateListener testListenerTwo = new SupportUpdateListener();
        stmt = epService.getEPAdministrator().createEPL("select * from AddedNameSecond");
        stmt.addListener(testListenerTwo);

        SupportBean eventTwo = new SupportBean("b", 2);
        epService.getEPRuntime().sendEvent(eventTwo);
        assertSame(eventTwo, testListener.assertOneGetNewAndReset().getUnderlying());
        assertSame(eventTwo, testListenerTwo.assertOneGetNewAndReset().getUnderlying());

        // Add the same name and type again
        epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", SupportBean.class);

        // Add the same name and a different type
        try {
            epService.getEPAdministrator().getConfiguration().addEventType("AddedNameSecond", SupportBean_A.class);
            fail();
        } catch (ConfigurationException ex) {
            // expected
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("AddedNameSecond", true);
    }

    private void tryInvalid(EPServiceProvider epService, String name) {
        try {
            epService.getEPAdministrator().createEPL("select * from " + name);
            fail();
        } catch (EPStatementException ex) {
            // expected
        }
    }

    private Document makeDOMEvent(String rootElementName) throws Exception {
        String xmlTemplate =
                "<VAL1>\n" +
                        "  <someelement/>\n" +
                        "</VAL1>";

        String xml = xmlTemplate.replaceAll("VAL1", rootElementName);

        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        return builderFactory.newDocumentBuilder().parse(source);
    }
}
