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

import static org.junit.Assert.assertSame;

public class ExecEventXMLSchemaPropertyDynamicDOMGetter implements RegressionExecution {
    protected final static String CLASSLOADER_SCHEMA_URI = "regression/simpleSchema.xsd";
    protected final static String SCHEMA_XML = "<simpleEvent xmlns=\"samples:schemas:simpleSchema\" \n" +
            "  xmlns:ss=\"samples:schemas:simpleSchema\" \n" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
            "  xsi:schemaLocation=\"samples:schemas:simpleSchema simpleSchema.xsd\">" +
            "<type>abc</type>\n" +
            "<dyn>1</dyn>\n" +
            "<dyn>2</dyn>\n" +
            "<nested>\n" +
            "<nes2>3</nes2>\n" +
            "</nested>\n" +
            "<map id='a'>4</map>\n" +
            "</simpleEvent>";

    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("simpleEvent");
        String schemaUri = ExecEventXMLSchemaInvalid.class.getClassLoader().getResource(CLASSLOADER_SCHEMA_URI).toString();
        desc.setSchemaResource(schemaUri);
        desc.setXPathPropertyExpr(false);
        desc.setEventSenderValidatesRoot(false);
        desc.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        desc.setDefaultNamespace("samples:schemas:simpleSchema");
        configuration.addEventType("MyEvent", desc);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String stmtText = "select type?,dyn[1]?,nested.nes2?,map('a')? from MyEvent";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
                new EventPropertyDescriptor("type?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("dyn[1]?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("nested.nes2?", Node.class, null, false, false, false, false, false),
                new EventPropertyDescriptor("map('a')?", Node.class, null, false, false, false, false, false),
        }, stmt.getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(stmt.getEventType());

        EventSender sender = epService.getEPRuntime().getEventSender("MyEvent");
        Document root = SupportXML.sendEvent(sender, SCHEMA_XML);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertSame(root.getDocumentElement().getChildNodes().item(0), theEvent.get("type?"));
        assertSame(root.getDocumentElement().getChildNodes().item(4), theEvent.get("dyn[1]?"));
        assertSame(root.getDocumentElement().getChildNodes().item(6).getChildNodes().item(1), theEvent.get("nested.nes2?"));
        assertSame(root.getDocumentElement().getChildNodes().item(8), theEvent.get("map('a')?"));
        SupportEventTypeAssertionUtil.assertConsistency(theEvent);
    }
}
