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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;

import static com.espertech.esper.supportregression.event.SupportXML.getDocument;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecEventXMLSchemaEventSender implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM typeMeta = new ConfigurationEventTypeXMLDOM();
        typeMeta.setRootElementName("a");
        typeMeta.addXPathProperty("element1", "/a/b/c", XPathConstants.STRING);
        configuration.addEventType("AEvent", typeMeta);
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportUpdateListener listener = new SupportUpdateListener();
        String stmtText = "select b.c as type, element1 from AEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        Document doc = getDocument("<a><b><c>text</c></b></a>");
        EventSender sender = epService.getEPRuntime().getEventSender("AEvent");
        sender.sendEvent(doc);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("text", theEvent.get("type"));
        assertEquals("text", theEvent.get("element1"));

        // send wrong event
        try {
            sender.sendEvent(getDocument("<xxxx><b><c>text</c></b></xxxx>"));
            fail();
        } catch (EPException ex) {
            assertEquals("Unexpected root element name 'xxxx' encountered, expected a root element name of 'a'", ex.getMessage());
        }

        try {
            sender.sendEvent(new SupportBean());
            fail();
        } catch (EPException ex) {
            assertEquals("Unexpected event object type '" + SupportBean.class.getName() + "' encountered, please supply a org.w3c.dom.Document or Element node", ex.getMessage());
        }

        // test adding a second type for the same root element
        ConfigurationEventTypeXMLDOM typeMeta = new ConfigurationEventTypeXMLDOM();
        typeMeta.setRootElementName("a");
        typeMeta.addXPathProperty("element2", "//c", XPathConstants.STRING);
        typeMeta.setEventSenderValidatesRoot(false);
        epService.getEPAdministrator().getConfiguration().addEventType("BEvent", typeMeta);

        stmtText = "select element2 from BEvent#lastevent";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtText);

        // test sender that doesn't care about the root element
        EventSender senderTwo = epService.getEPRuntime().getEventSender("BEvent");
        senderTwo.sendEvent(getDocument("<xxxx><b><c>text</c></b></xxxx>"));    // allowed, not checking

        theEvent = stmtTwo.iterator().next();
        assertEquals("text", theEvent.get("element2"));
    }
}
