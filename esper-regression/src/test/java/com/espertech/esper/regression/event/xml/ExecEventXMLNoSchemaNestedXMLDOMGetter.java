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
package com.espertech.esper.regression.event.xml;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class ExecEventXMLNoSchemaNestedXMLDOMGetter implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("a");
        xmlDOMEventTypeDesc.addXPathProperty("element1", "/a/b/c", XPathConstants.STRING);
        configuration.addEventType("AEvent", xmlDOMEventTypeDesc);
    }

    public void run(EPServiceProvider epService) throws Exception {

        String stmt = "select b.c as type, element1, result1 from AEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        joinView.addListener(updateListener);

        sendXMLEvent(epService, "<a><b><c></c></b></a>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("", theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent(epService, "<a><b></b></a>");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals(null, theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent(epService, "<a><b><c>text</c></b></a>");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("text", theEvent.get("type"));
        assertEquals("text", theEvent.get("element1"));
    }

    protected static void sendXMLEvent(EPServiceProvider epService, String xml) throws Exception {
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);

        epService.getEPRuntime().sendEvent(simpleDoc);
    }
}





