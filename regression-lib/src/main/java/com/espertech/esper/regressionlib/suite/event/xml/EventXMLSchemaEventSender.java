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
package com.espertech.esper.regressionlib.suite.event.xml;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import org.w3c.dom.Document;

import static com.espertech.esper.regressionlib.support.util.SupportXML.getDocument;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EventXMLSchemaEventSender implements RegressionExecution {
    public void run(RegressionEnvironment env) {

        String stmtText = "@name('s0') select b.c as type, element1 from EventABC";
        env.compileDeploy(stmtText).addListener("s0");

        Document doc = getDocument("<a><b><c>text</c></b></a>");
        EventSender sender = env.eventService().getEventSender("EventABC");
        sender.sendEvent(doc);

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
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
        env.undeployModuleContaining("s0");

        // test adding a second type for the same root element
        stmtText = "@name('s0') select element2 from BEvent#lastevent";
        env.compileDeploy(stmtText).addListener("s0");

        // test sender that doesn't care about the root element
        EventSender senderTwo = env.eventService().getEventSender("BEvent");
        senderTwo.sendEvent(getDocument("<xxxx><b><c>text</c></b></xxxx>"));    // allowed, not checking

        theEvent = env.statement("s0").iterator().next();
        assertEquals("text", theEvent.get("element2"));

        env.undeployAll();
    }
}
