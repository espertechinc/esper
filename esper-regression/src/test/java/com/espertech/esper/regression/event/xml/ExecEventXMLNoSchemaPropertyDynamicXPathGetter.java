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
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static com.espertech.esper.regression.event.xml.ExecEventXMLNoSchemaPropertyDynamicDOMGetter.NOSCHEMA_XML;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ExecEventXMLNoSchemaPropertyDynamicXPathGetter implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("simpleEvent");
        desc.setXPathPropertyExpr(true);
        configuration.addEventType("MyEvent", desc);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmtText = "select type?,dyn[1]?,nested.nes2?,map('a')?,other? from MyEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("type?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("dyn[1]?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("nested.nes2?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("map('a')?", Node.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("other?", Node.class, null, false, false, false, false, false),
        }, stmt.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmt.getEventType());

        Document root = SupportXML.sendEvent(epService.getEPRuntime(), NOSCHEMA_XML);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertSame(root.getDocumentElement().getChildNodes().item(1), theEvent.get("type?"));
        assertSame(root.getDocumentElement().getChildNodes().item(5), theEvent.get("dyn[1]?"));
        assertSame(root.getDocumentElement().getChildNodes().item(7).getChildNodes().item(1), theEvent.get("nested.nes2?"));
        assertSame(root.getDocumentElement().getChildNodes().item(9), theEvent.get("map('a')?"));
        assertNull(theEvent.get("other?"));
        SupportEventTypeAssertionUtil.assertConsistency(theEvent);
    }

}
