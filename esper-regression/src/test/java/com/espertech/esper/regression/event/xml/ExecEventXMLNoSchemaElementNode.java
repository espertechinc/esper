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
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class ExecEventXMLNoSchemaElementNode implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        // test for Esper-129
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.addXPathProperty("event.type", "//event/@type", XPathConstants.STRING);
        desc.addXPathProperty("event.uid", "//event/@uid", XPathConstants.STRING);
        desc.setRootElementName("batch-event");
        configuration.addEventType("MyEvent", desc);
    }

    public void run(EPServiceProvider epService) throws Exception {

        String stmt = "select event.type as type, event.uid as uid from MyEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        SupportUpdateListener updateListener = new SupportUpdateListener();
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
    }
}
