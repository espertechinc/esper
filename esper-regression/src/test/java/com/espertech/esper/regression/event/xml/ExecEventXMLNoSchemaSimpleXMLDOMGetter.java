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

import static com.espertech.esper.regression.event.xml.ExecEventXMLNoSchemaSimpleXMLXPathProperties.sendEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecEventXMLNoSchemaSimpleXMLDOMGetter implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        xmlDOMEventTypeDesc.setXPathPropertyExpr(false);    // <== DOM getter
        configuration.addEventType("TestXMLNoSchemaType", xmlDOMEventTypeDesc);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmt =
                "select element1, invalidelement, " +
                        "element4.element41 as nestedElement," +
                        "element2.element21('e21_2') as mappedElement," +
                        "element2.element21[1] as indexedElement," +
                        "element3.myattribute as invalidattribute " +
                        "from TestXMLNoSchemaType#length(100)";

        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        joinView.addListener(updateListener);

        // Generate document with the specified in element1 to confirm we have independent events
        sendEvent(epService, "EventA");
        assertDataGetter(updateListener, "EventA", false);

        sendEvent(epService, "EventB");
        assertDataGetter(updateListener, "EventB", false);
    }

    protected static void assertDataGetter(SupportUpdateListener updateListener, String element1, boolean isInvalidReturnsEmptyString) {
        assertNotNull(updateListener.getLastNewData());
        EventBean theEvent = updateListener.getLastNewData()[0];

        assertEquals(element1, theEvent.get("element1"));
        assertEquals("VAL4-1", theEvent.get("nestedElement"));
        assertEquals("VAL21-2", theEvent.get("mappedElement"));
        assertEquals("VAL21-2", theEvent.get("indexedElement"));

        if (isInvalidReturnsEmptyString) {
            assertEquals("", theEvent.get("invalidelement"));
            assertEquals("", theEvent.get("invalidattribute"));
        } else {
            assertEquals(null, theEvent.get("invalidelement"));
            assertEquals(null, theEvent.get("invalidattribute"));
        }
    }
}



