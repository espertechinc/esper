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
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.util.SupportXML.sendXMLEvent;
import static org.junit.Assert.assertEquals;

public class EventXMLNoSchemaNamespaceXPathAbsolute {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaNamespaceXPathAbsolutePreconfig());
        execs.add(new EventXMLNoSchemaNamespaceXPathAbsoluteCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaNamespaceXPathAbsolutePreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "StockQuote", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaNamespaceXPathAbsoluteCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='getQuote', defaultNamespace='http://services.samples/xsd', rootElementNamespace='http://services.samples/xsd'," +
                "  xpathResolvePropertiesAbsolute=true, xpathPropertyExpr=true)" +
                "@XMLSchemaNamespacePrefix(prefix='m0', namespace='http://services.samples/xsd')" +
                "@XMLSchemaField(name='symbol_a', xpath='//m0:symbol', type='string')" +
                "@XMLSchemaField(name='symbol_b', xpath='//*[local-name(.) = \"getQuote\" and namespace-uri(.) = \"http://services.samples/xsd\"]', type='string')" +
                "@XMLSchemaField(name='symbol_c', xpath='/m0:getQuote/m0:request/m0:symbol', type='string')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        String epl = "@name('s0') select symbol_a, symbol_b, symbol_c, request.symbol as symbol_d, symbol as symbol_e from " + eventTypeName;
        env.compileDeploy(epl, path).addListener("s0");

        String xml = "<m0:getQuote xmlns:m0=\"http://services.samples/xsd\"><m0:request><m0:symbol>IBM</m0:symbol></m0:request></m0:getQuote>";
        sendXMLEvent(env, xml, eventTypeName);

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

        EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
        assertEquals("IBM", theEvent.get("symbol_a"));
        assertEquals("IBM", theEvent.get("symbol_b"));
        assertEquals("IBM", theEvent.get("symbol_c"));
        assertEquals("IBM", theEvent.get("symbol_d"));
        assertEquals("", theEvent.get("symbol_e"));    // should be empty string as we are doing absolute XPath

        env.undeployAll();
    }
}
