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
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.xml.SupportXPathFunctionResolver;
import com.espertech.esper.supportregression.xml.SupportXPathVariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecEventXMLNoSchemaSimpleXMLXPathProperties implements RegressionExecution {
    protected static final String XML_NOSCHEMAEVENT =
            "<myevent>\n" +
                    "  <element1>VAL1</element1>\n" +
                    "  <element2>\n" +
                    "    <element21 id=\"e21_1\">VAL21-1</element21>\n" +
                    "    <element21 id=\"e21_2\">VAL21-2</element21>\n" +
                    "  </element2>\n" +
                    "  <element3 attrString=\"VAL3\" attrNum=\"5\" attrBool=\"true\"/>\n" +
                    "  <element4><element41>VAL4-1</element41></element4>\n" +
                    "</myevent>";

    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        xmlDOMEventTypeDesc.addXPathProperty("xpathElement1", "/myevent/element1", XPathConstants.STRING);
        xmlDOMEventTypeDesc.addXPathProperty("xpathCountE21", "count(/myevent/element2/element21)", XPathConstants.NUMBER);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrString", "/myevent/element3/@attrString", XPathConstants.STRING);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrNum", "/myevent/element3/@attrNum", XPathConstants.NUMBER);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrBool", "/myevent/element3/@attrBool", XPathConstants.BOOLEAN);
        xmlDOMEventTypeDesc.addXPathProperty("stringCastLong", "/myevent/element3/@attrNum", XPathConstants.STRING, "long");
        xmlDOMEventTypeDesc.addXPathProperty("stringCastDouble", "/myevent/element3/@attrNum", XPathConstants.STRING, "double");
        xmlDOMEventTypeDesc.addXPathProperty("numCastInt", "/myevent/element3/@attrNum", XPathConstants.NUMBER, "int");
        xmlDOMEventTypeDesc.setXPathFunctionResolver(SupportXPathFunctionResolver.class.getName());
        xmlDOMEventTypeDesc.setXPathVariableResolver(SupportXPathVariableResolver.class.getName());
        configuration.addEventType("TestXMLNoSchemaType", xmlDOMEventTypeDesc);

        xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("my.event2");
        configuration.addEventType("TestXMLWithDots", xmlDOMEventTypeDesc);
    }

    public void run(EPServiceProvider epService) throws Exception {
        SupportUpdateListener updateListener = new SupportUpdateListener();

        // assert type metadata
        EventTypeSPI type = (EventTypeSPI) ((EPServiceProviderSPI) epService).getEventAdapterService().getExistsTypeByName("TestXMLNoSchemaType");
        assertEquals(EventTypeMetadata.ApplicationType.XML, type.getMetadata().getOptionalApplicationType());
        assertEquals(null, type.getMetadata().getOptionalSecondaryNames());
        assertEquals("TestXMLNoSchemaType", type.getMetadata().getPrimaryName());
        assertEquals("TestXMLNoSchemaType", type.getMetadata().getPublicName());
        assertEquals("TestXMLNoSchemaType", type.getName());
        assertEquals(EventTypeMetadata.TypeClass.APPLICATION, type.getMetadata().getTypeClass());
        assertEquals(true, type.getMetadata().isApplicationConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfigured());
        assertEquals(true, type.getMetadata().isApplicationPreConfiguredStatic());

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("xpathElement1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("xpathCountE21", Double.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("xpathAttrString", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("xpathAttrNum", Double.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("xpathAttrBool", Boolean.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("stringCastLong", Long.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("stringCastDouble", Double.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("numCastInt", Integer.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        String stmt =
                "select xpathElement1, xpathCountE21, xpathAttrString, xpathAttrNum, xpathAttrBool," +
                        "stringCastLong," +
                        "stringCastDouble," +
                        "numCastInt " +
                        "from TestXMLNoSchemaType#length(100)";

        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        joinView.addListener(updateListener);

        // Generate document with the specified in element1 to confirm we have independent events
        sendEvent(epService, "EventA");
        assertDataSimpleXPath(updateListener, "EventA");

        sendEvent(epService, "EventB");
        assertDataSimpleXPath(updateListener, "EventB");
    }

    protected static void assertDataSimpleXPath(SupportUpdateListener updateListener, String element1) {
        assertNotNull(updateListener.getLastNewData());
        EventBean theEvent = updateListener.getLastNewData()[0];

        assertEquals(element1, theEvent.get("xpathElement1"));
        assertEquals(2.0, theEvent.get("xpathCountE21"));
        assertEquals("VAL3", theEvent.get("xpathAttrString"));
        assertEquals(5d, theEvent.get("xpathAttrNum"));
        assertEquals(true, theEvent.get("xpathAttrBool"));
        assertEquals(5L, theEvent.get("stringCastLong"));
        assertEquals(5d, theEvent.get("stringCastDouble"));
        assertEquals(5, theEvent.get("numCastInt"));
    }

    protected static void sendEvent(EPServiceProvider epService, String value) throws Exception {
        String xml = XML_NOSCHEMAEVENT.replaceAll("VAL1", value);
        log.debug(".sendEvent value=" + value);

        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);

        epService.getEPRuntime().sendEvent(simpleDoc);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecEventXMLNoSchemaSimpleXMLXPathProperties.class);
}
