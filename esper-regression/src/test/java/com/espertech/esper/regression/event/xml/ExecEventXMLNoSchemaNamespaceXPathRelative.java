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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class ExecEventXMLNoSchemaNamespaceXPathRelative implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.setRootElementName("getQuote");
        desc.setDefaultNamespace("http://services.samples/xsd");
        desc.setRootElementNamespace("http://services.samples/xsd");
        desc.addNamespacePrefix("m0", "http://services.samples/xsd");
        desc.setXPathResolvePropertiesAbsolute(false);
        desc.setXPathPropertyExpr(true);
        configuration.addEventType("StockQuote", desc);
    }

    public void run(EPServiceProvider epService) throws Exception {

        String stmt = "select request.symbol as symbol_a, symbol as symbol_b from StockQuote";
        EPStatement joinView = epService.getEPAdministrator().createEPL(stmt);
        SupportUpdateListener listener = new SupportUpdateListener();
        joinView.addListener(listener);

        String xml = "<m0:getQuote xmlns:m0=\"http://services.samples/xsd\"><m0:request><m0:symbol>IBM</m0:symbol></m0:request></m0:getQuote>";
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document doc = builderFactory.newDocumentBuilder().parse(source);

        epService.getEPRuntime().sendEvent(doc);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("IBM", theEvent.get("symbol_a"));
        assertEquals("IBM", theEvent.get("symbol_b"));
    }
}
