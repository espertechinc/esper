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
import org.w3c.dom.Node;

import javax.xml.xpath.XPathConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecEventXMLSchemaWithAll implements RegressionExecution {
    public static final String CLASSLOADER_SCHEMA_WITH_ALL_URI = "regression/simpleSchemaWithAll.xsd";

    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("event-page-visit");
        String schemaUri = ExecEventXMLSchemaWithAll.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_WITH_ALL_URI).toString();
        eventTypeMeta.setSchemaResource(schemaUri);
        eventTypeMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchemaWithAll");
        eventTypeMeta.addXPathProperty("url", "/ss:event-page-visit/ss:url", XPathConstants.STRING);
        configuration.addEventType("PageVisitEvent", eventTypeMeta);
    }

    public void run(EPServiceProvider epService) throws Exception {
        // url='page4'
        String text = "select a.url as sesja from pattern [ every a=PageVisitEvent(url='page1') ]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener updateListener = new SupportUpdateListener();
        stmt.addListener(updateListener);

        SupportXML.sendEvent(epService.getEPRuntime(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<event-page-visit xmlns=\"samples:schemas:simpleSchemaWithAll\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"samples:schemas:simpleSchemaWithAll simpleSchemaWithAll.xsd\">\n" +
                        "<url>page1</url>" +
                        "</event-page-visit>");
        EventBean theEvent = updateListener.getLastNewData()[0];
        assertEquals("page1", theEvent.get("sesja"));
        updateListener.reset();

        SupportXML.sendEvent(epService.getEPRuntime(),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<event-page-visit xmlns=\"samples:schemas:simpleSchemaWithAll\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"samples:schemas:simpleSchemaWithAll simpleSchemaWithAll.xsd\">\n" +
                        "<url>page2</url>" +
                        "</event-page-visit>");
        assertFalse(updateListener.isInvoked());

        EventType type = epService.getEPAdministrator().createEPL("select * from PageVisitEvent").getEventType();
        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("sessionId", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("customerId", Node.class, null, false, false, false, false, true),
            new EventPropertyDescriptor("url", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("method", Node.class, null, false, false, false, false, true),
        }, type.getPropertyDescriptors());
    }
}
