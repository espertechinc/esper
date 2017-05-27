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

import com.espertech.esper.client.ConfigurationEventTypeXMLDOM;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathConstants;

public class ExecEventXMLNoSchemaXPathArray implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        String xml = "<Event IsTriggering=\"True\">\n" +
                "<Field Name=\"A\" Value=\"987654321\"/>\n" +
                "<Field Name=\"B\" Value=\"2196958725202\"/>\n" +
                "<Field Name=\"C\" Value=\"1232363702\"/>\n" +
                "<Participants>\n" +
                "<Participant>\n" +
                "<Field Name=\"A\" Value=\"9876543210\"/>\n" +
                "<Field Name=\"B\" Value=\"966607340\"/>\n" +
                "<Field Name=\"D\" Value=\"353263010930650\"/>\n" +
                "</Participant>\n" +
                "</Participants>\n" +
                "</Event>";

        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("Event");
        desc.addXPathProperty("A", "//Field[@Name='A']/@Value", XPathConstants.NODESET, "String[]");
        epService.getEPAdministrator().getConfiguration().addEventType("Event", desc);

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from Event");
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        Document doc = SupportXML.getDocument(xml);
        epService.getEPRuntime().sendEvent(doc);

        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(theEvent, "A".split(","), new Object[]{new Object[]{"987654321", "9876543210"}});
    }
}
