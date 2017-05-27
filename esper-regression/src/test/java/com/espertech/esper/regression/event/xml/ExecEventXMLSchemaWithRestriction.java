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
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.FileUtil;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecEventXMLSchemaWithRestriction implements RegressionExecution {
    public static final String CLASSLOADER_SCHEMA_WITH_RESTRICTION_URI = "regression/simpleSchemaWithRestriction.xsd";

    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("order");
        InputStream schemaStream = ExecEventXMLSchemaWithRestriction.class.getClassLoader().getResourceAsStream(CLASSLOADER_SCHEMA_WITH_RESTRICTION_URI);
        assertNotNull(schemaStream);
        String schemaText = FileUtil.linesToText(FileUtil.readFile(schemaStream));
        eventTypeMeta.setSchemaText(schemaText);
        configuration.addEventType("OrderEvent", eventTypeMeta);
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportUpdateListener updateListener = new SupportUpdateListener();

        String text = "select order_amount from OrderEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        stmt.addListener(updateListener);

        SupportXML.sendEvent(epService.getEPRuntime(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<order>\n" +
                        "<order_amount>202.1</order_amount>" +
                        "</order>");
        EventBean theEvent = updateListener.getLastNewData()[0];
        assertEquals(Double.class, theEvent.get("order_amount").getClass());
        assertEquals(202.1d, theEvent.get("order_amount"));
        updateListener.reset();
    }
}
