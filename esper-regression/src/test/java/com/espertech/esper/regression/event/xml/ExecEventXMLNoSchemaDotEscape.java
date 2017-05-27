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

import static com.espertech.esper.regression.event.xml.ExecEventXMLNoSchemaNestedXMLDOMGetter.sendXMLEvent;
import static org.junit.Assert.assertEquals;

public class ExecEventXMLNoSchemaDotEscape implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myroot");
        configuration.addEventType("AEvent", xmlDOMEventTypeDesc);
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportUpdateListener updateListener = new SupportUpdateListener();

        String stmt = "select a\\.b.c\\.d as val from AEvent";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent(epService, "<myroot><a.b><c.d>value</c.d></a.b></myroot>");
        EventBean theEvent = updateListener.assertOneGetNewAndReset();
        assertEquals("value", theEvent.get("val"));
    }
}
