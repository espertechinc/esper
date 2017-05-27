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

import javax.xml.xpath.XPathConstants;

import static com.espertech.esper.regression.event.xml.ExecEventXMLNoSchemaNestedXMLDOMGetter.sendXMLEvent;
import static org.junit.Assert.assertEquals;

public class ExecEventXMLNoSchemaEventXML implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.addXPathProperty("event.type", "/event/@type", XPathConstants.STRING);
        desc.addXPathProperty("event.uid", "/event/@uid", XPathConstants.STRING);
        desc.setRootElementName("event");
        configuration.addEventType("MyEvent", desc);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmt = "select event.type as type, event.uid as uid from MyEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        joinView.addListener(updateListener);

        sendXMLEvent(epService, "<event type=\"a-f-G\" uid=\"terminal.55\" time=\"2007-04-19T13:05:20.22Z\" version=\"2.0\"></event>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("a-f-G", theEvent.get("type"));
        assertEquals("terminal.55", theEvent.get("uid"));
    }
}
