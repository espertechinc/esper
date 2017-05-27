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
import javax.xml.xpath.XPathConstants;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class ExecEventXMLNoSchemaNamespaceXPathAbsolute implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.addXPathProperty("symbol_a", "//m0:symbol", XPathConstants.STRING);
        desc.addXPathProperty("symbol_b", "//*[local-name(.) = 'getQuote' and namespace-uri(.) = 'http://services.samples/xsd']", XPathConstants.STRING);
        desc.addXPathProperty("symbol_c", "/m0:getQuote/m0:request/m0:symbol", XPathConstants.STRING);
        desc.setRootElementName("getQuote");
        desc.setDefaultNamespace("http://services.samples/xsd");
        desc.setRootElementNamespace("http://services.samples/xsd");
        desc.addNamespacePrefix("m0", "http://services.samples/xsd");
        desc.setXPathResolvePropertiesAbsolute(true);
        desc.setXPathPropertyExpr(true);
        configuration.addEventType("StockQuote", desc);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String epl = "select symbol_a, symbol_b, symbol_c, request.symbol as symbol_d, symbol as symbol_e from StockQuote";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String xml = "<m0:getQuote xmlns:m0=\"http://services.samples/xsd\"><m0:request><m0:symbol>IBM</m0:symbol></m0:request></m0:getQuote>";
        //String xml = "<getQuote><request><symbol>IBM</symbol></request></getQuote>";
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document doc = builderFactory.newDocumentBuilder().parse(source);

        // For XPath resolution testing and namespaces...
        /*
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        XPathNamespaceContext ctx = new XPathNamespaceContext();
        ctx.addPrefix("m0", "http://services.samples/xsd");
        xPath.setNamespaceContext(ctx);
        XPathExpression expression = xPath.compile("/m0:getQuote/m0:request/m0:symbol");
        xPath.setNamespaceContext(ctx);
        System.out.println("result=" + expression.evaluate(doc,XPathConstants.STRING));
        */

        epService.getEPRuntime().sendEvent(doc);
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals("IBM", theEvent.get("symbol_a"));
        assertEquals("IBM", theEvent.get("symbol_b"));
        assertEquals("IBM", theEvent.get("symbol_c"));
        assertEquals("IBM", theEvent.get("symbol_d"));
        assertEquals("", theEvent.get("symbol_e"));    // should be empty string as we are doing absolute XPath
    }
}
