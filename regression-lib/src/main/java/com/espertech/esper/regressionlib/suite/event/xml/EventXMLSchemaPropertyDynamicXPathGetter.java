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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static com.espertech.esper.regressionlib.suite.event.xml.EventXMLSchemaPropertyDynamicDOMGetter.SCHEMA_XML;
import static org.junit.Assert.assertSame;

public class EventXMLSchemaPropertyDynamicXPathGetter implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String stmtText = "@name('s0') select type?,dyn[1]?,nested.nes2?,map('a')? from MyEventWithXPath";
        env.compileDeploy(stmtText).addListener("s0");

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("type?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("dyn[1]?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested.nes2?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("map('a')?", Node.class, null, false, false, false, false, false),
        }, env.statement("s0").getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("s0").getEventType());

        EventSender sender = env.eventService().getEventSender("MyEventWithXPath");
        Document root = SupportXML.sendEvent(sender, SCHEMA_XML);

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertSame(root.getDocumentElement().getChildNodes().item(0), theEvent.get("type?"));
        assertSame(root.getDocumentElement().getChildNodes().item(4), theEvent.get("dyn[1]?"));
        assertSame(root.getDocumentElement().getChildNodes().item(6).getChildNodes().item(1), theEvent.get("nested.nes2?"));
        assertSame(root.getDocumentElement().getChildNodes().item(8), theEvent.get("map('a')?"));
        SupportEventTypeAssertionUtil.assertConsistency(theEvent);

        env.undeployAll();
    }
}
