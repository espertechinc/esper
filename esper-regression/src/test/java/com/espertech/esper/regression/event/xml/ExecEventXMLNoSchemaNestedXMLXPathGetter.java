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

public class ExecEventXMLNoSchemaNestedXMLXPathGetter implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("a");
        xmlDOMEventTypeDesc.setXPathPropertyExpr(true);
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
        assertEquals("", theEvent.get("type"));
        assertEquals("", theEvent.get("element1"));

        sendXMLEvent(epService, "<a><b><c>text</c></b></a>");
        theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("text", theEvent.get("type"));
        assertEquals("text", theEvent.get("element1"));
    }
}
